/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.waveprotocol.wave.client.gadget.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.NamedFrame;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.gadget.renderer.i18n.GadgetWidgetUiMessages;
import org.waveprotocol.wave.client.scheduler.ScheduleTimer;
import org.waveprotocol.wave.client.widget.common.HoverHelper;
import org.waveprotocol.wave.client.widget.common.HoverHelper.Hoverable;
import org.waveprotocol.wave.client.widget.generic.ElementPanel;
import org.waveprotocol.wave.client.widget.menu.MenuButton;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;

/**
 * GadgetWidget UI implementation.
 */
public class GadgetWidgetUi extends Composite implements Hoverable {

  /**
   * Class that exposes protected addDomHandler method from NamedFrame as addHandler. Serves as the
   * Gadget IFrame widget class.
   */
  private static class GadgetIFrame extends NamedFrame {

    GadgetIFrame(String name) {
      super(name);
    }

    <H extends EventHandler> HandlerRegistration addHandler(
        final H handler, DomEvent.Type<H> type) {
      return addDomHandler(handler, type);
    }
  }

  /** CSS resource for the style injector. */
  public interface Resources extends ClientBundle {
    /** The singleton instance of our CSS resources. */
    static final Resources RESOURCES = GWT.<Resources> create(Resources.class);

    @Source("Gadget.css")
    public Css css();

    @Source("broken_gadget.png")
    DataResource brokenGadget();

    @Source("loading_gadget.gif")
    ImageResource loadingGadgetLarge();

    @Source("loading_gadget_small.gif")
    ImageResource loadingGadgetSmall();

    @Source("meta_more.png")
    ImageResource moreImage();

    // Normal
    @Source("meta_left.png")
    ImageResource metaLeftImage();

    @Source("meta_right.png")
    ImageResource metaRightImage();

    static final String META_LEFT_WIDTH = "-" + RESOURCES.metaLeftImage().getWidth() + "px";
    static final String META_RIGHT_WIDTH = "-" + RESOURCES.metaRightImage().getWidth() + "px";
    static final String FADE_DELAY_STRING = FADE_DELAY_MS + "ms";

    @Source("meta_mid.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource metaMid();

    // Down
    @Source("meta_left_down.png")
    ImageResource metaLeftDown();

    @Source("meta_right_down.png")
    ImageResource metaRightDown();

    @Source("meta_mid_down.png")
    ImageResource metaMidDown();

    // Hover
    @Source("meta_left_hover.png")
    ImageResource metaLeftHover();

    @Source("meta_right_hover.png")
    ImageResource metaRightHover();

    @Source("meta_mid_hover.png")
    ImageResource metaMidHover();
  }

  /** CSS interface with style string methods. */
  interface Css extends CssResource {
    String panel();
    String inline();
    String title();
    String gadgetFrame();
    String iframeDiv();
    String gadgetIframe();
    String loadedGadgetFrame();
    String loadingGadgetFrame();
    String loadingGadgetSmallThrobber();
    String loadingGadgetLargeThrobber();
    String loadedGadget();
    String brokenGadgetIcon();
    String more();
    String metaButton();
    String metaRight();
    String metaButtonsPanel();
    String metaLeft();
    String metaButtons();
    String disabled();
  }

  /** The singleton instance of our CSS resources. */
  static final Css CSS = Resources.RESOURCES.css();

  /** Static initializer: injects stylesheet. */
  static {
    StyleInjector.inject(CSS.getText());
  }
  
  private static final GadgetWidgetUiMessages messages = GWT.create(GadgetWidgetUiMessages.class);
  
  @UiField
  ElementPanel enclosingBox;
  @UiField
  ElementPanel metaButtons;
  @UiField
  ElementPanel metaLeft;
  @UiField
  ElementPanel metaButtonsPanel;
  @UiField
  ElementPanel metaRight;
  @UiField
  Label titleLabel;
  @UiField
  ElementPanel gadgetFrame;
  @UiField
  ElementPanel iframeDiv;

  interface Binder extends UiBinder<ElementPanel, GadgetWidgetUi> {
  }
  private static final Binder BINDER = GWT.create(Binder.class);
  
  /** The maximum height of the gadget frame that should use the small throbber icon. */
  private static final int MAX_SMALL_GADGET_HEIGHT = 63;
  private static final int FADE_DELAY_MS = 300;
  
  /**
   * Gadget IFrame.
   */
  private final GadgetIFrame gadgetIframe;

  private GadgetUiListener listener = null;

  private MenuButton menu;

  private boolean menuEnabled;

  private final EditingIndicator editingIndicator;

  private static enum ThrobberState {
    SHOWN,
    BROKEN,
    DISABLED
  }
  
  private ThrobberState throbberState = ThrobberState.DISABLED;

  /**
   * Constructs gadget widget UI.
   */
  public GadgetWidgetUi(String gadgetName, EditingIndicator editingIndicator, String width, String height) {
    this.editingIndicator = editingIndicator;
    menuEnabled = false;      
    initWidget(BINDER.createAndBindUi(this));      
    gadgetIframe = new GadgetIFrame(gadgetName);      
    buildIFrame(gadgetName, width, height);
    hideMetaButtons();      
    makeWidgetsUnselectable();      
    HoverHelper.getInstance().setup(this);
  }

  private void makeWidgetsUnselectable() {
    makeUnselectable(enclosingBox);
    makeUnselectable(titleLabel);
    makeUnselectable(gadgetFrame);
    makeUnselectable(gadgetIframe);
    makeUnselectable(metaButtons);
    makeUnselectable(metaLeft);
    makeUnselectable(metaButtonsPanel);
    makeUnselectable(metaRight);
    if (menu != null) {
      makeUnselectable(menu.getButton());
    }  
  }

  private void makeUnselectable(Widget widget) {
    DomHelper.setContentEditable(widget.getElement(), false, false);
    DomHelper.makeUnselectable(widget.getElement());
  }
  
  private void removeThrobber() {
    gadgetIframe.removeStyleName(CSS.loadingGadgetSmallThrobber());
    gadgetIframe.removeStyleName(CSS.loadingGadgetLargeThrobber());
  }

  private void showThrobber() {
    gadgetIframe.addStyleName(isLarge(getIframeHeight())
        ? CSS.loadingGadgetLargeThrobber() : CSS.loadingGadgetSmallThrobber());
  }

  private void buildIFrame(String gadgetName, String width, String height) {
    gadgetIframe.getElement().setId(gadgetName);

    IFrameElement iframe = getIframeElement();
    iframe.setAttribute("vspace", "0");
    iframe.setAttribute("hspace", "0");
    iframe.setAttribute("frameBorder", "no");
    iframe.setAttribute("moduleId", gadgetName);
    iframe.setAttribute("display", "block");
    // TODO(user): scrolling policy/settings for the wave gadgets.
    iframe.setScrolling("no");

    //remove default style
    gadgetIframe.removeStyleName("gwt-Frame");
    gadgetIframe.addStyleName(CSS.gadgetIframe());

    if (width != null) {
      gadgetFrame.setWidth(width);
    }
    if (height != null) {
      gadgetFrame.setHeight(height);
    }

    iframeDiv.add(gadgetIframe);
  }

  private void buildMenu() {
    // TODO(user): Use menu builder.
    String styleName = CSS.metaButton() + " " + CSS.more();
    String title = messages.gadgetMenu();
    menu = new MenuButton(styleName, title);
    menu.addPopupEventListener(new PopupEventListener.Impl() {

      boolean menuItemsAreBuilt = false;
      
      @Override
      public void onBeforeShow(PopupEventSourcer source) {
        if (!menuItemsAreBuilt) {
          buildMenuItems();
          menuItemsAreBuilt = true;
        }  
      }
    });
    
    metaButtonsPanel.add(menu.getButton());
  }
  
  private void buildMenuItems() {
    menu.addItem(messages.deleteGadgetMenuItem(), new Command() {

      @Override
      public void execute() {
        if (listener != null) {
          listener.deleteGadget();
        }
      }
    }, true);

    menu.addItem(messages.selectGadgetMenuItem(), new Command() {

      @Override
      public void execute() {
        if (listener != null) {
          listener.selectGadget();
        }
      }
    }, true);

    menu.addItem(messages.resetGadgetMenuItem(), new Command() {

      @Override
      public void execute() {
        if (listener != null) {
          listener.resetGadget();
        }
      }
    }, true);    
  }
  
  /**
   * Task to set display:none to deactivate the menu. Note that opacity 0 leaves invisible, but
   * active element.
   */
  private final ScheduleTimer disableMenuTask = new ScheduleTimer() {
    @Override
    public void run() {
      metaButtons.getElement().getStyle().setDisplay(Display.NONE);
    }
  };

  private void hideMetaButtons() {
    metaButtons.getElement().getStyle().setOpacity(0);
    disableMenuTask.schedule(FADE_DELAY_MS);
  }

  private void showMetaButtons() {
    disableMenuTask.cancel();
    metaButtons.getElement().getStyle().clearDisplay();
    metaButtons.getOffsetWidth(); // Force the browser to render the first frame.
    metaButtons.getElement().getStyle().clearOpacity();
  }

  /**
   * Gets the gadget iframe element.
   *
   * @return Gadget iframe element.
   */
  public IFrameElement getIframeElement() {
    return IFrameElement.as(gadgetIframe.getElement());
  }

  /**
   * Sets the iframe height attribute.
   *
   * @param height New iframe height.
   */
  public void setIframeHeight(String height) {
    boolean wasLarge = isLarge(getIframeHeight());
    gadgetFrame.setHeight(height);
    if (throbberState == ThrobberState.SHOWN && wasLarge != isLarge(height)) {
      removeThrobber();
      showThrobber();
    }
  }

  /**
   * Sets the iframe height attribute.
   */
  public String getIframeHeight() {
    return DOM.getStyleAttribute(gadgetFrame.getElement(), "height");
  }

  /**
   * Sets the iframe width attribute.
   *
   * @param width New iframe width.
   */
  public void setIframeWidth(String width) {
    gadgetFrame.setWidth(width);
  }

  /**
   * Sets the iframe width attribute.
   */
  public String getIframeWidth() {
    return DOM.getStyleAttribute(gadgetFrame.getElement(), "width");
  }

  /**
   * Sets gadget iframe ID.
   *
   * @param newId new gadget iframe id.
   */
  public void setIframeId(String newId) {
    gadgetIframe.getElement().setId(newId);
  }

  /**
   * Sets the iframe source attribute.
   *
   * @param source New iframe source.
   */
  public void setIframeSource(String source) {
    if ((source != null) && !source.equals(getIframeElement().getSrc())) {
      getIframeElement().setSrc(source);
    }
  }

  /**
   * Removes the gadget loading throbber.
   */
  public void showAsLoaded() {
    removeThrobber();
    throbberState = ThrobberState.DISABLED;
    gadgetIframe.addStyleName(CSS.loadedGadget());
    gadgetFrame.removeStyleName(CSS.loadingGadgetFrame());
    gadgetFrame.addStyleName(CSS.loadedGadgetFrame());
  }

  /**
   * Shows broken gadget icon and error message.
   */
  public void showBrokenGadget(String error) {
    if (!isLarge(getIframeHeight())) {
      gadgetFrame.setHeight(Long.toString(MAX_SMALL_GADGET_HEIGHT + 1));
    }
    removeThrobber();
    throbberState = ThrobberState.BROKEN;
    gadgetIframe.addStyleName(CSS.brokenGadgetIcon());
  }

  public String getTitleLabelText() {
    return titleLabel.getText();
  }

  /**
   * Sets the text of the title label.
   *
   * @param text New title label text.
   */
  public void setTitleLabelText(String text) {
    // TODO(user): Remove when the editor ignores mutations from doodads.
    EditorStaticDeps.startIgnoreMutations();
    try {
      titleLabel.setText(text);
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  public void setGadgetUiListener(GadgetUiListener listener) {
    this.listener = listener;
  }

  /**
   * Enables the overlay Gadget menu.
   */
  public void enableMenu() {
    menuEnabled = true;
  }

  /**
   * Disables the overlay Gadget menu.
   */
  public void disableMenu() {
    hideMetaButtons();
    if (menu != null) {
      menu.onOff(menu.getButton());
    }  
    menuEnabled = false;
  }

  public void makeInline() {
    enclosingBox.addStyleName(CSS.inline());
  }

  @Override
  public void onMouseEnter() {
    if (!menuEnabled || !editingIndicator.isEditing()) {
      return;
    }
    if (menu == null) {
      buildMenu();
    }
    showMetaButtons();
  }

  @Override
  public void onMouseLeave() {
    hideMetaButtons();
  }

  @Override
  public void addHandlers(MouseOverHandler mouseOverHandler, MouseOutHandler mouseOutHandler) {
    addDomHandler(mouseOverHandler, MouseOverEvent.getType());
    addDomHandler(mouseOutHandler, MouseOutEvent.getType());
  }

  @Override
  public boolean isOrHasChild(Element e) {
    return enclosingBox.getElement().isOrHasChild(e);
  }

  private static boolean isLarge(String height) {
    return parseSizeString(height) > MAX_SMALL_GADGET_HEIGHT;
  }

  private static int parseSizeString(String heightString) {
    try {
      if (heightString.endsWith("px")) {
        return Integer.parseInt(heightString.substring(0, heightString.length() - 2));
      } else {
        return Integer.parseInt(heightString);
      }
    } catch (NumberFormatException ex) {
      return -1;
    }
  }
}
