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
package edu.iu.dsc.tws.api.cdfw;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import com.google.protobuf.Any;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.driver.IDriver;
import edu.iu.dsc.tws.common.driver.IDriverMessenger;
import edu.iu.dsc.tws.common.driver.IScaler;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;

public abstract class BaseDriver implements IDriver {
  private static final Logger LOG = Logger.getLogger(BaseDriver.class.getName());

  /**
   * Worker list
   */
  private volatile List<JobMasterAPI.WorkerInfo> workerInfoList;

  /**
   * The worker executor
   */
  private CDFWExecutor executor;

  /**
   * The queue to coordinate between driver and workers
   */
  private BlockingQueue<DriverEvent> inMessages = new LinkedBlockingDeque<>();

  @Override
  public void execute(Config config, IScaler scaler, IDriverMessenger messenger) {
    try {
      waitForEvent(DriveEventType.INITIALIZE);
    } catch (Exception e) {
      throw new RuntimeException("Failed to wait for event");
    }

    executor = new CDFWExecutor(config, messenger);
    executor.addWorkerList(workerInfoList);

    // call the execute method of the implementation
    execute(config, executor);

    // now lets close
    executor.close();
  }

  public abstract void execute(Config config, CDFWExecutor exec);

  @Override
  public void workerMessageReceived(Any anyMessage, int senderWorkerID) {
    executor.workerMessageReceived(anyMessage, senderWorkerID);
  }

  @Override
  public void allWorkersJoined(List<JobMasterAPI.WorkerInfo> workerList) {
    // Added to get the worker info list for testing
    if (workerList != null) {
      this.workerInfoList = workerList;
    }

    inMessages.offer(new DriverEvent(DriveEventType.INITIALIZE, null));
  }

  private DriverEvent waitForEvent(DriveEventType type) throws Exception {
    // lets wait for driver events
    try {
      DriverEvent event = inMessages.take();
      if (event.getType() != type) {
        throw new Exception("Un-expected event: " + type);
      }
      return event;
    } catch (InterruptedException e) {
      throw new RuntimeException("Failed to take event", e);
    }
  }
}
