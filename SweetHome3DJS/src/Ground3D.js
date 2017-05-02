/*
 * Ground3D.js
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

// Requires scene3d.js
//          Object3DBranch.js
//          TextureManager.js


/**
 * Creates a 3D ground for the given <code>home</code>.
 * @param {Home} home
 * @param {number} originX
 * @param {number} originY
 * @param {number} width
 * @param {number} depth
 * @param {boolean} waitTextureLoadingEnd
 * @constructor
 * @extends Object3DBranch
 * @author Emmanuel Puybaret
 */
function Ground3D(home, originX, originY, width, depth, waitTextureLoadingEnd) {
  Object3DBranch.call(this);
  this.setUserData(home);      
  this.originX = originX;
  this.originY = originY;
  this.width = width;
  this.depth = depth;

  var groundAppearance = new Appearance3D();
  var groundShape = new Shape3D();
  groundShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
  groundShape.setAppearance(groundAppearance);

  this.addChild(groundShape);
  this.update(waitTextureLoadingEnd);
}
Ground3D.prototype = Object.create(Object3DBranch.prototype);
Ground3D.prototype.constructor = Ground3D;

/**
 * Updates ground coloring and texture attributes from home ground color and texture.
 * @param {boolean} [waitTextureLoadingEnd]
 */
Ground3D.prototype.update = function(waitTextureLoadingEnd) {
  if (waitTextureLoadingEnd === undefined) {
    waitTextureLoadingEnd = false;
  }
  var home = this.getUserData();
  var groundShape = this.getChild(0);
  var currentGeometriesCount = groundShape.getGeometries().length;
  var groundAppearance = groundShape.getAppearance();
  var groundTexture = home.getEnvironment().getGroundTexture();
  if (groundTexture === null) {
    var groundColor = home.getEnvironment().getGroundColor();
    this.updateAppearanceMaterial(groundAppearance, groundColor, groundColor, 0);
    groundAppearance.setTextureImage(null);
  } else {
    this.updateAppearanceMaterial(groundAppearance, Object3DBranch.DEFAULT_COLOR, Object3DBranch.DEFAULT_COLOR, 0);
    this.updateTextureTransform(groundAppearance, groundTexture, true);
    TextureManager.getInstance().loadTexture(groundTexture.getImage(), waitTextureLoadingEnd, {
        textureUpdated : function(texture) {
          groundAppearance.setTextureImage(texture);
        },
        textureError : function(error) {
          return this.textureUpdated(TextureManager.getInstance().getErrorImage());
        },
        progression : function(part, info, percentage) {
        }
      });
  }
  
  var areaRemovedFromGround = new java.awt.geom.Area();
  var undergroundLevelAreas = [];
  var rooms = home.getRooms();
  for (var i = 0; i < rooms.length; i++) {
    var room = rooms[i];
    var roomLevel = room.getLevel();
    if ((roomLevel === null || roomLevel.isViewable()) 
        && room.isFloorVisible()) {
      var roomPoints = room.getPoints();
      if (roomPoints.length > 2) {
        var roomArea = new java.awt.geom.Area(this.getShape(roomPoints));
        var levelAreas = roomLevel !== null && roomLevel.getElevation() < 0 
            ? this.getUndergroundAreas(undergroundLevelAreas, roomLevel) 
            : null;
        if (roomLevel === null 
            || (roomLevel.getElevation() <= 0 
                && roomLevel.isViewableAndVisible())) {
          areaRemovedFromGround.add(roomArea);
          if (levelAreas !== null) {
            levelAreas.roomArea.add(roomArea);
          }
        }
        if (levelAreas !== null) {
          levelAreas.undergroundArea.add(roomArea);
        }
      }
    }
  }
  var furniture = home.getFurniture();
  for (var i = 0; i < furniture.length; i++) {
    var piece = furniture[i];
    var pieceLevel = piece.getLevel();
    if (piece.getGroundElevation() < 0 
        && pieceLevel !== null 
        && pieceLevel.isViewable() 
        && pieceLevel.getElevation() < 0) {
      var levelAreas = this.getUndergroundAreas(undergroundLevelAreas, pieceLevel);
      if (piece.getStaircaseCutOutShape() === null) {
        levelAreas.undergroundArea.add(new java.awt.geom.Area(this.getShape(piece.getPoints())));
      } else {
        levelAreas.undergroundArea.add(ModelManager.getInstance().getAreaOnFloor(piece));
      }
    }
  }
  var walls = home.getWalls();
  for (var i = 0; i < walls.length; i++) {
    var wall = walls[i];
    var wallLevel = wall.getLevel();
    if (wallLevel !== null 
        && wallLevel.isViewable() 
        && wallLevel.getElevation() < 0) {
      var levelAreas = this.getUndergroundAreas(undergroundLevelAreas, wallLevel);
      levelAreas.wallArea.add(new java.awt.geom.Area(this.getShape(wall.getPoints())));
    }
  }
  var undergroundAreas = undergroundLevelAreas;
  for (var i = 0; i < undergroundAreas.length; i++) {
    var levelAreas = undergroundAreas[i];
    var areaPoints = this.getPoints(levelAreas.wallArea);
    for (var j = 0; j < areaPoints.length; j++) {
      var points = areaPoints[j];
      if (!new Room(points).isClockwise()) {
        levelAreas.undergroundArea.add(new java.awt.geom.Area(this.getShape(points)));
      }
    }
  }
  
  undergroundAreas.sort(function (levelAreas1, levelAreas2) {
      return -(levelAreas1.level.getElevation() - levelAreas2.level.getElevation());
    });
  for (var i = 0; i < undergroundAreas.length; i++) {
    var levelAreas = undergroundAreas[i];
    var level = levelAreas.level;
    var area = levelAreas.undergroundArea;
    var areaAtStart = area.clone();
    levelAreas.undergroundSideArea.add(area.clone());
    for (var j = 0; j < undergroundAreas.length; j++) {
      var otherLevelAreas = undergroundAreas[j];
      if (otherLevelAreas.level.getElevation() < level.getElevation()) {
        var areaPoints = this.getPoints(otherLevelAreas.undergroundArea);
        for (var k = 0; k < areaPoints.length; k++) {
          var points = areaPoints[k];
          if (!new Room(points).isClockwise()) {
            var pointsArea = new java.awt.geom.Area(this.getShape(points));
            area.subtract(pointsArea);
            levelAreas.undergroundSideArea.add(pointsArea);
          }
        }
      }
    }
    var areaPoints = this.getPoints(area);
    for (var j = 0; j < areaPoints.length; j++) {
      var points = areaPoints[j];
      if (new Room(points).isClockwise()) {
        var coveredHole = new java.awt.geom.Area(this.getShape(points));
        coveredHole.exclusiveOr(areaAtStart);
        coveredHole.subtract(areaAtStart);
        levelAreas.upperLevelArea.add(coveredHole);
      } else {
        areaRemovedFromGround.add(new java.awt.geom.Area(this.getShape(points)));
      }
    }
  }
  for (var i = 0; i < undergroundAreas.length; i++) {
    var levelAreas = undergroundAreas[i];
    var roomArea = levelAreas.roomArea;
    if (roomArea != null) {
      levelAreas.undergroundArea.subtract(roomArea);
    }
  }
  
  var groundArea = new java.awt.geom.Area(this.getShape(
      [[this.originX, this.originY], 
       [this.originX, this.originY + this.depth], 
       [this.originX + this.width, this.originY + this.depth], 
       [this.originX + this.width, this.originY]]));
  var removedAreaBounds = areaRemovedFromGround.getBounds2D();
  if (!groundArea.getBounds2D().equals(removedAreaBounds)) {
    var outsideGroundArea = groundArea;
    if (areaRemovedFromGround.isEmpty()) {
      removedAreaBounds = new java.awt.geom.Rectangle2D.Float(Math.max(-5000.0, this.originX), Math.max(-5000.0, this.originY), 0, 0);
      removedAreaBounds.add(Math.min(5000.0, this.originX + this.width), 
          Math.min(5000.0, this.originY + this.depth));
    } else {
      removedAreaBounds.add(Math.max(removedAreaBounds.getMinX() - 5000.0, this.originX), 
          Math.max(removedAreaBounds.getMinY() - 5000.0, this.originY));
      removedAreaBounds.add(Math.min(removedAreaBounds.getMaxX() + 5000.0, this.originX + this.width), 
          Math.min(removedAreaBounds.getMaxY() + 5000.0, this.originY + this.depth));
    }
    groundArea = new java.awt.geom.Area(removedAreaBounds);
    outsideGroundArea.subtract(groundArea);
    this.addAreaGeometry(groundShape, groundTexture, outsideGroundArea, 0);
  }
  groundArea.subtract(areaRemovedFromGround);
  undergroundAreas.splice(0, 0, new Ground3D.LevelAreas(new Level("Ground", 0, 0, 0), groundArea));
  var previousLevelElevation = 0;
  for (var i = 0; i < undergroundAreas.length; i++) {
    var levelAreas = undergroundAreas[i];
    var elevation = levelAreas.level.getElevation();
    this.addAreaGeometry(groundShape, groundTexture, levelAreas.undergroundArea, elevation);
    if (previousLevelElevation - elevation > 0) {
      var areaPoints = this.getPoints(levelAreas.undergroundSideArea);
      for (var j = 0; j < areaPoints.length; j++) {
        var points = areaPoints[j];
        this.addAreaSidesGeometry(groundShape, groundTexture, points, elevation, previousLevelElevation - elevation);
      }
      this.addAreaGeometry(groundShape, groundTexture, levelAreas.upperLevelArea, previousLevelElevation);
    }
    previousLevelElevation = elevation;
  }
  
  for (var i = currentGeometriesCount - 1; i >= 0; i--) {
    groundShape.removeGeometry(i);
  }
}

/**
 * Returns the list of points that defines the given area.
 * @param {Area} area
 * @return {Array}
 * @private
 */
Ground3D.prototype.getPoints = function(area) {
  var areaPoints = [];
  var areaPartPoints = [];
  var previousRoomPoint = null;
  for (var it = area.getPathIterator(null, 1); !it.isDone(); it.next()) {
    var roomPoint = [0, 0];
    if (it.currentSegment(roomPoint) === java.awt.geom.PathIterator.SEG_CLOSE) {
      if (areaPartPoints[0][0] === previousRoomPoint[0] 
          && areaPartPoints[0][1] === previousRoomPoint[1]) {
        areaPartPoints.splice(areaPartPoints.length - 1, 1);
      }
      if (areaPartPoints.length > 2) {
        areaPoints.push(areaPartPoints.slice(0));
      }
      areaPartPoints.length = 0;
      previousRoomPoint = null;
    } else {
      if (previousRoomPoint === null 
          || roomPoint[0] !== previousRoomPoint[0] 
          || roomPoint[1] !== previousRoomPoint[1]) {
        areaPartPoints.push(roomPoint);
      }
      previousRoomPoint = roomPoint;
    }
  }
  return areaPoints;
}

/**
 * Returns the {@link LevelAreas} instance matching the given level.
 * @param {Object} undergroundAreas
 * @param {Level} level
 * @return {Ground3D.LevelAreas}
 * @private
 */
Ground3D.prototype.getUndergroundAreas = function(undergroundAreas, level) {
  var levelAreas = null;
  for (var i = 0; i < undergroundAreas.length; i++) { 
    if (undergroundAreas[i].level === level) { 
      levelAreas = undergroundAreas[i]; 
      break;
    } 
  } 
  if (levelAreas === null) {
    undergroundAreas.push(levelAreas = new Ground3D.LevelAreas(level));
  }
  return levelAreas;
};

/**
 * Adds to ground shape the geometry matching the given area.
 * @param {Shape3D} groundShape
 * @param {HomeTexture} groundTexture
 * @param {Area} area
 * @param {number} elevation
 * @private
 */
Ground3D.prototype.addAreaGeometry = function(groundShape, groundTexture, area, elevation) {
  var areaPoints = this.getAreaPoints(area, 1, false);
  if (areaPoints.length != 0) {
    var vertexCount = 0;
    var stripCounts = new Array(areaPoints.length);
    for (var i = 0; i < stripCounts.length; i++) {
      stripCounts[i] = areaPoints[i].length;
      vertexCount += stripCounts[i];
    }
    var geometryCoords = new Array(vertexCount);
    var geometryTextureCoords = groundTexture !== null 
        ? new Array(vertexCount) 
        : null;
        
    var j = 0;
    for (var index = 0; index < areaPoints.length; index++) {
      var areaPartPoints = areaPoints[index];
      for (var i = 0; i < areaPartPoints.length; i++, j++) {
        var point = areaPartPoints[i];
        geometryCoords[j] = vec3.fromValues(point[0], elevation, point[1]);
        if (groundTexture !== null) {
          geometryTextureCoords[j] = vec2.fromValues(point[0] - this.originX, this.originY - point[1]);
        }
      }
    }
      
    var geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
    geometryInfo.setCoordinates(geometryCoords);
    if (groundTexture !== null) {
      geometryInfo.setTextureCoordinates(geometryTextureCoords);
    }
    geometryInfo.setStripCounts(stripCounts);
    geometryInfo.setCreaseAngle(0);
    geometryInfo.setGeneratedNormals(true);
    groundShape.addGeometry(geometryInfo.getIndexedGeometryArray());
  }
}
  
/**
 * Adds to ground shape the geometry matching the given area sides.
 * @param {Shape3D} groundShape
 * @param {HomeTexture} groundTexture
 * @param {Array} areaPoints
 * @param {number} elevation
 * @param {number} sideHeight
 * @private
 */
Ground3D.prototype.addAreaSidesGeometry = function(groundShape, groundTexture, areaPoints, elevation, sideHeight) {
  var geometryCoords = new Array(areaPoints.length * 4);
  var geometryTextureCoords = groundTexture !== null 
      ? new Array(geometryCoords.length) 
      : null;
  for (var i = 0, j = 0; i < areaPoints.length; i++) {
    var point = areaPoints[i];
    var nextPoint = areaPoints[i < areaPoints.length - 1 ? i + 1 : 0];
    geometryCoords[j++] = vec3.fromValues(point[0], elevation, point[1]);
    geometryCoords[j++] = vec3.fromValues(point[0], elevation + sideHeight, point[1]);
    geometryCoords[j++] = vec3.fromValues(nextPoint[0], elevation + sideHeight, nextPoint[1]);
    geometryCoords[j++] = vec3.fromValues(nextPoint[0], elevation, nextPoint[1]);
    if (groundTexture !== null) {
      var distance = java.awt.geom.Point2D.distance(point[0], point[1], nextPoint[0], nextPoint[1]);
      geometryTextureCoords[j - 4] = vec2.fromValues(point[0], elevation);
      geometryTextureCoords[j - 3] = vec2.fromValues(point[0], elevation + sideHeight);
      geometryTextureCoords[j - 2] = vec2.fromValues(point[0] - distance, elevation + sideHeight);
      geometryTextureCoords[j - 1] = vec2.fromValues(point[0] - distance, elevation);
    }
  }
  
  var geometryInfo = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
  geometryInfo.setCoordinates(geometryCoords);
  if (groundTexture !== null) {
    geometryInfo.setTextureCoordinates(geometryTextureCoords);
  }
  geometryInfo.setCreaseAngle(0);
  geometryInfo.setGeneratedNormals(true);
  groundShape.addGeometry(geometryInfo.getIndexedGeometryArray());
}

/**
 * Areas of underground levels.
 * @constructor
 * @private
 */
Ground3D.LevelAreas = function(level, undergroundArea) {
  if (undergroundArea === undefined) {
    undergroundArea = new java.awt.geom.Area();
  }
  this.level = level;
  this.undergroundArea = undergroundArea;
  this.roomArea = new java.awt.geom.Area();
  this.wallArea = new java.awt.geom.Area();
  this.undergroundSideArea = new java.awt.geom.Area();
  this.upperLevelArea = new java.awt.geom.Area();
}