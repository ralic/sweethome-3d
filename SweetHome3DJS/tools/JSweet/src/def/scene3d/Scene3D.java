/*
 * Scene3D.java 
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
package def.scene3d;

import def.js.Error;

/**
 * Data of a vector storing 2 values.
 */
class vec2 {
}

/**
 * Data of a vector storing 3 values.
 */
class vec3 {
}

/**
 * Data of a matrix storing 4x4 values.
 */
class mat4 {
}

/**
 * Creates an abstract 3D node.
 * @author Emmanuel Puybaret
 */
abstract class Node3D {
  public native void setCapability(int capability);
  
  public native boolean getCapability(int capability);
  
  public native Group3D getParent();
  
  public native Object getUserData();
  
  public native void setUserData(Object userData);
  
  public native String getName();
  
  public native void setName(String name);
  
  public native Node3D clone();
}

/**
 * Creates a 3D shape.
 * @author Emmanuel Puybaret
 */
class Shape3D extends Node3D {
  public Shape3D(IndexedGeometryArray3D geometry, Appearance3D appearance) {    
  }

  public native void addGeometry(IndexedGeometryArray3D geometry3D);

  public native BoundingBox3D getBounds();

  public native IndexedGeometryArray3D [] getGeometries();

  public native Appearance3D getAppearance();

  public native void setAppearance(Appearance3D appearance);

  public native void setPickable(boolean pickable);

  public native boolean isPickable();
}

/**
 * Creates a 3D background.
 * @author Emmanuel Puybaret
 */
class Background3D extends Node3D {
  public Background3D(Node3D group) {
  } 

  public native Node3D getGeometry();
}

/**
 * Creates a 3D light.
 * @author Emmanuel Puybaret
 */
class Light3D extends Node3D {
  public Light3D(vec3 color) {
  }

  public native vec3 getColor();

  public native void setColor(vec3 color);
}

/**
 * Creates an ambient light.
 * @author Emmanuel Puybaret
 */
class AmbientLight3D extends Light3D {
  public AmbientLight3D(vec3 color) {    
    super(color);
  }
}

/**
 * Creates a directional light.
 * @author Emmanuel Puybaret
 */
class DirectionalLight3D extends Light3D {
  public DirectionalLight3D(vec3 color, vec3 direction) {
    super(color);
  }

  public native vec3 getDirection();
}

/**
 * Creates a group, parent of 3D shapes and other groups.
 * @author Emmanuel Puybaret
 */
class Group3D extends Node3D {
  public final static int ALLOW_CHILDREN_EXTEND = 1;
  
  public Group3D() {
  }

  public native void addChild(Node3D child);

  public native void insertChild(Node3D child, int index);

  public native Node3D getChild(int index);

  public native Node3D [] getChildren();

  public native void removeChild(int index);

  public native void removeAllChildren();
} 

/**
 * Creates a branch group with a children list that may change once a 3D scene is already live.
 * @author Emmanuel Puybaret
 */
class BranchGroup3D extends Group3D {
  public BranchGroup3D() {
  }

  public native void detach();
}

/**
 * Creates a shared group that may have multiple links parents.
 * @author Emmanuel Puybaret
 */
class SharedGroup3D extends Group3D {
  public SharedGroup3D() {
  }
}

/**
 * Creates a link that allows to use more than once a shared group in the graph.
 * @author Emmanuel Puybaret
 */
class Link3D extends Node3D {
  public Link3D(SharedGroup3D sharedGroup) {
  }
  
  public native SharedGroup3D getSharedGroup();
  
  public native void setSharedGroup(SharedGroup3D sharedGroup);
}

/**
 * Creates a transform group.
 * @param {mat4} transform
 * @constructor
 * @extends Group3D
 * @author Emmanuel Puybaret
 */
class TransformGroup3D extends Group3D {
  public static final int ALLOW_TRANSFORM_WRITE = 2;
  
  public TransformGroup3D(mat4 transform) {
  }

  public native void getTransform(mat4 transform);

  public native void setTransform(mat4 transform);
}

/**
 * Creates an appearance to store material attributes, transparency and texture.
 * @author Emmanuel Puybaret
 */
class Appearance3D {
  public static final int CULL_NONE = 0;
  public static final int CULL_BACK = 1;
  public static final int CULL_FRONT = 2;
  
  public Appearance3D(String name) {
  }

  public native void setName(String name);

  public native String getName();

  public native void setAmbientColor(vec3 ambientColor);

  public native vec3 getAmbientColor();

  public native void setEmissiveColor(vec3 emissiveColor);

  public native vec3 getEmissiveColor();

  public native void setDiffuseColor(vec3 diffuseColor);

  public native vec3 getDiffuseColor();

  public native void setSpecularColor(vec3 specularColor);

  public native vec3 getSpecularColor();

  public native void setShininess(float shininess);

  public native float getShininess();

  public native void setTransparency(float transparency);

  public native float getTransparency();

  public native void setIllumination(int illumination);

  public native int getIllumination();

  public native void setTextureImage(Object textureImage);

  public native Object getTextureImage();

  public native void setTextureCoordinatesGeneration(Object textureCoordinatesGeneration);
  
  public native Object getTextureCoordinatesGeneration();

  public native void setTextureTransform(mat4 textureTransform);

  public native mat4 getTextureTransform();

  public native void setVisible(boolean visible);

  public native boolean isVisible();

  public native void setCullFace(int cullFace);

  public native int getCullFace();

  public native void setBackFaceNormalFlip(boolean backFaceNormalFlip);
  
  public native boolean getBackFaceNormalFlip();

  public native boolean isBackFaceNormalFlip();

  public native Appearance3D clone();
}

/**
 * Creates an indexed 3D geometry array.
 * @author Emmanuel Puybaret
 */
abstract class IndexedGeometryArray3D {
  public IndexedGeometryArray3D(vec3 [] vertices, int [] vertexIndices,
                                vec2 [] textureCoordinates, int [] textureCoordinateIndices) {
  }
}

/**
 * Creates the 3D geometry of an indexed line array.
 * @author Emmanuel Puybaret
 */
abstract class IndexedLineArray3D extends IndexedGeometryArray3D {
  public IndexedLineArray3D(vec3 [] vertices, int [] vertexIndices,
                            vec2 [] textureCoordinates, int [] textureCoordinateIndices) {
    super(vertices, vertexIndices, textureCoordinates, textureCoordinateIndices);
  }
}

/**
 * Creates the 3D geometry of an indexed triangle array.
 * @author Emmanuel Puybaret
 */
abstract class IndexedTriangleArray3D extends IndexedGeometryArray3D {
  public IndexedTriangleArray3D(vec3 [] vertices, int [] vertexIndices,
                                vec2 [] textureCoordinates, int [] textureCoordinateIndices, 
                                vec3 [] normals, int [] normalIndices) {    
    super(vertices, vertexIndices, textureCoordinates, textureCoordinateIndices);
  }
}

/**
 * Creates a 3D bounding box.
 * @author Emmanuel Puybaret
 */
class BoundingBox3D {
  public BoundingBox3D(vec3 lower, vec3 upper) {
  }

  public native void getLower(vec3 p);

  public native void getUpper(vec3 p);

  public native void combine(BoundingBox3D bounds);

  public native void transform(mat4 transform);
  
  public native BoundingBox3D clone();
}

/**
 * Creates a 3D box shape.
 * @author Emmanuel Puybaret
 */
class Box3D extends Shape3D {
  public Box3D(float xdim, float ydim, float zdim, Appearance3D appearance) {
    super(null, appearance);
  }
}

/**
 * Creates data used to build the geometry of a shape.
 * @author Emmanuel Puybaret
 */
class GeometryInfo {
  public static final int TRIANGLE_ARRAY = 0;
  public static final int TRIANGLE_STRIP_ARRAY = 1;
  public static final int TRIANGLE_FAN_ARRAY = 2;
  public static final int QUAD_ARRAY = 10;
  public static final int POLYGON_ARRAY = 20;
  
  public GeometryInfo(int type) {
  }
  
  public native void setCoordinates(vec3 [] vertices);

  public native void setCoordinateIndices(int [] coordinatesIndices);

  public native void setNormals(vec3 [] normals);

  public native void setNormalIndices(int [] normalIndices);

  public native void setTextureCoordinates(vec2 [] textureCoordinates);

  public native void setTextureCoordinateIndices(int [] textureCoordinateIndices);

  public native void setStripCounts(int [] stripCounts);

  public native void setContourCounts(int [] contourCounts);

  public native void setCreaseAngle(float creaseAngle);

  public native void setGeneratedNormals(boolean generatedNormals);
  
  public native IndexedTriangleArray3D getGeometryArray();
}

/**
 * Creates an IncorrectFormat3DException instance.
 */
class IncorrectFormat3DException extends Error { 
  public IncorrectFormat3DException(String message) {
  }
}
