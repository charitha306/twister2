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
package edu.iu.dsc.tws.examples.batch.kmeans;

import java.io.BufferedReader;
import java.io.IOException;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.data.utils.HdfsUtils;

/**
 * This class is responsible for reading the input from the HDFS. It internally uses the HDFS Utils
 * class to set the configuration values required for accessing the HDFS. Then, it will create the
 * hadoop file system object to access the HDFS.
 */
public class KMeansHDFSFileReader {

  private final Config config;

  public KMeansHDFSFileReader(Config cfg) {
    this.config = cfg;
  }

  /**
   * It reads the datapoints from the corresponding file and store the data in a two-dimensional
   * array for the later processing.
   */
  public double[][] readDataPoints(String fName, int dimension, String filesystem) {

    HdfsUtils hdfsUtils = new HdfsUtils(config, fName);
    int lengthOfFile = hdfsUtils.getLengthOfFile(fName);
    double[][] dataPoints = new double[lengthOfFile][dimension];
    BufferedReader bufferedReader = KMeansUtils.getBufferedReader(config, fName, filesystem);
    try {
      int value = 0;
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] data = line.split(",");
        for (int i = 0; i < dimension; i++) {
          dataPoints[value][i] = Double.parseDouble(data[i].trim());
        }
        value++;
      }
    } catch (IOException e) {
      KMeansUtils.readClose();
      throw new RuntimeException("Error while reading file", e);
    } finally {
      KMeansUtils.readClose();
    }
    return dataPoints;
  }

  /**
   * It reads the centroids from the corresponding file and store the data in a two-dimensional
   * array for the later processing. The size of the two-dimensional array should be equal to the
   * number of clusters and the dimension considered for the clustering process.
   */
  public double[][] readCentroids(String fName, int dimension, int numberOfClusters,
                                  String filesys) {

    double[][] centroids = new double[numberOfClusters][dimension];
    BufferedReader bufferedReader = KMeansUtils.getBufferedReader(config, fName, filesys);
    try {
      int value = 0;
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] data = line.split(",");
        for (int i = 0; i < dimension - 1; i++) {
          centroids[value][i] = Double.parseDouble(data[i].trim());
          centroids[value][i + 1] = Double.parseDouble(data[i + 1].trim());
        }
        value++;
      }
      bufferedReader.close();
    } catch (IOException ioe) {
      KMeansUtils.readClose();
      throw new RuntimeException("Error while reading centroids", ioe);
    } finally {
      KMeansUtils.readClose();
    }
    return centroids;
  }
}


