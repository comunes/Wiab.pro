/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.box.webclient.contact;

/**
 * Implementation of {@link FetchContactsService}.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class FetchContactsServiceImpl implements FetchContactsService {

  private static final FetchContactsBuilder DEFAULT_BUILDER = FetchContactsBuilder.create();
  private final FetchContactsBuilder builder;

  FetchContactsServiceImpl(FetchContactsBuilder builder) {
    this.builder = builder;
  }

  public static FetchContactsServiceImpl create() {
    return new FetchContactsServiceImpl(DEFAULT_BUILDER);
  }

  @Override
  public void fetch(long timestamp, Callback callback) {
    builder.newFetchContactsRequest().setTimestamp(timestamp).fetchContacts(callback);
  }
}
