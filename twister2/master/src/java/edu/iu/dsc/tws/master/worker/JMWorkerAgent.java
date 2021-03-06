//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package edu.iu.dsc.tws.master.worker;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.net.tcp.Progress;
import edu.iu.dsc.tws.common.net.tcp.StatusCode;
import edu.iu.dsc.tws.common.net.tcp.request.BlockingSendException;
import edu.iu.dsc.tws.common.net.tcp.request.ConnectHandler;
import edu.iu.dsc.tws.common.net.tcp.request.MessageHandler;
import edu.iu.dsc.tws.common.net.tcp.request.RRClient;
import edu.iu.dsc.tws.common.net.tcp.request.RequestID;
import edu.iu.dsc.tws.common.resource.WorkerInfoUtils;
import edu.iu.dsc.tws.common.worker.JobListener;
import edu.iu.dsc.tws.master.JobMasterContext;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI.WorkerInfo;

/**
 * JMWorkerAgent class
 * It is started for each Twister2 worker
 * It handles the communication with the Job Master
 * <p>
 * It provides:
 * worker discovery
 * barrier method
 * Ping service
 * <p>
 * It can be started in two different modes:
 * Threaded and Blocking
 * <p>
 * If the user calls:
 * startThreaded()
 * It starts as a Thread and the call to this method returns
 * <p>
 * If the user calls:
 * startBlocking()
 * It uses the calling thread and this call does not return unless the close method is called
 */

public final class JMWorkerAgent {
  private static final Logger LOG = Logger.getLogger(JMWorkerAgent.class.getName());

  private static Progress looper;
  private boolean stopLooper = false;

  private Config config;
  private WorkerInfo thisWorker;

  private String masterAddress;
  private int masterPort;

  private RRClient rrClient;
  private Pinger pinger;

  private JMWorkerController workerController;
  private JMWorkerMessenger workerMessenger;

  private boolean registrationSucceeded;

  private int numberOfWorkers;

  /**
   * to control the connection error when we repeatedly try connecting
   */
  private boolean connectionRefused = false;

  /**
   * workers can register a jobListener to receive messages from the driver and the job master
   */
  private JobListener jobListener;

  /**
   * if messages arrive before JobListener added,
   * buffer those messages in this list and deliver them when a JobListener added
   */
  private LinkedList<Message> messageBuffer = new LinkedList<>();

  private static JMWorkerAgent workerAgent;

  /**
   * the maximum duration this client will try to connect to the Job Master
   * in milli seconds
   */
  private static final long CONNECTION_TRY_TIME_LIMIT = 100000;

  /**
   * Singleton JMWorkerAgent
   */
  private JMWorkerAgent(Config config,
                        WorkerInfo thisWorker,
                        String masterHost,
                        int masterPort,
                        int numberOfWorkers) {
    this.config = config;
    this.thisWorker = thisWorker;
    this.masterAddress = masterHost;
    this.masterPort = masterPort;
    this.numberOfWorkers = numberOfWorkers;
  }

  /**
   * create the singleton JMWorkerAgent
   * if it is already created, return the previous one.
   */
  public static JMWorkerAgent createJMWorkerAgent(Config config,
                                                  WorkerInfo thisWorker,
                                                  String masterHost,
                                                  int masterPort,
                                                  int numberOfWorkers) {
    if (workerAgent != null) {
      return workerAgent;
    }

    workerAgent = new JMWorkerAgent(config, thisWorker, masterHost, masterPort, numberOfWorkers);
    return workerAgent;
  }

  /**
   * return the singleton agent object
   */
  public static JMWorkerAgent getJMWorkerAgent() {
    return workerAgent;
  }

  /**
   * initialize JMWorkerAgent
   * wait until it connects to JobMaster
   * return false, if it can not connect to JobMaster
   */
  private void init() {

    looper = new Progress();

    ClientConnectHandler connectHandler = new ClientConnectHandler();

    // if job master assigns worker IDs, first assign workerID as WORKER_UNASSIGNED_ID
    if (JobMasterContext.jobMasterAssignsWorkerIDs(config)) {
      rrClient = new RRClient(masterAddress, masterPort, null, looper,
          RRClient.WORKER_UNASSIGNED_ID, connectHandler);
    } else {
      rrClient = new RRClient(masterAddress, masterPort, null, looper,
          thisWorker.getWorkerID(), connectHandler);
    }

    long interval = JobMasterContext.pingInterval(config);
    pinger = new Pinger(thisWorker.getWorkerID(), rrClient, interval);

    workerMessenger = new JMWorkerMessenger(this);

    // protocol buffer message registrations
    JobMasterAPI.Ping.Builder pingBuilder = JobMasterAPI.Ping.newBuilder();
    rrClient.registerResponseHandler(pingBuilder, pinger);

    JobMasterAPI.RegisterWorker.Builder registerWorkerBuilder =
        JobMasterAPI.RegisterWorker.newBuilder();
    JobMasterAPI.RegisterWorkerResponse.Builder registerWorkerResponseBuilder
        = JobMasterAPI.RegisterWorkerResponse.newBuilder();

    JobMasterAPI.WorkerStateChange.Builder stateChangeBuilder =
        JobMasterAPI.WorkerStateChange.newBuilder();
    JobMasterAPI.WorkerStateChangeResponse.Builder stateChangeResponseBuilder
        = JobMasterAPI.WorkerStateChangeResponse.newBuilder();

    JobMasterAPI.WorkerMessage.Builder workerMessageBuilder =
        JobMasterAPI.WorkerMessage.newBuilder();
    JobMasterAPI.WorkerMessageResponse.Builder workerResponseBuilder
        = JobMasterAPI.WorkerMessageResponse.newBuilder();

    JobMasterAPI.WorkersScaled.Builder scaledMessageBuilder =
        JobMasterAPI.WorkersScaled.newBuilder();

    JobMasterAPI.DriverMessage.Builder driverMessageBuilder =
        JobMasterAPI.DriverMessage.newBuilder();

    JobMasterAPI.WorkersJoined.Builder joinedBuilder = JobMasterAPI.WorkersJoined.newBuilder();

    ResponseMessageHandler responseMessageHandler = new ResponseMessageHandler();
    rrClient.registerResponseHandler(registerWorkerBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(registerWorkerResponseBuilder, responseMessageHandler);

    rrClient.registerResponseHandler(stateChangeBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(stateChangeResponseBuilder, responseMessageHandler);

    rrClient.registerResponseHandler(workerMessageBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(workerResponseBuilder, responseMessageHandler);

    rrClient.registerResponseHandler(scaledMessageBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(driverMessageBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(joinedBuilder, responseMessageHandler);

    // try to connect to JobMaster
    tryUntilConnected(CONNECTION_TRY_TIME_LIMIT);

    if (!rrClient.isConnected()) {
      throw new RuntimeException("JMWorkerAgent can not connect to Job Master. Exiting .....");
    }
  }

  /**
   * this will be initialized after worker registration
   * since WorkerInfo may change when job master assigns workerIDs
   */
  private void initJMWorkerController() {

    workerController = new JMWorkerController(config, thisWorker, rrClient, numberOfWorkers);

    JobMasterAPI.ListWorkersRequest.Builder listRequestBuilder =
        JobMasterAPI.ListWorkersRequest.newBuilder();
    JobMasterAPI.ListWorkersResponse.Builder listResponseBuilder =
        JobMasterAPI.ListWorkersResponse.newBuilder();
    rrClient.registerResponseHandler(listRequestBuilder, workerController);
    rrClient.registerResponseHandler(listResponseBuilder, workerController);

    JobMasterAPI.BarrierRequest.Builder barrierRequestBuilder =
        JobMasterAPI.BarrierRequest.newBuilder();
    JobMasterAPI.BarrierResponse.Builder barrierResponseBuilder =
        JobMasterAPI.BarrierResponse.newBuilder();
    rrClient.registerResponseHandler(barrierRequestBuilder, workerController);
    rrClient.registerResponseHandler(barrierResponseBuilder, workerController);
  }

  /**
   * start the client to listen for messages
   */
  private void startLooping() {

    while (!stopLooper) {
      long timeToNextPing = pinger.timeToNextPing();
      if (timeToNextPing < 30 && registrationSucceeded) {
        pinger.sendPingMessage();
      } else {
        looper.loopBlocking(timeToNextPing);
      }
    }

    rrClient.disconnect();
  }

  /**
   * start the Job Master Client in a Thread
   */
  public Thread startThreaded() {
    // first initialize the client, connect to Job Master
    init();

    Thread jmThread = new Thread() {
      public void run() {
        startLooping();
      }
    };

    jmThread.start();

    boolean registered = registerWorker();
    if (!registered) {
      throw new RuntimeException("Could not register JobMaster with Dashboard. Exiting .....");
    }

    return jmThread;
  }

  /**
   * start the Job Master Client in a blocking call
   */
  public void startBlocking() {
    // first initialize the client, connect to Job Master
    init();

    startLooping();

    boolean registered = registerWorker();
    if (!registered) {
      throw new RuntimeException("Could not register JobMaster with Dashboard. Exiting .....");
    }
  }

  /**
   * try connecting until the time limit is reached
   */
  public boolean tryUntilConnected(long timeLimit) {
    long startTime = System.currentTimeMillis();
    long duration = 0;
    long sleepInterval = 50;

    // log interval in milliseconds
    long logInterval = 1000;
    long nextLogTime = logInterval;

    // allow the first connection attempt
    connectionRefused = true;

    while (duration < timeLimit) {
      // try connecting
      if (connectionRefused) {
        rrClient.tryConnecting();
        connectionRefused = false;
      }

      // loop to connect
      looper.loop();

      if (rrClient.isConnected()) {
        return true;
      }

      try {
        Thread.sleep(sleepInterval);
      } catch (InterruptedException e) {
        LOG.warning("Sleep interrupted.");
      }

      if (rrClient.isConnected()) {
        return true;
      }

      duration = System.currentTimeMillis() - startTime;

      if (duration > nextLogTime) {
        LOG.info("Still trying to connect to the Job Master: " + masterAddress + ":" + masterPort);
        nextLogTime += logInterval;
      }
    }

    return false;
  }

  /**
   * return WorkerInfo for this worker
   */
  public WorkerInfo getWorkerInfo() {
    return thisWorker;
  }

  /**
   * return JMWorkerController for this worker
   */
  public JMWorkerController getJMWorkerController() {
    return workerController;
  }

  /**
   * return JMWorkerMessenger for this worker
   */
  public JMWorkerMessenger getJMWorkerMessenger() {
    return workerMessenger;
  }

  /**
   * only one JobListener can be added
   * if the second JobListener tried to be added, false returned
   */
  public static boolean addJobListener(JobListener jobListener) {
    if (workerAgent.jobListener != null) {
      return false;
    }

    workerAgent.jobListener = jobListener;

    // deliver buffered messages if any
    workerAgent.deliverBufferedMessages();

    return true;
  }

  /**
   * send RegisterWorker message to Job Master
   * put WorkerInfo in this message
   */
  private boolean registerWorker() {

    JobMasterAPI.RegisterWorker registerWorker = JobMasterAPI.RegisterWorker.newBuilder()
        .setWorkerID(thisWorker.getWorkerID())
        .setWorkerInfo(thisWorker)
        .build();

    LOG.fine("Sending RegisterWorker message: \n" + registerWorker);

    // wait for the response
    try {
      rrClient.sendRequestWaitResponse(registerWorker,
          JobMasterContext.responseWaitDuration(config));

      if (registrationSucceeded) {
        pinger.sendPingMessage();
        initJMWorkerController();
      }

      return registrationSucceeded;

    } catch (BlockingSendException bse) {
      LOG.log(Level.SEVERE, bse.getMessage(), bse);
      return false;
    }
  }

  public boolean sendWorkerRunningMessage() {

    JobMasterAPI.WorkerStateChange workerStateChange = JobMasterAPI.WorkerStateChange.newBuilder()
        .setWorkerID(thisWorker.getWorkerID())
        .setState(JobMasterAPI.WorkerState.RUNNING)
        .build();

    RequestID requestID = rrClient.sendRequest(workerStateChange);
    if (requestID == null) {
      LOG.severe("Could not send Worker RUNNING message.");
      return false;
    }

    LOG.fine("Sent Worker RUNNING message: \n" + workerStateChange);
    return true;
  }

  public boolean sendWorkerCompletedMessage() {

    JobMasterAPI.WorkerStateChange workerStateChange = JobMasterAPI.WorkerStateChange.newBuilder()
        .setWorkerID(thisWorker.getWorkerID())
        .setState(JobMasterAPI.WorkerState.COMPLETED)
        .build();

    LOG.fine("Sending Worker COMPLETED message: \n" + workerStateChange);
    try {
      rrClient.sendRequestWaitResponse(workerStateChange,
          JobMasterContext.responseWaitDuration(config));
    } catch (BlockingSendException e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
      return false;
    }

    return true;
  }

  public boolean sendWorkerToDriverMessage(Message message) {

    JobMasterAPI.WorkerMessage workerMessage = JobMasterAPI.WorkerMessage.newBuilder()
        .setData(Any.pack(message).toByteString())
        .setWorkerID(thisWorker.getWorkerID())
        .build();

    RequestID requestID = rrClient.sendRequest(workerMessage);
    if (requestID == null) {
      LOG.severe("Could not send WorkerToDriver message.");
      return false;
    }

    LOG.fine("Sent WorkerToDriver message: \n" + workerMessage);
    return true;
  }


  /**
   * stop the JMWorkerAgent
   */
  public void close() {
    stopLooper = true;
    looper.wakeup();
  }

  /**
   * deliver all buffered messages to the JobListener
   * in the order they are received
   */
  private void deliverBufferedMessages() {

    while (!messageBuffer.isEmpty()) {
      deliverMessageToJobListener(messageBuffer.poll());
    }

  }

  /**
   * deliver the received message to JobListener
   */
  private void deliverMessageToJobListener(Message message) {

    if (message instanceof JobMasterAPI.WorkersScaled) {

      JobMasterAPI.WorkersScaled scaledMessage = (JobMasterAPI.WorkersScaled) message;
      if (scaledMessage.getChange() > 0) {
        jobListener.workersScaledUp(scaledMessage.getChange());
      } else if (scaledMessage.getChange() < 0) {
        jobListener.workersScaledDown(0 - scaledMessage.getChange());
      }

    } else if (message instanceof JobMasterAPI.DriverMessage) {

      JobMasterAPI.DriverMessage driverMessage =
          (JobMasterAPI.DriverMessage) message;
      try {
        Any any = Any.parseFrom(driverMessage.getData());
        jobListener.driverMessageReceived(any);
      } catch (InvalidProtocolBufferException e) {
        LOG.log(Level.SEVERE, "Can not parse received protocol buffer message to Any", e);
      }

    } else if (message instanceof JobMasterAPI.WorkersJoined) {

      JobMasterAPI.WorkersJoined joinedMessage = (JobMasterAPI.WorkersJoined) message;
      jobListener.allWorkersJoined(joinedMessage.getWorkerList());

    }
  }

  class ResponseMessageHandler implements MessageHandler {

    @Override
    public void onMessage(RequestID id, int workerId, Message message) {

      if (message instanceof JobMasterAPI.RegisterWorkerResponse) {

        LOG.fine("Received a RegisterWorkerResponse message from the master. \n" + message);

        JobMasterAPI.RegisterWorkerResponse responseMessage =
            (JobMasterAPI.RegisterWorkerResponse) message;

        registrationSucceeded = responseMessage.getResult();

        if (JobMasterContext.jobMasterAssignsWorkerIDs(config)) {
          thisWorker = WorkerInfoUtils.updateWorkerID(thisWorker, responseMessage.getWorkerID());
          pinger.setWorkerID(responseMessage.getWorkerID());
          rrClient.setWorkerID(responseMessage.getWorkerID());
        }

      } else if (message instanceof JobMasterAPI.WorkerStateChangeResponse) {
        LOG.fine("Received a WorkerStateChange response from the master. \n" + message);

        // nothing to do
      } else if (message instanceof JobMasterAPI.WorkerMessageResponse) {
        LOG.info("Received a WorkerMessageResponse from the master. \n" + message);

      } else if (message instanceof JobMasterAPI.DriverMessage
          || message instanceof JobMasterAPI.WorkersJoined
          || message instanceof JobMasterAPI.WorkersScaled) {

        LOG.fine("Received " + message.getClass().getSimpleName()
            + " message from the master. \n" + message);

        if (jobListener == null) {
          messageBuffer.add(message);
        } else {
          deliverMessageToJobListener(message);
        }

        if (message instanceof JobMasterAPI.WorkersScaled) {
          JobMasterAPI.WorkersScaled scaledMessage = (JobMasterAPI.WorkersScaled) message;
          workerController.scaled(scaledMessage.getChange(), scaledMessage.getNumberOfWorkers());
        }

      } else {
        LOG.warning("Received message unrecognized. \n" + message);
      }

    }
  }

  public class ClientConnectHandler implements ConnectHandler {
    @Override
    public void onError(SocketChannel channel) {

    }

    @Override
    public void onConnect(SocketChannel channel, StatusCode status) {
      if (status == StatusCode.SUCCESS) {
        LOG.info(thisWorker.getWorkerID() + " JMWorkerAgent connected to JobMaster: " + channel);
      }

      if (status == StatusCode.CONNECTION_REFUSED) {
        connectionRefused = true;
      }
    }

    @Override
    public void onClose(SocketChannel channel) {

    }
  }

}
