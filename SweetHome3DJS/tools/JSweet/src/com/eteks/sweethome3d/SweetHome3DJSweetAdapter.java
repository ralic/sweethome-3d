/*
 * SweetHome3DJSweetAdapter.java 
 *
 * Sweet Home 3D, Copyright (c) 2017 Emmanuel PUYBARET / eTeks <info@eteks.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.QualifiedNameable;

import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.Java2TypeScriptTranslator;
import org.jsweet.transpiler.extension.AnnotationManager;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.extension.RemoveJavaDependenciesAdapter;
import org.jsweet.transpiler.model.CaseElement;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.IdentifierElement;
import org.jsweet.transpiler.model.LiteralElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.NewClassElement;
import org.jsweet.transpiler.model.VariableAccessElement;

/**
 * This adapter tunes the JavaScript generation for some SweetHome3D
 * specificities.
 * 
 * <p>
 * It is a subclass of {@link RemoveJavaDependenciesAdapter} since we always
 * want the Java APIs to be removed form the generated code and use no runtime.
 * 
 * @author Renaud Pawlak
 */
public class SweetHome3DJSweetAdapter extends PrinterAdapter {

  // A local type map to save mapping that can be handled in a generic way
  private Map<String, String> sh3dTypeMapping = new HashMap<>();

  public SweetHome3DJSweetAdapter(PrinterAdapter parent) {
    super(parent);
    // Types that are supported in core.js
    sh3dTypeMapping.put(IllegalArgumentException.class.getName(), "IllegalArgumentException");
    sh3dTypeMapping.put(IllegalStateException.class.getName(), "IllegalStateException");
    sh3dTypeMapping.put(InternalError.class.getName(), "InternalError");
    sh3dTypeMapping.put(NoSuchElementException.class.getName(), "NoSuchElementException");
    sh3dTypeMapping.put(NullPointerException.class.getName(), "NullPointerException");
    sh3dTypeMapping.put(UnsupportedOperationException.class.getName(), "UnsupportedOperationException");
    sh3dTypeMapping.put(PropertyChangeEvent.class.getName(), "PropertyChangeEvent");
    sh3dTypeMapping.put(PropertyChangeListener.class.getName(), "PropertyChangeListener");
    sh3dTypeMapping.put(PropertyChangeSupport.class.getName(), "PropertyChangeSupport");
    // We assume we have the big.js lib and we map BigDecimal to Big
    sh3dTypeMapping.put(BigDecimal.class.getName(), "Big");
    // Activate the local type map
    addTypeMappings(sh3dTypeMapping);
    // We don't have a specific implementation for ResourceURLContent in JS...
    // Use the default one
    addTypeMapping("com.eteks.sweethome3d.tools.ResourceURLContent", "URLContent");
    // All enums that are named *Property will be translated to string in JS
    addTypeMapping(
        (typeTree, name) -> typeTree.getTypeAsElement().getKind() == ElementKind.ENUM && name.endsWith("Property")
            ? "string" : null);

    // All the Java elements to be ignored (will generate no JS)
    addAnnotation("jsweet.lang.Erased", //
        "**.readObject(..)", //
        "**.writeObject(..)", //
        "**.hashCode(..)", //
        "**.Compass.updateSunLocation(..)", //
        "**.Compass.getSunAzimuth(..)", //
        "**.Compass.getSunElevation(..)", //
        "**.serialVersionUID", //
        "**.Content.openStream(..)", //
        "com.eteks.sweethome3d.model.UserPreferences", //
        "com.eteks.sweethome3d.model.LengthUnit", //
        "com.eteks.sweethome3d.mobile", //
        "com.eteks.sweethome3d.io.*", //
        "com.eteks.sweethome3d.tools", //
        "com.eteks.sweethome3d.viewcontroller.*", //
        "com.eteks.sweethome3d.j3d", //
        // "com.eteks.sweethome3d.j3d.*", //
        // "!com.eteks.sweethome3d.j3d.Wall3D", //
        "!com.eteks.sweethome3d.viewcontroller.HomeController3D", //
        "!com.eteks.sweethome3d.viewcontroller.Controller", //
        "!com.eteks.sweethome3d.viewcontroller.View", //
        "com.eteks.sweethome3d.viewcontroller.HomeController3D.modifyAttributes(**)",
        "!com.eteks.sweethome3d.io.HomeXMLHandler", //
        "com.eteks.sweethome3d.io.HomeXMLHandler.contentContext", //
        "com.eteks.sweethome3d.io.HomeXMLHandler.setContentContext(**)");

    // We now ignore some Java elements with a programmatic adapter
    addAnnotationManager(new AnnotationManager() {
      @Override
      public Action manageAnnotation(Element element, String annotationType) {
        // We add the @Erased annotation upon some specific conditions
        if (JSweetConfig.ANNOTATION_ERASED.equals(annotationType)) {
          if (element.getKind() == ElementKind.ENUM && element.getSimpleName().toString().endsWith("Property")) {
            // All enums named *Property will be erased (because they will be
            // strings in the generated code)
            return Action.ADD;
          } else if (util().isDeprecated(element)) {
            // All deprecated elements will be erased
            return Action.ADD;
          } else if (element.getKind() == ElementKind.CONSTRUCTOR && ((QualifiedNameable) element.getEnclosingElement())
              .getQualifiedName().toString().equals("com.eteks.sweethome3d.model.CatalogPieceOfFurniture")) {
            // Only keep 2 public constructors of CatalogPieceOfFurniture (and
            // the private one)
            ExecutableElement c = (ExecutableElement) element;
            if (!element.getModifiers().contains(Modifier.PRIVATE)) {
              if (c.getParameters().size() != 14 && c.getParameters().size() != 26) {
                return Action.ADD;
              }
            }
            // Keep less constructors in CatalogLight and CatalogDoorOrWindow
          } else if (element.getKind() == ElementKind.CONSTRUCTOR && ((QualifiedNameable) element.getEnclosingElement())
              .getQualifiedName().toString().equals("com.eteks.sweethome3d.model.CatalogLight")) {
            // Only keep 1 public constructor of CatalogLight
            ExecutableElement c = (ExecutableElement) element;
            if (c.getParameters().size() != 27) {
              return Action.ADD;
            }
          } else if (element.getKind() == ElementKind.CONSTRUCTOR && ((QualifiedNameable) element.getEnclosingElement())
              .getQualifiedName().toString().equals("com.eteks.sweethome3d.model.CatalogDoorOrWindow")) {
            // Only keep 2 public constructors of CatalogDoorOrWindow
            ExecutableElement c = (ExecutableElement) element;
            if (c.getParameters().size() != 29 && c.getParameters().size() != 16) {
              return Action.ADD;
            }
          }
        }
        return Action.VOID;
      }

    });

    // We erase some packages: all the elements in these packages will be top
    // level in JS
    addAnnotation("@Root", "com.eteks.sweethome3d.model", "com.eteks.sweethome3d.io",
        "com.eteks.sweethome3d.viewcontroller", "com.eteks.sweethome3d.j3d");

    // Replace some Java implementations with some JS-specific implementations
    addAnnotation(
        "@Replace('if (this.shapeCache == null) { this.shapeCache = this.getPolylinePath(); } return this.shapeCache; ')",
        "com.eteks.sweethome3d.model.Polyline.getShape()");
    addAnnotation(
        "@Replace('if(content == null) { return null; } else if(content.indexOf('://') >= 0) { return new URLContent(content); } else { return new HomeURLContent('jar:'+this['homeUrl']+'!/'+content); }')",
        "com.eteks.sweethome3d.io.HomeXMLHandler.parseContent(java.lang.String)");

    addAnnotation(
        "@Replace('#BODY##BASEINDENT#if(attributes['structure']) { home['structure'] = this.parseContent(attributes['structure']); }')",
        "com.eteks.sweethome3d.io.HomeXMLHandler.setHomeAttributes(..)");

    // uncomment and adapt to log some method(s)
    // addAnnotation(
    // "@Replace('console.log('before #CLASSNAME#.#METHODNAME#:
    // '+arguments[0]); let result = (() => { #BODY# })(); console.log('after
    // #CLASSNAME#.#METHODNAME#'); return result;')",
    // "com.eteks.sweethome3d.io.HomeXMLHandler.parse*(..)");

    // Force some interface to be mapped so functional types when possible
    addAnnotation(FunctionalInterface.class, "com.eteks.sweethome3d.model.CollectionListener",
        "com.eteks.sweethome3d.model.LocationAndSizeChangeListener");

  }

  @Override
  public boolean substituteNewClass(NewClassElement newClass) {
    String className = newClass.getTypeAsElement().toString();
    // Handle generically all types that are locally mapped
    if (sh3dTypeMapping.containsKey(className)) {
      print("new ").print(sh3dTypeMapping.get(className)).print("(").printArgList(newClass.getArguments()).print(")");
      return true;
    }
    switch (className) {
    // This is a hack until we have actual locale support (just create JS Date
    // objects)
    case "java.util.GregorianCalendar":
      if (newClass.getArguments().size() == 1) {
        if (newClass.getArguments().get(0) instanceof LiteralElement) {
          Object value = ((LiteralElement) newClass.getArguments().get(0)).getValue();
          if (!(value instanceof String && "UTC".equals(value))) {
            // This will use the user's locale
            print("new Date()");
            return true;
          }
        } else {
          // This will use the user's locale
          print("new Date()");
          return true;
        }
      }
    case "com.eteks.sweethome3d.io.DefaultUserPreferences":
      print("new UserPreferences()");
      return true;
    }
    return super.substituteNewClass(newClass);
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (invocation.getTargetExpression() != null) {
      Element targetType = invocation.getTargetExpression().getTypeAsElement();
      switch (targetType.toString()) {
      // override invocations to LengthUnit so that it is not handled as a
      // complex enum and use the JS implementation instead
      case "com.eteks.sweethome3d.model.LengthUnit":
        print(invocation.getTargetExpression()).print(".").print(invocation.getMethodName()).print("(")
            .printArgList(invocation.getArguments()).print(")");
        return true;
      case "java.text.Collator":
        switch (invocation.getMethodName()) {
        case "setStrength":
          printMacroName(invocation.getMethodName());
          // Erase setStrength completely
          print(invocation.getTargetExpression());
          return true;
        }
        break;
      case "java.math.BigDecimal":
        // Support for Java big decimal (method are mapped to their Big.js
        // equivalent)
        switch (invocation.getMethodName()) {
        case "multiply":
          printMacroName(invocation.getMethodName());
          print(invocation.getTargetExpression()).print(".times(").printArgList(invocation.getArguments()).print(")");
          return true;
        case "add":
          printMacroName(invocation.getMethodName());
          print(invocation.getTargetExpression()).print(".plus(").printArgList(invocation.getArguments()).print(")");
          return true;
        case "scale":
          printMacroName(invocation.getMethodName());
          // Always have a scale of 2 (we only have currencies, so 2 is a
          // standard)
          print("2");
          return true;
        case "setScale":
          printMacroName(invocation.getMethodName());
          print(invocation.getTargetExpression()).print(".round(").print(invocation.getArguments().get(0)).print(")");
          return true;
        case "compareTo":
          printMacroName(invocation.getMethodName());
          print(invocation.getTargetExpression()).print(".cmp(").print(invocation.getArguments().get(0)).print(")");
          return true;
        }
        break;

      }
      // SH3D maps Property enums to strings
      if (targetType.getKind() == ElementKind.ENUM && targetType.toString().endsWith("Property")) {
        switch (invocation.getMethodName()) {
        case "name":
          printMacroName(invocation.getMethodName());
          print(invocation.getTargetExpression());
          return true;
        case "valueOf":
          printMacroName(invocation.getMethodName());
          print(invocation.getArgument(0));
          return true;
        case "equals":
          printMacroName(invocation.getMethodName());
          print("(").print(invocation.getTargetExpression()).print(" == ").print(invocation.getArguments().get(0))
              .print(")");
          return true;
        }
      }
      // special case for the AspectRatio enum
      if (targetType.toString().endsWith(".AspectRatio") && invocation.getMethodName().equals("getValue")) {
        print(
            "{FREE_RATIO:null,VIEW_3D_RATIO:null,RATIO_4_3:4/3,RATIO_3_2:1.5,RATIO_16_9:16/9,RATIO_2_1:2/1,SQUARE_RATIO:1}[")
                .print(invocation.getTargetExpression()).print("]");
        return true;
      }
    }
    boolean substituted = super.substituteMethodInvocation(invocation);
    if (!substituted) {
      // support for equals in case not supported by existing adapters
      if (invocation.getMethodName().equals("equals")) {
        print("((o:any, o2) => { return o.equals?o.equals(o2):o===o2 })(").print(invocation.getTargetExpression())
            .print(",").print(invocation.getArguments().get(0)).print(")");
        return true;
      }
    }
    return substituted;
  }

  @Override
  public boolean substituteVariableAccess(VariableAccessElement variableAccess) {
    switch (variableAccess.getTargetElement().toString()) {
    case "java.text.Collator":
      switch (variableAccess.getVariableName()) {
      case "CANONICAL_DECOMPOSITION":
      case "FULL_DECOMPOSITION":
      case "IDENTICAL":
      case "NO_DECOMPOSITION":
      case "PRIMARY":
      case "SECONDARY":
      case "TERTIARY":
        print("undefined");
        return true;
      }
    }
    // Map *Property enums to strings
    if (variableAccess.getTargetElement().getKind() == ElementKind.ENUM
        && variableAccess.getTargetElement().toString().endsWith("Property")) {
      print("\"" + variableAccess.getVariableName() + "\"");
      return true;
    }
    return super.substituteVariableAccess(variableAccess);
  }

  @Override
  public boolean substituteIdentifier(IdentifierElement identifier) {
    // Map *Property enums to strings
    if (identifier.getTypeAsElement().getKind() == ElementKind.ENUM && identifier.isConstant()
        && identifier.getTypeAsElement().getSimpleName().toString().endsWith("Property")) {
      print("\"" + identifier + "\"");
      return true;
    }
    return super.substituteIdentifier(identifier);
  }

  @Override
  public boolean substituteCaseStatementPattern(CaseElement caseStatement, ExtendedElement pattern) {
    // Map *Property enums to strings
    if (pattern.getTypeAsElement().getKind() == ElementKind.ENUM
        && pattern.getTypeAsElement().getSimpleName().toString().endsWith("Property")) {
      print("\"" + pattern + "\"");
      return true;
    }
    return super.substituteCaseStatementPattern(caseStatement, pattern);
  }

}
