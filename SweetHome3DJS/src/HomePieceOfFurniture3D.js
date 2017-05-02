/*
 * HomePieceOfFurniture3D.js
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

// Requires scene3d.js
//          Object3DBranch.js
//          ModelManager.js
//          HomeObject.js
//          HomePieceOfFurniture.js


/**
 * Creates the 3D piece matching the given home <code>piece</code>.
 * @param {HomePieceOfFurniture} piece
 * @param {Home} home
 * @param {boolean} waitModelAndTextureLoadingEnd
 * @constructor
 * @extends Object3DBranch
 * @author Emmanuel Puybaret
 */
function HomePieceOfFurniture3D(piece, home, waitModelAndTextureLoadingEnd) {
  Object3DBranch.call(this);
  this.setUserData(piece);      
  this.home = home;
  
  this.createPieceOfFurnitureNode(piece, waitModelAndTextureLoadingEnd);
}
HomePieceOfFurniture3D.prototype = Object.create(Object3DBranch.prototype);
HomePieceOfFurniture3D.prototype.constructor = HomePieceOfFurniture3D;

HomePieceOfFurniture3D.DEFAULT_BOX = new Object();

/**
 * Creates the piece node with its transform group and add it to the piece branch. 
 * @private
 */
HomePieceOfFurniture3D.prototype.createPieceOfFurnitureNode = function(piece, waitModelAndTextureLoadingEnd) {
  var pieceTransformGroup = new TransformGroup3D();
  pieceTransformGroup.setCapability(Group3D.ALLOW_CHILDREN_EXTEND);
  pieceTransformGroup.setCapability(TransformGroup3D.ALLOW_TRANSFORM_WRITE);
  this.addChild(pieceTransformGroup);
  
  // While loading model use a temporary node that displays a white box  
  var waitBranch = new BranchGroup3D();
  var normalization = new TransformGroup3D();
  normalization.addChild(this.getModelBox(vec3.fromValues(1, 1, 1)));
  waitBranch.addChild(normalization);      
  pieceTransformGroup.addChild(waitBranch);
  
  // Set piece model initial location, orientation and size      
  this.updatePieceOfFurnitureTransform();
  
  // Load piece real 3D model
  var model = piece.getModel();
  var piece3D = this;
  ModelManager.getInstance().loadModel(model, waitModelAndTextureLoadingEnd, {
      modelUpdated : function(modelRoot) {
        var modelRotation = piece.getModelRotation();
        // Add piece model scene to a normalized transform group
        var modelTransformGroup = ModelManager.getInstance().getNormalizedTransformGroup(modelRoot, modelRotation, 1);
        piece3D.updatePieceOfFurnitureModelNode(modelRoot, modelTransformGroup, waitModelAndTextureLoadingEnd);            
      },        
      modelError : function(ex) {
        // In case of problem use a default red box
        piece3D.updatePieceOfFurnitureModelNode(piece3D.getModelBox(vec3.fromValues(1, 0, 0)), 
            new TransformGroup3D(), waitModelAndTextureLoadingEnd);            
      },
      progression : function() {
      }
    });
}

/**
 * Updates this branch from the home piece it manages.
 */
HomePieceOfFurniture3D.prototype.update = function() {
  this.updatePieceOfFurnitureTransform();
  this.updatePieceOfFurnitureModelMirrored();
  this.updatePieceOfFurnitureColorAndTexture(false);      
  this.updatePieceOfFurnitureVisibility();      
}

/**
 * Sets the transformation applied to piece model to match
 * its location, its angle and its size.
 * @private
 */
HomePieceOfFurniture3D.prototype.updatePieceOfFurnitureTransform = function() {
  var pieceTransform = ModelManager.getInstance().
      getPieceOFFurnitureNormalizedModelTransformation(this.getUserData());
  // Change model transformation      
  this.getChild(0).setTransform(pieceTransform);
}

/**
 * Sets the color and the texture applied to piece model.
 * @private
 */
HomePieceOfFurniture3D.prototype.updatePieceOfFurnitureColorAndTexture = function(waitTextureLoadingEnd) {
  var piece = this.getUserData();
  var modelNode = this.getModelNode();
  var modelChild = modelNode.getChild(0);
  if (modelChild.getUserData() !== HomePieceOfFurniture3D.DEFAULT_BOX) {
    if (piece.getColor() !== null) {
      this.setColorAndTexture(modelNode, piece.getColor(), null, piece.getShininess(), null, false, 
          null, null, []);
    } else if (piece.getTexture() !== null) {
      this.setColorAndTexture(modelNode, null, piece.getTexture(), piece.getShininess(), null, waitTextureLoadingEnd,
          vec3.fromValues(piece.getWidth(), piece.getHeight(), piece.getDepth()), ModelManager.getInstance().getBounds(modelChild),
          []);
    } else if (piece.getModelMaterials() !== null) {
      this.setColorAndTexture(modelNode, null, null, null, piece.getModelMaterials(), waitTextureLoadingEnd,
          vec3.fromValues(piece.getWidth(), piece.getHeight(), piece.getDepth()), ModelManager.getInstance().getBounds(modelChild), 
          []);
    } else {
      // Set default material and texture of model
      this.setColorAndTexture(modelNode, null, null, piece.getShininess(), null, false, null, null, []);
    }
  }
}

/**
 * Returns the node of the filled model.
 * @private
 */
HomePieceOfFurniture3D.prototype.getModelNode = function() {
  var transformGroup = this.getChild(0);
  var branchGroup = transformGroup.getChild(0);
  return branchGroup.getChild(0);
}

/**
 * Sets whether this piece model is visible or not.
 * @private
 */
HomePieceOfFurniture3D.prototype.updatePieceOfFurnitureVisibility = function() {
  var piece = this.getUserData();
  // Update visibility of filled model shapes
  var visible = piece.isVisible() 
      && (piece.getLevel() === null
          || piece.getLevel().isViewableAndVisible()); 
  var materials = piece.getColor() === null && piece.getTexture() === null
      ? piece.getModelMaterials()
      : null;
  this.setVisible(this.getModelNode(), visible, materials);
}

/**
 * Sets whether this piece model is mirrored or not.
 * @private
 */
HomePieceOfFurniture3D.prototype.updatePieceOfFurnitureModelMirrored = function() {
  var piece = this.getUserData();
  // Cull front or back model faces whether its model is mirrored or not
  this.setCullFace(this.getModelNode(), piece.isModelMirrored(), piece.isBackFaceShown());
}

/**
 * Updates transform group children with <code>modelMode</code>.
 * @private
 */
HomePieceOfFurniture3D.prototype.updatePieceOfFurnitureModelNode = function(modelNode, normalization, waitTextureLoadingEnd) {    
  normalization.addChild(modelNode);
  // Add model node to branch group
  var modelBranch = new BranchGroup3D();
  modelBranch.addChild(normalization);

  var transformGroup = this.getChild(0);
  // Remove previous nodes    
  transformGroup.removeAllChildren();
  // Add model branch to live scene
  transformGroup.addChild(modelBranch);

  // Flip normals if back faces of model are shown
  if (this.getUserData().isBackFaceShown()) {
    this.setBackFaceNormalFlip(this.getModelNode(), true);
  }
  // Update piece color, visibility and model mirror in dispatch thread as
  // these attributes may be changed in that thread
  this.updatePieceOfFurnitureModelMirrored();
  this.updatePieceOfFurnitureColorAndTexture(waitTextureLoadingEnd);      
  this.updatePieceOfFurnitureVisibility();
  if (this.getUserData().isDoorOrWindow()) {
    this.setTransparentShapeNotPickable(this);
  }
}

/**
 * Returns a box that may replace model. 
 * @private
 */
HomePieceOfFurniture3D.prototype.getModelBox = function(color) {
  var boxAppearance = new Appearance3D();
  boxAppearance.setDiffuseColor(color);
  boxAppearance.setAmbientColor(vec3.scale(vec3.create(), color, 0.7));
  var box = new Box3D(0.5, 0.5, 0.5, boxAppearance);
  box.setUserData(HomePieceOfFurniture3D.DEFAULT_BOX);
  return box;
}

/**
 * Sets the material and texture attribute of all <code>Shape3D</code> children nodes of <code>node</code> 
 * from the given <code>color</code> and <code>texture</code>. 
 * @private
 */
HomePieceOfFurniture3D.prototype.setColorAndTexture = function(node, color, texture, shininess, materials, waitTextureLoadingEnd, 
                                                               pieceSize, modelBounds, modifiedAppearances) {
  if (node instanceof Group3D) {
    // Set material and texture of all children
    var children = node.getChildren(); 
    for (var i = 0; i < children.length; i++) {
      this.setColorAndTexture(children [i], color, 
          texture, shininess, materials, waitTextureLoadingEnd, pieceSize,
          modelBounds, modifiedAppearances);
    }
  } else if (node instanceof Link3D) {
    this.setColorAndTexture(node.getSharedGroup(), color,
        texture, shininess, materials, waitTextureLoadingEnd, pieceSize,
        modelBounds, modifiedAppearances);
  } else if (node instanceof Shape3D) {
    var shape = node;
    var shapeName = shape.getName();
    var appearance = shape.getAppearance();
    if (appearance === null) {
      appearance = new Appearance3D();
      node.setAppearance(appearance);
    }
    
    // Check appearance wasn't already changed
    if (modifiedAppearances.indexOf(appearance) === -1) {
      var defaultAppearance = null;
      var colorModified = color !== null;
      var textureModified = !colorModified 
          && texture !== null;
      var materialModified = !colorModified
          && !textureModified
          && materials !== null && materials.length > 0;
      var appearanceModified = colorModified            
          || textureModified
          || materialModified
          || shininess !== null;
      var windowPane = shapeName !== null
          && shapeName.indexOf(ModelManager.WINDOW_PANE_SHAPE_PREFIX) === 0;
      if (!windowPane && appearanceModified            
          || windowPane && materialModified) {
        // Store shape default appearance 
        // (global color or texture change doesn't have effect on window panes)
        if (appearance.defaultAppearance === undefined) {
          appearance.defaultAppearance = appearance.clone();
        }
        defaultAppearance = appearance.defaultAppearance;
      }
      var materialShininess = 0.;
      if (appearanceModified) {
        materialShininess = shininess !== null
            ? shininess
            : (appearance.getSpecularColor() !== undefined
                && appearance.getShininess() !== undefined
                ? appearance.getShininess() / 128
                : 0);
      }
      if (colorModified) {
        // Change color only of shapes that are not window panes
        if (windowPane) {
          this.restoreDefaultAppearance(appearance, null);
        } else {
          // Change material if no default texture is displayed on the shape
          // (textures always keep the colors of their image file)
          this.updateAppearanceMaterial(appearance, color, color, materialShininess);
          if (defaultAppearance.getTransparency() !== undefined) {
            appearance.setTransparency(defaultAppearance.getTransparency());
          }
          if (defaultAppearance.getCullFace() !== undefined) {
            appearance.setCullFace(defaultAppearance.getCullFace());
          }
          if (defaultAppearance.getBackFaceNormalFlip() !== undefined) {
            appearance.setBackFaceNormalFlip(defaultAppearance.getBackFaceNormalFlip());
          }
          appearance.setTextureCoordinatesGeneration(defaultAppearance.getTextureCoordinatesGeneration());
          appearance.setTextureImage(null);
        }
      } else if (textureModified) {            
        // Change texture only of shapes that are not window panes
        if (windowPane) {
          this.restoreDefaultAppearance(appearance, null);
        } else {
          appearance.setTextureCoordinatesGeneration(this.getTextureCoordinates(appearance, texture, pieceSize, modelBounds));
          this.updateTextureTransform(appearance, texture, true);
          this.updateAppearanceMaterial(appearance, Object3DBranch.DEFAULT_COLOR, Object3DBranch.DEFAULT_AMBIENT_COLOR, materialShininess);
          TextureManager.getInstance().loadTexture(texture.getImage(), 0, 
              waitTextureLoadingEnd, this.getTextureObserver(appearance));
        }
      } else if (materialModified) {
        var materialFound = false;
        // Apply color, texture and shininess of the material named as appearance name
        for (var i = 0; i < materials.length; i++) {
          var material = materials [i];
          if (material !== null
              && (material.getKey() != null
                      && material.getKey() == appearance.getName()
                  || material.getKey() == null
                      && material.getName() == appearance.getName())) {
            if (material.getShininess() !== null) {
              materialShininess = material.getShininess();
            }
            color = material.getColor();                
            if (color !== null
                && (color & 0xFF000000) != 0) {
              this.updateAppearanceMaterial(appearance, color, color, materialShininess);
              if (defaultAppearance.getTransparency() !== undefined) {
                appearance.setTransparency(defaultAppearance.getTransparency());
              }
              if (defaultAppearance.getCullFace() !== undefined) {
                appearance.setCullFace(defaultAppearance.getCullFace());
              }
              if (defaultAppearance.getBackFaceNormalFlip() !== undefined) {
                appearance.setBackFaceNormalFlip(defaultAppearance.getBackFaceNormalFlip());
              }
              appearance.setTextureImage(null);
            } else if (color === null && material.getTexture() !== null) {
              var materialTexture = material.getTexture();
              if (this.isTexturesCoordinatesDefined(shape)) {
                this.restoreDefaultTextureCoordinatesGeneration(appearance);
                this.updateTextureTransform(appearance, materialTexture);
              } else {
                appearance.setTextureCoordinatesGeneration(this.getTextureCoordinates(appearance, material.getTexture(), pieceSize, modelBounds));
                this.updateTextureTransform(appearance, materialTexture, true);
              }
              this.updateAppearanceMaterial(appearance, Object3DBranch.DEFAULT_COLOR, Object3DBranch.DEFAULT_AMBIENT_COLOR, materialShininess);
              var materialTexture = material.getTexture();
              TextureManager.getInstance().loadTexture(materialTexture.getImage(), 0, 
                  waitTextureLoadingEnd, this.getTextureObserver(appearance));
            } else {
              this.restoreDefaultAppearance(appearance, material.getShininess());
            }
            materialFound = true;
            break;
          }
        }
        if (!materialFound) {
          this.restoreDefaultAppearance(appearance, null);
        }
      } else {
        this.restoreDefaultAppearance(appearance, shininess);
      }
      // Store modified appearances to avoid changing their values more than once
      modifiedAppearances.push(appearance);
    }
  }
}

/**
 * Returns a texture observer that will update the given <code>appearance</code>.
 * @private
 */
HomePieceOfFurniture3D.prototype.getTextureObserver = function(appearance) {
  return {
      textureUpdated : function(textureImage) {
        if (TextureManager.getInstance().isTextureTransparent(textureImage)) {
          appearance.setCullFace(Appearance3D.CULL_NONE);
        } else {
          var defaultAppearance = appearance.defaultAppearance;
          if (defaultAppearance !== null
              && defaultAppearance.getCullFace() !== null) {
            appearance.setCullFace(defaultAppearance.getCullFace());
          }
        }
        if (appearance.getTextureImage() !== textureImage) {
          appearance.setTextureImage(textureImage);
        }
      },
      textureError : function(error) {
        return this.textureUpdated(TextureManager.getInstance().getErrorImage());
      },
      progression : function(part, info, percentage) {
      }
    };
}

/**
 * Returns a texture coordinates generator that wraps the given texture on front face.
 * @private
 */
HomePieceOfFurniture3D.prototype.getTextureCoordinates = function(appearance, texture, pieceSize, modelBounds) {
  var lower = vec3.create();
  modelBounds.getLower(lower);
  var upper = vec3.create();
  modelBounds.getUpper(upper);
  var minimumSize = ModelManager.getInstance().getMinimumSize();
  var sx = pieceSize [0] / Math.max(upper [0] - lower [0], minimumSize);
  var sw = -lower [0] * sx;
  var ty = pieceSize [1] / Math.max(upper [1] - lower [1], minimumSize);
  var tz = pieceSize [2] / Math.max(upper [2] - lower [2], minimumSize);
  var tw = -lower [1] * ty + upper [2] * tz;
  return {planeS : vec4.fromValues(sx, 0, 0, sw), 
          planeT : vec4.fromValues(0, ty, -tz, tw)};
}

/**
 * Returns <code>true</code> if all the geometries of the given <code>shape</code> define some texture coordinates.
 * @private
 */
HomePieceOfFurniture3D.prototype.isTexturesCoordinatesDefined = function(shape) {
  var geometries = shape.getGeometries();
  for (var i = 0, n = geometries.length; i < n; i++) {
    if (!geometries [i].hasTextureCoordinates()) {
      return false;
    }
  }
  return true;
}

/**
 * Restores default material and texture of the given <code>appearance</code>.
 * @private
 */
HomePieceOfFurniture3D.prototype.restoreDefaultAppearance = function(appearance, shininess) {
  if (appearance.defaultAppearance !== undefined) {
    var defaultAppearance = appearance.defaultAppearance;
    if (defaultAppearance.getAmbientColor() !== undefined) {
      appearance.setAmbientColor(defaultAppearance.getAmbientColor());
    }
    if (defaultAppearance.getDiffuseColor() !== undefined) {
      appearance.setDiffuseColor(defaultAppearance.getDiffuseColor());
      appearance.setSpecularColor(vec3.fromValues(shininess, shininess, shininess));
      appearance.setShininess(shininess * 128);
    }
    if (defaultAppearance.getTransparency() !== undefined) {
      appearance.setTransparency(defaultAppearance.getTransparency());
    }
    if (appearance.getCullFace() !== undefined) {
      appearance.setCullFace(defaultAppearance.getCullFace());
      appearance.setBackFaceNormalFlip(defaultAppearance.isBackFaceNormalFlip());
    }
    if (defaultAppearance.getTextureCoordinatesGeneration() !== undefined) {
      appearance.setTextureCoordinatesGeneration(defaultAppearance.getTextureCoordinatesGeneration());
    }
    if (appearance.getTextureImage() !== undefined) {
      appearance.setTextureImage(defaultAppearance.getTextureImage());
    }
  }
}

/**
 * Restores default texture coordinates generation of the given <code>appearance</code>.
 * @private
 */
HomePieceOfFurniture3D.prototype.restoreDefaultTextureCoordinatesGeneration = function(appearance) {
  if (appearance.defaultAppearance !== undefined) {
    var defaultAppearance = appearance.defaultAppearance;
    if (defaultAppearance.getTextureCoordinatesGeneration() !== undefined) {
      appearance.setTextureCoordinatesGeneration(defaultAppearance.getTextureCoordinatesGeneration());
    }
  }
}

/**
 * Sets the visible attribute of the <code>Shape3D</code> children nodes of <code>node</code>.
 * @private
 */
HomePieceOfFurniture3D.prototype.setVisible = function(node, visible, materials) {
  if (node instanceof Group3D) {
    // Set visibility of all children
    var children = node.getChildren(); 
    for (var i = 0; i < children.length; i++) {
      this.setVisible(children [i], visible, materials);
    }
  } else if (node instanceof Link3D) {
    this.setVisible(node.getSharedGroup(), visible, materials);
  } else if (node instanceof Shape3D) {
    var shape = node;
    var appearance = shape.getAppearance();
    if (appearance === null) {
      appearance = new Appearance3D();
      node.setAppearance(appearance);
    }
    var shapeName = shape.getName();
    if (visible 
        && shapeName !== null
        && shapeName.indexOf(ModelManager.LIGHT_SHAPE_PREFIX) === 0
        && this.home !== null
        && !this.isSelected(this.home.getSelectedItems())
        && (typeof HomeLight === "undefined"
            || this.getUserData() instanceof HomeLight)) {
      // Don't display light sources shapes of unselected lights
      visible = false;
    }
    
    if (visible
        && materials !== null) {
      // Check whether the material color used by this shape isn't invisible 
      for (var i = 0; i < materials.length; i++) {
        var material = materials [i];
        if (material !== null 
            && material.getName() == appearance.getName()) {
          var color = material.getColor();  
          visible = color === null
              || (color & 0xFF000000) !== 0;
          break;
        }
      }
    }  

    // Change visibility
    appearance.setVisible(visible);
  } 
} 

/**
 * Returns <code>true</code> if this piece of furniture belongs to <code>selectedItems</code>.
 * @private
 */
HomePieceOfFurniture3D.prototype.isSelected = function(selectedItems) {
  for (var i = 0; i < selectedItems.length; i++) {
    var item = selectedItems [i];
    if (item === this.getUserData()
        || (item instanceof HomeFurnitureGroup
            && this.isSelected(item.getFurniture()))) {
      return true;
    }
  }
  return false;
}

/**
 * Sets the cull face of all <code>Shape3D</code> children nodes of <code>node</code>.
 * @param cullFace <code>Appearance3D.CULL_FRONT</code> or <code>Appearance3D.CULL_BACK</code>
 * @private
 */
HomePieceOfFurniture3D.prototype.setCullFace = function(node, mirrored, backFaceShown) {
  if (node instanceof Group3D) {
    // Set cull face of all children
    var children = node.getChildren(); 
    for (var i = 0; i < children.length; i++) {
      this.setCullFace(children [i], mirrored, backFaceShown);
    }
  } else if (node instanceof Link3D) {
    this.setCullFace(node.getSharedGroup(), mirrored, backFaceShown);
  } else if (node instanceof Shape3D) {
    var appearance = node.getAppearance();
    if (appearance === null) {
      appearance = new Appearance3D();
      node.setAppearance(appearance);
    }
    // Change cull face 
    if (appearance.getCullFace() !== Appearance3D.CULL_NONE) {
      var cullFace = appearance.getCullFace() !== undefined 
          ? appearance.getCullFace()
          : Appearance3D.CULL_BACK;
      var defaultCullFace = appearance.defaultCullFace; 
      if (defaultCullFace === undefined) {
        appearance.defaultCullFace = (defaultCullFace = cullFace);
      }
      appearance.setCullFace((mirrored ^ backFaceShown ^ defaultCullFace === Appearance3D.CULL_FRONT)
          ? Appearance3D.CULL_FRONT 
          : Appearance3D.CULL_BACK);
    }
  }
}

/**
 * Sets whether all <code>Shape3D</code> children nodes of <code>node</code> should have 
 * their normal flipped or not.
 * Caution !!! Should be executed only once per instance 
 * @param backFaceNormalFlip <code>true</code> if normals should be flipped.
 * @private
 */
HomePieceOfFurniture3D.prototype.setBackFaceNormalFlip = function(node, backFaceNormalFlip) {
  if (node instanceof Group3D) {
    // Set back face normal flip of all children
    var children = node.getChildren(); 
    for (var i = 0; i < children.length; i++) {
      this.setBackFaceNormalFlip(children [i], backFaceNormalFlip);
    }
  } else if (node instanceof Link3D) {
    this.setBackFaceNormalFlip(node.getSharedGroup(), backFaceNormalFlip);
  } else if (node instanceof Shape3D) {
    var appearance = node.getAppearance();
    if (appearance === null) {
      appearance = new Appearance3D();
      node.setAppearance(appearance);
    }
    // Change back face normal flip
    appearance.setBackFaceNormalFlip(
        backFaceNormalFlip ^ appearance.getCullFace() === Appearance3D.CULL_FRONT);
  }
}

/**
 * Cancels the pickability of the <code>Shape3D</code> children nodes of <code>node</code> 
 * when it uses a transparent appearance. 
 * @private
 */
HomePieceOfFurniture3D.prototype.setTransparentShapeNotPickable = function(node) {
  if (node instanceof Group3D) {
    var children = node.getChildren(); 
    for (var i = 0; i < children.length; i++) {
      this.setTransparentShapeNotPickable(children [i]);
    }
  } else if (node instanceof Link3D) {
    this.setTransparentShapeNotPickable(node.getSharedGroup());
  } else if (node instanceof Shape3D) {
    var appearance = node.getAppearance();
    if (appearance !== null
        && appearance.getTransparency() > 0) {
      node.setPickable(false);
    }
  }
}

