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
package edu.iu.dsc.tws.rsched.spi.resource;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent a resource
 */
public class ResourceContainer {
  private int id;
  // include properties of the resource
  // this can include things like available ports
  private Map<String, Object> properties = new HashMap<>();

  // no of cpus in this container
  private int noOfCpus;

  // memory available to the container
  private int memoryMegaBytes;

  public ResourceContainer(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public void addProperty(String key, Object property) {
    properties.put(key, property);
  }

  public Object getProperty(String key) {
    return properties.get(key);
  }
}