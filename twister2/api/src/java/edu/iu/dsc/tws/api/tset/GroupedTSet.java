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
package edu.iu.dsc.tws.api.tset;

import edu.iu.dsc.tws.api.task.ComputeConnection;
import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.common.config.Config;

public class GroupedTSet<T, K> extends BaseTSet<T> {
  private PartitionFunction<K> partitioner;

  private Selector<T, K> selector;

  private BaseTSet<T> parent;

  public GroupedTSet(Config cfg, TaskGraphBuilder bldr, BaseTSet<T> prnt,
                     PartitionFunction<K> partFn, Selector<T, K> selc) {
    super(cfg, bldr);
    this.partitioner = partFn;
    this.selector = selc;
    this.parent = prnt;
    this.name = "grouped-" + parent.getName();
  }

  public KeyedReduceTSet<T, K> keyedReduce(ReduceFunction<T> reduceFn) {
    KeyedReduceTSet<T, K> reduce = new KeyedReduceTSet<>(config, builder, parent,
        reduceFn, partitioner, selector);
    children.add(reduce);
    return reduce;
  }

  public KeyedPartitionTSet<T, K> keyedPartition() {
    KeyedPartitionTSet<T, K> partition = new KeyedPartitionTSet<>(config, builder,
        parent, partitioner, selector);
    children.add(partition);
    return partition;
  }

  public KeyedGatherTSet<T, K> keyedGather() {
    KeyedGatherTSet<T, K> gather = new KeyedGatherTSet<>(config, builder, parent,
        partitioner, selector);
    children.add(gather);
    return gather;
  }

  public BaseTSet<T> getParent() {
    return parent;
  }

  @Override
  public boolean baseBuild() {
    return true;
  }

  @Override
  void buildConnection(ComputeConnection connection) {
  }
}
