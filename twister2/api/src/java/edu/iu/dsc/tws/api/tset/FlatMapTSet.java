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

import java.util.logging.Logger;

import edu.iu.dsc.tws.api.task.ComputeConnection;
import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.api.tset.ops.FlatMapOp;
import edu.iu.dsc.tws.common.config.Config;

/**
 * Apply a flat map operation
 *
 * @param <T> the input type
 * @param <P> the output type
 */
public class FlatMapTSet<T, P> extends BaseTSet<T> {
  private static final Logger LOG = Logger.getLogger(FlatMapTSet.class.getName());

  private BaseTSet<P> parent;

  private FlatMapFunction<P, T> mapFn;

  public FlatMapTSet(Config cfg, TaskGraphBuilder bldr,
                     BaseTSet<P> parent, FlatMapFunction<P, T> mapFunc) {
    super(cfg, bldr);
    this.parent = parent;
    this.mapFn = mapFunc;
  }

  @SuppressWarnings("unchecked")
  public boolean baseBuild() {
    boolean isIterable = isIterableInput(parent, builder.getMode());
    boolean keyed = isKeyedInput(parent);

    int p = calculateParallelism(parent);
    String newName = generateName("flat-map", parent);

    ComputeConnection connection = builder.addCompute(newName,
        new FlatMapOp<>(mapFn, isIterable, keyed), p);
    parent.buildConnection(connection);
    return true;
  }

  @Override
  void buildConnection(ComputeConnection connection) {
  }
}
