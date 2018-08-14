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
package edu.iu.dsc.tws.executor.core.batch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.executor.api.DefaultOutputCollection;
import edu.iu.dsc.tws.executor.api.INodeInstance;
import edu.iu.dsc.tws.executor.api.INodeInstanceListener;
import edu.iu.dsc.tws.executor.api.IParallelOperation;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.ISource;
import edu.iu.dsc.tws.task.api.OutputCollection;
import edu.iu.dsc.tws.task.api.TaskContext;

public class SourceBatchInstance implements INodeInstance, INodeInstanceListener {

  /**
   * The actual task executing
   */
  private ISource batchTask;

  /**
   * Output will go throuh a single queue
   */
  private BlockingQueue<IMessage> outBatchQueue;

  /**
   * The configuration
   */
  private Config config;

  /**
   * The output collection to be used
   */
  private OutputCollection outputBatchCollection;

  /**
   * Parallel operations
   */
  private Map<String, IParallelOperation> outBatchParOps = new HashMap<>();

  /**
   * The globally unique task id
   */
  private int batchTaskId;

  /**
   * Task index that goes from 0 to parallism - 1
   */
  private int batchTaskIndex;

  /**
   * Number of parallel tasks
   */
  private int parallelism;

  /**
   * Name of the task
   */
  private String batchTaskName;

  /**
   * Node configurations
   */
  private Map<String, Object> nodeConfigs;

  /**
   * Worker id
   */
  private int workerId;

  /**
   * For batch tasks: identifying the final message
   **/

  private boolean isDone;

  private boolean isFinalTask;

  private int callBackCount = 0;

  private int count = 0;

  private boolean isFinish = false;

  public SourceBatchInstance(ISource task, BlockingQueue<IMessage> outQueue,
                             Config config, String tName, int tId, int tIndex, int parallel,
                             int wId, Map<String, Object> cfgs) {
    this.batchTask = task;
    this.outBatchQueue = outQueue;
    this.config = config;
    this.batchTaskId = tId;
    this.batchTaskIndex = tIndex;
    this.parallelism = parallel;
    this.batchTaskName = tName;
    this.nodeConfigs = cfgs;
    this.workerId = wId;
  }

  public SourceBatchInstance(ISource task, BlockingQueue<IMessage> outQueue, Config config,
                             String tName, int tId, int tIndex, int parallel, int wId,
                             Map<String, Object> cfgs, boolean isDone) {
    this.batchTask = task;
    this.outBatchQueue = outQueue;
    this.config = config;
    this.batchTaskId = tId;
    this.batchTaskIndex = tIndex;
    this.parallelism = parallel;
    this.batchTaskName = tName;
    this.nodeConfigs = cfgs;
    this.workerId = wId;
    this.isDone = isDone;
  }

  public void prepare() {
    outputBatchCollection = new DefaultOutputCollection(outBatchQueue);

    batchTask.prepare(config, new TaskContext(batchTaskIndex, batchTaskId, batchTaskName,
        parallelism, workerId, outputBatchCollection, nodeConfigs));
  }

  @Override
  public void interrupt() {

  }

  /**
   * Execution Method calls the SourceTasks run method to get context
   **/
  public boolean execute() {

    if (batchTask != null) {
      batchTask.run();
      // now check the output queue
      while (!outBatchQueue.isEmpty()) {

        IMessage message = outBatchQueue.poll();
        if (message != null) {
          String edge = message.edge();
          IParallelOperation op = outBatchParOps.get(edge);
          if (message.getContent().equals(MessageFlags.LAST_MESSAGE)) {
            while (!op.send(batchTaskId, message, MessageFlags.FLAGS_LAST)) {
              //
            }
            //op.communicationProgress();
            this.isFinish = true;
          } else {
            while (!op.send(batchTaskId, message, 0)) {
              //
            }
            //op.communicationProgress();
          }
        }
      }
    }

    return this.isFinish;
  }

  //TODO : BOOLEAN RESPONSE AFTER FINISHING COMMUNICATION
  public boolean communicationProgress() {
    boolean allDone = true;
    for (Map.Entry<String, IParallelOperation> e : outBatchParOps.entrySet()) {
      if (e.getValue().progress()) {
        allDone = false;
      }
    }
    return allDone;
  }


  public BlockingQueue<IMessage> getOutQueue() {
    return outBatchQueue;
  }

  public void registerOutParallelOperation(String edge, IParallelOperation op) {
    outBatchParOps.put(edge, op);
  }

  public boolean isFinish() {
    return isFinish;
  }

  @Override
  public boolean OnDone() {
    return isDone;
  }

  @Override
  public boolean onStart() {
    return false;
  }

  @Override
  public boolean onStop() {
    return false;
  }


  public int getBatchTaskId() {
    return batchTaskId;
  }

  public int getBatchTaskIndex() {
    return batchTaskIndex;
  }

  public int getParallelism() {
    return parallelism;
  }

  public String getBatchTaskName() {
    return batchTaskName;
  }

  public int getWorkerId() {
    return workerId;
  }
}