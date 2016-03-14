/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.waveprotocol.box.server.rpc.render.view;

/**
 * Base interface for all views of wave panel components.
 *
 */
public interface View {

  //
  // The engineering intent of this enum is to encourage new view types to be
  // added only with a full understanding of what it means to add a new view
  // type, including implications for paging, server-side rendering, client-side
  // stitching, etc. Adding new view types is not be done lightly.
  //
  // As the universe of view objects grows, this set should be modularized for
  // ease of maintenance. However, a global set of all kinds still needs exist
  // somewhere, e.g., for disambiguation on even handling.
  //

  /**
   * View grammar:
   *
   * <pre>
   *   Wave ::= Conversation*
   *   Conversation ::= Participants RootThread
   *   Participants ::= Participant* Add
   *   RootThread ::= Blip*
   *   Blip ::= Meta Anchor* Conversation*
   *   Meta ::= MenuItem* DOM Anchor*
   *   Anchor ::= [ InlineThread ]
   *   InlineThread ::= Toggle Blip*
   * </pre>
   */
  enum Type {
    WAVE,
    ROOT_CONVERSATION,
    INLINE_CONVERSATION,
    ROOT_THREAD,
    INLINE_THREAD,
    REPLY_BOX,
    BLIP_CONTINUATION_BAR,
    CONTINUATION_BAR,
    CONTINUATION_BUTTON,
    CONTINUATION_TEXT,
    TOGGLE,
    BLIP,
    META,
    BLIP_MENU_BUTTON,
    BLIP_TIME,
    BLIP_FOCUS_FRAME,
    ANCHOR,
    PARTICIPANT,
    PARTICIPANTS,
    ADD_PARTICIPANT,
    TAG,
    TAGS,
    ADD_TAG
  }
}
