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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.openWith;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicReplyBoxView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.ReplyBoxMessages;

/**
 * This class is the view builder for the reply box that exists at the end of a
 * root thread.
 */
public final class ReplyBoxViewBuilder
    implements UiBuilder, IntrinsicReplyBoxView {
  
  public interface Resources extends ClientBundle {
    @Source("ReplyBox.css")
    Css css();
  }

  public interface Css extends CssResource {
    /** The main reply box container. */
    String replyBox();
  }

  /** A unique id for this builder. */
  private final String id;

  /** The CSS resources for this class. */
  private final Css css;

  /** The message constants for this class. */
  private final ReplyBoxMessages messages;

  //
  // Intrinsic state.
  //

  /** Specifies weather the reply box should be rendered as enabled or not. **/
  private boolean enabled = true;

  /**
   * Creates a new reply box view builder with the given id.
   *
   * @param id unique id for this builder, it must only contains alphanumeric
   *        characters
   */
  public static ReplyBoxViewBuilder create(String id) {
    return new ReplyBoxViewBuilder(WavePanelResourceLoader.getReplyBox().css(),
        WavePanelResourceLoader.getReplyBoxMessages(), id);
  }

  @VisibleForTesting
  ReplyBoxViewBuilder(Css css, ReplyBoxMessages messages, String id) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    this.css = css;
    this.messages = messages;
    this.id = id;    
  }

  //
  // DomImpl nature.
  //

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    openWith(output, id, css.replyBox(), TypeCodes.kind(Type.REPLY_BOX),
        enabled ? "" : "style='display:none'");
    {
      output.appendEscaped(messages.clickHereToAdd());
    }
    close(output);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void enable() {
    setEnabled(true);
  }

  @Override
  public void disable() {
    setEnabled(false);
  }

  private void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}