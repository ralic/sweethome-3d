/*
 * ModelManager.js
 *
 * Sweet Home 3D, Copyright (c) 2015 Emmanuel PUYBARET / eTeks <info@eteks.com>
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

// Requires URLContent.js
//          scene3d.js
//          ModelLoader.js
//          OBJLoader.js
//          HomeObject.js
//          HomePieceOfFurniture.js

/**
 * Singleton managing 3D models cache.
 * @constructor
 * @author Emmanuel Puybaret
 */
function ModelManager() {
  this.loadedModelNodes = {};
  this.loadingModelObservers = {};
  this.parsedShapes = {};
}

/**
 * <code>Shape3D</code> name prefix for window pane shapes. 
 */
ModelManager.WINDOW_PANE_SHAPE_PREFIX = "sweethome3d_window_pane";
/**
 * <code>Shape3D</code> name prefix for mirror shapes. 
 */
ModelManager.MIRROR_SHAPE_PREFIX = "sweethome3d_window_mirror";
/**
 * <code>Shape3D</code> name prefix for lights. 
 */
ModelManager.LIGHT_SHAPE_PREFIX = "sweethome3d_light";

// Singleton
ModelManager.instance = null;

/**
 * Returns an instance of this singleton.
 * @return {ModelManager} 
 */
ModelManager.getInstance = function() {
  if (ModelManager.instance == null) {
    ModelManager.instance = new ModelManager();
  }
  return ModelManager.instance;
}

/**
 * Clears loaded models cache. 
 */
ModelManager.prototype.clear = function() {
  this.loadedModelNodes = {};
  this.loadingModelObservers = {};
  if (this.modelLoaders) {
    for (var i = 0; i < this.modelLoaders.length; i++) {
      this.modelLoaders [i].clear();
    } 
  }
}

/**
 * Returns the minimum size of a model.
 */
ModelManager.prototype.getMinimumSize = function() {
  return 0.001;
}

/**
 * Returns the size of 3D shapes of node after an additional optional transformation.
 * @param {Node3D} node  the root of a model 
 * @param {Array}  [transformation] the optional transformation applied to the model  
 */
ModelManager.prototype.getSize = function(node, transformation) {
  if (transformation === undefined) {
    transformation = mat4.create();
  }
  var bounds = this.getBounds(node, transformation);
  var lower = vec3.create();
  bounds.getLower(lower);
  var upper = vec3.create();
  bounds.getUpper(upper);
  return vec3.fromValues(Math.max(this.getMinimumSize(), upper[0] - lower[0]), 
      Math.max(this.getMinimumSize(), upper[1] - lower[1]), 
      Math.max(this.getMinimumSize(), upper[2] - lower[2]));
}

/**
 * Returns the bounds of the 3D shapes of node with an additional optional transformation.
 * @param {Node3D} node  the root of a model 
 * @param {Array}  [transformation] the optional transformation applied to the model  
 */
ModelManager.prototype.getBounds = function(node, transformation) {
  if (transformation === undefined) {
    transformation = mat4.create();
  }
  var objectBounds = new BoundingBox3D(
      vec3.fromValues(Infinity, Infinity, Infinity), 
      vec3.fromValues(-Infinity, -Infinity, -Infinity));
  this.computeBounds(node, objectBounds, transformation, !this.isOrthogonalRotation(transformation));
  return objectBounds;
}

/**
 * Returns true if the rotation matrix matches only rotations of 
 * a multiple of 90° degrees around x, y or z axis.
 * @private
 */
ModelManager.prototype.isOrthogonalRotation = function(transformation) {
  for (var i = 0; i < 3; i++) {
    for (var j = 0; j < 3; j++) {
      // Return false if the matrix contains a value different from 0 1 or -1
      if (Math.abs(transformation[i * 4 + j]) > 1E-6
          && Math.abs(transformation[i * 4 + j] - 1) > 1E-6
          && Math.abs(transformation[i * 4 + j] + 1) > 1E-6) {
        return false;
      }
    }
  }
  return true;
}

/**
 * @private
 */
ModelManager.prototype.computeBounds = function(node, bounds, parentTransformations, transformShapeGeometry) {
  if (node instanceof Group3D) {
    if (node instanceof TransformGroup3D) {
      parentTransformations = mat4.clone(parentTransformations);
      mat4.mul(parentTransformations, parentTransformations, node.transform);
    }
    // Compute the bounds of all the node children
    for (var i = 0; i < node.children.length; i++) {
      this.computeBounds(node.children [i], bounds, parentTransformations, transformShapeGeometry);
    }
  } else if (node instanceof Link3D) {
    this.computeBounds(node.getSharedGroup(), bounds, parentTransformations, transformShapeGeometry);
  } else if (node instanceof Shape3D) {
    var shapeBounds;
    if (transformShapeGeometry) {
      shapeBounds = this.computeTransformedGeometryBounds(node, parentTransformations);
    } else {
      shapeBounds = node.getBounds();
      shapeBounds.transform(parentTransformations);
    }
    bounds.combine(shapeBounds);
  }
}

/**
 * @private
 */
ModelManager.prototype.computeTransformedGeometryBounds = function(shape, transformation) {
  var lower = vec3.fromValues(Infinity, Infinity, Infinity);
  var upper = vec3.fromValues(-Infinity, -Infinity, -Infinity);    
  for (var i = 0; i < shape.geometries.length; i++) {
    // geometry instanceof IndexedGeometryArray3D
    var geometry = shape.geometries [i];
    var vertex = vec3.create();
    for (var index = 0; index < geometry.vertexIndices.length; index++) {
      vec3.copy(vertex, geometry.vertices [geometry.vertexIndices [index]]);
      this.updateBounds(vertex, transformation, lower, upper);
    }
  }
  return new BoundingBox3D(lower, upper);
}

/**
 * @private
 */
ModelManager.prototype.updateBounds = function(vertex, transformation, lower, upper) {
  if (transformation !== null) {
    vec3.transformMat4(vertex, vertex, transformation);
  }
  vec3.min(lower, lower, vertex);
  vec3.max(upper, upper, vertex);
}

/**
 * Returns a transform group that will transform the model node
 * to let it fill a box of the given width centered on the origin.
 * @param {Node3D} node     the root of a model with any size and location
 * @param {Array}  modelRotation the rotation applied to the model before normalization 
 *                 or <code>null</code> if no transformation should be applied to node.
 * @param {number} width    the width of the box
 */
ModelManager.prototype.getNormalizedTransformGroup = function(node, modelRotation, width) {
  return new TransformGroup3D(this.getNormalizedTransform(node, modelRotation, width));
}

/**
 * Returns a transformation matrix that will transform the model node
 * to let it fill a box of the given width centered on the origin.
 * @param {Node3D} node     the root of a model with any size and location
 * @param {?Array} modelRotation the rotation applied to the model before normalization 
 *                 or <code>null</code> if no transformation should be applied to node.
 * @param {number} width    the width of the box
 */
ModelManager.prototype.getNormalizedTransform = function(node, modelRotation, width) {
  // Get model bounding box size 
  var modelBounds = this.getBounds(node);
  var lower = vec3.create();
  modelBounds.getLower(lower);
  var upper = vec3.create();
  modelBounds.getUpper(upper);
  // Translate model to its center
  var translation = mat4.create();
  mat4.translate(translation, translation,
      vec3.fromValues(-lower[0] - (upper[0] - lower[0]) / 2, 
          -lower[1] - (upper[1] - lower[1]) / 2, 
          -lower[2] - (upper[2] - lower[2]) / 2));
  
  var modelTransform;
  if (modelRotation !== undefined && modelRotation !== null) {
    // Get model bounding box size with model rotation
    var modelTransform = this.getRotationTransformation(modelRotation);
    mat4.mul(modelTransform, modelTransform, translation);
    var rotatedModelBounds = this.getBounds(node, modelTransform);
    rotatedModelBounds.getLower(lower);
    rotatedModelBounds.getUpper(upper);
  } else {
    modelTransform = translation;
  }

  // Scale model to make it fill a 1 unit wide box
  var scaleOneTransform = mat4.create();
  mat4.scale(scaleOneTransform, scaleOneTransform,
      vec3.fromValues(width / Math.max(this.getMinimumSize(), upper[0] - lower[0]), 
          width / Math.max(this.getMinimumSize(), upper[1] - lower[1]), 
          width / Math.max(this.getMinimumSize(), upper[2] - lower[2])));
  mat4.mul(scaleOneTransform, scaleOneTransform, modelTransform);
  return scaleOneTransform;
}

/**
 * Returns a transformation matching the given rotation.
 * @param {Array}  modelRotation  the desired rotation.
 */
ModelManager.prototype.getRotationTransformation = function(modelRotation) {
  var modelTransform = mat4.create();
  modelTransform [0] = modelRotation [0][0];
  modelTransform [4] = modelRotation [0][1];
  modelTransform [8] = modelRotation [0][2];
  modelTransform [1] = modelRotation [1][0];
  modelTransform [5] = modelRotation [1][1];
  modelTransform [9] = modelRotation [1][2];
  modelTransform [2] = modelRotation [2][0];
  modelTransform [6] = modelRotation [2][1];
  modelTransform [10] = modelRotation [2][2];
  return modelTransform;
}

/**
 * Returns a transformation able to place in the scene the normalized model 
 * of the given <code>piece</code>.
 * @param {HomePieceOfFurniture} piece   a piece of furniture
 */
ModelManager.prototype.getPieceOFFurnitureNormalizedModelTransformation = function(piece) {
  // Set piece size
  var scale = mat4.create();
  var pieceWidth = piece.getWidth();
  // If piece model is mirrored, inverse its width
  if (piece.isModelMirrored()) {
    pieceWidth *= -1;
  }
  mat4.scale(scale, scale, vec3.fromValues(pieceWidth, piece.getHeight(), piece.getDepth()));
  // Change its angle around y axis
  var orientation = mat4.create();
  mat4.fromYRotation(orientation, -piece.getAngle());
  mat4.mul(orientation, orientation, scale);
  // Translate it to its location
  var pieceTransform = mat4.create();
  var z = piece.getElevation() + piece.getHeight() / 2.;
  if (piece.getLevel() !== null) {
    z += piece.getLevel().getElevation();
  }
  mat4.translate(pieceTransform, pieceTransform, vec3.fromValues(piece.getX(), z, piece.getY()));      
  mat4.mul(pieceTransform, pieceTransform, orientation);
  return pieceTransform;
}

/**
 * Reads a 3D node from content with supported loaders
 * and notifies the loaded model to the given modelObserver once available
 * with its modelUpdated and modelError methods. 
 * @param {URLContent} content an object containing a model
 * @param {boolean} [synchronous] optional parameter equal to false by default
 * @param {{modelUpdated, modelError, progression}} modelObserver  
 *           the observer that will be notified once the model is available
 *           or if an error happens
 */
ModelManager.prototype.loadModel = function(content, synchronous, modelObserver) {
  if (modelObserver === undefined) {
    modelObserver = synchronous;
    synchronous = false;
  }
  var contentUrl = content.getURL();
  if (contentUrl in this.loadedModelNodes) {
    // Notify cached model to observer with a clone of the model
    var model = this.loadedModelNodes [contentUrl];
    modelObserver.modelUpdated(this.cloneNode(model));
  } else {
    if (contentUrl in this.loadingModelObservers) {
      // If observers list exists, content model is already being loaded
      // register observer for future notification
      this.loadingModelObservers [contentUrl].push(modelObserver);
    } else {
      // Create a list of observers that will be notified once content model is loaded
      var observers = [];
      observers.push(modelObserver);
      this.loadingModelObservers [contentUrl] = observers;
      if (!this.modelLoaders) {
        // As model loaders are reentrant, use the same loaders for multiple loading
        this.modelLoaders = [new OBJLoader()];
        // Optional loaders
        if (typeof DAELoader !== "undefined") {
          this.modelLoaders.push(new DAELoader());
        }
        if (typeof Max3DSLoader !== "undefined") {
          this.modelLoaders.push(new Max3DSLoader());
        }
      }
      var modelManager = this;
      var modelObserver = {
          modelLoaderIndex : 0,
          modelLoaded : function(model) {
            var bounds = modelManager.getBounds(model);
            var lower = vec3.create();
            bounds.getLower(lower);
            if (lower [0] !== Infinity) {
              var observers = modelManager.loadingModelObservers [contentUrl];
              if (observers) {
                delete modelManager.loadingModelObservers [contentUrl];
                modelManager.updateWindowPanesTransparency(model);
                modelManager.loadedModelNodes [contentUrl] = model;
                for (var i = 0; i < observers.length; i++) {
                  observers [i].modelUpdated(modelManager.cloneNode(model));
                }
              }
            } else if (++this.modelLoaderIndex < modelManager.modelLoaders.length) {
              modelManager.modelLoaders [this.modelLoaderIndex].load(contentUrl, synchronous, this);
            } else {
              this.modelError("Unsupported 3D format");
            }
          },
          modelError : function(err) {
            var observers = modelManager.loadingModelObservers [contentUrl];
            if (observers) {
              delete modelManager.loadingModelObservers [contentUrl];
              for (var i = 0; i < observers.length; i++) {
                observers [i].modelError(err);
              }
            }
          },
          progression : function(part, info, percentage) {
            var observers = modelManager.loadingModelObservers [contentUrl];
            if (observers) {
              for (var i = 0; i < observers.length; i++) {
                observers [i].progression(part, info, percentage);
              } 
            }
          }
        };
      modelManager.modelLoaders [0].load(contentUrl, synchronous, modelObserver);
    }
  }
}

/**
 * Removes the model matching the given content from the manager. 
 * @param {URLContent} content an object containing a model
 * @param {boolean}    disposeGeometries if <code>true</code> model geometries will be disposed too
 */
ModelManager.prototype.unloadModel = function(content, disposeGeometries) {
  var contentUrl = content.getURL();
  var modelRoot = this.loadedModelNodes [contentUrl];
  delete this.loadedModelNodes [contentUrl];
  delete this.loadingModelObservers [contentUrl];
  if (disposeGeometries) {
    this.disposeGeometries(modelRoot);
  }
}

/**
 * Frees geometry data of the given <code>node</code>.
 * @param {Node3D} node  the root of a model
 * @package 
 */
ModelManager.prototype.disposeGeometries = function(node) {
  if (node instanceof Group3D) {
    for (var i = 0; i < node.children.length; i++) {
      this.disposeGeometries(node.children [i]);
    }
  } else if (node instanceof Link3D) {
    // Not a problem to dispose more than once geometries of a shared group
    this.disposeGeometries(node.getSharedGroup());
  } else if (node instanceof Shape3D) {
    var geometries = node.getGeometries();
    for (var i = 0; i < geometries.length; i++) {
      geometries [i].disposeCoordinates(); 
    }
  }
}

/**
 * Returns a clone of the given <code>node</code>.
 * All the children and the attributes of the given node are duplicated except the geometries 
 * and the texture images of shapes.
 * @param {Node3D} node  the root of a model 
 */
ModelManager.prototype.cloneNode = function(node, clonedSharedGroups) {
  if (clonedSharedGroups === undefined) {
    return this.cloneNode(node, []);
  } else {
    var clonedNode = node.clone();
    if (node instanceof Shape3D) {
      var clonedAppearance;
      if (node.getAppearance()) {
        clonedNode.setAppearance(node.getAppearance().clone());
      }
    } else if (node instanceof Link3D) {
      var clonedLink = node.clone();
      // Force duplication of shared groups too if not duplicated yet
      var sharedGroup = clonedLink.getSharedGroup();
      if (sharedGroup !== null) {
        var clonedSharedGroup = null;
        for (var i = 0; i < clonedSharedGroups.length; i++) {
          if (clonedSharedGroups [i].sharedGroup === sharedGroup) {
            clonedSharedGroup = clonedSharedGroups [i].clonedSharedGroup;
            break;
          }
        }
        if (clonedSharedGroup === null) {
          clonedSharedGroup = this.cloneNode(sharedGroup, clonedSharedGroups);
          clonedSharedGroups.push({sharedGroup : sharedGroup, 
                                   clonedSharedGroup : clonedSharedGroup});          
        }
        clonedLink.setSharedGroup(clonedSharedGroup);
      }
      return clonedLink;
    } else {
      clonedNode = node.clone();
      if (node instanceof Group3D) {
        var children = node.getChildren();
        for (var i = 0; i < children.length; i++) {
          var clonedChild = this.cloneNode(children [i], clonedSharedGroups);
          clonedNode.addChild(clonedChild);
        }
      }
    }
    return clonedNode;
  }
}

/**
 * Updates the transparency of window panes shapes.
 * @private
 */
ModelManager.prototype.updateWindowPanesTransparency = function(node) {
  if (node instanceof Group3D) {
    for (var i = 0; i < node.children.length; i++) {
      this.updateWindowPanesTransparency(node.children [i]);
    }
  } else if (node instanceof Link3D) {
    this.updateWindowPanesTransparency(node.getSharedGroup());
  } else if (node instanceof Shape3D) {
    var name = node.getName();
    if (name && name.indexOf(ModelManager.WINDOW_PANE_SHAPE_PREFIX) === 0) {
      var appearance = node.getAppearance();
      if (appearance === null) {
        appearance = new Appearance3D();
        node.setAppearance(appearance);
      }
      if (appearance.getTransparency() === undefined) {
        appearance.setTransparency(0.5);
      }
    }
  }
}

/**
 * Returns the shape matching the given cut out shape if not <code>null</code> 
 * or the 2D area of the 3D shapes children of the <code>node</code> 
 * projected on its front side. The returned area is normalized in a 1 unit square
 * centered at the origin.
 */
ModelManager.prototype.getFrontArea = function(cutOutShape, node) {
  var frontArea; 
  if (cutOutShape != null) {
    frontArea = new java.awt.geom.Area(this.getShape(cutOutShape));
    frontArea.transform(java.awt.geom.AffineTransform.getScaleInstance(1, -1));
    frontArea.transform(java.awt.geom.AffineTransform.getTranslateInstance(-0.5, 0.5));
  } else {
    var vertexCount = this.getVertexCount(node);
    if (vertexCount < 1000000) {
      var frontAreaWithHoles = new java.awt.geom.Area();
      this.computeBottomOrFrontArea(node, frontAreaWithHoles, mat4.create(), false, false);
      frontArea = new java.awt.geom.Area();
      var currentPathPoints = ([]);
      var previousRoomPoint = null;
      for (var it = frontAreaWithHoles.getPathIterator(null, 1); !it.isDone(); it.next()) {
        var areaPoint = [0, 0];
        switch ((it.currentSegment(areaPoint))) {
          case java.awt.geom.PathIterator.SEG_MOVETO :
          case java.awt.geom.PathIterator.SEG_LINETO :
            if (previousRoomPoint === null 
                || areaPoint[0] !== previousRoomPoint[0] 
                || areaPoint[1] !== previousRoomPoint[1]) {
              currentPathPoints.push(areaPoint);
            }
            previousRoomPoint = areaPoint;
            break;
          case java.awt.geom.PathIterator.SEG_CLOSE :
            if (currentPathPoints[0][0] === previousRoomPoint[0] 
                && currentPathPoints[0][1] === previousRoomPoint[1]) {
              currentPathPoints.splice(currentPathPoints.length - 1, 1);
            }
            if (currentPathPoints.length > 2) {
              var pathPoints = currentPathPoints.slice(0);
              var subRoom = new Room(pathPoints);
              if (subRoom.getArea() > 0) {
                if (!subRoom.isClockwise()) {
                  var currentPath = new java.awt.geom.GeneralPath();
                  currentPath.moveTo(pathPoints[0][0], pathPoints[0][1]);
                  for (var i = 1; i < pathPoints.length; i++) {
                    currentPath.lineTo(pathPoints[i][0], pathPoints[i][1]);
                  }
                  currentPath.closePath();
                  frontArea.add(new java.awt.geom.Area(currentPath));
                }
              }
            }
            currentPathPoints.length = 0;
            previousRoomPoint = null;
            break;
        }
      }
      var bounds = frontAreaWithHoles.getBounds2D();
      frontArea.transform(java.awt.geom.AffineTransform.getTranslateInstance(-bounds.getCenterX(), -bounds.getCenterY()));
      frontArea.transform(java.awt.geom.AffineTransform.getScaleInstance(1 / bounds.getWidth(), 1 / bounds.getHeight()));
    }
    else {
      frontArea = new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(-0.5, -0.5, 1, 1));
    }
  }
  return frontArea;
}

/**
 * Returns the 2D area of the 3D shapes children of the given scene 3D <code>node</code>
 * projected on the floor (plan y = 0), or of the given staircase if <code>node</code> is an
 * instance of <code>HomePieceOfFurniture</code>.
 * @param {Node3D|HomePieceOfFurniture} node
 * @return {Area}
 */
ModelManager.prototype.getAreaOnFloor = function(node) {
  if (node instanceof Node3D) {
    var modelAreaOnFloor;
    var vertexCount = this.getVertexCount(node);
    if (vertexCount < 10000) {
      modelAreaOnFloor = new java.awt.geom.Area();
      this.computeBottomOrFrontArea(node, modelAreaOnFloor, mat4.create(), true, true);
    } else {
      var vertices = [];
      this.computeVerticesOnFloor(node, vertices, mat4.create());
      var surroundingPolygon = this.getSurroundingPolygon(vertices.slice(0));
      var generalPath = new java.awt.geom.GeneralPath(java.awt.geom.Path2D.WIND_NON_ZERO, surroundingPolygon.length);
      generalPath.moveTo(surroundingPolygon[0][0], surroundingPolygon[0][1]);
      for (var i = 0; i < surroundingPolygon.length; i++) {
        generalPath.lineTo(surroundingPolygon[i][0], surroundingPolygon[i][1]);
      }
      generalPath.closePath();
      modelAreaOnFloor = new java.awt.geom.Area(generalPath);
    }
    return modelAreaOnFloor;
  } else {
    var staircase = node;
    if (staircase.getStaircaseCutOutShape() === null) {
      throw new IllegalArgumentException("No cut out shape associated to piece");
    }
    var shape = this.getShape(staircase.getStaircaseCutOutShape());
    var staircaseArea = new java.awt.geom.Area(shape);
    if (staircase.isModelMirrored()) {
      staircaseArea = this.getMirroredArea(staircaseArea);
    }
    var staircaseTransform = java.awt.geom.AffineTransform.getTranslateInstance(
            staircase.getX() - staircase.getWidth() / 2, 
            staircase.getY() - staircase.getDepth() / 2);
    staircaseTransform.concatenate(java.awt.geom.AffineTransform.getRotateInstance(staircase.getAngle(), 
            staircase.getWidth() / 2, staircase.getDepth() / 2));
    staircaseTransform.concatenate(java.awt.geom.AffineTransform.getScaleInstance(staircase.getWidth(), staircase.getDepth()));
    staircaseArea.transform(staircaseTransform);
    return staircaseArea;
  }
}

/**
 * Returns the total count of vertices in all geometries.
 * @param {Node3D} node
 * @return {number}
 * @private
 */
ModelManager.prototype.getVertexCount = function(node) {
  var count = 0;
  if (node instanceof Group3D) {
    var children = node.getChildren();
    for (var i = 0; i < children.length; i++) {
      count += this.getVertexCount(children [i]);
    }
  } else if (node instanceof Link3D) {
    count = this.getVertexCount(node.getSharedGroup());
  } else if (node instanceof Shape3D) {
    var appearance = node.getAppearance();
    if (appearance.isVisible()) {
      var geometries = node.getGeometries(); 
      for (var i = 0, n = geometries.length; i < n; i++) {
        var geometry = geometries[i];
        count += geometry.vertices.length;
      }
    }
  }
  return count;
}

/**
 * Computes the 2D area on floor or on front side of the 3D shapes children of <code>node</code>.
 * @param {Node3D} node
 * @param {Area} nodeArea
 * @param {mat4} parentTransformations
 * @param {boolean} ignoreTransparentShapes
 * @param {boolean} bottom
 * @private
 */
ModelManager.prototype.computeBottomOrFrontArea = function(node, nodeArea, parentTransformations, ignoreTransparentShapes, bottom) {
  if (node instanceof Group3D) {
    if (node instanceof TransformGroup3D) {
      parentTransformations = mat4.create();
      var transform = mat4.create();
      node.getTransform(transform);
      mat4.mul(parentTransformations, parentTransformations, transform);
    }
    var children = node.getChildren();
    for (var i = 0; i < children.length; i++) {
      this.computeBottomOrFrontArea(children [i], nodeArea, parentTransformations, ignoreTransparentShapes, bottom);
    }
  } else if (node instanceof Link3D) {
    this.computeBottomOrFrontArea(node.getSharedGroup(), nodeArea, parentTransformations, ignoreTransparentShapes, bottom);
  } else if (node instanceof Shape3D) {
    var appearance = node.getAppearance();
    if (appearance.isVisible() && (!ignoreTransparentShapes || appearance.getTransparency() < 1)) {
      var geometries = node.getGeometries(); 
      for (var i = 0, n = geometries.length; i < n; i++) {
        var geometry = geometries[i];
        this.computeBottomOrFrontGeometryArea(geometry, nodeArea, parentTransformations, bottom);
      }
    }
  }
}

/**
 * Computes the bottom area of a 3D geometry if <code>bottom</code> is <code>true</code>,
 * and the front area if not.
 * @param {IndexedGeometryArray3D} geometryArray
 * @param {Area} nodeArea
 * @param {mat4} parentTransformations
 * @param {boolean} bottom
 * @private
 */
ModelManager.prototype.computeBottomOrFrontGeometryArea = function(geometryArray, nodeArea, parentTransformations, bottom) {
  var vertexCount = geometryArray.vertices.length;
  var vertices = new Array(vertexCount * 2);
  var vertex = vec3.create();
  for (var index = 0, i = 0; index < vertices.length; i++) {
    vec3.copy(vertex, geometryArray.vertices[i]);
    vec3.transformMat4(vertex, vertex, parentTransformations);
    vertices[index++] = vertex[0];
    if (bottom) {
      vertices[index++] = vertex[2];
    } else {
      vertices[index++] = vertex[1];
    }
  }

  geometryPath = new java.awt.geom.GeneralPath(java.awt.geom.Path2D.WIND_NON_ZERO, 1000);
  for (var i = 0, triangleIndex = 0, n = geometryArray.vertexIndices.length; i < n; i += 3) {
    this.addTriangleToPath(geometryArray, geometryArray.vertexIndices [i], geometryArray.vertexIndices [i + 1], geometryArray.vertexIndices [i + 2], vertices, geometryPath, triangleIndex, nodeArea);
  }
  nodeArea.add(new java.awt.geom.Area(geometryPath));
}

/**
 * Adds to <code>nodePath</code> the triangle joining vertices at
 * vertexIndex1, vertexIndex2, vertexIndex3 indices,
 * only if the triangle has a positive orientation.
 * @param {javax.media.j3d.GeometryArray} geometryArray
 * @param {number} vertexIndex1
 * @param {number} vertexIndex2
 * @param {number} vertexIndex3
 * @param {Array} vertices
 * @param {GeneralPath} geometryPath
 * @param {number} triangleIndex
 * @param {Area} nodeArea
 * @private
 */
ModelManager.prototype.addTriangleToPath = function(geometryArray, vertexIndex1, vertexIndex2, vertexIndex3, vertices, geometryPath, triangleIndex, nodeArea) {
  var xVertex1 = vertices[2 * vertexIndex1];
  var yVertex1 = vertices[2 * vertexIndex1 + 1];
  var xVertex2 = vertices[2 * vertexIndex2];
  var yVertex2 = vertices[2 * vertexIndex2 + 1];
  var xVertex3 = vertices[2 * vertexIndex3];
  var yVertex3 = vertices[2 * vertexIndex3 + 1];
  if ((xVertex2 - xVertex1) * (yVertex3 - yVertex2) - (yVertex2 - yVertex1) * (xVertex3 - xVertex2) > 0) {
    if (triangleIndex > 0 && triangleIndex % 1000 === 0) {
      nodeArea.add(new java.awt.geom.Area(geometryPath));
      geometryPath.reset();
    }
    geometryPath.moveTo(xVertex1, yVertex1);
    geometryPath.lineTo(xVertex2, yVertex2);
    geometryPath.lineTo(xVertex3, yVertex3);
    geometryPath.closePath();
  }
}

/**
 * Computes the vertices coordinates projected on floor of the 3D shapes children of <code>node</code>.
 * @param {Node3D} node
 * @param {Array} vertices
 * @param {mat4} parentTransformations
 * @private
 */
ModelManager.prototype.computeVerticesOnFloor = function (node, vertices, parentTransformations) {
  if (node instanceof Group3D) {
    if (node instanceof TransformGroup3D) {
      parentTransformations = mat4.create();
      var transform = mat4.create();
      node.getTransform(transform);
      mat4.mul(parentTransformations, parentTransformations, transform);
    }
    var children = node.getChildren();
    for (var i = 0; i < children.length; i++) {
      this.computeVerticesOnFloor(children [i], vertices, parentTransformations);
    }
  } else if (node instanceof Link3D) {
    this.computeVerticesOnFloor(node.getSharedGroup(), vertices, parentTransformations);
  } else if (node instanceof Shape3D) {
    var appearance = node.getAppearance();
    if (appearance.isVisible() && (!ignoreTransparentShapes || appearance.getTransparency() < 1)) {
      var geometries = node.getGeometries(); 
      for (var i = 0, n = geometries.length; i < n; i++) {
        var geometryArray = geometries[i];
        var vertexCount = geometryArray.vertices.length;
        var vertices = new Array(vertexCount * 2);
        var vertex = vec3.create();
        for (var index = 0, j = 0; index < vertexCount; j++, index++) {
          vec3.copy(vertex, geometryArray.vertices[j]);
          vec3.transformMat4(vertex, vertex, parentTransformations);
          vertices.push([vertex[0], vertex[2]]);
        }
      }
    }
  }
}

/**
 * Returns the convex polygon that surrounds the given <code>vertices</code>.
 * From Andrew's monotone chain 2D convex hull algorithm described at
 * http://softsurfer.com/Archive/algorithm%5F0109/algorithm%5F0109.htm
 * @param {Array} vertices
 * @return {Array}
 * @private
 */
ModelManager.prototype.getSurroundingPolygon = function (vertices) {
  vertices.sort(function (vertex1, vertex2) {
      var testedValue;
      if (vertex1[0] === vertex2[0]) {
        testedValue = vertex2[1] - vertex1[1];
      } else {
        testedValue = vertex2[0] - vertex1[0];
      }
      if (testedValue > 0) {
        return 1;
      } else if (testedValue < 0) {
        return -1;
      } else {
        return 0;
      }
    });
  var polygon = new Array(vertices.length);
  var bottom = 0;
  var top = -1;
  var i;
  
  var minMin = 0;
  var minMax;
  var xmin = vertices[0][0];
  for (i = 1; i < vertices.length; i++) {
    if (vertices[i][0] !== xmin) {
      break;
    }
  }
  minMax = i - 1;
  if (minMax === vertices.length - 1) {
    polygon[++top] = vertices[minMin];
    if (vertices[minMax][1] !== vertices[minMin][1]) {
      polygon[++top] = vertices[minMax];
    }
    polygon[++top] = vertices[minMin];
    var surroundingPolygon = new Array(top + 1);
    System.arraycopy(polygon, 0, surroundingPolygon, 0, surroundingPolygon_1.length);
    return surroundingPolygon;
  }
  
  var maxMin;
  var maxMax = vertices.length - 1;
  var xMax = vertices[vertices.length - 1][0];
  for (i = vertices.length - 2; i >= 0; i--) {
    if (vertices[i][0] !== xMax) {
      break;
    }
  }
  maxMin = i + 1;
  
  polygon[++top] = vertices[minMin];
  i = minMax;
  while ((++i <= maxMin)) {
    if (this.isLeft(vertices[minMin], vertices[maxMin], vertices[i]) >= 0 && i < maxMin) {
      continue;
    }
    while ((top > 0)) {
      if (this.isLeft(polygon[top - 1], polygon[top], vertices[i]) > 0)
        break;
      else
        top--;
    }
    polygon[++top] = vertices[i];
  }

  if (maxMax !== maxMin) {
    polygon[++top] = vertices[maxMax];
  }
  bottom = top;
  i = maxMin;
  while ((--i >= minMax)) {
    if (this.isLeft(vertices[maxMax], vertices[minMax], vertices[i]) >= 0 && i > minMax) {
      continue;
    }
    while ((top > bottom)) {
      if (this.isLeft(polygon[top - 1], polygon[top], vertices[i]) > 0) {
        break;
      }
      else {
        top--;
      }
    }
    polygon[++top] = vertices[i];
  }
  if (minMax !== minMin) {
    polygon[++top] = vertices[minMin];
  }
  var surroundingPolygon = new Array(top + 1);
  System.arraycopy(polygon, 0, surroundingPolygon, 0, surroundingPolygon.length);
  return surroundingPolygon;
}

ModelManager.prototype.isLeft = function(vertex0, vertex1, vertex2) {
  return (vertex1[0] - vertex0[0]) * (vertex2[1] - vertex0[1]) - (vertex2[0] - vertex0[0]) * (vertex1[1] - vertex0[1]);
}

/**
 * Returns the mirror area of the given <code>area</code>.
 * @param {Area} area
 * @return {Area}
 * @private
 */
ModelManager.prototype.getMirroredArea = function (area) {
  var mirrorPath = new java.awt.geom.GeneralPath();
  var point = [0, 0, 0, 0, 0, 0];
  for (var it = area.getPathIterator(null); !it.isDone(); it.next()) {
    switch ((it.currentSegment(point))) {
    case java.awt.geom.PathIterator.SEG_MOVETO :
      mirrorPath.moveTo(1 - point[0], point[1]);
      break;
    case java.awt.geom.PathIterator.SEG_LINETO :
      mirrorPath.lineTo(1 - point[0], point[1]);
      break;
    case java.awt.geom.PathIterator.SEG_QUADTO :
      mirrorPath.quadTo(1 - point[0], point[1], 1 - point[2], point[3]);
      break;
    case java.awt.geom.PathIterator.SEG_CUBICTO :
      mirrorPath.curveTo(1 - point[0], point[1], 1 - point[2], point[3], 1 - point[4], point[5]);
      break;
    case java.awt.geom.PathIterator.SEG_CLOSE :
      mirrorPath.closePath();
      break;
    }
  }
  return new java.awt.geom.Area(mirrorPath);
}

/**
 * Returns the shape matching the given <a href="http://www.w3.org/TR/SVG/paths.html">SVG path shape</a>.
 * @param {string} svgPathShape
 * @return {Shape}
 */
ModelManager.prototype.getShape = function(svgPathShape) {
  var shape2D = this.parsedShapes [svgPathShape];
  if (!shape2D) {
    shape2D = new java.awt.geom.Rectangle2D.Float(0, 0, 1, 1);
    try {
      var pathProducer = new org.apache.batik.parser.AWTPathProducer();
      var pathParser = new org.apache.batik.parser.PathParser();
      pathParser.setPathHandler(pathProducer);
      pathParser.parse(svgPathShape);
      shape2D = pathProducer.getShape();
    } catch (ex) {
      // Keep default value if Batik is not available or if the path is incorrect
    }
    this.parsedShapes[svgPathShape] = shape2D;
  }
  return shape2D;
}
