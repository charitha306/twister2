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
package edu.iu.dsc.tws.comms.api.batch;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import edu.iu.dsc.tws.comms.api.BulkReceiver;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.DestinationSelector;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.DataFlowMultiGather;
import edu.iu.dsc.tws.comms.dfw.io.Tuple;
import edu.iu.dsc.tws.comms.dfw.io.gather.GatherMultiBatchFinalReceiver;
import edu.iu.dsc.tws.comms.dfw.io.gather.GatherMultiBatchPartialReceiver;

public class BKeyedGather {
  private DataFlowMultiGather keyedGather;

  private DestinationSelector destinationSelector;

  private MessageType keyType;

  private MessageType dataType;

  public BKeyedGather(Communicator comm, TaskPlan plan,
                      Set<Integer> sources, Set<Integer> destinations,
                      MessageType kType, MessageType dType,
                      BulkReceiver rcvr, DestinationSelector destSelector, boolean shuffle) {
    this.keyType = kType;
    this.dataType = dType;
    Set<Integer> edges = new HashSet<>();
    for (int i = 0; i < destinations.size(); i++) {
      edges.add(comm.nextEdge());
    }
    this.keyedGather = new DataFlowMultiGather(comm.getChannel(), sources, destinations,
        new GatherMultiBatchFinalReceiver(rcvr, shuffle, false, comm.getPersistentDirectory(),
            null), new GatherMultiBatchPartialReceiver(),
        edges, kType, dType);
    this.keyedGather.init(comm.getConfig(), dType, plan);
    this.destinationSelector = destSelector;
    this.destinationSelector.prepare(comm, sources, destinations);
  }

  public BKeyedGather(Communicator comm, TaskPlan plan,
                      Set<Integer> sources, Set<Integer> destinations,
                      MessageType kType, MessageType dType, BulkReceiver rcvr,
                      Comparator<Object> comparator, DestinationSelector destSelector,
                      boolean shuffle) {
    this.keyType = kType;
    this.dataType = dType;
    Set<Integer> edges = new HashSet<>();
    for (int i = 0; i < destinations.size(); i++) {
      edges.add(comm.nextEdge());
    }
    this.keyedGather = new DataFlowMultiGather(comm.getChannel(), sources, destinations,
        new GatherMultiBatchFinalReceiver(rcvr, shuffle, true, comm.getPersistentDirectory(),
            comparator), new GatherMultiBatchPartialReceiver(), edges,
        kType, dType);
    this.keyedGather.init(comm.getConfig(), dType, plan);
    this.destinationSelector = destSelector;
    this.destinationSelector.prepare(comm, sources, destinations);
  }

  public boolean gather(int source, Object key, Object data, int flags) {
    int dest = destinationSelector.next(source, key, data);
    return keyedGather.send(source, new Tuple(key, data, keyType,
        dataType), flags, dest);
  }

  public boolean hasPending() {
    return !keyedGather.isComplete();
  }

  public void finish(int source) {
    keyedGather.finish(source);
  }

  public boolean progress() {
    return keyedGather.progress();
  }

  public void close() {
    // deregister from the channel
    keyedGather.close();
  }
}
