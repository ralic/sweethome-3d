/*
 * SweetHome3DJava3DJSweetAdapter.java 
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

import java.util.HashMap;
import java.util.Map;

import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.NewClassElement;
import org.jsweet.transpiler.model.VariableAccessElement;

/**
 * This adapter tunes the JavaScript generation for some SweetHome3D
 * specificities.
 * 
 * @author Emmanuel Puybaret
 * @author Renaud Pawlak
 */
public class SweetHome3DJava3DJSweetAdapter extends PrinterAdapter {
  private Map<String, String> java3dTypeMapping = new HashMap<>();
  
  public SweetHome3DJava3DJSweetAdapter(PrinterAdapter parent) {
    super(parent);

    this.java3dTypeMapping.put("javax.media.j3d.Appearance", "Appearance3D");
    this.java3dTypeMapping.put("javax.media.j3d.BranchGroup", "BranchGroup3D");
    this.java3dTypeMapping.put("javax.media.j3d.Geometry", "IndexedGeometryArray3D");
    this.java3dTypeMapping.put("javax.media.j3d.Group", "Group3D");
    this.java3dTypeMapping.put("javax.media.j3d.Link", "Link3D");
    this.java3dTypeMapping.put("javax.media.j3d.Node", "Node3D");
    this.java3dTypeMapping.put("javax.media.j3d.PolygonAttributes", "Appearance3D");
    this.java3dTypeMapping.put("javax.media.j3d.RenderingAttributes", "Appearance3D");
    this.java3dTypeMapping.put("javax.media.j3d.Shape3D", "Shape3D");
    this.java3dTypeMapping.put("javax.media.j3d.SharedGroup", "SharedGroup3D");
    this.java3dTypeMapping.put("javax.media.j3d.TexCoordGeneration", "Appearance3D");
    this.java3dTypeMapping.put("javax.media.j3d.Texture", "Object");
    this.java3dTypeMapping.put("javax.media.j3d.TextureAttributes", "Appearance3D");
    this.java3dTypeMapping.put("javax.media.j3d.Transform3D", "mat4");
    this.java3dTypeMapping.put("javax.media.j3d.TransformGroup", "TransformGroup3D");
    this.java3dTypeMapping.put("javax.media.j3d.TransparencyAttributes", "Appearance3D");
    this.java3dTypeMapping.put("javax.vecmath.Point3f", "vec3");
    this.java3dTypeMapping.put("javax.vecmath.TexCoord2f", "vec2");
    this.java3dTypeMapping.put("javax.vecmath.Vector3f", "vec3");
    this.java3dTypeMapping.put("javax.vecmath.Vector3d", "vec3");
    this.java3dTypeMapping.put("com.sun.j3d.utils.geometry.Box", "Box3D");
    this.java3dTypeMapping.put("com.sun.j3d.utils.geometry.GeometryInfo", "GeometryInfo");
    this.java3dTypeMapping.put("com.sun.j3d.utils.geometry.NormalGenerator", "GeometryInfo");
    
    this.java3dTypeMapping.put("com.eteks.sweethome3d.j3d.Wall3D", "Wall3D");
    this.java3dTypeMapping.put("com.eteks.sweethome3d.j3d.Ground3D", "Ground3D");
    this.java3dTypeMapping.put("com.eteks.sweethome3d.j3d.Room3D", "Room3D");
    this.java3dTypeMapping.put("com.eteks.sweethome3d.j3d.HomePieceOfFurniture3D", "HomePieceOfFurniture3D");
    this.java3dTypeMapping.put("com.eteks.sweethome3d.j3d.TextureManager", "TextureManager");
    this.java3dTypeMapping.put("com.eteks.sweethome3d.j3d.ModelManager", "ModelManager");
    addTypeMappings(this.java3dTypeMapping);
    
    addAnnotation(FunctionalInterface.class, "com.eteks.sweethome3d.j3d.TextureManager.TextureObserver");
  }

  private ExtendedElement lastGeometryInfoName;

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (invocation.getTargetExpression() != null) {
      switch (invocation.getTargetExpression().getTypeAsElement().toString()) {
        case "javax.media.j3d.Appearance":
          switch (invocation.getMethodName()) {
            case "getTransparencyAttributes":
              print(invocation.getTargetExpression());
              return true;
            case "setTransparencyAttributes":
            case "setRenderingAttributes":
              // Ignore call
              return true;
            case "setTexture":
              print(invocation.getTargetExpression()).print(".setTextureImage").print("(").printArgList(invocation.getArguments()).print(")");
              return true;
          }
          break;
        case "com.sun.j3d.utils.geometry.GeometryInfo":
          this.lastGeometryInfoName = invocation.getTargetExpression();
          switch (invocation.getMethodName()) {
            case "setTextureCoordinateParams":
              // Ignore call
              return true;
            case "setTextureCoordinates":
              print(invocation.getTargetExpression()).print(".").print(invocation.getMethodName()).print("(").print(invocation.getArguments().get(1)).print(")");
              return true;
          }
          break;
        case "com.sun.j3d.utils.geometry.NormalGenerator":
          switch (invocation.getMethodName()) {
            case "setCreaseAngle":
              print(this.lastGeometryInfoName).print(".").print(invocation.getMethodName()).print("(").print(invocation.getArguments().get(0)).print(")");
              return true;
            case "generateNormals":
              print(this.lastGeometryInfoName).print(".setGeneratedNormals(true)");
              return true;
          }
          break;
        case "javax.media.j3d.Transform3D":
          switch (invocation.getMethodName()) {
            case "setTranslation":
              print("mat4.fromTranslation(").print(invocation.getTargetExpression()).print(", ").printArgList(invocation.getArguments()).print(")");
              return true;
            case "rotX":
              print("mat4.fromXRotation(").print(invocation.getTargetExpression()).print(", ").printArgList(invocation.getArguments()).print(")");
              return true;
            case "rotY":
              print("mat4.fromYRotation(").print(invocation.getTargetExpression()).print(", ").printArgList(invocation.getArguments()).print(")");
              return true;
            case "rotZ":
              print("mat4.fromZRotation(").print(invocation.getTargetExpression()).print(", ").printArgList(invocation.getArguments()).print(")");
              return true;
            case "setScale":
              print("mat4.scale(").print(invocation.getTargetExpression()).print(", ").print(invocation.getTargetExpression()).print(", ").printArgList(invocation.getArguments()).print(")");
              return true;
            case "mul":
              print("mat4.mul(").print(invocation.getTargetExpression()).print(", ").print(invocation.getTargetExpression()).print(", ").printArgList(invocation.getArguments()).print(")");
              return true;
            case "transform":
              print("vec3.transformMat4(").print(invocation.getArguments().get(0)).print(", ").print(invocation.getArguments().get(0)).print(", ").print(invocation.getTargetExpression()).print(")");
              return true;
            case "invert":
              print("mat4.invert(").print(invocation.getTargetExpression()).print(", ").printArgList(invocation.getArguments()).print(")");
              return true;
          }
          break;
        case "javax.media.j3d.Group":
        case "javax.media.j3d.BranchGroup":
          switch (invocation.getMethodName()) {
            case "numChildren":
              print(invocation.getTargetExpression()).print(".getChildren().length");
              return true;
          }
          break;
        case "javax.media.j3d.Shape3D":
          switch (invocation.getMethodName()) {
            case "numGeometries":
              print(invocation.getTargetExpression()).print(".getGeometries().length");
              return true;
          }
          break;
      }
      if (this.java3dTypeMapping.containsKey(invocation.getTargetExpression().getTypeAsElement().toString())
          && "setCapability".equals(invocation.getMethodName())
          && !invocation.getArguments().get(0).toString().endsWith("ALLOW_CHILDREN_EXTEND")
          && !invocation.getArguments().get(0).toString().endsWith("ALLOW_TRANSFORM_WRITE")
          && !invocation.getArguments().get(0).toString().endsWith("ALLOW_GEOMETRY_WRITE")) {
        // Ignore other calls to setCapability
        return true;
      }
      if ("getHomeTextureClone".equals(invocation.getMethodName())) {
        print(invocation.getArguments().get(0));
        return true;
      };      
    }
    return super.substituteMethodInvocation(invocation);
  }
  
  @Override
  public boolean substituteVariableAccess(VariableAccessElement variableAccess) {
    if (variableAccess.getTargetExpression() != null) {
      switch (variableAccess.getTargetExpression().getTypeAsElement().toString()) {
        case "javax.vecmath.Point3f":
        case "javax.vecmath.TexCoord2f":
        case "javax.vecmath.Vector3f":
        case "javax.vecmath.Vector3d":
        case "javax.vecmath.Vector4f":
          switch (variableAccess.getVariableName()) {
            case "x":
              print(variableAccess.getTargetExpression()).print("[0]");
              return true;
            case "y":
              print(variableAccess.getTargetExpression()).print("[1]");
              return true;
            case "z":
              print(variableAccess.getTargetExpression()).print("[2]");
              return true;
            case "w":
              print(variableAccess.getTargetExpression()).print("[3]");
              return true;
          }
          break;
      }
    }
    return super.substituteVariableAccess(variableAccess);
  }

  @Override
  public boolean substituteNewClass(NewClassElement newClass) {
    String className = newClass.getTypeAsElement().toString();
    switch (className) {
      case "javax.vecmath.Vector4f":
        print("vec4.fromValues(").printArgList(newClass.getArguments()).print(")");
        return true;
      case "javax.vecmath.Point3f":
      case "javax.vecmath.Vector3f":
      case "javax.vecmath.Vector3d":
        print("vec3.fromValues(").printArgList(newClass.getArguments()).print(")");
        return true;
      case "javax.vecmath.TexCoord2f":
        print("vec2.fromValues(").printArgList(newClass.getArguments()).print(")");
        return true;
      case "javax.media.j3d.Transform3D":
        print("mat4.create()");
        return true;
    }
    // Remove package in front of other Java 3D classes
    if (this.java3dTypeMapping.containsKey(className)) {
      print("new ").print(this.java3dTypeMapping.get(className)).print("(")
          .printArgList(newClass.getArguments()).print(")");
      return true;
    }
    return super.substituteNewClass(newClass);
  }
}
