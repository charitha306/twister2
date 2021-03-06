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
package edu.iu.dsc.tws.executor.comms;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.executor.api.IParallelOperation;
import edu.iu.dsc.tws.executor.core.EdgeGenerator;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.TaskKeySelector;
import edu.iu.dsc.tws.task.api.TaskMessage;

public abstract class AbstractParallelOperation implements IParallelOperation {
  protected Config config;

  protected Communicator channel;

  protected Map<Integer, BlockingQueue<IMessage>> outMessages;

  protected TaskPlan taskPlan;

  protected EdgeGenerator edgeGenerator;

  protected int communicationEdge;

  public AbstractParallelOperation(Config config, Communicator network, TaskPlan tPlan) {
    this.config = config;
    this.taskPlan = tPlan;
    this.channel = network;
    this.outMessages = new HashMap<>();
  }

  @Override
  public void register(int targetTask, BlockingQueue<IMessage> queue) {
    if (outMessages.containsKey(targetTask)) {
      throw new RuntimeException("Existing queue for target task");
    }
    outMessages.put(targetTask, queue);
  }

  @Override
  public boolean progress() {
    return true;
  }

  public static Object extractKey(TaskMessage taskMessage, TaskKeySelector selector) {
    Object key = null;
    if (taskMessage.getKey() == null && selector != null) {
      key = selector.select(taskMessage.getContent());
    } else if (taskMessage.getKey() != null) {
      key = taskMessage.getKey();
    } else if (taskMessage.getKey() == null && selector == null) {
      throw new RuntimeException("Use should specify the key or use a selector");
    }
    return key;
  }
}
