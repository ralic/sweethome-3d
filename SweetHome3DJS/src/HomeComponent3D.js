/*
 * HomeComponent3D.js
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

// Requires Home.js
//          HomeObject.js
//          HomePieceOfFurniture.js
//          scene3d.js
//          ModelManager.js
//          HomePieceOfFurniture3D.js
//          HomeController3D.js
//          HTMLCanvas3D.js

/**
 * Creates a 3D component that displays <code>home</code> walls, rooms and furniture.
 * @param {string} canvasId the id of the HTML canvas associated to this component
 * @param {Home} home the home to display in this component
 * @param {UserPreferences} preferences user preferences
 * @param {Object3DBranchFactory} object3dFactory a factory able to create 3D objects from <code>home</code> items
 *            or <code>null</code> to use default one.
 *            The <code>createObject3D</code> method of this factory is expected to return 
 *            an instance of {@link Object3DBranch} in current implementation.
 * @param {HomeController3D} controller the controller that manages modifications in <code>home</code> (optional).
 * @constructor   
 * @author Emmanuel Puybaret
 */
function HomeComponent3D(canvasId, home, preferences, object3dFactory, controller) {
  this.home = home;
  this.object3dFactory = object3dFactory !== null 
      ? object3dFactory
      : new Object3DBranchFactory();
  this.homeObjects = [];
  this.defaultLights = [];
  this.camera = null;
  this.windowSizeListener = null;
  // Listeners bound to home that updates 3D scene objects
  this.cameraChangeListener = null;
  this.homeCameraListener = null;
  this.groundChangeListener = null;
  this.backgroundChangeListener = null;
  this.lightColorListener = null;
  this.elevationChangeListener = null;
  this.wallsAlphaListener = null;
  this.levelListener = null;
  this.levelChangeListener = null;
  this.wallListener = null;
  this.wallChangeListener = null;  
  this.furnitureListener = null;
  this.furnitureChangeListener = null;
  this.roomListener = null;
  this.roomChangeListener = null;
  this.labelListener = null;
  this.labelChangeListener = null;
  this.approximateHomeBoundsCache = null;
  this.createComponent3D(canvasId, preferences, controller);
}

/**
 * Creates the 3D canvas associated to the given <code>canvasId</code>.
 * @private 
 */
HomeComponent3D.prototype.createComponent3D = function(canvasId, preferences, controller) {
  this.canvas3D = new HTMLCanvas3D(canvasId);
  if (controller) {
    this.addMouseListeners(controller, this.canvas3D);
    if (preferences !== null) {
      this.navigationPanelId = this.createNavigationPanel(this.home, preferences, controller);
      this.setNavigationPanelVisible(preferences.isNavigationPanelVisible());
      var component3D = this;
      preferences.addPropertyChangeListener("NAVIGATION_PANEL_VISIBLE",
          function(ev) {
            component3D.setNavigationPanelVisible(ev.getNewValue());
          });
    }
    this.createActions(controller);
    this.installKeyboardActions();
  }

  // Update field of view from current camera
  this.updateView(this.home.getCamera(), this.home.getTopCamera() === this.home.getCamera());
  // Update point of view from current camera
  this.updateViewPlatformTransform(this.home.getCamera(), false);
  // Add camera listeners to update later point of view from camera
  this.addCameraListeners();
  
  this.canvas3D.setScene(this.createSceneTree(true, false));
}

/**
 * Disposes the 3D shapes geometries displayed by this component. 
 * @package
 * @ignore
 */
HomeComponent3D.prototype.disposeGeometries = function() {
  if (this.home.structure) {
    ModelManager.getInstance().unloadModel(this.home.structure, true);
  }
  ModelManager.getInstance().disposeGeometries(this.canvas3D.getScene());
} 

/**
 * Returns the id of a component displayed as navigation panel upon this 3D view.
 * @private
 */
HomeComponent3D.prototype.createNavigationPanel = function(home, preferences, controller) {
  // Retrieve body elements with a data-simulated-key attribute
  var simulatedKeys = this.getSimulatedKeyElements(document.getElementsByTagName("body") [0]);
  var navigationPanelDiv = null;
  var innerHtml;
  try {
    innerHtml = preferences.getLocalizedString(HomeComponent3D, "navigationPanel.innerHTML");
  } catch (ex) {
    innerHtml = 
          '<img src="' + ZIPTools.getScriptFolder("gl-matrix-min.js") + '/navigationPanel.png"'
        + '     style="width: 56px; height:59px; margin:5px; user-drag: none; user-select: none; -moz-user-select: none; -webkit-user-drag: none; -webkit-user-select: none; -ms-user-select: none;"' 
        + '     usemap="#navigationPanelMap"/>'
        + '<map name="navigationPanelMap" id="navigationPanelMap">'
        + '  <area shape="poly" coords="19,13,28,2,36,13,31,13,31,18,24,18,24,13" data-simulated-key="UP" />'
        + '  <area shape="poly" coords="3,28,13,20,13,25,18,25,18,32,13,32,13,37" data-simulated-key="LEFT" />'
        + '  <area shape="poly" coords="24,40,31,40,31,45,36,45,27,55,19,45,24,45" data-simulated-key="DOWN" />'
        + '  <area shape="poly" coords="37,25,42,25,42,20,52,28,42,37,42,32,37,31" data-simulated-key="RIGHT" />'
        + '  <area shape="poly" coords="20,28,28,19,35,28" data-simulated-key="PAGE_UP" />'
        + '  <area shape="poly" coords="20,30,36,30,28,39" data-simulated-key="PAGE_DOWN" />'
        + '</map>';
  }
  var component3D = this;
  if (innerHtml !== null) {
    navigationPanelDiv = document.createElement("div")
    navigationPanelDiv.setAttribute("id", "div" + Math.floor(Math.random() * 1E10));
    navigationPanelDiv.style.position = "absolute";
    var canvas = this.canvas3D.getCanvas();
    this.windowSizeListener = function() {
        var canvasBounds = canvas.getBoundingClientRect();
        navigationPanelDiv.style.left = (canvasBounds.left + window.pageXOffset) + "px";
        navigationPanelDiv.style.top = (canvasBounds.top + window.pageYOffset) + "px";
        component3D.canvas3D.updateViewportSize();
      };
    window.addEventListener("resize", this.windowSizeListener);
    this.windowSizeListener();
    // Search the first existing zIndex among parents
    var parentZIndex = 0;
    for (var element = this.canvas3D.getCanvas();  
         element && element.style && isNaN(parentZIndex = parseInt(element.style.zIndex));
         element = element.parentElement) {
    }
    navigationPanelDiv.style.zIndex = isNaN(parentZIndex) ? "1" : (parentZIndex + 1).toString();
    navigationPanelDiv.style.visibility = "hidden";
    navigationPanelDiv.innerHTML = innerHtml;
    simulatedKeys.push.apply(simulatedKeys, this.getSimulatedKeyElements(navigationPanelDiv));
    var bodyElement = document.getElementsByTagName("body") [0];
    bodyElement.insertBefore(navigationPanelDiv, bodyElement.firstChild);
    // Redirect mouse clicks out of div elements to this component 
    navigationPanelDiv.addEventListener("mousedown", 
        function(ev) {
          component3D.userActionsListener.mousePressed(ev);
        });
  }
  
  this.simulatedElementMousePressedListener = function(ev) {
      var simulatedElement = ev.target;
      var repeatKeyAction = function() {
          var attribute = simulatedElement.getAttribute("data-simulated-key");
          component3D.callAction(attribute.substring(attribute.indexOf(":") + 1), ev);
        };
      var stopInterval = function(ev) {
          window.clearInterval(intervalId);
          simulatedElement.removeEventListener("mouseup", stopInterval);
          simulatedElement.removeEventListener("mouseleave", stopInterval);
          component3D.userActionsListener.windowMouseReleased(ev);
          ev.stopPropagation();
        };
      simulatedElement.addEventListener("mouseup", stopInterval);
      simulatedElement.addEventListener("mouseleave", stopInterval);
      repeatKeyAction();
      var intervalId = window.setInterval(repeatKeyAction, 80);
    };
  for (var i = 0; i < simulatedKeys.length; i++) {
    // Add a listener that simulates the given key and repeats it until mouse is released 
    simulatedKeys [i].addEventListener("mousedown", this.simulatedElementMousePressedListener);
  }

  if (navigationPanelDiv !== null) {
    return navigationPanelDiv.getAttribute("id");
  } else {
    return null;
  }
}

/**
 * Returns the child elements with a <code>data-simulated-key</code> attribute set.
 * @package
 * @ignore
 */
HomeComponent3D.prototype.getSimulatedKeyElements = function(element) {
  var simulatedKeyElements = [];
  if (element.hasChildNodes()) {
    for (var i = 0; i < element.childNodes.length; i++) {
      var child = element.childNodes [i];
      if (child.hasAttribute
          && child.hasAttribute("data-simulated-key")) {
        // Take into account only components with a data-simulated-key attribute 
        // that contains no colon or that starts with canvas id followed by a colon
        var simulatedKey = child.getAttribute("data-simulated-key");
        if (simulatedKey.indexOf(":") === -1
            || simulatedKey.indexOf(this.canvas3D.getCanvas().getAttribute("id") + ":") === 0) {
          simulatedKeyElements.push(child);
        }
      }
      simulatedKeyElements.push.apply(simulatedKeyElements, this.getSimulatedKeyElements(child));
    }
  }
  return simulatedKeyElements;
}

/**
 * Sets the image that will be drawn upon the 3D component shown by this component.
 * @private
 */
HomeComponent3D.prototype.setNavigationPanelVisible = function(visible) {
  if (this.navigationPanelId != null) {
    document.getElementById(this.navigationPanelId).style.visibility = visible ? "visible" : "hidden";
  }
}

/**
 * Returns the closest home item displayed at client coordinates (x, y). 
 * @param {number} x
 * @param {number} y
 * @returns {Object}
 * @since 1.1
 */
HomeComponent3D.prototype.getClosestItemAt = function(x, y) {
  var node = this.canvas3D.getClosestShapeAt(x, y);
  while (node !== null
         && !(node instanceof Object3DBranch)) {
    node = node.getParent();
  }
  if (node != null) {
    return node.getUserData();
  } else {
    return null;
  }
}

/**
 * Remove all listeners bound to home that updates 3D scene objects.
 * @private 
 */
HomeComponent3D.prototype.removeHomeListeners = function() {
  this.home.removePropertyChangeListener("CAMERA", this.homeCameraListener);
  var homeEnvironment = this.home.getEnvironment();
  homeEnvironment.removePropertyChangeListener("SKY_COLOR", this.backgroundChangeListener);
  homeEnvironment.removePropertyChangeListener("SKY_TEXTURE", this.backgroundChangeListener);
  homeEnvironment.removePropertyChangeListener("GROUND_COLOR", this.backgroundChangeListener);
  homeEnvironment.removePropertyChangeListener("GROUND_TEXTURE", this.backgroundChangeListener);
  homeEnvironment.removePropertyChangeListener("GROUND_COLOR", this.groundChangeListener);
  homeEnvironment.removePropertyChangeListener("GROUND_TEXTURE", this.groundChangeListener);
  homeEnvironment.removePropertyChangeListener("LIGHT_COLOR", this.lightColorListener);
  homeEnvironment.removePropertyChangeListener("WALLS_ALPHA", this.wallsAlphaListener);
  this.home.getCamera().removePropertyChangeListener(this.cameraChangeListener);
  this.home.removePropertyChangeListener("CAMERA", this.elevationChangeListener);
  this.home.getCamera().removePropertyChangeListener(this.elevationChangeListener);
  this.home.removeLevelsListener(this.levelListener);
  var levels = this.home.getLevels();
  for (var i = 0; i < levels.length; i++) {
    levels[i].removePropertyChangeListener(this.levelChangeListener);
  }
  this.home.removeWallsListener(this.wallListener);
  var walls = this.home.getWalls();
  for (var i = 0; i < walls.length; i++) {
    walls[i].removePropertyChangeListener(this.wallChangeListener);
  }
  this.home.removeFurnitureListener(this.furnitureListener);
  var furniture = this.home.getFurniture();
  for (var i = 0; i < furniture.length; i++) {
    var piece = furniture [i];
    piece.removePropertyChangeListener(this.furnitureChangeListener);
    if (piece instanceof HomeFurnitureGroup) {
      var groupFurniture = piece.getAllFurniture();
      for (var j = 0; j < groupFurniture.length; j++) {
        groupFurniture [j].removePropertyChangeListener(this.furnitureChangeListener);
      }
    }
  }
  this.home.removeRoomsListener(this.roomListener);
  var rooms = this.home.getRooms();
  for (var i = 0; i < rooms.length; i++) {
    rooms[i].removePropertyChangeListener(this.roomChangeListener);
  }
  this.home.removeLabelsListener(this.labelListener);
  var labels = this.home.getLabels();
  for (var i = 0; i < labels.length; i++) {
    labels[i].removePropertyChangeListener(this.labelChangeListener);
  }
}

/**
 * Remove all mouse listeners bound to the canvas3D and window.
 * @private 
 */
HomeComponent3D.prototype.removeMouseListeners = function(canvas3D) {
  if (this.userActionsListener) {
    if (window.PointerEvent) {
      // Multi touch support for IE and Edge
      canvas3D.getCanvas().removeEventListener("pointerdown", this.userActionsListener.pointerPressed);
      canvas3D.getCanvas().removeEventListener("mousedown", this.userActionsListener.pointerMousePressed);
      window.removeEventListener("pointermove", this.userActionsListener.windowPointerMoved);
      window.removeEventListener("pointerup", this.userActionsListener.windowPointerReleased);
    } else {
      canvas3D.getCanvas().removeEventListener("touchstart", this.userActionsListener.touchStarted);
      canvas3D.getCanvas().removeEventListener("touchmove", this.userActionsListener.touchMoved);
      canvas3D.getCanvas().removeEventListener("touchend", this.userActionsListener.touchEnded);
      canvas3D.getCanvas().removeEventListener("mousedown", this.userActionsListener.mousePressed);
      window.removeEventListener("mousemove", this.userActionsListener.windowMouseMoved);
      window.removeEventListener("mouseup", this.userActionsListener.windowMouseReleased);
    }
    canvas3D.getCanvas().removeEventListener("DOMMouseScroll", this.userActionsListener.mouseScrolled);
    canvas3D.getCanvas().removeEventListener("mousewheel", this.userActionsListener.mouseWheelMoved);
  }
}

/**
 * Frees listeners and canvas data.
 */
HomeComponent3D.prototype.dispose = function() {
  this.removeHomeListeners();
  this.removeMouseListeners(this.canvas3D);
  if (this.navigationPanelId != null) {
    window.removeEventListener("resize", this.windowSizeListener);
    var simulatedKeys = this.getSimulatedKeyElements(document.getElementsByTagName("body") [0]);
    for (var i = 0; i < simulatedKeys.length; i++) {
      simulatedKeys [i].removeEventListener("mousedown", this.simulatedElementMousePressedListener);
    }
    var navigationPanel = document.getElementById(this.navigationPanelId);
    navigationPanel.parentElement.removeChild(navigationPanel);
  }
  this.canvas3D.clear();
}

/**
 * Adds listeners to home to update point of view from current camera.
 * @private 
 */
HomeComponent3D.prototype.addCameraListeners = function() {
  var component3D = this;
  var home = this.home;
  this.cameraChangeListener = function(ev) {
      // Update view transform later to let finish camera changes  
      setTimeout(
          function() {
            if (component3D.canvas3D) {
              component3D.updateView(home.getCamera(), home.getTopCamera() === home.getCamera());
              component3D.updateViewPlatformTransform(home.getCamera(), true);
            }
          }, 0);
    };
  home.getCamera().addPropertyChangeListener(this.cameraChangeListener);
  this.homeCameraListener = function(ev) {
      component3D.updateView(home.getCamera(), home.getTopCamera() === home.getCamera());
      component3D.updateViewPlatformTransform(home.getCamera(), false);
      // Add camera change listener to new active camera
      ev.getOldValue().removePropertyChangeListener(component3D.cameraChangeListener);
      home.getCamera().addPropertyChangeListener(component3D.cameraChangeListener);
    };
  this.home.addPropertyChangeListener("CAMERA", this.homeCameraListener);
}

/**
 * Updates <code>view</code> from <code>camera</code> field of view.
 * @private 
 */
HomeComponent3D.prototype.updateView = function(camera, topCamera) {
  var fieldOfView = camera.getFieldOfView();
  if (fieldOfView === 0) {
    fieldOfView = Math.PI * 63 / 180;
  }
  this.canvas3D.setFieldOfView(fieldOfView);
  var frontClipDistance;
  var backClipDistance;
  if (topCamera) {
    var approximateHomeBounds = this.getApproximateHomeBoundsCache();
    if (approximateHomeBounds === null) {
      frontClipDistance = 5;
    } else {
      var lower = vec3.create();
      approximateHomeBounds.getLower(lower);
      var upper = vec3.create();
      approximateHomeBounds.getUpper(upper);
      // Use a variable front clip distance for top camera depending on the distance to home objects center
      frontClipDistance = 1 + Math.sqrt(Math.pow((lower [0] + upper [0]) / 2 - camera.getX(), 2) 
          + Math.pow((lower [1] + upper [1]) / 2 - camera.getY(), 2) 
          + Math.pow((lower [2] + upper [2]) / 2 - camera.getZ(), 2)) / 100;
    }
    // It's recommended to keep ratio between back and front clip distances under 3000
    backClipDistance = frontClipDistance * 3000;
  } else {
    // Use a variable front clip distance for observer camera depending on the elevation 
    // Under 125 cm keep a front clip distance equal to 2.5 cm 
    frontClipDistance = 2.5;
    backClipDistance = frontClipDistance * 5000;
    var minElevation = 125;
    if (camera.getZ() > minElevation) {
      var intermediateGrowFactor = 1 / 250;
      var approximateHomeBounds = this.getApproximateHomeBoundsCache();
      var highestPoint = 0; 
      if (approximateHomeBounds !== null) {
        var upper = vec3.create();
        approximateHomeBounds.getUpper(upper);
        highestPoint = Math.min(upper [2], 10000);
      }
      if (camera.getZ() < highestPoint + minElevation) {
        // Between 200 cm and the highest point, make front clip distance grow slowly and increase front/back ratio  
        frontClipDistance += (camera.getZ() - minElevation) * intermediateGrowFactor;
        backClipDistance  += (frontClipDistance - 2.5) * 25000;
      } else {
        // Above, make front clip distance grow faster
        frontClipDistance += 
            highestPoint * intermediateGrowFactor 
          + (camera.getZ() - highestPoint - minElevation) / 50;
        backClipDistance  += 
            + (highestPoint * intermediateGrowFactor) * 25000
            + (frontClipDistance - highestPoint * intermediateGrowFactor - 2.5) * 5000;
      }
    }
  }
  
  // Update front and back clip distance 
  this.canvas3D.setFrontClipDistance(frontClipDistance);
  this.canvas3D.setBackClipDistance(backClipDistance);
}

/**
 * Returns quickly computed bounds of the objects in home.
 * @private 
 */
HomeComponent3D.prototype.getApproximateHomeBoundsCache = function() {
  if (this.approximateHomeBoundsCache === null) {
    var approximateHomeBounds = null;
    var furniture = this.home.getFurniture();
    for (var i = 0; i < furniture.length; i++) {
      var piece = furniture [i];
      if (piece.isVisible()
          && (piece.getLevel() === null
              || piece.getLevel().isViewable())) {
        var pieceLocation = vec3.fromValues(piece.getX(), piece.getY(), piece.getGroundElevation());
        if (approximateHomeBounds === null) {
          approximateHomeBounds = new BoundingBox3D(pieceLocation, pieceLocation);
        } else {
          approximateHomeBounds.combine(pieceLocation);
        }
      }
    }
    var walls = this.home.getWalls();
    for (var i = 0; i < walls.length; i++) {
      var wall = walls [i];
      if (wall.getLevel() === null
          || wall.getLevel().isViewable()) {
        var startPoint = vec3.fromValues(wall.getXStart(), wall.getYStart(), 
            wall.getLevel() !== null ? wall.getLevel().getElevation() : 0);
        if (approximateHomeBounds === null) {
          approximateHomeBounds = new BoundingBox3D(startPoint, startPoint);
        } else {
          approximateHomeBounds.combine(startPoint);
        }
        approximateHomeBounds.combine(vec3.fromValues(wall.getXEnd(), wall.getYEnd(), 
            startPoint [2] + (wall.getHeight() !== null ? wall.getHeight() : this.home.getWallHeight())));
      }
    }
    var rooms = this.home.getRooms();
    for (var i = 0; i < rooms.length; i++) {
      var room = rooms [i];
      if (room.getLevel() === null
          || room.getLevel().isViewable()) {
         var center = vec3.fromValues(room.getXCenter(), room.getYCenter(), 
            room.getLevel() !== null ? room.getLevel().getElevation() : 0);
        if (approximateHomeBounds === null) {
          approximateHomeBounds = new BoundingBox3D(center, center);
        } else {
          approximateHomeBounds.combine(center);
        }
      }
    }
    var labels = this.home.getLabels();
    for (var i = 0; i < labels.length; i++) {
      var label = labels [i];
      if ((label.getLevel() === null
            || label.getLevel().isViewable())
          && label.getPitch() !== null) {
        var center = vec3.fromValues(label.getX(), label.getY(), label.getGroundElevation());
        if (approximateHomeBounds === null) {
          approximateHomeBounds = new BoundingBox3D(center, center);
        } else {
          approximateHomeBounds.combine(center);
        }
      }
    }
    this.approximateHomeBoundsCache = approximateHomeBounds;
  }
  return this.approximateHomeBoundsCache;
}

/**
 * Updates view transform from <code>camera</code> angles and location.
 * @private 
 */
HomeComponent3D.prototype.updateViewPlatformTransform = function(camera, updateWithAnimation) {
  if (updateWithAnimation) {
    this.moveCameraWithAnimation(camera);
  } else {
    delete this.cameraInterpolator;
    var viewPlatformTransform = mat4.create();
    this.computeViewPlatformTransform(viewPlatformTransform, camera.getX(), camera.getY(), 
        camera.getZ(), camera.getYaw(), camera.getPitch());
    this.canvas3D.setViewPlatformTransform(viewPlatformTransform);
  }
}

/**
 * Moves the camera to a new location using an animation for smooth moves.
 * @private 
 */
HomeComponent3D.prototype.moveCameraWithAnimation = function(finalCamera) {
  if (this.cameraInterpolator === undefined) {
    this.cameraInterpolator = {initialCamera : null, finalCamera : null, alpha : null};
  }
  if (this.cameraInterpolator.finalCamera === null
      || this.cameraInterpolator.finalCamera.getX() !== finalCamera.getX()
      || this.cameraInterpolator.finalCamera.getY() !== finalCamera.getY()
      || this.cameraInterpolator.finalCamera.getZ() !== finalCamera.getZ()
      || this.cameraInterpolator.finalCamera.getYaw() !== finalCamera.getYaw()
      || this.cameraInterpolator.finalCamera.getPitch() !== finalCamera.getPitch()) {
    if (this.cameraInterpolator.alpha === null || this.cameraInterpolator.alpha === 1) {
      this.cameraInterpolator.initialCamera = new Camera(this.camera.getX(), this.camera.getY(), this.camera.getZ(), 
          this.camera.getYaw(), this.camera.getPitch(), this.camera.getFieldOfView());
    } else if (this.cameraInterpolator.alpha < 0.3) {
      var finalTransformation = mat4.create();
      // Jump directly to final location
      this.computeViewPlatformTransform(finalTransformation, this.cameraInterpolator.finalCamera.getX(), this.cameraInterpolator.finalCamera.getY(), this.cameraInterpolator.finalCamera.getZ(), 
          this.cameraInterpolator.finalCamera.getYaw(), this.cameraInterpolator.finalCamera.getPitch());
      this.canvas3D.setViewPlatformTransform(finalTransformation);
      this.cameraInterpolator.initialCamera = this.cameraInterpolator.finalCamera;
    } else {
      // Compute initial location from current alpha value 
      this.cameraInterpolator.initialCamera = new Camera(this.cameraInterpolator.initialCamera.getX() + (this.cameraInterpolator.finalCamera.getX() - this.cameraInterpolator.initialCamera.getX()) * this.cameraInterpolator.alpha, 
          this.cameraInterpolator.initialCamera.getY() + (this.cameraInterpolator.finalCamera.getY() - this.cameraInterpolator.initialCamera.getY()) * this.cameraInterpolator.alpha, 
          this.cameraInterpolator.initialCamera.getZ() + (this.cameraInterpolator.finalCamera.getZ() - this.cameraInterpolator.initialCamera.getZ()) * this.cameraInterpolator.alpha,
          this.cameraInterpolator.initialCamera.getYaw() + (this.cameraInterpolator.finalCamera.getYaw() - this.cameraInterpolator.initialCamera.getYaw()) * this.cameraInterpolator.alpha, 
          this.cameraInterpolator.initialCamera.getPitch() + (this.cameraInterpolator.finalCamera.getPitch() - this.cameraInterpolator.initialCamera.getPitch()) * this.cameraInterpolator.alpha, 
          finalCamera.getFieldOfView());
    }
    this.cameraInterpolator.finalCamera = new Camera(finalCamera.getX(), finalCamera.getY(), finalCamera.getZ(), 
        finalCamera.getYaw(), finalCamera.getPitch(), finalCamera.getFieldOfView());
    // Create an animation that will interpolate camera location 
    // between initial camera and final camera in 75 ms
    if (this.cameraInterpolator.alpha === null) {
      this.cameraInterpolator.animationDuration = 75;
    }
    // Start animation now
    this.cameraInterpolator.startTime = Date.now();
    this.cameraInterpolator.alpha = 0;
    var component3D = this;
    requestAnimationFrame(
        function() {
          component3D.interpolateUntilAlphaEquals1();
        });
  }
}

/**
 * Increases alpha according to elapsed time and interpolates transformation.
 * @private 
 */
HomeComponent3D.prototype.interpolateUntilAlphaEquals1 = function() {
  if (this.cameraInterpolator) {
    var now = Date.now();
    var alpha = Math.min(1, (now - this.cameraInterpolator.startTime) / this.cameraInterpolator.animationDuration);
    if (this.cameraInterpolator.alpha !== alpha) {
      var transform = mat4.create();
      this.computeTransform(alpha, transform);
      this.canvas3D.setViewPlatformTransform(transform);
      this.cameraInterpolator.alpha = alpha;
    }
    if (this.cameraInterpolator.alpha < 1) {
      var component3D = this;
      requestAnimationFrame(
          function() {
            component3D.interpolateUntilAlphaEquals1();
          });
    }
  }
}

/**
 * Computes the transformation interpolated between initial and final camera position 
 * according to alpha. 
 * @private 
 */
HomeComponent3D.prototype.computeTransform = function(alpha, transform) {
  this.computeViewPlatformTransform(transform, 
      this.cameraInterpolator.initialCamera.getX() + (this.cameraInterpolator.finalCamera.getX() - this.cameraInterpolator.initialCamera.getX()) * alpha, 
      this.cameraInterpolator.initialCamera.getY() + (this.cameraInterpolator.finalCamera.getY() - this.cameraInterpolator.initialCamera.getY()) * alpha, 
      this.cameraInterpolator.initialCamera.getZ() + (this.cameraInterpolator.finalCamera.getZ() - this.cameraInterpolator.initialCamera.getZ()) * alpha, 
      this.cameraInterpolator.initialCamera.getYaw() + (this.cameraInterpolator.finalCamera.getYaw() - this.cameraInterpolator.initialCamera.getYaw()) * alpha, 
      this.cameraInterpolator.initialCamera.getPitch() + (this.cameraInterpolator.finalCamera.getPitch() - this.cameraInterpolator.initialCamera.getPitch()) * alpha);
}

/**
 * Updates view transform from camera angles and location.
 * @private 
 */
HomeComponent3D.prototype.computeViewPlatformTransform = function(transform, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch) {
  var yawRotation = mat4.create();
  mat4.fromYRotation(yawRotation, -cameraYaw + Math.PI);
  
  var pitchRotation = mat4.create();
  mat4.fromXRotation(pitchRotation, -cameraPitch);
  mat4.mul(yawRotation, yawRotation, pitchRotation);

  mat4.identity(transform);
  mat4.translate(transform, transform, vec3.fromValues(cameraX, cameraZ, cameraY));
  mat4.mul(transform, transform, yawRotation);
  
  this.camera = new Camera(cameraX, cameraY, cameraZ, cameraYaw, cameraPitch, 0);
}

/**
 * Adds mouse listeners to the canvas3D that calls back <code>controller</code> methods.  
 * @private 
 */
HomeComponent3D.prototype.addMouseListeners = function(controller, canvas3D) {
  var component3D = this; 
  var userActionsListener = {
      xLastMove : -1,
      yLastMove : -1,
      buttonPressed : -1,
      pointerTouches : {},
      distanceLastPinch : -1,
      
      mousePressed : function(ev) {
        userActionsListener.xLastMove = ev.clientX;
        userActionsListener.yLastMove = ev.clientY;
        userActionsListener.buttonPressed  = ev.button;
        userActionsListener.mousePressedInCanvas = true;
        ev.stopPropagation();
      },
      windowMouseMoved : function(ev) {
        if (userActionsListener.mousePressedInCanvas) {
          userActionsListener.moved(ev.clientX, ev.clientY, ev.altKey, ev.shiftKey);
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
        // Required to avoid click simulation
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
          userActionsListener.xLastMove = ev.targetTouches [0].pageX;
          userActionsListener.yLastMove = ev.targetTouches [0].pageY;
          if (component3D.home.getCamera() === component3D.home.getObserverCamera()) {
            userActionsListener.xLastMove = -userActionsListener.xLastMove;
            userActionsListener.yLastMove = -userActionsListener.yLastMove;
          }
          userActionsListener.buttonPressed = 0;
        } else if (ev.targetTouches.length == 2) {
          userActionsListener.distanceLastPinch = userActionsListener.distance(
              ev.targetTouches [0], ev.targetTouches [1]);
        }
      },
      touchMoved : function(ev) {
        ev.preventDefault();
        if (ev.targetTouches.length == 1) {
          if (component3D.home.getCamera() === component3D.home.getObserverCamera()) {
            userActionsListener.moved(-ev.targetTouches [0].pageX, -ev.targetTouches [0].pageY, false, false);
          } else {
            userActionsListener.moved(ev.targetTouches [0].pageX,  ev.targetTouches [0].pageY, false, false);
          }
        } else if (ev.targetTouches.length == 2) {
          var newDistance = userActionsListener.distance(ev.targetTouches [0], ev.targetTouches [1]);
          var scaleDifference = newDistance / userActionsListener.distanceLastPinch;
          userActionsListener.zoomed((1 - scaleDifference) * 50, false);
          userActionsListener.distanceLastPinch = newDistance;
        }
      },
      touchEnded : function(ev) {
        userActionsListener.buttonPressed = -1;
      },
      copyPointerToTargetTouches : function(ev) {
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
      moved : function(x, y, altKey, shiftKey) {
        if (userActionsListener.buttonPressed === 0) {
          if (altKey) {
            // Mouse move along Y axis while alt is down changes camera location
            var delta = 1.25 * (userActionsListener.yLastMove - y);
            // Multiply delta by 5 if shift is down
            if (shiftKey) {
              delta *= 5;
            } 
            controller.moveCamera(delta);
          } else {
            var ANGLE_FACTOR = 0.005;
            // Mouse move along X axis changes camera yaw 
            var yawDelta = ANGLE_FACTOR * (x - userActionsListener.xLastMove);
            // Multiply yaw delta by 5 if shift is down
            if (shiftKey) {
              yawDelta *= 5;
            } 
            controller.rotateCameraYaw(yawDelta);
            
            // Mouse move along Y axis changes camera pitch 
            var pitchDelta = ANGLE_FACTOR * (y - userActionsListener.yLastMove);
            controller.rotateCameraPitch(pitchDelta);
          }
          userActionsListener.xLastMove = x;
          userActionsListener.yLastMove = y;
        }
      },
      mouseScrolled : function(ev) {
        userActionsListener.zoomed(ev.detail, ev.shiftKey);
      },
      mouseWheelMoved : function(ev) {
        ev.preventDefault();
        userActionsListener.zoomed((ev.deltaY !== undefined ? ev.deltaY : -ev.wheelDelta) / 4, ev.shiftKey);
      },        
      zoomed : function(delta, shiftKey) {
        // Mouse wheel changes camera location 
        var delta = -2.5 * delta;
        // Multiply delta by 10 if shift is down
        if (shiftKey) {
          delta *= 5;
        } 
        controller.moveCamera(delta);
      }
    };
    
  if (window.PointerEvent) {
    // Multi touch support for IE and Edge
    canvas3D.getCanvas().addEventListener("pointerdown", userActionsListener.pointerPressed);
    canvas3D.getCanvas().addEventListener("mousedown", userActionsListener.pointerMousePressed);
    // Add pointermove and pointerup event listeners to window to capture pointer events out of the canvas 
    window.addEventListener("pointermove", userActionsListener.windowPointerMoved);
    window.addEventListener("pointerup", userActionsListener.windowPointerReleased);
  } else {
    canvas3D.getCanvas().addEventListener("touchstart", userActionsListener.touchStarted);
    canvas3D.getCanvas().addEventListener("touchmove", userActionsListener.touchMoved);
    canvas3D.getCanvas().addEventListener("touchend", userActionsListener.touchEnded);
    canvas3D.getCanvas().addEventListener("mousedown", userActionsListener.mousePressed);
    // Add mousemove and mouseup event listeners to window to capture mouse events out of the canvas 
    window.addEventListener("mousemove", userActionsListener.windowMouseMoved);
    window.addEventListener("mouseup", userActionsListener.windowMouseReleased);
  }
  canvas3D.getCanvas().addEventListener("DOMMouseScroll", userActionsListener.mouseScrolled);
  canvas3D.getCanvas().addEventListener("mousewheel", userActionsListener.mouseWheelMoved);

  this.userActionsListener = userActionsListener;
}

/**
 * Installs keys bound to actions. 
 * @private 
 */
HomeComponent3D.prototype.installKeyboardActions = function() {
  // Tolerate alt modifier for forward and backward moves with UP and DOWN keys to avoid 
  // the user to release the alt key when he wants to alternate forward/backward and sideways moves
  this.inputMap = {
      "shift pressed UP" : "MOVE_CAMERA_FAST_FORWARD",
      "alt shift pressed UP" : "MOVE_CAMERA_FAST_FORWARD",
      "shift pressed W" : "MOVE_CAMERA_FAST_FORWARD",
      "pressed UP" : "MOVE_CAMERA_FORWARD",
      "alt pressed UP" : "MOVE_CAMERA_FORWARD",
      "pressed W" : "MOVE_CAMERA_FORWARD",
      "shift pressed DOWN" : "MOVE_CAMERA_FAST_BACKWARD",
      "alt shift pressed DOWN" : "MOVE_CAMERA_FAST_BACKWARD",
      "shift pressed S" : "MOVE_CAMERA_FAST_BACKWARD",
      "pressed DOWN" : "MOVE_CAMERA_BACKWARD",
      "alt pressed DOWN" : "MOVE_CAMERA_BACKWARD",
      "pressed S" : "MOVE_CAMERA_BACKWARD",
      "alt shift pressed LEFT" : "MOVE_CAMERA_FAST_LEFT",
      "alt pressed LEFT" : "MOVE_CAMERA_LEFT",
      "alt shift pressed RIGHT" : "MOVE_CAMERA_FAST_RIGHT",
      "alt pressed RIGHT" : "MOVE_CAMERA_RIGHT",
      "shift pressed LEFT" : "ROTATE_CAMERA_YAW_FAST_LEFT",
      "shift pressed A" : "ROTATE_CAMERA_YAW_FAST_LEFT",
      "pressed LEFT" : "ROTATE_CAMERA_YAW_LEFT",
      "pressed A" : "ROTATE_CAMERA_YAW_LEFT",
      "shift pressed RIGHT" : "ROTATE_CAMERA_YAW_FAST_RIGHT",
      "shift pressed D" : "ROTATE_CAMERA_YAW_FAST_RIGHT",
      "pressed RIGHT" : "ROTATE_CAMERA_YAW_RIGHT",
      "pressed D" : "ROTATE_CAMERA_YAW_RIGHT",
      "shift pressed PAGE_UP" : "ROTATE_CAMERA_PITCH_FAST_UP",
      "pressed PAGE_UP" : "ROTATE_CAMERA_PITCH_UP",
      "shift pressed PAGE_DOWN" : "ROTATE_CAMERA_PITCH_FAST_DOWN",
      "pressed PAGE_DOWN" : "ROTATE_CAMERA_PITCH_DOWN",
      "shift pressed HOME" : "ELEVATE_CAMERA_FAST_UP",
      "pressed HOME" : "ELEVATE_CAMERA_UP",
      "shift pressed END" : "ELEVATE_CAMERA_FAST_DOWN",
      "pressed END" : "ELEVATE_CAMERA_DOWN",
  };
  var component3D = this;
  this.canvas3D.getCanvas().addEventListener("keydown", 
      function(ev) {
        if (HomeComponent3D.KEY_CODE_TEXTS === undefined) {
          HomeComponent3D.KEY_CODE_TEXTS = new Array(223);
          HomeComponent3D.KEY_CODE_TEXTS [8]  = "BACK_SPACE";
          HomeComponent3D.KEY_CODE_TEXTS [9]  = "TAB";
          HomeComponent3D.KEY_CODE_TEXTS [13] = "ENTER";
          HomeComponent3D.KEY_CODE_TEXTS [16] = "SHIFT";
          HomeComponent3D.KEY_CODE_TEXTS [17] = "CONTROL";
          HomeComponent3D.KEY_CODE_TEXTS [18] = "ALT";
          HomeComponent3D.KEY_CODE_TEXTS [19] = "PAUSE";
          HomeComponent3D.KEY_CODE_TEXTS [20] = "CAPS_LOCK";
          HomeComponent3D.KEY_CODE_TEXTS [27] = "ESCAPE";
          HomeComponent3D.KEY_CODE_TEXTS [33] = "PAGE_UP";
          HomeComponent3D.KEY_CODE_TEXTS [34] = "PAGE_DOWN";
          HomeComponent3D.KEY_CODE_TEXTS [35] = "END";
          HomeComponent3D.KEY_CODE_TEXTS [36] = "HOME";
          HomeComponent3D.KEY_CODE_TEXTS [37] = "LEFT";
          HomeComponent3D.KEY_CODE_TEXTS [38] = "UP";
          HomeComponent3D.KEY_CODE_TEXTS [39] = "RIGHT";
          HomeComponent3D.KEY_CODE_TEXTS [40] = "DOWN";
          HomeComponent3D.KEY_CODE_TEXTS [45] = "INSERT";
          HomeComponent3D.KEY_CODE_TEXTS [46] = "DELETE";
          HomeComponent3D.KEY_CODE_TEXTS [48] = "0";
          HomeComponent3D.KEY_CODE_TEXTS [49] = "1";
          HomeComponent3D.KEY_CODE_TEXTS [50] = "2";
          HomeComponent3D.KEY_CODE_TEXTS [51] = "3";
          HomeComponent3D.KEY_CODE_TEXTS [52] = "4";
          HomeComponent3D.KEY_CODE_TEXTS [53] = "5";
          HomeComponent3D.KEY_CODE_TEXTS [54] = "6";
          HomeComponent3D.KEY_CODE_TEXTS [55] = "7";
          HomeComponent3D.KEY_CODE_TEXTS [56] = "8";
          HomeComponent3D.KEY_CODE_TEXTS [57] = "9";
          HomeComponent3D.KEY_CODE_TEXTS [65] = "A";
          HomeComponent3D.KEY_CODE_TEXTS [66] = "B";
          HomeComponent3D.KEY_CODE_TEXTS [67] = "C";
          HomeComponent3D.KEY_CODE_TEXTS [68] = "D";
          HomeComponent3D.KEY_CODE_TEXTS [69] = "E";
          HomeComponent3D.KEY_CODE_TEXTS [70] = "F";
          HomeComponent3D.KEY_CODE_TEXTS [71] = "G";
          HomeComponent3D.KEY_CODE_TEXTS [72] = "H";
          HomeComponent3D.KEY_CODE_TEXTS [73] = "I";
          HomeComponent3D.KEY_CODE_TEXTS [74] = "J";
          HomeComponent3D.KEY_CODE_TEXTS [75] = "K";
          HomeComponent3D.KEY_CODE_TEXTS [76] = "L";
          HomeComponent3D.KEY_CODE_TEXTS [77] = "M";
          HomeComponent3D.KEY_CODE_TEXTS [78] = "N";
          HomeComponent3D.KEY_CODE_TEXTS [79] = "O";
          HomeComponent3D.KEY_CODE_TEXTS [80] = "P";
          HomeComponent3D.KEY_CODE_TEXTS [81] = "Q";
          HomeComponent3D.KEY_CODE_TEXTS [82] = "R";
          HomeComponent3D.KEY_CODE_TEXTS [83] = "S";
          HomeComponent3D.KEY_CODE_TEXTS [84] = "T";
          HomeComponent3D.KEY_CODE_TEXTS [85] = "U";
          HomeComponent3D.KEY_CODE_TEXTS [86] = "V";
          HomeComponent3D.KEY_CODE_TEXTS [87] = "W";
          HomeComponent3D.KEY_CODE_TEXTS [88] = "X";
          HomeComponent3D.KEY_CODE_TEXTS [89] = "Y";
          HomeComponent3D.KEY_CODE_TEXTS [90] = "Z";
          HomeComponent3D.KEY_CODE_TEXTS [91] = "META";
          HomeComponent3D.KEY_CODE_TEXTS [92] = "META";
          HomeComponent3D.KEY_CODE_TEXTS [96] = "NUMPAD0";
          HomeComponent3D.KEY_CODE_TEXTS [97] = "NUMPAD1";
          HomeComponent3D.KEY_CODE_TEXTS [98] = "NUMPAD2";
          HomeComponent3D.KEY_CODE_TEXTS [99] = "NUMPAD3";
          HomeComponent3D.KEY_CODE_TEXTS [100] = "NUMPAD4";
          HomeComponent3D.KEY_CODE_TEXTS [101] = "NUMPAD5";
          HomeComponent3D.KEY_CODE_TEXTS [102] = "NUMPAD6";
          HomeComponent3D.KEY_CODE_TEXTS [103] = "NUMPAD7";
          HomeComponent3D.KEY_CODE_TEXTS [104] = "NUMPAD8";
          HomeComponent3D.KEY_CODE_TEXTS [105] = "NUMPAD9";
          HomeComponent3D.KEY_CODE_TEXTS [106] = "MULTIPLY";
          HomeComponent3D.KEY_CODE_TEXTS [107] = "ADD";
          HomeComponent3D.KEY_CODE_TEXTS [109] = "VK_SUBTRACT";
          HomeComponent3D.KEY_CODE_TEXTS [110] = "VK_DECIMAL";
          HomeComponent3D.KEY_CODE_TEXTS [111] = "VK_DIVIDE";
          HomeComponent3D.KEY_CODE_TEXTS [112] = "F1";
          HomeComponent3D.KEY_CODE_TEXTS [113] = "F2";
          HomeComponent3D.KEY_CODE_TEXTS [114] = "F3";
          HomeComponent3D.KEY_CODE_TEXTS [115] = "F4";
          HomeComponent3D.KEY_CODE_TEXTS [116] = "F5";
          HomeComponent3D.KEY_CODE_TEXTS [117] = "F6";
          HomeComponent3D.KEY_CODE_TEXTS [118] = "F7";
          HomeComponent3D.KEY_CODE_TEXTS [119] = "F8";
          HomeComponent3D.KEY_CODE_TEXTS [120] = "F9";
          HomeComponent3D.KEY_CODE_TEXTS [121] = "F10";
          HomeComponent3D.KEY_CODE_TEXTS [122] = "F11";
          HomeComponent3D.KEY_CODE_TEXTS [123] = "F12";
          HomeComponent3D.KEY_CODE_TEXTS [144] = "VK_NUM_LOCK";
          HomeComponent3D.KEY_CODE_TEXTS [145] = "VK_SCROLL_LOCK";
          HomeComponent3D.KEY_CODE_TEXTS [186] = "VK_SEMICOLON";
          HomeComponent3D.KEY_CODE_TEXTS [187] = "VK_EQUALS";
          HomeComponent3D.KEY_CODE_TEXTS [188] = "VK_COMMA";
          HomeComponent3D.KEY_CODE_TEXTS [190] = "VK_PERIOD";
          HomeComponent3D.KEY_CODE_TEXTS [191] = "VK_SLASH";
          HomeComponent3D.KEY_CODE_TEXTS [219] = "VK_OPEN_BRACKET";
          HomeComponent3D.KEY_CODE_TEXTS [220] = "VK_BACK_SLASH";
          HomeComponent3D.KEY_CODE_TEXTS [221] = "VK_CLOSE_BRACKET";
          HomeComponent3D.KEY_CODE_TEXTS [222] = "VK_QUOTE";
        }
        component3D.callAction(HomeComponent3D.KEY_CODE_TEXTS [ev.keyCode], ev);
      }, false);
}

/**
 * Runs the action bound to the key in parameter.
 * @private 
 */
HomeComponent3D.prototype.callAction = function(keyName, ev) {
  if (keyName) {
    var keyStroke = ""; 
    if (ev.ctrlKey || keyName.indexOf("control ") != -1) {
      keyStroke += "control ";
    }
    if (ev.altKey || keyName.indexOf("alt ") != -1) {
      keyStroke += "alt ";
    }
    if (ev.metaKey || keyName.indexOf("meta ") != -1) {
      keyStroke += "meta ";
    }
    if (ev.shiftKey || keyName.indexOf("shift ") != -1) {
      keyStroke += "shift ";
    }
    keyStroke += "pressed " + keyName.substring(keyName.lastIndexOf(' ') + 1);
    var actionKey = this.inputMap [keyStroke];
    if (actionKey !== undefined) {
      var action = this.actionMap [actionKey];
      if (action !== undefined) {
        action.actionPerformed(ev);
      }
      ev.stopPropagation();
    }
  } 
}

/**
 * Creates actions that calls back <code>controller</code> methods.  
 * @private 
 */
HomeComponent3D.prototype.createActions = function(controller) {
  // Move camera action mapped to arrow keys.
  function MoveCameraAction(delta) {
    this.delta = delta;
  }

  MoveCameraAction.prototype.actionPerformed = function(ev) {
    controller.moveCamera(this.delta);
  }

  // Move camera sideways action mapped to arrow keys.
  function MoveCameraSidewaysAction (delta) {
    this.delta = delta;
  }

  MoveCameraSidewaysAction.prototype.actionPerformed = function(ev) {
    controller.moveCameraSideways(this.delta);
  }

  // Elevate camera action mapped to arrow keys.
  function ElevateCameraAction(delta) {
    this.delta = delta;
  }

  ElevateCameraAction.prototype.actionPerformed = function(ev) {
    controller.elevateCamera(this.delta);
  }

  // Rotate camera yaw action mapped to arrow keys.
  function RotateCameraYawAction (delta) {
    this.delta = delta;
  }

  RotateCameraYawAction.prototype.actionPerformed = function(ev) {
    controller.rotateCameraYaw(this.delta);
  }

  // Rotate camera pitch action mapped to arrow keys.
  function RotateCameraPitchAction(delta) {
    this.delta = delta;
  }

  RotateCameraPitchAction.prototype.actionPerformed = function(ev) {
    controller.rotateCameraPitch(this.delta);
  }

  this.actionMap = {
      "MOVE_CAMERA_FORWARD" : new MoveCameraAction(6.5),
      "MOVE_CAMERA_FAST_FORWARD" : new MoveCameraAction(32.5),
      "MOVE_CAMERA_BACKWARD" : new MoveCameraAction(-6.5),
      "MOVE_CAMERA_FAST_BACKWARD" : new MoveCameraAction(-32.5),
      "MOVE_CAMERA_LEFT" : new MoveCameraSidewaysAction(-2.5),
      "MOVE_CAMERA_FAST_LEFT" : new MoveCameraSidewaysAction(-10),
      "MOVE_CAMERA_RIGHT" : new MoveCameraSidewaysAction(2.5),
      "MOVE_CAMERA_FAST_RIGHT" : new MoveCameraSidewaysAction(10),
      "ELEVATE_CAMERA_DOWN" : new ElevateCameraAction(-2.5),
      "ELEVATE_CAMERA_FAST_DOWN" : new ElevateCameraAction(-10),
      "ELEVATE_CAMERA_UP" : new ElevateCameraAction(2.5),
      "ELEVATE_CAMERA_FAST_UP" : new ElevateCameraAction(10),
      "ROTATE_CAMERA_YAW_LEFT" : new RotateCameraYawAction(-Math.PI / 60),
      "ROTATE_CAMERA_YAW_FAST_LEFT" : new RotateCameraYawAction(-Math.PI / 12),
      "ROTATE_CAMERA_YAW_RIGHT" : new RotateCameraYawAction(Math.PI / 60),
      "ROTATE_CAMERA_YAW_FAST_RIGHT" : new RotateCameraYawAction(Math.PI / 12),
      "ROTATE_CAMERA_PITCH_UP" : new RotateCameraPitchAction(-Math.PI / 120),
      "ROTATE_CAMERA_PITCH_FAST_UP" : new RotateCameraPitchAction(-Math.PI / 24),
      "ROTATE_CAMERA_PITCH_DOWN" : new RotateCameraPitchAction(Math.PI / 120),
      "ROTATE_CAMERA_PITCH_FAST_DOWN" : new RotateCameraPitchAction(Math.PI / 24),
  };
}

/**
 * Returns the action map of this component.
 */
HomeComponent3D.prototype.getActionMap = function() {
  return this.actionMap;
}

/**
 * Returns the input map of this component.
 */
HomeComponent3D.prototype.getInputMap = function() {
  return this.inputMap;
}

/**
 * Returns a new scene tree root.
 * @private 
 */
HomeComponent3D.prototype.createSceneTree = function(listenToHomeUpdates, waitForLoading) {
  var root = new Group3D();
  // Build scene tree
  var groundNode = this.createGroundNode(-0.5E5, -0.5E5, 1E5, 1E5, listenToHomeUpdates, waitForLoading);
  root.addChild(groundNode);
  root.addChild(this.createBackgroundNode(listenToHomeUpdates, waitForLoading));
  root.addChild(this.createHomeTree(listenToHomeUpdates, waitForLoading)); 
  this.defaultLights = this.createLights(listenToHomeUpdates);
  for (var i = 0; i < this.defaultLights.length; i++) {
    root.addChild(this.defaultLights [i]);
  }
  
  return root;
}

/**
 * Returns a new background node.  
 * @private 
 */
HomeComponent3D.prototype.createBackgroundNode = function(listenToHomeUpdates, waitForLoading) {
  var skyBackgroundAppearance = new Appearance3D();
  var halfSphereGeometry = this.createHalfSphereGeometry(true);   
  var halfSphere = new Shape3D(halfSphereGeometry, skyBackgroundAppearance);
  var backgroundGroup = new BranchGroup3D();
  backgroundGroup.addChild(halfSphere);
  backgroundGroup.addChild(new Shape3D(this.createHalfSphereGeometry(false)));

  // Add a plane at ground level to complete landscape at the horizon when camera is above horizon 
  var groundBackgroundAppearance = new Appearance3D();
  var groundBackground = new Shape3D( 
      new IndexedTriangleArray3D([vec3.fromValues(-1, -0.01, -1),
                                  vec3.fromValues(-1, -0.01, 1),
                                  vec3.fromValues(1, -0.01, 1),
                                  vec3.fromValues(1, -0.01, -1)],
                                 [0, 1, 2, 0, 2, 3],
                                 [], [],
                                 [vec3.fromValues(0., 1., 0.)], [0, 0, 0, 0, 0, 0]),
      groundBackgroundAppearance);
  backgroundGroup.addChild(groundBackground);
  
  var background = new Background3D(backgroundGroup);
  this.updateBackgroundColorAndTexture(skyBackgroundAppearance, groundBackgroundAppearance, this.home, waitForLoading);
  groundBackgroundAppearance.setVisible(this.home.getCamera().getZ() >= 0);

  if (listenToHomeUpdates) {
    // Add a listener on home properties change 
    var component3D = this;
    this.backgroundChangeListener = function(ev) {
        component3D.updateBackgroundColorAndTexture(skyBackgroundAppearance, groundBackgroundAppearance, 
            component3D.home, waitForLoading);
      };
    component3D.home.getEnvironment().addPropertyChangeListener("SKY_COLOR", this.backgroundChangeListener);
    component3D.home.getEnvironment().addPropertyChangeListener("SKY_TEXTURE", this.backgroundChangeListener);
    component3D.home.getEnvironment().addPropertyChangeListener("GROUND_COLOR", this.backgroundChangeListener);
    component3D.home.getEnvironment().addPropertyChangeListener("GROUND_TEXTURE", this.backgroundChangeListener);
    // Make groundBackground invisible if camera is below the ground
    this.elevationChangeListener = function(ev) {
        if (ev.getSource() === component3D.home) {
          ev.getOldValue().removePropertyChangeListener(component3D.elevationChangeListener);
          component3D.home.getCamera().addPropertyChangeListener(component3D.elevationChangeListener);
          groundBackgroundAppearance.setVisible(component3D.home.getCamera().getZ() >= 0);
        } else if (ev.getPropertyName() === "Z") {
          // Camera elevation change
          groundBackgroundAppearance.setVisible(component3D.home.getCamera().getZ() >= 0);
        }
      };
    this.home.getCamera().addPropertyChangeListener(this.elevationChangeListener);
    this.home.addPropertyChangeListener("CAMERA", this.elevationChangeListener);
  }
  return background;
}

/**
 * Returns a half sphere oriented inward and with texture ordinates 
 * that spread along an hemisphere. 
 * @param {boolean} top  if true returns an upper geometry
 * @private 
 */
HomeComponent3D.prototype.createHalfSphereGeometry = function(top) {
  var divisionCount = 48; 
  var coords = [];
  var coordIndices = [];
  var textureCoords = [];
  for (var i = 0, k = 0; i < divisionCount; i++) {
    var alpha = i * 2 * Math.PI / divisionCount;
    var cosAlpha = Math.cos(alpha);
    var sinAlpha = Math.sin(alpha);
    var nextAlpha = (i  + 1) * 2 * Math.PI / divisionCount;
    var cosNextAlpha = Math.cos(nextAlpha);
    var sinNextAlpha = Math.sin(nextAlpha);
    for (var j = 0; j < divisionCount / 4; j++, k += 4) {
      var beta = 2 * j * Math.PI / divisionCount;
      var cosBeta = Math.cos(beta); 
      var sinBeta = Math.sin(beta);
      // Correct the bottom of the hemisphere to avoid seeing a bottom hemisphere at the horizon
      var y = j !== 0 ? (top ? sinBeta : -sinBeta) : -0.01;
      var nextBeta = 2 * (j + 1) * Math.PI / divisionCount;
      if (!top) {
        nextBeta = -nextBeta;
      }
      var cosNextBeta = Math.cos(nextBeta);
      var sinNextBeta = Math.sin(nextBeta);
      coords.push(vec3.fromValues(cosAlpha * cosBeta, y, sinAlpha * cosBeta));
      coords.push(vec3.fromValues(cosNextAlpha * cosBeta, y, sinNextAlpha * cosBeta));
      coords.push(vec3.fromValues(cosNextAlpha * cosNextBeta, sinNextBeta, sinNextAlpha * cosNextBeta));
      coords.push(vec3.fromValues(cosAlpha * cosNextBeta, sinNextBeta, sinAlpha * cosNextBeta));
      if (top) {
        coordIndices.push(k);
        coordIndices.push(k + 1);
        coordIndices.push(k + 2);
        coordIndices.push(k);
        coordIndices.push(k + 2);
        coordIndices.push(k + 3);
        textureCoords.push(vec2.fromValues(i / divisionCount, sinBeta)); 
        textureCoords.push(vec2.fromValues((i + 1) / divisionCount, sinBeta)); 
        textureCoords.push(vec2.fromValues((i + 1) / divisionCount, sinNextBeta)); 
        textureCoords.push(vec2.fromValues(i / divisionCount, sinNextBeta));
      } else {
        coordIndices.push(k);
        coordIndices.push(k + 2);
        coordIndices.push(k + 1);
        coordIndices.push(k);
        coordIndices.push(k + 3);
        coordIndices.push(k + 2);
      }
    }
  }
  
  return new IndexedTriangleArray3D(coords, coordIndices, textureCoords, coordIndices, [], []);
}

/**
 * Updates <code>skyBackgroundAppearance</code> and <code>groundBackgroundAppearance</code> 
 * color / texture from <code>home</code> sky color and texture.
 * @param {Appearance3D} skyBackgroundAppearance    the sky appearance to update
 * @param {Appearance3D} groundBackgroundAppearance the shape of the ground used in the background     
 * @param {Home}         home
 * @param {boolean}      waitForLoading
 * @private 
 */
HomeComponent3D.prototype.updateBackgroundColorAndTexture = function(skyBackgroundAppearance, groundBackgroundAppearance, 
                                                                     home, waitForLoading) {
  var skyColor = home.getEnvironment().getSkyColor();
  skyBackgroundAppearance.setDiffuseColor(vec3.fromValues(((skyColor >>> 16) & 0xFF) / 255.,
                                                          ((skyColor >>> 8) & 0xFF) / 255.,
                                                           (skyColor & 0xFF) / 255.));
  var skyTexture = home.getEnvironment().getSkyTexture();
  if (skyTexture !== null) {
    TextureManager.getInstance().loadTexture(skyTexture.getImage(), 0, waitForLoading,
        {
          textureUpdated : function(textureImage) {
            skyBackgroundAppearance.setTextureImage(textureImage);
          },
          textureError : function(error) {
            return this.textureUpdated(TextureManager.getInstance().getErrorImage());
          },
          progression : function(part, info, percentage) {
          } 
        });
  } else {
    skyBackgroundAppearance.setTextureImage(null);
  }
  
  var groundColor = home.getEnvironment().getGroundColor();
  var color = vec3.fromValues(((groundColor >>> 16) & 0xFF) / 255.,
                              ((groundColor >>> 8) & 0xFF) / 255.,
                               (groundColor & 0xFF) / 255.);
  groundBackgroundAppearance.setDiffuseColor(color);
  groundBackgroundAppearance.setAmbientColor(color);
  var groundTexture = home.getEnvironment().getGroundTexture();
  if (groundTexture !== null) {
    TextureManager.getInstance().loadTexture(groundTexture.getImage(), 0, waitForLoading,
        {
          textureUpdated : function(textureImage) {
            // Display texture very small to get an average color at the horizon 
            groundBackgroundAppearance.setTextureImage(textureImage);
            groundBackgroundAppearance.setTextureCoordinatesGeneration(
                {planeS : vec4.fromValues(1E5, 0, 0, 0), 
                 planeT : vec4.fromValues(0, 0, 1E5, 0)});
          },
          textureError : function(error) {
            return this.textureUpdated(TextureManager.getInstance().getErrorImage());
          },
          progression : function(part, info, percentage) {
          }
        });
  } else {
    groundBackgroundAppearance.setTextureImage(null);
  }
}

/**
 * Returns a new ground node.  
 * @private 
 */
HomeComponent3D.prototype.createGroundNode = function(groundOriginX, groundOriginY, groundWidth, groundDepth, 
                                                      listenToHomeUpdates, waitForLoading) {
  if (this.home.structure) {
    var structureGroup = new BranchGroup3D();
    structureGroup.setCapability(Group3D.ALLOW_CHILDREN_EXTEND);
    ModelManager.getInstance().loadModel(this.home.structure, waitForLoading,
        { 
          modelUpdated : function(structureNode) {
            structureGroup.addChild(structureNode);
          },
          modelError : function(ex) {
            // Display a large red box at ground level
            var boxAppearance = new Appearance3D();
            boxAppearance.setDiffuseColor(vec3.fromValues(1, 0, 0));
            structureGroup.addChild(new Box3D(1E7, 0, 1E7, boxAppearance));
          },
          progression : function() {
          }
        });
    
    this.groundChangeListener = function(ev) {}; // Dummy listener
    return structureGroup;
  } else {
    var ground3D = typeof Ground3D !== "undefined" 
        ? new Ground3D(this.home, groundOriginX, groundOriginY, groundWidth, groundDepth, waitForLoading) 
        : new Box3D(1E7, 0, 1E7, new Appearance3D());
    var translation = mat4.create();
    mat4.translate(translation, translation, vec3.fromValues(0, -0.2, 0));
    var transformGroup = new TransformGroup3D(translation);
    transformGroup.addChild(ground3D);

    if (listenToHomeUpdates) {
      // Add a listener on ground color and texture properties change 
      this.groundChangeListener = function(ev) {
          if (!this.updater) {
            var context = this;
            this.updater = function() {
                ground3D.update();
                delete context.updater;
              };
            setTimeout(this.updater, 0);
          }
        };
      var homeEnvironment = this.home.getEnvironment();
      homeEnvironment.addPropertyChangeListener("GROUND_COLOR", this.groundChangeListener);
      homeEnvironment.addPropertyChangeListener("GROUND_TEXTURE", this.groundChangeListener);
    }    
    return transformGroup;
  }
}

/**
 * Returns the lights of the scene.
 * @private 
 */
HomeComponent3D.prototype.createLights = function(listenToHomeUpdates) {
  var lights = [
      new DirectionalLight3D(vec3.fromValues(0.9, 0.9, 0.9), vec3.fromValues(1.5, -0.8, -1)),         
      new DirectionalLight3D(vec3.fromValues(0.9, 0.9, 0.9), vec3.fromValues(-1.5, -0.8, -1)), 
      new DirectionalLight3D(vec3.fromValues(0.9, 0.9, 0.9), vec3.fromValues(0, -0.8, 1)), 
      new DirectionalLight3D(vec3.fromValues(0.7, 0.7, 0.7), vec3.fromValues(0, 1, 0)), 
      new AmbientLight3D(vec3.fromValues(0.2, 0.2, 0.2))]; 
  for (var i = 0; i < lights.length - 1; i++) {
    // Store default color 
    lights [i].defaultColor = lights [i].getColor();
    this.updateLightColor(lights [i]);
  }
  
  if (listenToHomeUpdates) {
    // Add a listener on light color property change to home
    var component3D = this;
    this.lightColorListener = function(ev) {
        for (var i = 0; i < lights.length - 1; i++) {
          component3D.updateLightColor(lights [i]);
        }
      };
    this.home.getEnvironment().addPropertyChangeListener(
        "LIGHT_COLOR", this.lightColorListener);
  }

  return lights;
}

/**
 * Updates<code>light</code> color from <code>home</code> light color.
 * @param {Light3D} light the light to update 
 * @private 
 */
HomeComponent3D.prototype.updateLightColor = function(light) {
  var defaultColor = light.defaultColor;
  var lightColor = this.home.getEnvironment().getLightColor();
  light.setColor(vec3.fromValues(((lightColor >>> 16) & 0xFF) / 255 * defaultColor [0],
                                  ((lightColor >>> 8) & 0xFF) / 255 * defaultColor [1],
                                          (lightColor & 0xFF) / 255 * defaultColor [2]));
}

/**
 * Returns a <code>home</code> new tree node, with branches for each wall 
 * and piece of furniture of <code>home</code>. 
 * @private 
 */
HomeComponent3D.prototype.createHomeTree = function(listenToHomeUpdates, waitForLoading) {
  var homeRoot = new BranchGroup3D();
  homeRoot.setCapability(Group3D.ALLOW_CHILDREN_EXTEND);
  // Add walls, pieces, rooms and labels already available
  var labels = this.home.getLabels();
  for (var i = 0; i < labels.length; i++) {
    this.addObject(homeRoot, labels [i], listenToHomeUpdates, waitForLoading);
  }
  var rooms = this.home.getRooms();
  for (var i = 0; i < rooms.length; i++) {
    this.addObject(homeRoot, rooms [i], listenToHomeUpdates, waitForLoading);
  }    
  var walls = this.home.getWalls();
  for (var i = 0; i < walls.length; i++) {
    this.addObject(homeRoot, walls [i], listenToHomeUpdates, waitForLoading);
  }
  var furniture = this.home.getFurniture();
  for (var i = 0; i < furniture.length; i++) { 
    var piece = furniture [i];
    if (piece instanceof HomeFurnitureGroup) {
      var groupFurniture = piece.getAllFurniture();
      for (var j = 0; j < groupFurniture.length; j++) {
        var childPiece = groupFurniture [j];
        if (!(childPiece instanceof HomeFurnitureGroup)) {
          this.addObject(homeRoot, childPiece, listenToHomeUpdates, waitForLoading);
        }
      }
    } else {
      this.addObject(homeRoot, piece, listenToHomeUpdates, waitForLoading);
    }
  }
  if (listenToHomeUpdates) {
    // Add level, wall, furniture, room listeners to home for further update    
    this.addLevelListener(homeRoot);
    this.addWallListener(homeRoot);
    this.addFurnitureListener(homeRoot);
    this.addRoomListener(homeRoot);
    this.addLabelListener(homeRoot);
    this.addEnvironmentListeners();
  }
  return homeRoot;
}

/**
 * Adds a level listener to home levels that updates the children of the given
 * <code>group</code>, each time a level is added, updated or deleted.
 * @param {Group3D} group
 * @private
 */
HomeComponent3D.prototype.addLevelListener = function(group) {
  var component3D = this;
  this.levelChangeListener = function(ev) {
      var propertyName = ev.getPropertyName();
      if ("ELEVATION" == propertyName
          || "VISIBLE" == propertyName
          || "VIEWABLE" == propertyName) {
        component3D.updateObjects(component3D.homeObjects.slice(0));          
        component3D.groundChangeListener(null);
      } else if ("FLOOR_THICKNESS" == propertyName) {
        component3D.updateObjects(component3D.home.getWalls());          
        component3D.updateObjects(component3D.home.getRooms());
      } else if ("HEIGHT" == propertyName) {
        component3D.updateObjects(component3D.home.getRooms());
      }  
    };
  var levels = this.home.getLevels();
  for (var i = 0; i < levels.length; i++) {
    levels[i].addPropertyChangeListener(this.levelChangeListener);
  }

  this.levelListener = function(ev) {
      var level = ev.getItem();
      switch ((ev.getType())) {
        case CollectionEvent.Type.ADD :
          level.addPropertyChangeListener(component3D.levelChangeListener);
          break;
        case CollectionEvent.Type.DELETE :
          level.removePropertyChangeListener(component3D.levelChangeListener);
          break;
        }
      component3D.updateObjects(component3D.home.getRooms());
    };
  this.home.addLevelsListener(this.levelListener);
}

/**
 * Returns <code>true</code> if the given <code>piece</code> is or contains a door or window.
 * @param {HomePieceOfFurniture} piece
 * @return {boolean}
 * @private
 */
HomeComponent3D.prototype.containsDoorsAndWindows = function(piece) {
  if (piece instanceof HomeFurnitureGroup) {
    var furniture = piece.getFurniture();
    for (var i = 0; i < furniture.length; i++) {
      if (this.containsDoorsAndWindows(furniture[i])) {
        return true;
      }
    }
    return false;
  } else {
    return piece.isDoorOrWindow();
  }
}

/**
 * Returns <code>true</code> if the given <code>piece</code> is or contains a staircase
 * with a top cut out shape.
 * @param {HomePieceOfFurniture} piece
 * @return {boolean}
 * @private
 */
HomeComponent3D.prototype.containsStaircases = function(piece) {
  if (piece instanceof HomeFurnitureGroup) {
    var furniture = piece.getFurniture();
    for (var i = 0; i < furniture.length; i++) {
      if (this.containsStaircases(furniture[i])) {
        return true;
      }
    }
    return false;
  } else {
    return piece.getStaircaseCutOutShape() !== null;
  }
}

/**
 * Adds a wall listener to home walls that updates the children of the given
 * <code>group</code>, each time a wall is added, updated or deleted.
 * @param {Group3D} group
 * @private
 */
HomeComponent3D.prototype.addWallListener = function(group) {
  var component3D = this;
  this.wallChangeListener = function(ev) {
      var propertyName = ev.getPropertyName();
      if ("PATTERN" != propertyName) {
        var updatedWall = ev.getSource();
        component3D.updateWall(updatedWall);          
        component3D.updateObjects(component3D.home.getRooms());
        if (updatedWall.getLevel() != null && updatedWall.getLevel().getElevation() < 0) {
          component3D.groundChangeListener(null);
        }
      }
    };
  var walls = this.home.getWalls();
  for (var i = 0; i < walls.length; i++) {
    walls[i].addPropertyChangeListener(this.wallChangeListener);
  }
  this.wallListener = function(ev) {
      var wall = ev.getItem();
      switch ((ev.getType())) {
        case CollectionEvent.Type.ADD :
          component3D.addObject(group, wall, true, false);
          wall.addPropertyChangeListener(component3D.wallChangeListener);
          break;
        case CollectionEvent.Type.DELETE :
          component3D.deleteObject(wall);
          wall.removePropertyChangeListener(component3D.wallChangeListener);
          break;
      }
      component3D.updateObjects(component3D.home.getRooms());
      component3D.groundChangeListener(null);
    };
  this.home.addWallsListener(this.wallListener);
}

/**
 * Adds a furniture listener to home that updates the children of the given <code>group</code>, 
 * each time a piece of furniture is added, updated or deleted.
 * @private 
 */
HomeComponent3D.prototype.addFurnitureListener = function(group) {
  var component3D = this;
  var updatePieceOfFurnitureGeometry = function(piece) {
      component3D.updateObjects([piece]);
      if (component3D.containsDoorsAndWindows(piece)) {
        component3D.updateObjects(component3D.home.getWalls());
      } else if (component3D.containsStaircases(piece)) {
        component3D.updateObjects(component3D.home.getRooms());
      }
      if (piece.getLevel() !== null && piece.getLevel().getElevation() < 0) {
        component3D.groundChangeListener(null);
      }
    };  
  this.furnitureChangeListener = function(ev) {
      var updatedPiece = ev.getSource();
      var propertyName = ev.getPropertyName();
      if ("X" == propertyName
          || "Y" == propertyName
          || "ANGLE" == propertyName
          || "WIDTH" == propertyName
          || "DEPTH" == propertyName) {
        updatePieceOfFurnitureGeometry(updatedPiece);
      } else if ("HEIGHT" == propertyName
          || "ELEVATION" == propertyName
          || "MODEL_MIRRORED" == propertyName
          || "VISIBLE" == propertyName
          || "LEVEL" == propertyName) {
        updatePieceOfFurnitureGeometry(updatedPiece);
      } else if ("COLOR" == propertyName
          || "TEXTURE" == propertyName
          || "MODEL_MATERIALS" == propertyName
          || "SHININESS" == propertyName
          || ("POWER" == propertyName
              && home.getEnvironment().getSubpartSizeUnderLight() > 0)) {
        component3D.updateObjects([updatedPiece]);
      }
    };

  var furniture = this.home.getFurniture();
  for (var i = 0; i < furniture.length; i++) { 
    var piece = furniture [i];
    if (piece instanceof HomeFurnitureGroup) {
      var groupFurniture = piece.getAllFurniture();
      for (var j = 0; j < groupFurniture.length; j++) {
        groupFurniture [j].addPropertyChangeListener(this.furnitureChangeListener);
      }
    } else {
      piece.addPropertyChangeListener(this.furnitureChangeListener);
    }
  }      
  this.furnitureListener = function(ev) {
      var piece = ev.getItem();
      switch (ev.getType()) {
        case CollectionEvent.Type.ADD :
          if (piece instanceof HomeFurnitureGroup) {
            var groupFurniture = piece.getAllFurniture();
            for (var j = 0; j < groupFurniture.length; j++) {
              var childPiece = groupFurniture [j];
              if (!(childPiece instanceof HomeFurnitureGroup)) {
                component3D.addObject(group, childPiece, true, false);
                childPiece.addPropertyChangeListener(component3D.furnitureChangeListener);
              }
            }
          } else {
            component3D.addObject(group, piece, true, false);
            piece.addPropertyChangeListener(component3D.furnitureChangeListener);
          }
          break;
        case CollectionEvent.Type.DELETE : 
          if (piece instanceof HomeFurnitureGroup) {
            var groupFurniture = piece.getAllFurniture();
            for (var j = 0; j < groupFurniture.length; j++) {
              var childPiece = groupFurniture [j];
              if (!(childPiece instanceof HomeFurnitureGroup)) {
                component3D.deleteObject(childPiece);
                childPiece.removePropertyChangeListener(component3D.furnitureChangeListener);
              }
            }
          } else {
            component3D.deleteObject(piece);
            piece.removePropertyChangeListener(component3D.furnitureChangeListener);
          }
          break;
      }
      // If piece is or contains a door or a window, update walls that intersect with piece
      if (component3D.containsDoorsAndWindows(piece)) {
        component3D.updateObjects(component3D.home.getWalls());
      } else if (component3D.containsStaircases(piece)) {
        component3D.updateObjects(component3D.home.getRooms());
      }
      component3D.groundChangeListener(null);
    };
  this.home.addFurnitureListener(this.furnitureListener);
}

/**
 * Adds a room listener to home rooms that updates the children of the given
 * <code>group</code>, each time a room is added, updated or deleted.
 * @param {Group3D} group
 * @private
 */
HomeComponent3D.prototype.addRoomListener = function(group) {
  var component3D = this;
  this.roomChangeListener = function(ev) {
      var updatedRoom = ev.getSource();
      var propertyName = ev.getPropertyName();
      if ("FLOOR_COLOR" == propertyName
          || "FLOOR_TEXTURE" == propertyName
          || "FLOOR_SHININESS" == propertyName
          || "CEILING_COLOR" == propertyName
          || "CEILING_TEXTURE" == propertyName
          || "CEILING_SHININESS" == propertyName) {
        component3D.updateObjects([updatedRoom]);
      } else if ("FLOOR_VISIBLE" == propertyName
          || "CEILING_VISIBLE" == propertyName
          || "LEVEL" == propertyName) {   
        component3D.updateObjects(component3D.home.getRooms());
        component3D.groundChangeListener(null);
      } else if ("POINTS" == propertyName) {   
        if (component3D.homeObjectsToUpdate != null) {
          // Don't try to optimize if more than one room to update
          component3D.updateObjects(component3D.home.getRooms());
        } else {
          component3D.updateObjects([updatedRoom]);
          // Search the rooms that overlap the updated one
          var oldArea = new java.awt.geom.Area(component3D.getShape(ev.getOldValue()));
          var newArea = new java.awt.geom.Area(component3D.getShape(ev.getNewValue()));
          var updatedRoomLevel = updatedRoom.getLevel(); 
          var rooms = component3D.home.getRooms();
          for (var i = 0; i < rooms.length; i++) {
            var room = rooms[i];
            var roomLevel = room.getLevel();
            if (room != updatedRoom
                && (roomLevel == null
                    || Math.abs(updatedRoomLevel.getElevation() + updatedRoomLevel.getHeight() - (roomLevel.getElevation() + roomLevel.getHeight())) < 1E-5
                    || Math.abs(updatedRoomLevel.getElevation() + updatedRoomLevel.getHeight() - (roomLevel.getElevation() - roomLevel.getFloorThickness())) < 1E-5)) {
              var roomAreaIntersectionWithOldArea = new java.awt.geom.Area(component3D.getShape(room.getPoints()));
              var roomAreaIntersectionWithNewArea = new java.awt.geom.Area(roomAreaIntersectionWithOldArea);
              roomAreaIntersectionWithNewArea.intersect(newArea);                  
              if (!roomAreaIntersectionWithNewArea.isEmpty()) {
                updateObjects([room]);
              } else {
                roomAreaIntersectionWithOldArea.intersect(oldArea);
                if (!roomAreaIntersectionWithOldArea.isEmpty()) {
                  updateObjects([room]);
                }
              }
            }
          }              
        }
        component3D.groundChangeListener(null);
      }            
    };
  var rooms = this.home.getRooms();
  for (var i = 0; i < rooms.length; i++) {
    rooms[i].addPropertyChangeListener(this.roomChangeListener);
  }
  this.roomListener = function(ev) {
      var room = ev.getItem();
      switch ((ev.getType())) {
        case CollectionEvent.Type.ADD :
          component3D.addObject(group, room, ev.getIndex(), true, false);
          room.addPropertyChangeListener(component3D.roomChangeListener);
          break;
        case CollectionEvent.Type.DELETE :
          component3D.deleteObject(room);
          room.removePropertyChangeListener(component3D.roomChangeListener);
          break;
      }
      component3D.updateObjects(component3D.home.getRooms());
      component3D.groundChangeListener(null);
    };
  this.home.addRoomsListener(this.roomListener);
}

/**
 * Returns the path matching points.
 * @param {Array} points
 * @return {GeneralPath}
 * @private
 */
HomeComponent3D.prototype.getShape = function(points) {
  var path = new java.awt.geom.GeneralPath();
  path.moveTo(points[0][0], points[0][1]);
  for (var i = 1; i < points.length; i++) {
    path.lineTo(points[i][0], points[i][1]);
  }
  path.closePath();
  return path;
}

/**
 * Adds a label listener to home labels that updates the children of the given
 * <code>group</code>, each time a label is added, updated or deleted.
 * @param {Group3D} group
 * @private
 */
HomeComponent3D.prototype.addLabelListener = function(group) {
  var component3D = this;
  this.labelChangeListener = function(ev) {
      var label = ev.getSource();
      component3D.updateObjects([label]);
    };
  var labels = this.home.getLabels();
  for (var i = 0; i < labels.length; i++) {
    labels[i].addPropertyChangeListener(this.labelChangeListener);
  }
  this.labelListener = function(ev) {
      var label = ev.getItem();
      switch ((ev.getType())) {
        case CollectionEvent.Type.ADD :
          component3D.addObject(group, label, true, false);
          label.addPropertyChangeListener(component3D.labelChangeListener);
          break;
        case CollectionEvent.Type.DELETE :
          component3D.deleteObject(label);
          label.removePropertyChangeListener(component3D.labelChangeListener);
          break;
      }
    };
  this.home.addLabelsListener(this.labelListener);
}

/**
 * Adds a walls alpha change listener and drawing mode change listener to home
 * environment that updates the home scene objects appearance.
 * @private
 */
HomeComponent3D.prototype.addEnvironmentListeners = function() {
  var component3D = this;
  this.wallsAlphaListener = function(ev) {
      component3D.updateObjects(component3D.home.getWalls());
      component3D.updateObjects(component3D.home.getRooms());
    };
  this.home.getEnvironment().addPropertyChangeListener("WALLS_ALPHA", this.wallsAlphaListener);
}

/**
 * Adds to <code>group</code> a branch matching <code>homeObject</code> at a given <code>index</code>.
 * If <code>index</code> is missing or equal to -1, <code>homeObject</code> will be added at the end of the group.
 * @param {Group3D} group
 * @param {Object}  homeObject
 * @param {number}  [index]
 * @param {boolean} listenToHomeUpdates
 * @param {boolean} waitForLoading
 * @private
 */
HomeComponent3D.prototype.addObject = function(group, homeObject, index, 
                                               listenToHomeUpdates, waitForLoading) {
  if (waitForLoading === undefined) {
    waitForLoading = listenToHomeUpdates;
    listenToHomeUpdates = index;
    index = -1;
  }
  var object3D = this.object3dFactory.createObject3D(this.home, homeObject, waitForLoading);
  if (listenToHomeUpdates) {
    homeObject.object3D = object3D;
    this.homeObjects.push(homeObject);
  }
  if (index === -1) {
    group.addChild(object3D);
  } else {
    group.insertChild(object3D, index);
  }
  return object3D;
}

/**
 * Detaches from the scene the branch matching <code>homeObject</code>.
 * @param {Object}  homeObject
 * @private
 */
HomeComponent3D.prototype.deleteObject = function(homeObject) {
  if (homeObject.object3D) {
    homeObject.object3D.detach();
    delete homeObject.object3D;
    this.homeObjects.splice(this.homeObjects.indexOf(homeObject), 1);
  }
}

/**
 * Updates <code>objects</code> later. 
 * @param {Array} objects
 * @private
 */
HomeComponent3D.prototype.updateObjects = function(objects) {
  if (this.homeObjectsToUpdate) {
    for (var i = 0; i < objects.length; i++) {
      var object = objects [i];
      if (this.homeObjectsToUpdate.indexOf(object) <= -1) {
        this.homeObjectsToUpdate.push(object);
      }
    }
  } else {
    this.homeObjectsToUpdate = objects.slice(0);
    // Invoke later the update of objects of homeObjectsToUpdate
    setTimeout(
        function(component3D) {
          for (var i = 0; i < component3D.homeObjectsToUpdate.length; i++) {
            var homeObject = component3D.homeObjectsToUpdate [i];
            // Check object wasn't deleted since updateObjects call
            if (homeObject.object3D) { 
              homeObject.object3D.update();
            }
          }
          delete component3D.homeObjectsToUpdate;
        }, 0, this);
  }
  this.approximateHomeBoundsCache = null;
}

/**
 * Updates <code>wall</code> geometry,
 * and the walls at its end or start.
 * @param {Wall} wall
 * @private
 */
HomeComponent3D.prototype.updateWall = function(wall) {
  var wallsToUpdate = [];
  wallsToUpdate.push(wall);
  if (wall.getWallAtStart() != null) {
    wallsToUpdate.push(wall.getWallAtStart());
  }
  if (wall.getWallAtEnd() != null) {
    wallsToUpdate.push(wall.getWallAtEnd());
  }
  this.updateObjects(wallsToUpdate);
}


/**
 * A factory able to create instances of {@link Object3DBranch} class.
 * @constructor
 * @author Emmanuel Puybaret
 */
function Object3DBranchFactory() {
}

/**
 * Returns the 3D object matching a given <code>item</code>.
 * @return {Object3DBranch} an instance of a subclass of {@link Object3DBranch}
 */
Object3DBranchFactory.prototype.createObject3D = function(home, item, waitForLoading) {
  if (item instanceof HomePieceOfFurniture) {
    return new HomePieceOfFurniture3D(item, home, waitForLoading);
  } else if (item instanceof Wall) {
    return new Wall3D(item, home, waitForLoading);
  } else if (item instanceof Room) {
    return new Room3D(item, home, false, waitForLoading);
   } else if (item instanceof Label) {
     return new Label3D(item, home, waitForLoading);
  } else {
    return new Group3D();
  }  
}
