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

package org.waveprotocol.box.server.rpc;

import com.google.protobuf.RpcController;

import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Extends RpcController to include additional state wave in a box requires.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public interface ServerRpcController extends RpcController, Runnable {
  /**
   * Get the currently logged in user
   *
   * @return the currently logged in user, or null.
   */
  ParticipantId getLoggedInUser();

  /**
   * Get the connection id
   *
   * @return the connection id.
   */
  String getConnectionId();

  /**
   * Mark this controller as cancelled, i.e., as the result of a client request.
   * Call the pending cancellation callback, if there is one.
   */
  void cancel();
}
