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
package edu.iu.dsc.tws.examples.internal.task.checkpoint.barrier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.JobConfig;
import edu.iu.dsc.tws.api.Twister2Submitter;
import edu.iu.dsc.tws.api.job.Twister2Job;
import edu.iu.dsc.tws.api.net.Network;
import edu.iu.dsc.tws.checkpointmanager.barrier.CheckpointBarrier;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.controller.IWorkerController;
import edu.iu.dsc.tws.common.exceptions.TimeoutException;
import edu.iu.dsc.tws.common.worker.IPersistentVolume;
import edu.iu.dsc.tws.common.worker.IVolatileVolume;
import edu.iu.dsc.tws.common.worker.IWorker;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.TWSChannel;
import edu.iu.dsc.tws.executor.api.ExecutionPlan;
import edu.iu.dsc.tws.executor.core.ExecutionPlanBuilder;
import edu.iu.dsc.tws.executor.threading.Executor;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.rsched.core.ResourceAllocator;
import edu.iu.dsc.tws.rsched.core.SchedulerContext;
import edu.iu.dsc.tws.task.api.BaseSink;
import edu.iu.dsc.tws.task.api.BaseSource;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.Operations;
import edu.iu.dsc.tws.task.graph.DataFlowTaskGraph;
import edu.iu.dsc.tws.task.graph.GraphBuilder;
import edu.iu.dsc.tws.task.graph.OperationMode;
import edu.iu.dsc.tws.tsched.spi.scheduler.Worker;
import edu.iu.dsc.tws.tsched.spi.scheduler.WorkerPlan;
import edu.iu.dsc.tws.tsched.spi.taskschedule.TaskSchedulePlan;
import edu.iu.dsc.tws.tsched.streaming.roundrobin.RoundRobinTaskScheduler;

public class TaskBarrierExample implements IWorker {
  private static final Logger LOG = Logger.getLogger(TaskBarrierExample.class.getName());

  @Override
  public void execute(Config config, int workerID,
                      IWorkerController workerController,
                      IPersistentVolume persistentVolume,
                      IVolatileVolume volatileVolume) {
    GeneratorBarrierTask g = new GeneratorBarrierTask();
    RecevingBarrierTask r = new RecevingBarrierTask();

    GraphBuilder builder = GraphBuilder.newBuilder();
    builder.addSource("source", g);
    builder.setParallelism("source", 1);
    builder.addSink("sink", r);
    builder.setParallelism("sink", 4);
    builder.connect("source", "sink", "partition-edge", Operations.PARTITION);

    DataFlowTaskGraph graph = builder.build();

    RoundRobinTaskScheduler roundRobinTaskScheduling = new RoundRobinTaskScheduler();
    roundRobinTaskScheduling.initialize(config);

    List<JobMasterAPI.WorkerInfo> workerList = null;
    try {
      workerList = workerController.getAllWorkers();
    } catch (TimeoutException timeoutException) {
      LOG.log(Level.SEVERE, timeoutException.getMessage(), timeoutException);
      return;
    }
    WorkerPlan workerPlan = createWorkerPlan(workerList);
    TaskSchedulePlan taskSchedulePlan = roundRobinTaskScheduling.schedule(graph, workerPlan);

    TWSChannel network = Network.initializeChannel(config, workerController);
    ExecutionPlanBuilder executionPlanBuilder = new ExecutionPlanBuilder(workerID,
          workerList, new Communicator(config, network));
    ExecutionPlan plan = executionPlanBuilder.build(config, graph, taskSchedulePlan);
    Executor executor = new Executor(config, workerID, plan, network,
        OperationMode.STREAMING);
    executor.execute();

  }

  private static class GeneratorBarrierTask extends BaseSource {
    private static final long serialVersionUID = -254264903510284748L;
    private long id = 5555;

    @Override
    public void execute() {
      CheckpointBarrier cb = new CheckpointBarrier(id, 2141535, null);
      context.write("partition-edge", cb);
      id++;
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        System.out.print("Sleep failed");
      }
    }
  }

  private static final class RecevingBarrierTask extends BaseSink {
    private static final long serialVersionUID = -254264903510284798L;
    private int count = 0;

    @Override
    public boolean execute(IMessage message) {
      CheckpointBarrier barrier = (CheckpointBarrier) message.getContent();
      if (barrier != null) {
        System.out.println("retrieved the check point barrier with Id :" + barrier.getId());
      }
      count++;
      return true;
    }
  }

  public WorkerPlan createWorkerPlan(List<JobMasterAPI.WorkerInfo> workerInfoList) {
    List<Worker> workers = new ArrayList<>();
    for (JobMasterAPI.WorkerInfo workerInfo: workerInfoList) {
      Worker w = new Worker(workerInfo.getWorkerID());
      workers.add(w);
    }

    return new WorkerPlan(workers);
  }

  public static void main(String[] args) {
    // first load the configurations from command line and config files
    Config config = ResourceAllocator.loadConfig(new HashMap<>());

    // build JobConfig
    HashMap<String, Object> configurations = new HashMap<>();
    configurations.put(SchedulerContext.THREADS_PER_WORKER, 8);

    // build JobConfig
    JobConfig jobConfig = new JobConfig();
    jobConfig.putAll(configurations);

    Twister2Job.Twister2JobBuilder jobBuilder = Twister2Job.newBuilder();
    jobBuilder.setJobName("task-barrier-example");
    jobBuilder.setWorkerClass(TaskBarrierExample.class.getName());
    jobBuilder.addComputeResource(1, 512, 4);
    jobBuilder.setConfig(jobConfig);

    // now submit the job
    Twister2Submitter.submitJob(jobBuilder.build(), config);
  }
}
