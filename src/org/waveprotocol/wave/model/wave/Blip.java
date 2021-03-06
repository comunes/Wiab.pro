/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.model.wave;

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;

import java.util.Set;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * A mutable view of a blip.
 *
 * NOTE(user):  All implementations of this interface must implement {@link Object#hashCode()}
 *   and {@link Object#equals(Object)} based on {@link #getId()}.
 *
 */
public interface Blip {

  /**
   * Submits this blip.
   */
  void submit();

  /**
   * Checks is content exists.
   */
  boolean hasContent();

  /**
   * Checks is content in raw format.
   */
  boolean isContentInitialized();
  
  /**
   * Initializes snapshot.
   */
  void initializeSnapshot();

  /**
   * Processes diff-operations.
   */
  void processDiffs() throws OperationException;

  /**
   * Gets content of blip.
   */
  DocumentOperationSink getContent();

  /**
   * Adapts the primitive-blip's document-operation sink as a {@link MutableDocument}.
   */
  Document getDocument();

  /**
   * Gets the owner wavelet.
   */
  Wavelet getWavelet();

  /**
   * Gets the participant if of the author of the blip.
   */
  ParticipantId getAuthorId();

  /**
   * Gets the set of contributors to the blip (this may include the author).
   */
  Set<ParticipantId> getContributorIds();

  /**
   * The creation time of blip.
   */
  long getCreationTime();

  /**
   * The creation version of blip.
   */
  long getCreationVersion();

  /**
   * The last time this blip was modified.
   */
  long getLastModifiedTime();

  /**
   * The last wavelet version this blip was modified.
   */
  long getLastModifiedVersion();

  /**
   * Gets an identifier of the blip.  This is unique across all blips both
   * within a wavelet and across wavelets.
   *
   * @return  of the blip
   */
  String getId();
}
