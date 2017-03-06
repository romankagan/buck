/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java.abi.source;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * An implementation of {@link TypeElement} that uses only the information available from one or
 * more {@link com.sun.source.tree.CompilationUnitTree}s. This results in an incomplete
 * implementation; see documentation for individual methods and
 * {@link com.facebook.buck.jvm.java.abi.source} for more information.
 */
class TreeBackedPackageElement extends TreeBackedElement implements PackageElement {
  private final PackageElement javacPackage;
  private boolean resolved = false;

  public TreeBackedPackageElement(
      PackageElement javacPackage,
      TypeResolverFactory resolverFactory) {
    super(javacPackage, null, null, resolverFactory);
    this.javacPackage = javacPackage;
  }

  @Override
  public List<? extends Element> getEnclosedElements() {
    resolve();

    return super.getEnclosedElements();
  }

  private void resolve() {
    if (resolved) {
      return;
    }

    if (javacPackage != null) {
      // Java allows packages to be split across modules, so once we've found all the tree-backed
      // elements (i.e., elements from the current module), we fill in the ones from modules on
      // the classpath.

      List<? extends Element> treeBackedEnclosedElements = super.getEnclosedElements();
      Set<Name> enclosedElementNames = treeBackedEnclosedElements.stream()
          .map(Element::getSimpleName)
          .collect(Collectors.toSet());

      for (Element element : javacPackage.getEnclosedElements()) {
        if (enclosedElementNames.contains(element.getSimpleName())) {
          // This was already added when parsing the tree-backed elements
          continue;
        }

        addEnclosedElement(element);
      }
    }

    resolved = true;
  }

  @Override
  public Name getQualifiedName() {
    return javacPackage.getQualifiedName();
  }

  @Override
  public boolean isUnnamed() {
    return javacPackage.isUnnamed();
  }

  @Override
  public TypeMirror asType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <R, P> R accept(ElementVisitor<R, P> v, P p) {
    return v.visitPackage(this, p);
  }
}
