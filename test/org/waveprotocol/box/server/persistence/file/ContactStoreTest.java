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

package org.waveprotocol.box.server.persistence.file;

import java.io.File;
import org.waveprotocol.box.server.persistence.ContactStore;
import org.waveprotocol.box.server.persistence.ContactStoreTestBase;
import org.waveprotocol.box.server.persistence.PersistenceException;

/**
 * Unittest for the {@link ContactStore}.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class ContactStoreTest extends ContactStoreTestBase {
  private File path;

  @Override
  protected void setUp() throws Exception {
    path = FileUtils.createTemporaryDirectory();
    super.setUp();
  }

  @Override
  protected ContactStore newContactStore() throws PersistenceException {
    ContactStore store = new FileContactStore(path.getAbsolutePath());
    store.initializeContactStore();
    return store;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    org.apache.commons.io.FileUtils.deleteDirectory(path);
  }
}
