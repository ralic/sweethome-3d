/*
 * Room3D.js
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
 * Creates the 3D room matching the given home <code>room</code>.
 * @param {Room} room
 * @param {Home} home
 * @param {boolean} ignoreCeilingPart
 * @param {boolean} waitTextureLoadingEnd
 * @constructor
 * @extends Object3DBranch
 * @author Emmanuel Puybaret
 */
function Room3D(room, home, ignoreCeilingPart, waitTextureLoadingEnd) {
  Object3DBranch.call(this);
  if (ignoreCeilingPart === undefined) {
    ignoreCeilingPart = false;
    waitTextureLoadingEnd = false;
  }
  
  this.setUserData(room);      
  this.home = home;

  this.addChild(this.createRoomPartShape());
  this.addChild(this.createRoomPartShape());
  this.updateRoomGeometry();
  this.updateRoomAppearance(waitTextureLoadingEnd);
  if (ignoreCeilingPart) {
    this.removeChild(Room3D.CEILING_PART);
  }
}
Room3D.prototype = Object.create(Object3DBranch.prototype);
Room3D.prototype.constructor = Room3D;

Room3D.FLOOR_PART  = 0;
Room3D.CEILING_PART = 1;

/**
 * Returns a new room part shape with no geometry
 * and a default appearance with a white material.
 * @return {Node3D}
 * @private
 */
Room3D.prototype.createRoomPartShape = function() {
  var roomShape = new Shape3D();
  roomShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
  var roomAppearance = new Appearance3D();
  roomShape.setAppearance(roomAppearance);
  this.updateAppearanceMaterial(roomAppearance, Object3DBranch.DEFAULT_COLOR, Object3DBranch.DEFAULT_AMBIENT_COLOR, 0);
  return roomShape;
}

Room3D.prototype.update = function() {
  this.updateRoomGeometry();
  this.updateRoomAppearance(false);
}

/**
 * Sets the 3D geometry of this room shapes that matches its 2D geometry.
 * @private
 */
Room3D.prototype.updateRoomGeometry = function() {
  this.updateRoomPartGeometry(Room3D.FLOOR_PART, this.getUserData().getFloorTexture());
  this.updateRoomPartGeometry(Room3D.CEILING_PART, this.getUserData().getCeilingTexture());
}

Room3D.prototype.updateRoomPartGeometry = function(roomPart, texture) {
  var roomShape = this.getChild(roomPart);
  var currentGeometriesCount = roomShape.getGeometries().length;
  var room = this.getUserData();
  if (room.getLevel() == null || room.getLevel().isViewableAndVisible()) {
    var geometries = this.createRoomGeometries(roomPart, texture);
    for (var i = 0; i < geometries.length; i++) {
      roomShape.addGeometry(geometries[i]);
    }
  }
  for (var i = currentGeometriesCount - 1; i >= 0; i--) {
    roomShape.removeGeometry(i);
  }
}

/**
 * Returns room geometry computed from its points.
 * @param {number} roomPart
 * @param {HomeTexture} texture
 * @return {Array}
 * @private
 */
Room3D.prototype.createRoomGeometries = function(roomPart, texture) {
  var room = this.getUserData();
  var points = room.getPoints();
  if ((roomPart === Room3D.FLOOR_PART && room.isFloorVisible() 
        || roomPart === Room3D.CEILING_PART && room.isCeilingVisible()) 
      && points.length > 2) {
    var roomLevel = room.getLevel();
    var levels = this.home.getLevels();
    var lastLevel = this.isLastLevel(roomLevel, levels);
    var floorBottomElevation;
    var roomElevation;
    if (roomLevel != null) {
      roomElevation = roomLevel.getElevation();
      floorBottomElevation = roomElevation - roomLevel.getFloorThickness();
    } else {
      roomElevation = 0;
      floorBottomElevation = 0;
    }
    var firstLevelElevation;
    if (levels.length === 0) {
      firstLevelElevation = 0;
    } else {
      firstLevelElevation = levels[0].getElevation();
    }
    var floorBottomVisible = roomPart === Room3D.FLOOR_PART 
        && roomLevel !== null 
        && roomElevation !== firstLevelElevation;
    
    var roomsAtSameElevation = [];
    var ceilingsAtSameFloorBottomElevation = [];
    var rooms = this.home.getRooms();
    for (var i = 0; i < rooms.length; i++) {
      var homeRoom = rooms[i];
      var homeRoomLevel = homeRoom.getLevel();
      if (homeRoomLevel === null || homeRoomLevel.isViewableAndVisible()) {
        if (room === homeRoom 
            || roomLevel === homeRoomLevel 
                && (roomPart === Room3D.FLOOR_PART && homeRoom.isFloorVisible() 
                    || roomPart === Room3D.CEILING_PART && homeRoom.isCeilingVisible()) 
            || roomLevel != null 
                && homeRoomLevel != null 
                && (roomPart === Room3D.FLOOR_PART 
                        && homeRoom.isFloorVisible() 
                        && Math.abs(roomElevation - homeRoomLevel.getElevation()) < 1.0E-4 
                    || roomPart === Room3D.CEILING_PART 
                        && homeRoom.isCeilingVisible() 
                        && !lastLevel 
                        && !this.isLastLevel(homeRoomLevel, levels) 
                        && Math.abs(roomElevation + roomLevel.getHeight() - (homeRoomLevel.getElevation() + homeRoomLevel.getHeight())) < 1.0E-4)) {
          roomsAtSameElevation.push(homeRoom);
        } else if (floorBottomVisible 
                    && homeRoomLevel != null 
                    && homeRoom.isCeilingVisible() 
                    && !this.isLastLevel(homeRoomLevel, levels) 
                    && Math.abs(floorBottomElevation - (homeRoomLevel.getElevation() + homeRoomLevel.getHeight())) < 1.0E-4) {
          ceilingsAtSameFloorBottomElevation.push(homeRoom);
        }
      }
    }
    if (roomLevel != null) {
      roomsAtSameElevation.sort(function (room1, room2) {
          var comparison = (room1.getLevel().getElevation() - room2.getLevel().getElevation());
          if (comparison !== 0) {
            return comparison;
          } else {
            return room1.getLevel().getElevationIndex() - room2.getLevel().getElevationIndex();
          }
        });
    }
    
    var visibleStaircases;
    if (roomLevel === null 
        || roomPart === Room3D.CEILING_PART 
            && lastLevel) {
      visibleStaircases = [];
    } else {
      visibleStaircases = this.getVisibleStaircases(this.home.getFurniture(), roomPart, roomLevel, 
          roomLevel.getElevation() === firstLevelElevation);
    }
    
    var sameElevation = true;
    if (roomPart === Room3D.CEILING_PART && (roomLevel === null || lastLevel)) {
      var firstPointElevation = this.getRoomHeightAt(points[0][0], points[0][1]);
      for (var i = 1; i < points.length && sameElevation; i++) {
        sameElevation = this.getRoomHeightAt(points[i][0], points[i][1]) === firstPointElevation;
      }
    }
    
    var roomPoints;
    var roomHoles;
    var roomPointsWithoutHoles;
    var roomVisibleArea;
    if (!room.isSingular() 
        || sameElevation 
            && (roomsAtSameElevation[roomsAtSameElevation.length - 1] !== room 
                || visibleStaircases.length > 0)) {
      roomVisibleArea = new java.awt.geom.Area(this.getShape(points));
      if ((roomsAtSameElevation.indexOf(room) >= 0)) {
        for (var i = roomsAtSameElevation.length - 1; i > 0 && roomsAtSameElevation[i] !== room; i--) {
          var otherRoom = roomsAtSameElevation[i];
          roomVisibleArea.subtract(new java.awt.geom.Area(this.getShape(otherRoom.getPoints())));
        }
      }
      this.removeStaircasesFromArea(visibleStaircases, roomVisibleArea);
      roomPoints = [];
      roomHoles = [];
      roomPointsWithoutHoles = this.getAreaPoints(roomVisibleArea, roomPoints, roomHoles, 1, roomPart === Room3D.CEILING_PART);
    } else {
      var clockwise = room.isClockwise();
      if (clockwise && roomPart === Room3D.FLOOR_PART 
          || !clockwise && roomPart === Room3D.CEILING_PART) {
        points = this.getReversedArray(points);
      }
      roomPointsWithoutHoles = 
      roomPoints = [points];
      roomHoles = [];
      roomVisibleArea = null;
    }
    
    var geometries = [];
    var subpartSize = this.home.getEnvironment().getSubpartSizeUnderLight();
    
    if (roomPointsWithoutHoles.length !== 0) {
      var roomPointElevations = [];
      var roomAtSameElevation = true;
      for (var i = 0; i < roomPointsWithoutHoles.length; i++) {
        var roomPartPoints = roomPointsWithoutHoles[i];
        var roomPartPointElevations = new Array(roomPartPoints.length);
        for (var j = 0; j < roomPartPoints.length; j++) {
          roomPartPointElevations[j] = roomPart === Room3D.FLOOR_PART 
              ? roomElevation 
              : this.getRoomHeightAt(roomPartPoints[j][0], roomPartPoints[j][1]);
          if (roomAtSameElevation && j > 0) {
            roomAtSameElevation = roomPartPointElevations[j] === roomPartPointElevations[j - 1];
          }
        }
        roomPointElevations.push(roomPartPointElevations);
      }
      
      if (roomAtSameElevation && subpartSize > 0) {
        for (var j = 0; j < roomPointsWithoutHoles.length; j++) {
          var roomPartPoints = roomPointsWithoutHoles[j];
          var xMin = Number.MAX_VALUE;
          var xMax = Number.MIN_VALUE;
          var zMin = Number.MAX_VALUE;
          var zMax = Number.MIN_VALUE;
          for (var i = 0; i < roomPartPoints.length; i++) {
            var point = roomPartPoints[i];
            xMin = Math.min(xMin, point[0]);
            xMax = Math.max(xMax, point[0]);
            zMin = Math.min(zMin, point[1]);
            zMax = Math.max(zMax, point[1]);
          }
          
          var roomPartArea = new java.awt.geom.Area(this.getShape(roomPartPoints));
          for (var xSquare = xMin; xSquare < xMax; xSquare += subpartSize) {
            for (var zSquare = zMin; zSquare < zMax; zSquare += subpartSize) {
              var roomPartSquare = new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(xSquare, zSquare, subpartSize, subpartSize));
              roomPartSquare.intersect(roomPartArea);
              if (!roomPartSquare.isEmpty()) {
                var geometryPartPointsWithoutHoles = this.getAreaPoints(roomPartSquare, 1, roomPart === Room3D.CEILING_PART);
                if (!(geometryPartPointsWithoutHoles.length == 0)) {
                  geometries.push(this.computeRoomPartGeometry(geometryPartPointsWithoutHoles, 
                      null, roomLevel, roomPointElevations[i][0], floorBottomElevation, 
                      roomPart === Room3D.FLOOR_PART, false, texture));
                }
              }
            }
          }
        }
      } else {
        geometries.push(this.computeRoomPartGeometry(roomPointsWithoutHoles, roomPointElevations, roomLevel, 
            roomElevation, floorBottomElevation, roomPart === Room3D.FLOOR_PART, false, texture));
      }
      if (roomLevel != null 
          && roomPart === Room3D.FLOOR_PART 
          && roomLevel.getElevation() !== firstLevelElevation) {
        geometries.push(this.computeRoomBorderGeometry(roomPoints, roomHoles, roomLevel, roomElevation, texture));
      }
    }
    
    if (floorBottomVisible) {
      var floorBottomPointsWithoutHoles;
      if (roomVisibleArea != null 
          || ceilingsAtSameFloorBottomElevation.length > 0) {
        var floorBottomVisibleArea = roomVisibleArea != null ? roomVisibleArea : new java.awt.geom.Area(this.getShape(points));
        for (var i = 0; i < ceilingsAtSameFloorBottomElevation.length; i++) {
          var otherRoom = ceilingsAtSameFloorBottomElevation[i];
          floorBottomVisibleArea.subtract(new java.awt.geom.Area(this.getShape(otherRoom.getPoints())));
        }
        floorBottomPointsWithoutHoles = this.getAreaPoints(floorBottomVisibleArea, 1, true);
      } else {
        floorBottomPointsWithoutHoles = [this.getReversedArray(points)];
      }
      
      if (floorBottomPointsWithoutHoles.length !== 0) {
        if (subpartSize > 0) {
          for (var i = 0; i < floorBottomPointsWithoutHoles.length; i++) {
            var floorBottomPartPoints = floorBottomPointsWithoutHoles[i];
            var xMin = Number.MAX_VALUE;
            var xMax = Number.MIN_VALUE;
            var zMin = Number.MAX_VALUE;
            var zMax = Number.MIN_VALUE;
            for (var j = 0; j < floorBottomPartPoints.length; j++) {
              var point = floorBottomPartPoints[j];
              xMin = Math.min(xMin, point[0]);
              xMax = Math.max(xMax, point[0]);
              zMin = Math.min(zMin, point[1]);
              zMax = Math.max(zMax, point[1]);
            }
            
            var floorBottomPartArea = new java.awt.geom.Area(this.getShape(floorBottomPartPoints));
            for (var xSquare = xMin; xSquare < xMax; xSquare += subpartSize) {
              for (var zSquare = zMin; zSquare < zMax; zSquare += subpartSize) {
                var floorBottomPartSquare = new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(xSquare, zSquare, subpartSize, subpartSize));
                floorBottomPartSquare.intersect(floorBottomPartArea);
                if (!floorBottomPartSquare.isEmpty()) {
                  var geometryPartPointsWithoutHoles = this.getAreaPoints(floorBottomPartSquare, 1, true);
                  if (geometryPartPointsWithoutHoles.length !== 0) {
                    geometries.push(this.computeRoomPartGeometry(geometryPartPointsWithoutHoles, 
                        null, roomLevel, roomElevation, floorBottomElevation, 
                        true, true, texture));
                  }
                }
              }
            }
          }
        } else {
          geometries.push(this.computeRoomPartGeometry(floorBottomPointsWithoutHoles, null, roomLevel, 
              roomElevation, floorBottomElevation, true, true, texture));
        }
      }
    }
    
    return geometries.slice(0);
  } else {
    return [];
  }
}

/**
 * Returns the room part geometry matching the given points.
 * @param {Array} geometryPoints
 * @param {Array} roomPointElevations
 * @param {Level} roomLevel
 * @param {number} roomPartElevation
 * @param {number} floorBottomElevation
 * @param {boolean} floorPart
 * @param {boolean} floorBottomPart
 * @param {HomeTexture} texture
 * @return {IndexedGeometryArray3D}
 * @private
 */
Room3D.prototype.computeRoomPartGeometry = function(geometryPoints, roomPointElevations, 
                                                    roomLevel, roomPartElevation, floorBottomElevation, 
                                                    floorPart, floorBottomPart, texture) {
  var stripCounts = new Array(geometryPoints.length);
  var vertexCount = 0;
  for (var i = 0; i < geometryPoints.length; i++) {
    var areaPoints = geometryPoints[i];
    stripCounts[i] = areaPoints.length;
    vertexCount += stripCounts[i];
  }
  var coords = new Array(vertexCount);
  var i = 0;
  for (var j = 0; j < geometryPoints.length; j++) {
    var areaPoints = geometryPoints[j];
    var roomPartPointElevations = roomPointElevations != null 
        ? roomPointElevations[j] 
        : null;
    for (var k = 0; k < areaPoints.length; k++) {
      var y = floorBottomPart 
          ? floorBottomElevation 
          : (roomPartPointElevations != null 
                ? roomPartPointElevations[k] 
                : roomPartElevation);
      coords[i++] = vec3.fromValues(areaPoints[k][0], y, areaPoints[k][1]);
    }
  }
  
  var geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
  geometryInfo.setCoordinates(coords);
  geometryInfo.setStripCounts(stripCounts);
  
  if (texture != null) {
    var textureCoords = new Array(vertexCount);
    i = 0;
    for (var j = 0; j < geometryPoints.length; j++) {
      var areaPoints = geometryPoints[j];
      for (var k = 0; k < areaPoints.length; k++) {
        textureCoords[i++] = vec2.fromValues(areaPoints[k][0], 
            floorPart 
                ? -areaPoints[k][1] 
                : areaPoints[k][1]);
      }
    }
    geometryInfo.setTextureCoordinates(textureCoords);
  }
  geometryInfo.setGeneratedNormals(true);
  return geometryInfo.getIndexedGeometryArray();
}

/**
 * Returns the room border geometry matching the given points.
 * @param {Array} geometryRooms
 * @param {Array} geometryHoles
 * @param {Level} roomLevel
 * @param {number} roomElevation
 * @param {HomeTexture} texture
 * @return {IndexedGeometryArray3D}
 * @private
 */
Room3D.prototype.computeRoomBorderGeometry = function(geometryRooms, geometryHoles, 
                                                      roomLevel, roomElevation, texture) {
  var vertexCount = 0;
  for (var i = 0; i < geometryRooms.length; i++) {
    vertexCount += geometryRooms[i].length;
  }
  for (var i = 0; i < geometryHoles.length; i++) {
    vertexCount += geometryHoles[i].length;
  }
  vertexCount = vertexCount * 4;
  
  var i = 0;
  var coords = new Array(vertexCount);
  var floorBottomElevation = roomElevation - roomLevel.getFloorThickness();
  for (var index = 0; index < geometryRooms.length; index++) {
    var geometryPoints = geometryRooms[index];
    for (var j = 0; j < geometryPoints.length; j++) {
      coords[i++] = vec3.fromValues(geometryPoints[j][0], roomElevation, geometryPoints[j][1]);
      coords[i++] = vec3.fromValues(geometryPoints[j][0], floorBottomElevation, geometryPoints[j][1]);
      var nextPoint = j < geometryPoints.length - 1 
          ? j + 1 
          : 0;
      coords[i++] = vec3.fromValues(geometryPoints[nextPoint][0], floorBottomElevation, geometryPoints[nextPoint][1]);
      coords[i++] = vec3.fromValues(geometryPoints[nextPoint][0], roomElevation, geometryPoints[nextPoint][1]);
    }
  }
  for (var index = 0; index < geometryHoles.length; index++) {
    var geometryHole = geometryHoles[index];
    for (var j = 0; j < geometryHole.length; j++) {
      coords[i++] = vec3.fromValues(geometryHole[j][0], roomElevation, geometryHole[j][1]);
      var nextPoint = j < geometryHole.length - 1 
          ? j + 1 
          : 0;
      coords[i++] = vec3.fromValues(geometryHole[nextPoint][0], roomElevation, geometryHole[nextPoint][1]);
      coords[i++] = vec3.fromValues(geometryHole[nextPoint][0], floorBottomElevation, geometryHole[nextPoint][1]);
      coords[i++] = vec3.fromValues(geometryHole[j][0], floorBottomElevation, geometryHole[j][1]);
    }
  }
  
  var geometryInfo = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
  geometryInfo.setCoordinates(coords);
  
  if (texture != null) {
    var textureCoords = new Array(vertexCount);
    i = 0;
    for (var index = 0; index < geometryRooms.length; index++) {
      var geometryPoints = geometryRooms[index];
      for (var j = 0; j < geometryPoints.length; j++) {
        textureCoords[i++] = vec2.fromValues(0, roomLevel.getFloorThickness());
        textureCoords[i++] = vec2.fromValues(0, 0);
        var nextPoint = j < geometryPoints.length - 1 
            ? j + 1 
            : 0;
        var textureCoord = java.awt.geom.Point2D.distance(geometryPoints[j][0], geometryPoints[j][1], geometryPoints[nextPoint][0], geometryPoints[nextPoint][1]);
        textureCoords[i++] = vec2.fromValues(textureCoord, 0);
        textureCoords[i++] = vec2.fromValues(textureCoord, roomLevel.getFloorThickness());
      }
    }
    for (var index = 0; index < geometryHoles.length; index++) {
      var geometryHole = geometryHoles[index];
      for (var j = 0; j < geometryHole.length; j++) {
        textureCoords[i++] = vec2.fromValues(0, 0);
        var nextPoint = j < geometryHole.length - 1 
            ? j + 1 
            : 0;
        var textureCoord = java.awt.geom.Point2D.distance(geometryHole[j][0], geometryHole[j][1], geometryHole[nextPoint][0], geometryHole[nextPoint][1]);
        textureCoords[i++] = vec2.fromValues(textureCoord, 0);
        textureCoords[i++] = vec2.fromValues(textureCoord, roomLevel.getFloorThickness());
        textureCoords[i++] = vec2.fromValues(0, roomLevel.getFloorThickness());
      }
    }
    geometryInfo.setTextureCoordinates(textureCoords);
  }
  
  geometryInfo.setCreaseAngle(Math.PI / 8);
  geometryInfo.setGeneratedNormals(true);
  return geometryInfo.getIndexedGeometryArray();
}

Room3D.prototype.removeStaircasesFromArea = function (visibleStaircases, area) {
  var modelManager = ModelManager.getInstance();
  for (var i = 0; i < visibleStaircases.length; i++) {
    var staircase = visibleStaircases[i];
    area.subtract(modelManager.getAreaOnFloor(staircase));
  }
}

/**
 * Returns the visible staircases among the given <code>furniture</code>.
 * @param {Array} furniture
 * @param {number} roomPart
 * @param {Level} roomLevel
 * @param {boolean} firstLevel
 * @return {Array}
 * @private
 */
Room3D.prototype.getVisibleStaircases = function(furniture, roomPart, roomLevel, firstLevel) {
  var visibleStaircases = [];
  for (var i = 0; i < furniture.length; i++) {
    var piece = furniture[i];
    if (piece.isVisible() 
        && (piece.getLevel() === null 
            || piece.getLevel().isViewableAndVisible())) {
      if (piece instanceof HomeFurnitureGroup) {
        visibleStaircases.push.apply(visibleStaircases, this.getVisibleStaircases(piece.getFurniture(), roomPart, roomLevel, firstLevel));
      } else if (piece.getStaircaseCutOutShape() != null 
          && "false" != piece.getStaircaseCutOutShape().toLowerCase() 
          && ((roomPart === Room3D.FLOOR_PART 
                  && piece.getGroundElevation() < roomLevel.getElevation() 
                  && piece.getGroundElevation() + piece.getHeight() >= roomLevel.getElevation() - (firstLevel ? 0 : roomLevel.getFloorThickness()) 
              || roomPart === Room3D.CEILING_PART 
                  && piece.getGroundElevation() < roomLevel.getElevation() + roomLevel.getHeight() 
                  && piece.getGroundElevation() + piece.getHeight() >= roomLevel.getElevation() + roomLevel.getHeight()))) {
        visibleStaircases.push(piece);
      }
    }
  }
  return visibleStaircases;
}

/**
 * Returns an array that cites <code>points</code> in reverse order.
 * @param {Array} points
 * @return {Array}
 * @private
 */
Room3D.prototype.getReversedArray = function (points) {
  points = points.slice(0);
  return points.reverse();
}

/**
 * Returns the room height at the given point.
 * @param {number} x
 * @param {number} y
 * @return {number}
 * @private
 */
Room3D.prototype.getRoomHeightAt = function (x, y) {
  var smallestDistance = Infinity;
  var room = this.getUserData();
  var roomLevel = room.getLevel();
  var roomElevation = roomLevel !== null 
      ? roomLevel.getElevation() 
      : 0;
  var roomHeight = roomElevation + 
      (roomLevel == null ? this.home.getWallHeight() : roomLevel.getHeight());
  var levels = this.home.getLevels();
  if (roomLevel == null || this.isLastLevel(roomLevel, levels)) {
    var closestWall = null;
    var closestWallPoints = null;
    var closestIndex = -1;
    var walls = this.home.getWalls();
    for (var index = 0; index < walls.length; index++) {
      var wall = walls[index];
      if ((wall.getLevel() === null || wall.getLevel().isViewable()) 
          && wall.isAtLevel(roomLevel)) {
        var points = wall.getPoints();
        for (var i = 0; i < points.length; i++) {
          var distanceToWallPoint = java.awt.geom.Point2D.distanceSq(points[i][0], points[i][1], x, y);
          if (distanceToWallPoint < smallestDistance) {
            closestWall = wall;
            closestWallPoints = points;
            closestIndex = i;
            smallestDistance = distanceToWallPoint;
          }
        }
      }
    }
    
    if (closestWall != null) {
      roomHeight = closestWall.getLevel() == null ? 0 : closestWall.getLevel().getElevation();
      var wallHeightAtStart = closestWall.getHeight();
      if (closestIndex === 0 || closestIndex === closestWallPoints.length - 1) {
        roomHeight += wallHeightAtStart != null 
            ? wallHeightAtStart 
            : this.home.getWallHeight();
      } else {
        if (closestWall.isTrapezoidal()) {
          var arcExtent = closestWall.getArcExtent();
          if (arcExtent == null 
              || arcExtent === 0 
              || closestIndex === Math.floor(closestWallPoints.length / 2) 
              || closestIndex === Math.floor(closestWallPoints.length / 2) - 1) {
            roomHeight += closestWall.getHeightAtEnd();
          } else {
            var xArcCircleCenter = closestWall.getXArcCircleCenter();
            var yArcCircleCenter = closestWall.getYArcCircleCenter();
            var xClosestPoint = closestWallPoints[closestIndex][0];
            var yClosestPoint = closestWallPoints[closestIndex][1];
            var centerToClosestPointDistance = java.awt.geom.Point2D.distance(xArcCircleCenter, yArcCircleCenter, xClosestPoint, yClosestPoint);
            var xStart = closestWall.getXStart();
            var yStart = closestWall.getYStart();
            var centerToStartPointDistance = java.awt.geom.Point2D.distance(xArcCircleCenter, yArcCircleCenter, xStart, yStart);
            var scalarProduct = (xClosestPoint - xArcCircleCenter) * (xStart - xArcCircleCenter) 
                + (yClosestPoint - yArcCircleCenter) * (yStart - yArcCircleCenter);
            scalarProduct /= (centerToClosestPointDistance * centerToStartPointDistance);
            var arcExtentToClosestWallPoint = Math.acos(scalarProduct) * (arcExtent > 0 ? 1 : (arcExtent < 0 ? -1 : 0));;
            roomHeight += wallHeightAtStart 
                + (closestWall.getHeightAtEnd() - wallHeightAtStart) * arcExtentToClosestWallPoint / arcExtent;
          }
        } else {
          roomHeight += (wallHeightAtStart != null ? wallHeightAtStart : this.home.getWallHeight());
        }
      }
    }
  }
  return roomHeight;
}

/**
 * Returns <code>true</code> if the given level is the last level in home.
 * @param {Level} level
 * @param {Array} levels
 * @return {boolean}
 * @private
 */
Room3D.prototype.isLastLevel = function(level, levels) {
  return levels.indexOf(level) === levels.length - 1;
}

/**
 * Sets room appearance with its color, texture.
 * @param {boolean} waitTextureLoadingEnd
 * @private
 */
Room3D.prototype.updateRoomAppearance = function(waitTextureLoadingEnd) {
  var room = this.getUserData();
  var ignoreFloorTransparency = room.getLevel() == null || room.getLevel().getElevation() <= 0;
  this.updateRoomPartAppearance(this.getChild(Room3D.FLOOR_PART).getAppearance(), 
      room.getFloorTexture(), waitTextureLoadingEnd, room.getFloorColor(), room.getFloorShininess(), room.isFloorVisible(), ignoreFloorTransparency);
  var ignoreCeillingTransparency = room.getLevel() == null;
  this.updateRoomPartAppearance(this.getChild(Room3D.CEILING_PART).getAppearance(), 
      room.getCeilingTexture(), waitTextureLoadingEnd, room.getCeilingColor(), room.getCeilingShininess(), room.isCeilingVisible(), ignoreCeillingTransparency);
}

/**
 * Sets room part appearance with its color, texture and visibility.
 * @param {Appearance3D} roomPartAppearance
 * @param {HomeTexture} roomPartTexture
 * @param {boolean} waitTextureLoadingEnd
 * @param {number} roomPartColor
 * @param {number} shininess
 * @param {boolean} visible
 * @param {boolean} ignoreTransparency
 * @private
 */
Room3D.prototype.updateRoomPartAppearance = function(roomPartAppearance, roomPartTexture, waitTextureLoadingEnd, roomPartColor, shininess, visible, ignoreTransparency) {
  if (roomPartTexture == null) {
    this.updateAppearanceMaterial(roomPartAppearance, roomPartColor, roomPartColor, shininess);
    roomPartAppearance.setTextureImage(null);
  } else {
    this.updateAppearanceMaterial(roomPartAppearance, Object3DBranch.DEFAULT_COLOR, Object3DBranch.DEFAULT_AMBIENT_COLOR, shininess);
    this.updateTextureTransform(roomPartAppearance, roomPartTexture, true);
    TextureManager.getInstance().loadTexture(roomPartTexture.getImage(), waitTextureLoadingEnd, {
        textureUpdated : function(texture) {
          roomPartAppearance.setTextureImage(texture);
        },
        textureError : function(error) {
          return this.textureUpdated(TextureManager.getInstance().getErrorImage());
        },
        progression : function(part, info, percentage) {
        }
      });
  }
  if (!ignoreTransparency) {
    var upperRoomsAlpha = this.home.getEnvironment().getWallsAlpha();
    roomPartAppearance.setTransparency(upperRoomsAlpha);
  }
  roomPartAppearance.setVisible(visible);
}