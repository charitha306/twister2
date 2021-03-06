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
package edu.iu.dsc.tws.examples.verification;


import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.iu.dsc.tws.comms.dfw.io.Tuple;
import edu.iu.dsc.tws.executor.core.OperationNames;
import edu.iu.dsc.tws.task.graph.OperationMode;

public class ExperimentVerification implements IVerification {

  private static final Logger LOG = Logger.getLogger(ExperimentVerification.class.getName());

  private ExperimentData experimentData;
  private String operationNames;

  public ExperimentVerification(ExperimentData experimentData, String operationNames) {
    this.experimentData = experimentData;
    this.operationNames = operationNames;
  }

  @Override
  public boolean isVerified() throws VerificationException {
    boolean isVerified = false;
    if (experimentData.getOperationMode() == OperationMode.STREAMING) {
      if (experimentData.getInput() instanceof int[]
          && experimentData.getOutput() instanceof int[]) {
        if (OperationNames.REDUCE.equals(this.operationNames)) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if (sourceCount < sinkCount && sinkCount == 1) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            Object[] res = Arrays.stream(input)
                .map(i -> i * sourceCount * experimentData.getIterations())
                .boxed()
                .collect(Collectors.toList())
                .toArray();
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outString = Arrays.toString(output);
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outString);
            isVerified = resString.equals(outString);
          }
        }
      }

      if (OperationNames.ALLREDUCE.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if (sourceCount != sinkCount) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            Object[] res = Arrays.stream(input)
                .map(i -> i * sourceCount * experimentData.getIterations())
                .boxed()
                .collect(Collectors.toList())
                .toArray();
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outString = Arrays.toString(output);
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outString);
            isVerified = resString.equals(outString);
          }
        }
      }

      if (OperationNames.GATHER.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount < sinkCount) && (sinkCount != 1)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }

      if (OperationNames.ALLGATHER.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount < sinkCount) && (sinkCount != 1)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }


      if (OperationNames.KEYED_REDUCE.equals(this.operationNames)) {
        Object keyedOutput = null;
        Object response = experimentData.getOutput();
        if (response instanceof Tuple) {
          keyedOutput = (Tuple) response;
        } else {
          keyedOutput = response;
        }
        if (experimentData.getInput() instanceof int[]
            && keyedOutput instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if (sourceCount < sinkCount && sinkCount != 1) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) keyedOutput;
            Object[] res = Arrays.stream(input)
                .map(i -> i * sourceCount * experimentData.getIterations())
                .boxed()
                .collect(Collectors.toList())
                .toArray();
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String resGen = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Results : " + resGen);
            String outString = Arrays.toString(output);
            isVerified = resString.equals(outString);
          }
        }
      }

      if (OperationNames.BROADCAST.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount > sinkCount) && (sinkCount < 2)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            LOG.info("Task Id : " + experimentData.getTaskId());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            String msg = String.format("Expected Result : %s Generated Result : %s",
                resString, outputRes);
            if (isVerified) {
              LOG.info(msg);
            } else {
              LOG.severe(msg);
              throw new VerificationException(msg);
            }
          }
        }
      }

      if (OperationNames.PARTITION.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount > sinkCount) && (sinkCount < 2)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            LOG.info("Task Id : " + experimentData.getTaskId());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            //isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            isVerified = resString.equals(outputRes);
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }

      if (OperationNames.KEYED_PARTITION.equals(this.operationNames)) {
        Object response = experimentData.getOutput();
        Object outputMessage = null;
        if (response instanceof Tuple) {
          Tuple tuple = (Tuple) response;
          outputMessage = tuple.getValue();
        } else {
          outputMessage = response;
        }
        if (experimentData.getInput() instanceof int[]
            && outputMessage instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount > sinkCount) && (sinkCount < 2)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            LOG.info("Task Id : " + experimentData.getTaskId());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) outputMessage;
            int[] res = input;
            //isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            isVerified = resString.equals(outputRes);
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }

    }

    if (experimentData.getOperationMode() == OperationMode.BATCH) {
      if (experimentData.getInput() instanceof int[]
          && experimentData.getOutput() instanceof int[]) {
        if (OperationNames.REDUCE.equals(this.operationNames)) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if (sourceCount < sinkCount && sinkCount == 1) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            Object[] res = Arrays.stream(input)
                .map(i -> i * sourceCount * experimentData.getIterations())
                .boxed()
                .collect(Collectors.toList())
                .toArray();
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outString = Arrays.toString(output);
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outString);
            isVerified = resString.equals(outString);
          }
        }
      }

      if (OperationNames.ALLREDUCE.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if (sourceCount != sinkCount) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            Object[] res = Arrays.stream(input)
                .map(i -> i * sourceCount * experimentData.getIterations())
                .boxed()
                .collect(Collectors.toList())
                .toArray();
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outString = Arrays.toString(output);
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outString);
            isVerified = resString.equals(outString);
          }
        }
      }

      if (OperationNames.GATHER.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount < sinkCount) && (sinkCount != 1)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }

      if (OperationNames.KEYED_GATHER.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount < sinkCount) && (sinkCount != 1)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }

      if (OperationNames.ALLGATHER.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount < sinkCount) && (sinkCount != 1)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }


      if (OperationNames.KEYED_REDUCE.equals(this.operationNames)) {
        Object outputExp = experimentData.getOutput();
        Object keyedOutput = null;
        if (outputExp instanceof Tuple) {
          keyedOutput = ((Tuple) experimentData.getOutput()).getValue();
        } else {
          keyedOutput = outputExp;
        }

        if (experimentData.getInput() instanceof int[]
            && keyedOutput instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if (sourceCount < sinkCount && sinkCount != 1) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) keyedOutput;
            Object[] res = Arrays.stream(input)
                .map(i -> i * sourceCount * experimentData.getIterations())
                .boxed()
                .collect(Collectors.toList())
                .toArray();
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String resGen = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Results : " + resGen);
            String outString = Arrays.toString(output);
            isVerified = resString.equals(outString);
          }
        }
      }

      if (OperationNames.BROADCAST.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount > sinkCount) && (sinkCount < 2)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            LOG.info("Task Id : " + experimentData.getTaskId());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            //isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            isVerified = resString.equals(outputRes);
            String msg = String.format("Expected Result : %s Generated Result : %s",
                resString, outputRes);
            if (isVerified) {
              LOG.info(msg);
            } else {
              LOG.severe("Results Not Verified : " + msg);
              throw new VerificationException(msg);
            }
          }
        }
      }

      if (OperationNames.PARTITION.equals(this.operationNames)) {
        if (experimentData.getInput() instanceof int[]
            && experimentData.getOutput() instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount > sinkCount) && (sinkCount < 2)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            LOG.info("Task Id : " + experimentData.getTaskId());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            //isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            isVerified = resString.equals(outputRes);
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }

      if (OperationNames.KEYED_PARTITION.equals(this.operationNames)) {
        Object response = experimentData.getOutput();
        Object outputMessage = null;
        if (response instanceof Tuple) {
          Tuple tuple = (Tuple) response;
          outputMessage = tuple.getValue();
        } else {
          outputMessage = response;
        }
        if (experimentData.getInput() instanceof int[]
            && outputMessage instanceof int[]) {
          int sourceCount = experimentData.getTaskStages().get(0);
          int sinkCount = experimentData.getTaskStages().get(1);
          if ((sourceCount > sinkCount) && (sinkCount < 2)) {
            throw new VerificationException("Invalid task stages : "
                + sourceCount + "," + sinkCount);
          } else {
            LOG.info("Current Worker : " + experimentData.getWorkerId()
                + "/" + experimentData.getNumOfWorkers());
            LOG.info("Task Id : " + experimentData.getTaskId());
            int[] input = (int[]) experimentData.getInput();
            int[] output = (int[]) experimentData.getOutput();
            int[] res = input;
            //isVerified = Arrays.equals(input, output);
            String resString = Arrays
                .toString(Arrays.copyOfRange(res, 0, res.length));
            String outputRes = Arrays
                .toString(Arrays.copyOfRange(output, 0, res.length));
            isVerified = resString.equals(outputRes);
            LOG.info("Expected Result : " + resString);
            LOG.info("Generated Result : " + outputRes);
          }
        }
      }
    }
    return isVerified;
  }
}
