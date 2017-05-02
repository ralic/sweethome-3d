/*
 * ModelPreviewComponent.js
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

// Requires gl-matrix-min.js
//          scene3d.js
//          ModelManager.js
//          HTMLCanvas3D.js

/**
 * Creates a model preview component.
 * @param {string} canvasId  the ID of the 3D canvas where a model will be viewed
 * @param {boolean} pitchAndScaleChangeSupported if <code>true</code> the component 
 *           will handles events to let the user rotate the displayed model
 * @constructor
 * @author Emmanuel Puybaret
 */
function ModelPreviewComponent(canvasId, pitchAndScaleChangeSupported) {
  this.canvas3D = new HTMLCanvas3D(canvasId);
  this.setDefaultTransform();
  
  if (pitchAndScaleChangeSupported) {
    var ANGLE_FACTOR = 0.015;
    var ZOOM_FACTOR = 0.02;
    var MIN_VIEW_PITCH = -Math.PI / 2;
    var previewComponent = this;
    var userActionsListener = {
        buttonPressed : -1,
        pointerTouches : {},
        
        rotationUpdater : function(x, y, altKey) {
          if (userActionsListener.buttonPressed === 0) {
            if (altKey) {
              userActionsListener.zoomUpdater(userActionsListener.yLastMove - y);
            } else {
              previewComponent.viewYaw = previewComponent.viewYaw - ANGLE_FACTOR * (x - userActionsListener.xLastMove);
              previewComponent.viewPitch = Math.max(MIN_VIEW_PITCH, Math.min(0, previewComponent.viewPitch - ANGLE_FACTOR * (y - userActionsListener.yLastMove)));
              previewComponent.updateViewPlatformTransform();
            }
          }
        },
        zoomUpdater : function(delta) {
          previewComponent.viewScale = Math.max(0.5, Math.min(1.3, previewComponent.viewScale * Math.exp(delta * ZOOM_FACTOR)));
          previewComponent.stopRotationAnimation();
          previewComponent.updateViewPlatformTransform();
        },
        mousePressed : function(ev) {
          userActionsListener.xLastMove = ev.clientX;
          userActionsListener.yLastMove = ev.clientY;
          userActionsListener.buttonPressed = ev.button;
          userActionsListener.mousePressedInCanvas = true;
          previewComponent.stopRotationAnimation();
          ev.stopPropagation();
        },
        windowMouseMoved : function(ev) {
          if (userActionsListener.mousePressedInCanvas) {
            userActionsListener.rotationUpdater(ev.clientX, ev.clientY, ev.altKey);
            userActionsListener.xLastMove = ev.clientX;
            userActionsListener.yLastMove = ev.clientY;
          }
        },
        windowMouseReleased : function(ev) {
          userActionsListener.buttonPressed = -1;
          if (userActionsListener.mousePressedInCanvas) {
            delete userActionsListener.mousePressedInCanvas;
          }
        },
        pointerPressed : function(ev) {
          if (ev.pointerType == "mouse") {
            userActionsListener.mousePressed(ev);
          } else {
            // Multi touch support for IE and Edge
            userActionsListener.copyPointerToTargetTouches(ev);
            userActionsListener.touchStarted(ev);
          }
        },
        pointerMousePressed : function(ev) {
          ev.stopPropagation();
        },
        windowPointerMoved : function(ev) {
          if (ev.pointerType == "mouse") {
            userActionsListener.windowMouseMoved(ev);
          } else {
            // Multi touch support for IE and Edge
            userActionsListener.copyPointerToTargetTouches(ev);
            userActionsListener.touchMoved(ev);
          }
        },
        windowPointerReleased : function(ev) {
          if (ev.pointerType == "mouse") {
            userActionsListener.windowMouseReleased(ev);
          } else {
            delete userActionsListener.pointerTouches [ev.pointerId];
            userActionsListener.touchEnded(ev);
          }
        },
        touchStarted : function(ev) {
          ev.preventDefault();
          if (ev.targetTouches.length == 1) {
            userActionsListener.buttonPressed = 0;
            userActionsListener.xLastMove = ev.targetTouches [0].pageX;
            userActionsListener.yLastMove = ev.targetTouches [0].pageY;
          } else if (ev.targetTouches.length == 2) {
            userActionsListener.distanceLastPinch = userActionsListener.distance(
                ev.targetTouches [0], ev.targetTouches [1]);
          }
          previewComponent.stopRotationAnimation();
        },
        touchMoved : function(ev) {
          ev.preventDefault();
          if (ev.targetTouches.length == 1) {
            var x = ev.targetTouches [0].pageX;
            var y = ev.targetTouches [0].pageY;
            userActionsListener.rotationUpdater(x, y, false);
            userActionsListener.xLastMove = x;
            userActionsListener.yLastMove = y;
          } else if (ev.targetTouches.length == 2) {
            var newDistance = userActionsListener.distance(ev.targetTouches [0], ev.targetTouches [1]);
            var scale = userActionsListener.distanceLastPinch / newDistance;
            previewComponent.viewScale = Math.max(0.5, Math.min(1.3, previewComponent.viewScale * scale));
            previewComponent.updateViewPlatformTransform();
            userActionsListener.distanceLastPinch = newDistance;
          }
        },
        touchEnded : function(ev) {
          userActionsListener.buttonPressed = -1;
        },
        copyPointerToTargetTouches : function (ev) {
          // Copy the IE and Edge pointer location to ev.targetTouches
          userActionsListener.pointerTouches [ev.pointerId] = {pageX : ev.clientX, pageY : ev.clientY};
          ev.targetTouches = [];
          for (var attribute in userActionsListener.pointerTouches) {
            if (userActionsListener.pointerTouches.hasOwnProperty(attribute)) {
              ev.targetTouches.push(userActionsListener.pointerTouches [attribute]);
            }
          }
        },
        distance : function(p1, p2) {
          return Math.sqrt(Math.pow(p2.pageX - p1.pageX, 2) + Math.pow(p2.pageY - p1.pageY, 2));
        },
        mouseScrolled : function(ev) {
          ev.preventDefault();
          userActionsListener.zoomUpdater(ev.detail);
        },
        mouseWheelMoved : function(ev) {
          ev.preventDefault();
          userActionsListener.zoomUpdater(ev.deltaY !== undefined ? ev.deltaY / 2 : -ev.wheelDelta / 3);
        },
        visibilityChanged : function(ev) {
          if (document.visibilityState == "hidden") {
            previewComponent.stopRotationAnimation();
          }
        }
      };
      
    if (window.PointerEvent) {
      // Multi touch support for IE and Edge
      this.canvas3D.getCanvas().addEventListener("pointerdown", userActionsListener.pointerPressed);
      this.canvas3D.getCanvas().addEventListener("mousedown", userActionsListener.pointerMousePressed);
      // Add pointermove and pointerup event listeners to window to capture pointer events out of the canvas 
      window.addEventListener("pointermove", userActionsListener.windowPointerMoved);
      window.addEventListener("pointerup", userActionsListener.windowPointerReleased);
    } else {
      this.canvas3D.getCanvas().addEventListener("touchstart", userActionsListener.touchStarted);
      this.canvas3D.getCanvas().addEventListener("touchmove", userActionsListener.touchMoved);
      this.canvas3D.getCanvas().addEventListener("touchend", userActionsListener.touchEnded);
      this.canvas3D.getCanvas().addEventListener("mousedown", userActionsListener.mousePressed);
      // Add mousemove and mouseup event listeners to window to capture mouse events out of the canvas 
      window.addEventListener("mousemove", userActionsListener.windowMouseMoved);
      window.addEventListener("mouseup", userActionsListener.windowMouseReleased);
    }
    this.canvas3D.getCanvas().addEventListener("DOMMouseScroll", userActionsListener.mouseScrolled);
    this.canvas3D.getCanvas().addEventListener("mousewheel", userActionsListener.mouseWheelMoved);
    document.addEventListener("visibilitychange", userActionsListener.visibilityChanged);
    this.userActionsListener = userActionsListener;
  }
}

/**
 * @private
 */
ModelPreviewComponent.prototype.setDefaultTransform = function() {
  this.viewYaw = Math.PI / 8;
  this.viewPitch = -Math.PI / 8; 
  this.viewScale = 1;
  this.updateViewPlatformTransform();
}

/**
 * @private
 */
ModelPreviewComponent.prototype.updateViewPlatformTransform = function() {
  // Default distance used to view a 2 unit wide scene
  var nominalDistanceToCenter = 1.4 / Math.tan(Math.PI / 8);  
  var translation = mat4.create();
  mat4.translate(translation, translation, vec3.fromValues(0, 0, nominalDistanceToCenter));
  var pitchRotation = mat4.create();
  mat4.rotateX(pitchRotation, pitchRotation, this.viewPitch);
  var yawRotation = mat4.create();
  mat4.rotateY(yawRotation, yawRotation, this.viewYaw);
  var scale = mat4.create();
  mat4.scale(scale, scale, vec3.fromValues(this.viewScale, this.viewScale, this.viewScale));
  
  mat4.mul(pitchRotation, pitchRotation, translation);
  mat4.mul(yawRotation, yawRotation, pitchRotation);
  mat4.mul(scale, scale, yawRotation);
  this.canvas3D.setViewPlatformTransform(scale);
}

/**
 * Loads and displays the given 3D model.
 * @param {URLContent} content a content with a URL pointing to a 3D model to parse and view
 * @param {Array} modelRotation  a 3x3 array describing how to transform the 3D model
 * @param onerror       callback called in case of error while reading the model
 * @param onprogression callback to follow the reading of the model
 */
ModelPreviewComponent.prototype.setModel = function(content, modelRotation, onerror, onprogression) {
  this.content = content;
  this.canvas3D.clear();          
  var previewComponent = this;
  ModelManager.getInstance().loadModel(content,
      {
        modelUpdated : function(model) {
          if (content === previewComponent.content) {
            // Place model at origin in a box as wide as the canvas
            var modelManager = ModelManager.getInstance();
            var modelTransform = modelRotation  
                ? modelManager.getRotationTransformation(modelRotation)  
                : mat4.create();
            var size = modelManager.getSize(model);
            var scaleFactor = 1.8 / Math.max(Math.max(size[0], size[1]), size[2]);
            mat4.scale(modelTransform, modelTransform, vec3.fromValues(scaleFactor, scaleFactor, scaleFactor));
            mat4.scale(modelTransform, modelTransform, size);
            mat4.mul(modelTransform, modelTransform, modelManager.getNormalizedTransform(model, null, 1));
            
            var transformGroup = new TransformGroup3D(modelTransform); 
            transformGroup.addChild(model);
            var scene = new BranchGroup3D(); 
            scene.addChild(transformGroup);
            // Add lights
            scene.addChild(new DirectionalLight3D(vec3.fromValues(0.9, 0.9, 0.9), vec3.fromValues(1.732, -0.8, -1)));
            scene.addChild(new DirectionalLight3D(vec3.fromValues(0.9, 0.9, 0.9), vec3.fromValues(-1.732, -0.8, -1))); 
            scene.addChild(new DirectionalLight3D(vec3.fromValues(0.9, 0.9, 0.9), vec3.fromValues(0, -0.8, 1)));
            scene.addChild(new DirectionalLight3D(vec3.fromValues(0.66, 0.66, 0.66), vec3.fromValues(0, 1, 0)));
            scene.addChild(new AmbientLight3D(vec3.fromValues(0.2, 0.2, 0.2))); 
            
            previewComponent.setDefaultTransform();
            previewComponent.canvas3D.setScene(scene, onprogression);
            previewComponent.canvas3D.updateViewportSize();
          }
        },
        modelError : function(err) {
          if (content === previewComponent.content
              && onerror !== undefined) {
            onerror(err);
          }
        },
        progression : function(part, info, percentage) {
          if (content === previewComponent.content
              && onprogression !== undefined) {
            onprogression(part, info, percentage);
          }
        }
      });
}

/**
 * Stops rotation animation and clears buffers used by its canvas.
 */
ModelPreviewComponent.prototype.clear = function() {
  this.stopRotationAnimation();
  this.canvas3D.clear();
}

/**
 * Removes listeners bound to global objects and clears this component.
 * This method should be called to free resources in the browser when this component is not needed anymore.
 */
ModelPreviewComponent.prototype.dispose = function() {
  if (window.PointerEvent) {
    window.removeEventListener("pointermove", this.userActionsListener.windowPointerMoved);
    window.removeEventListener("pointerup", this.userActionsListener.windowPointerReleased);
  } else {
    window.removeEventListener("mousemove", this.userActionsListener.windowMouseMoved);
    window.removeEventListener("mouseup", this.userActionsListener.windowMouseReleased);
  }
  document.removeEventListener("visibilitychange", this.userActionsListener.visibilityChanged);
  this.clear();
}

/**
 * Starts rotation animation.
 * @param {number} [roundsPerMinute]  the rotation speed in rounds per minute, 5rpm if missing
 */
ModelPreviewComponent.prototype.startRotationAnimation = function(roundsPerMinute) {
  this.roundsPerMinute = roundsPerMinute !== undefined ? roundsPerMinute : 5;
  if (!this.rotationAnimationStarted) {
    this.rotationAnimationStarted = true;
    this.animate();
  }
}

/**
 * @private
 */
ModelPreviewComponent.prototype.animate = function() {
  if (this.rotationAnimationStarted) {
    var now = Date.now();
    if (this.lastRotationAnimationTime !== undefined) {
      var angularSpeed = this.roundsPerMinute * 2 * Math.PI / 60000; 
      this.viewYaw += ((now - this.lastRotationAnimationTime) * angularSpeed) % (2 * Math.PI);
      this.updateViewPlatformTransform();
    }
    this.lastRotationAnimationTime = now;
    var previewComponent = this;
    requestAnimationFrame(
        function() {
          previewComponent.animate();
        });
  }
}

/**
 * Stops the running rotation animation.
 */
ModelPreviewComponent.prototype.stopRotationAnimation = function() {
  delete this.lastRotationAnimationTime;
  delete this.rotationAnimationStarted;
}
