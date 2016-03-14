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

package org.waveprotocol.box.webclient.search.i18n;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.Messages.DefaultMessage;

/**
 *
 * @author akaplanov (Andrew Kaplanov)
 */
public interface SearchPresenterMessages extends Messages {
  @DefaultMessage("New Wave")
  String newWave();

  @DefaultMessage("Create new wave")
  String newWaveHint();  
  
  @DefaultMessage("of {0}")
  String of(int count);

  @DefaultMessage("of unknown")
  String ofUnknown();

  @DefaultMessage("To Inbox")
  String toInbox();

  @DefaultMessage("To Archive")
  String toArchive();

  @DefaultMessage("Modify")
  String modify();

  @DefaultMessage("Searching...")
  String searching();
}
