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

package org.waveprotocol.wave.model.document.dom.impl;

import org.waveprotocol.wave.model.document.dom.DomDocument;
import org.waveprotocol.wave.model.document.dom.DomDocumentProviderImpl;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.OffsetList;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Defines how the RawDocument interface is provided by the state in Node,
 * Element and Text.
 *
 */
public class DomDocumentImpl
    implements DomDocument<Node, Element, Text> {

  /**
   * Builder class for this document type.
   */
  public static class Factory
      implements DomDocument.Factory<DomDocumentImpl> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DomDocumentImpl create(
        String tagName, Map<String, String> attributes) {
      return new DomDocumentImpl(tagName, attributes);
    }
  }

  /** Singleton builder for documents of this type. */
  public static final Factory BUILDER = new Factory();

  /** Sample provider of this document implementation. */
  public static final DomDocument.Provider<DomDocumentImpl> PROVIDER
      = DomDocumentProviderImpl.create(BUILDER);

  /** Root document element. */
  private final Element documentElement;

  /**
   * Creates a RawDocumentImpl.
   *
   * @param tagName The tag name of the document element.
   * @param attributes The attributes of the documentelement.
   */
  DomDocumentImpl(String tagName, Map<String, String> attributes) {
    this.documentElement = createElement(tagName, attributes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element getDocumentElement() {
    return documentElement;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element createElement(String tagName, Map<String, String> attributes,
      Element parentElement, Node nodeAfter) {
    Element el = createElement(tagName, attributes);
    insertBefore(parentElement, el, nodeAfter);
    return el;
  }

  /**
   * Creates an element.
   * This factory method is the only place where elements get created.
   *
   * @param tagName
   * @param attributes
   * @return the new element
   */
  protected Element createElement(String tagName, Map<String, String> attributes) {
    Element element = new Element(tagName);
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      element.setAttribute(attribute.getKey(), attribute.getValue());
    }
    return element;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Text createTextNode(String data, Element parentElement, Node nodeAfter) {
    Text node = new Text(data);
    insertBefore(parentElement, node, nodeAfter);
    return node;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element getParentElement(Node node) {
    return node.getParentElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short getNodeType(Node node) {
    return node.getNodeType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getFirstChild(Node node) {
    return node.getFirstChild();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getLastChild(Node node) {
    return node.getLastChild();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getPreviousSibling(Node node) {
    return node.getPreviousSibling();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getNextSibling(Node node) {
    return node.getNextSibling();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node insertBefore(Element parent, Node newChild, Node refChild) {
    return parent.insertBefore(newChild, refChild);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node insertBefore(Element parent, Node from, Node to, Node refChild) {
    for (Node newChild = from; newChild != to; ) {
      Node next = newChild.getNextSibling();
      parent.insertBefore(newChild, refChild);
      newChild = next;
    }
    return from;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeChild(Element parent, Node oldChild) {
    parent.removeChild(oldChild);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSameNode(Node node, Node other) {
    return node == other;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String,String> getAttributes(Element element) {
    return element.getAttributes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTagName(Element element) {
    return element.getTagName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAttribute(Element element, String name) {
    return element.getAttribute(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttribute(Element element, String name, String value) {
    Preconditions.checkNotNull(value, "Null attribute value in setAttribute");
    element.setAttribute(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAttribute(Element element, String name) {
    element.removeAttribute(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getData(Text textNode) {
    return textNode.getData();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getLength(Text textNode) {
    return textNode.getLength();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void appendData(Text textNode, String arg) {
    textNode.appendData(arg);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void insertData(Text textNode, int offset, String arg) {
    textNode.insertData(offset, arg);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteData(Text textNode, int offset, int count) {
    textNode.deleteData(offset, count);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Text splitText(Text textNode, int offset) {
    return textNode.splitText(offset);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Text mergeText(Text secondSibling) {
    Text previous = asText(getPreviousSibling(secondSibling));
    if (previous != null) {
      previous.appendData(secondSibling.getData());
      return previous;
    } else {
      return null;
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Element asElement(Node node) {
    return node != null ? node.asElement() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Text asText(Node node) {
    return node != null ? node.asText() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OffsetList.Container<Node> getIndexingContainer(Node domNode) {
    return domNode.getIndexingContainer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIndexingContainer(Node domNode, OffsetList.Container<Node> indexingContainer) {
    domNode.setIndexingContainer(indexingContainer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return XmlStringBuilder.outerXml(this).toString();
  }

  /**
   * Gets the inner XML content of this document as a string.
   *
   * @return inner content of this document.
   */
  public String innerXml() {
    return XmlStringBuilder.createChildren(this, documentElement).toString();
  }
}
