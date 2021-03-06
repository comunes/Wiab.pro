/**
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
 */

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import org.waveprotocol.box.webclient.client.WebClient;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.common.util.KeyComboContext;
import org.waveprotocol.wave.client.common.util.KeyComboManager;
import org.waveprotocol.wave.client.common.util.KeyComboTask;
import org.waveprotocol.wave.client.uibuilder.BuilderHelper.Component;
import org.waveprotocol.wave.client.uibuilder.HtmlClosureCollection;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.appendSpan;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.appendSpanWith;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.closeSpan;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.open;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.openSpan;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.openSpanWith;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.TagMessages;

/**
 * UiBuilder for a collection of tags.
 *
 */
public final class TagsViewBuilder
    implements UiBuilder {

  /** Resources used by this widget. */
  public interface Resources
      extends ClientBundle {

    /** CSS */
    @Source("Tags.css")
    Css css();

    @Source("expand_button.png")
    ImageResource expandButton();

    @Source("collapse_button.png")
    ImageResource collapseButton();

    @Source("small_add_button.png")
    ImageResource addButton();

    @Source("small_add_button_pressed.png")
    ImageResource addButtonPressed();

    @Source("delete_button.png")
    ImageResource deleteButton();
  }

  /** CSS for this widget. */
  public interface Css
      extends CssResource {

    String panel();
    String flow();
    String tag();
    String normal();
    String added();
    String removed();
    String title();
    String toggleGroup();
    String simple();
    String extra();
    String expandButton();
    String collapseButton();
    String addButton();
    String addButtonPressed();
    String deleteButton();
  }

  /** An enum for all the components of a tags view. */
  public enum Components
      implements Component {

    /** Element to which tag UIs are attached. */
    CONTAINER("C"),
    /** Add button. */
    ADD("A");
    private final String postfix;

    Components(String postfix) {
      this.postfix = postfix;
    }

    @Override
    public String getDomId(String baseId) {
      return baseId + postfix;
    }
  }

  private final static TagMessages messages = GWT.create(TagMessages.class);

  /**
   * Creates a new TagsViewBuilder.
   *
   * @param id attribute-HTML-safe encoding of the view's HTML id
   */
  public static TagsViewBuilder create(
      String id, HtmlClosureCollection tagUis) {
    return new TagsViewBuilder(
        WavePanelResourceLoader.getTags().css(), id, tagUis);
  }

  private final Css css;
  private final HtmlClosureCollection tagUis;
  private final String id;

  @VisibleForTesting
  private TagsViewBuilder(Css css, String id, HtmlClosureCollection tagUis) {
    this.css = css;
    this.id = id;
    this.tagUis = tagUis;
  }

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    String addTagHotKey = KeyComboManager.getKeyComboHintByTask(
        KeyComboContext.WAVE, KeyComboTask.ADD_TAG);
    open(output, id, css.panel(), TypeCodes.kind(Type.TAGS));
    {
      open(output, null, css.flow(), null);
      {
        open(output, Components.CONTAINER.getDomId(id), null, null);
        {
          // Append title
          open(output, null, css.title(), null);
          output.appendPlainText(messages.tags());
          close(output);

          tagUis.outputHtml(output);

          // Overflow-mode panel.
          openSpan(output, null, css.extra(), null);
          {
            openSpanWith(output, null, css.toggleGroup(), null,
                "onclick=\"" + onClickJs() + "\"");
            {
              appendSpan(output, null, css.expandButton(), null);
              openSpan(output, null, null, null);
              {
                output.appendPlainText(messages.more());
              }
              closeSpan(output);
            }
            closeSpan(output);
            appendSpanWith(output, null, css.addButton(), TypeCodes.kind(Type.ADD_TAG), null,
                messages.addTagHint(), addTagHotKey);
          }
          closeSpan(output);

          // Single-line mode panel.
          openSpan(output, null, css.simple(), null);
          {
            appendSpanWith(output, WebClient.ADD_TAG_BUTTON_ID, css.addButton(),
                TypeCodes.kind(Type.ADD_TAG), null, messages.addTagHint(), addTagHotKey);
          }
          closeSpan(output);
        }
        close(output);
      }
      close(output);
    }
    close(output);
  }

  // Rather than install a regular handler, this is an experiment at injecting
  // JS directly, so that this piece of UI is functional from the initial
  // rendering, without needing to wait for any scripts to load (like the GWT
  // app).
  /** @return a JS click handler for toggling expanded and collapsed modes. */
  private String onClickJs() {
    StringBuilder sb = new StringBuilder();
    sb.append("var p=document.getElementById('").append(id).append("');")
        .append("var x=p.getAttribute('s')=='e';")
        .append("p.style.height=x?'':'auto';")
        .append("p.setAttribute('s',x?'':'e');")
        .append("lastChild.innerHTML=x?'").append(messages.more())
        .append("':'").append(messages.less()).append("';")
        .append("firstChild.className=x?'").append(css.expandButton())
        .append("':'").append(css.collapseButton()).append("';")
        .append("parentNode.nextSibling.style.display=x?'':'none';");
    String js = sb.toString();
    // The constructed string has no double-quote characters in it, so it can be
    // double-quoted verbatim.
    assert !js.contains("\"");
    return js;
  }
}
