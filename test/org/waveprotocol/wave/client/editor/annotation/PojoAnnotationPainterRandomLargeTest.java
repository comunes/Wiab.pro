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

package org.waveprotocol.wave.client.editor.annotation;

import java.util.Random;
import org.waveprotocol.wave.model.document.dom.impl.Element;
import org.waveprotocol.wave.model.document.dom.impl.Node;
import org.waveprotocol.wave.model.document.dom.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.RandomProvider;

/**
 * Randomised tests for the annotation painter using a POJO dom substrate
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class PojoAnnotationPainterRandomLargeTest extends AnnotationPainterRandomTestBase {

  public PojoAnnotationPainterRandomLargeTest() {
    super(AnnotationPainterRandomTestBase.JUNIT_TEST_CONFIG, new RandomProvider() {
      final Random r = new Random(1);
      @Override
      public boolean nextBoolean() {
        return r.nextBoolean();
      }

      @Override
      public int nextInt(int upperBound) {
        return r.nextInt(upperBound);
      }
    });
  }

  @Override
  protected void callRunMutationsWithContext() {
    TestDocumentContext<Node, Element, Text> cxt = createAnnotationContext();
    runMutations(cxt, cxt.getIndexedDoc());
  }
}
