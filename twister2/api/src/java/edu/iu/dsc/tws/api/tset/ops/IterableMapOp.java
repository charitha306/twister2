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
package edu.iu.dsc.tws.api.tset.ops;

import java.util.ArrayList;
import java.util.Iterator;

import edu.iu.dsc.tws.api.tset.Constants;
import edu.iu.dsc.tws.api.tset.IterableMapFunction;
import edu.iu.dsc.tws.api.tset.TSetContext;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.task.api.ICompute;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.TaskContext;

public class IterableMapOp<T, R> implements ICompute {
  private static final long serialVersionUID = -1220168533L;

  private IterableMapFunction<T, R> mapFn;

  private TaskContext context;

  private boolean inputIterator;

  private boolean keyed;

  public IterableMapOp() {
  }

  public IterableMapOp(IterableMapFunction<T, R> mapFn, boolean inputItr, boolean kyd) {
    this.mapFn = mapFn;
    this.inputIterator = inputItr;
    this.keyed = kyd;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean execute(IMessage content) {
    Iterable<T> data;
    if (inputIterator) {
      data = new TSetIterable<>((Iterator<T>) content.getContent());
    } else {
      ArrayList<T> itr = new ArrayList<>();
      itr.add((T) content.getContent());
      data = new TSetIterable<>(itr.iterator());
    }

    R result = mapFn.map(data);
    return context.write(Constants.DEFAULT_EDGE, result);
  }

  @Override
  public void prepare(Config cfg, TaskContext ctx) {
    this.context = ctx;
    TSetContext tSetContext = new TSetContext(cfg, ctx.taskIndex(), ctx.taskId(), ctx.taskName(),
        ctx.getParallelism(), ctx.getWorkerId(), ctx.getConfigurations());

    mapFn.prepare(tSetContext);
  }
}
