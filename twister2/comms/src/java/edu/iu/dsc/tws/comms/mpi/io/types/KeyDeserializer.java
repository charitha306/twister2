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
package edu.iu.dsc.tws.comms.mpi.io.types;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.mpi.MPIBuffer;
import edu.iu.dsc.tws.comms.utils.KryoSerializer;

public final class KeyDeserializer {
  private static final Logger LOG = Logger.getLogger(KeyDeserializer.class.getName());

  private KeyDeserializer() {
  }

  /**
   * Desetrialize
   * @param keyType
   * @param buffers
   * @param serializer
   * @return
   */
  public static Pair<Object, Integer> deserializeKey(MessageType keyType,
                                                     List<MPIBuffer> buffers,
                                                     KryoSerializer serializer) {
    int currentIndex = 0;
    int keyLength = 0;
    Object key = null;
    // first we need to read the key type
    switch (keyType) {
      case INTEGER:
        currentIndex = getReadIndex(buffers, currentIndex, 4);
        ByteBuffer byteBuffer = buffers.get(currentIndex).getByteBuffer();
        LOG.info(String.format("Key deserialize position %d", byteBuffer.position()));
        key = byteBuffer.getInt();
        keyLength = 4;
        break;
      case SHORT:
        currentIndex = getReadIndex(buffers, currentIndex, 2);
        key = buffers.get(currentIndex).getByteBuffer().getShort();
        keyLength = 2;
        break;
      case LONG:
        currentIndex = getReadIndex(buffers, currentIndex, 8);
        key = buffers.get(currentIndex).getByteBuffer().getInt();
        keyLength = 8;
        break;
      case DOUBLE:
        currentIndex = getReadIndex(buffers, currentIndex, 8);
        key = buffers.get(currentIndex).getByteBuffer().getInt();
        keyLength = 8;
        break;
      case OBJECT:
        currentIndex = getReadIndex(buffers, currentIndex, 4);
        keyLength = buffers.get(currentIndex).getByteBuffer().getInt();
        key = DataDeserializer.deserializeObject(buffers, keyLength, serializer);
        break;
      case BYTE:
        currentIndex = getReadIndex(buffers, currentIndex, 4);
        keyLength = buffers.get(currentIndex).getByteBuffer().getInt();
        key = readBytes(buffers, keyLength);
        break;
      case STRING:
        currentIndex = getReadIndex(buffers, currentIndex, 4);
        keyLength = buffers.get(currentIndex).getByteBuffer().getInt();
        key = new String(readBytes(buffers, keyLength));
        break;
      default:
        break;
    }
    return new ImmutablePair<>(key, keyLength);
  }

  private static byte[] readBytes(List<MPIBuffer> buffers, int length) {
    byte[] bytes = new byte[length];
    int currentRead = 0;
    int index = 0;
    while (currentRead < length) {
      ByteBuffer byteBuffer = buffers.get(index).getByteBuffer();
      int remaining = byteBuffer.remaining();
      int needRead = length - currentRead;
      int canRead =  remaining > needRead ? needRead : remaining;
      byteBuffer.get(bytes, currentRead, canRead);
      currentRead += canRead;
      index++;
      if (index >= buffers.size()) {
        throw new RuntimeException("Error in buffer management");
      }
    }
    return bytes;
  }

  private static int getReadIndex(List<MPIBuffer> buffers, int currentIndex, int expectedSize) {
    for (int i = currentIndex; i < buffers.size(); i++) {
      ByteBuffer byteBuffer = buffers.get(i).getByteBuffer();
      int remaining = byteBuffer.remaining();
      if (remaining > expectedSize) {
        return i;
      }
    }
    throw new RuntimeException("Something is wrong in the buffer management");
  }
}