/*
 * Triangulator.js
 * 
 * Copyright (c) 2016 Emmanuel PUYBARET / eTeks <info@eteks.com>
 * 
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

// Java 3D com.sun.j3d.utils.geometry.Triangulator class translated to Javascript

// Requires gl-matrix-min.js

/**
 * Triangulator class.
 * @constructor
 * @author Emmanuel Puybaret
 */
function Triangulator() {
  this.loops = null;
  this.chains = null;
  this.points = null;
  this.numPoints = 0;
  this.triangles = null;
  this.list = null;

  this.firstNode = 0;
  
  // For Clean class
  this.pointsUnsorted = null;
  this.maxNumPUnsorted = 0;

  // For NoHash class
  this.noHashingEdges = false;
  this.noHashingPnts = false;
  this.loopMin = 0;
  this.loopMax = 0;
  this.vertexList = null;
  this.reflexVertices = 0;
  this.numReflex = 0;

  // For Bridge class
  this.distances = null;
  this.maxNumDist = 0;

  // For Heap class
  this.heap = null;
  this.numHeap = 0;
  this.numZero = 0;

  // For Orientation class
  this.polyArea = null;

  this.ccwLoop = true;
  this.identCntr;  

  this.epsilon = 1.0e-12;
}

Triangulator.ZERO = 1.0e-8;

/**
 * Triangulates the polygons described by the 5 first parameters into the 3 last arrays.
 */
Triangulator.prototype.triangulate = function(vertices, vertexIndices, textureCoordinateIndices, normalIndices, stripCounts,
                                              triangleCoordinateIndices, triangleTextureCoordinateIndices, triangleNormalIndices) {
  this.vertices = vertices;  
  this.maxNumDist = 0;
  this.maxNumPUnsorted = 0;
  this.loops = [];
  this.list = [];
  this.numReflex = 0;
  this.numPoints = 0;
  var index = 0;
  if (stripCounts === null) {
    stripCounts = [vertexIndices.length];
  }
  for (var j = 0; j < stripCounts.length; j++) {
    var currLoop = this.makeLoopHeader();
    var lastInd = this.loops[currLoop];
    for (var k = 0; k < stripCounts[j]; k++) {
      var ind = this.list.length;
      this.list.push(new ListNode(vertexIndices[index]));
      this.insertAfter(lastInd, ind);
      this.list[ind].setCommonIndex(index);
      lastInd = ind;
      index++;
    } 
    this.deleteHook(currLoop);
  }

  this.triangles = [];
  this.epsilon = Triangulator.ZERO;
  var reset = false, troubles = false;
  var done = [false];
  var gotIt = [false];
  for (var j = 0; j < stripCounts.length; j++) {
    this.ccwLoop = true;
    if (!this.simpleFace(this.loops[j])) {
      this.preProcessList(j);
      this.projectFace(j, j + 1);
      var removed = this.cleanPolyhedralFace(j, j + 1);
      this.determineOrientation(this.loops[j]);
      this.noHashingEdges = false;
      this.noHashingPnts  = false;
  
      this.classifyAngles(this.loops[j]);
      this.resetPolyList(this.loops[j]);
      this.prepareNoHashPoints(j);
      this.classifyEars(this.loops[j]);
      done[0] = false;
  
      // Triangulate the polygon
      while (!done[0]) {
        if (!this.clipEar(done)) {
          if (reset) {
            var ind = this.getNode();
            this.resetPolyList(ind);
            this.loops[j] = ind;
            if (this.desperate(ind, j, done)) {
              if (!this.letsHope(ind)) {
                return;
              }
            } else {
              reset = false;
            }
          } else {
            troubles = true;
            var ind = this.getNode();
            this.resetPolyList(ind);
            this.classifyEars(ind);
            reset = true;
          }
        } else {
          reset = false;
        }
  
        if (done[0]) {
          var ind = this.getNextChain(gotIt);
          if (gotIt[0]) {
            this.resetPolyList(ind);
            this.loops[j] = ind;
            this.noHashingPnts = false;
            this.prepareNoHashPoints(j);
            this.classifyEars(ind);
            reset = false;
            done[0]  = false;
          }
        }
      }
    }
  }
  
  this.copyToTriangles(vertexIndices, textureCoordinateIndices, normalIndices,
      triangleCoordinateIndices, triangleTextureCoordinateIndices, triangleNormalIndices);
}

Triangulator.prototype.preProcessList = function(i1) {
  this.resetPolyList(this.loops[i1]);
  var tInd = this.loops[i1];
  var tInd1 = tInd;
  var tInd2 = this.list[tInd1].next;
  while (tInd2 !== tInd) {
    if (this.list[tInd1].index === this.list[tInd2].index) {
      if (tInd2 === this.loops[i1]) {
        this.loops[i1] = this.list[tInd2].next;
      }
      this.deleteLinks(tInd2);
    }
    tInd1 = this.list[tInd1].next;
    tInd2 = this.list[tInd1].next;
  }
}

Triangulator.prototype.copyToTriangles = function(vertexIndices, textureCoordinateIndices, normalIndices,
                                                  triangleCoordinateIndices, triangleTextureCoordinateIndices, triangleNormalIndices) {
  for (var i = 0; i < this.triangles.length; i++) {
    var index = this.list[this.triangles[i].v1].getCommonIndex();
    triangleCoordinateIndices.push(vertexIndices[index]);
    index = this.list[this.triangles[i].v2].getCommonIndex();
    triangleCoordinateIndices.push(vertexIndices[index]);
    index = this.list[this.triangles[i].v3].getCommonIndex();
    triangleCoordinateIndices.push(vertexIndices[index]);
  }

  if (textureCoordinateIndices !== null
      && textureCoordinateIndices.length > 0) {
    for (var i = 0; i < this.triangles.length; i++) {
      var index = this.list[this.triangles[i].v1].getCommonIndex();
      triangleTextureCoordinateIndices.push(textureCoordinateIndices[index]);
      index = this.list[this.triangles[i].v2].getCommonIndex();
      triangleTextureCoordinateIndices.push(textureCoordinateIndices[index]);
      index = this.list[this.triangles[i].v3].getCommonIndex();
      triangleTextureCoordinateIndices.push(textureCoordinateIndices[index]);
    }
  }

  if (normalIndices !== null
      && normalIndices.length) {
    currIndex = 0;
    for (var i = 0; i < this.triangles.length; i++) {
      var index = this.list[this.triangles[i].v1].getCommonIndex();
      triangleNormalIndices.push(normalIndices[index]);
      index = this.list[this.triangles[i].v2].getCommonIndex();
      triangleNormalIndices.push(normalIndices[index]);
      index = this.list[this.triangles[i].v3].getCommonIndex();
      triangleNormalIndices.push(normalIndices[index]);
    }
  }
}

// Methods of handling ListNode.
Triangulator.prototype.isInPolyList = function(ind) {
  return (ind >= 0)  && (ind < this.list.length);
}

Triangulator.prototype.updateIndex = function(ind, index) {
  this.list[ind].index = index;
}

Triangulator.prototype.getAngle = function(ind) {
  return this.list[ind].convex;
}

Triangulator.prototype.setAngle = function(ind, convex) {
  this.list[ind].convex = convex;
}

Triangulator.prototype.resetPolyList = function(ind) {
  this.firstNode = ind;
}

Triangulator.prototype.getNode = function() {
  return this.firstNode;
}

Triangulator.prototype.deleteHook = function(currLoop) {
  var ind1 = this.loops[currLoop];
  var ind2 = this.list[ind1].next;
  if (this.isInPolyList(ind1) && this.isInPolyList(ind2)) {
    this.deleteLinks(ind1);
    this.loops[currLoop] = ind2;
  }
}

Triangulator.prototype.deleteLinks = function(ind) {
  if (this.isInPolyList(ind) && this.isInPolyList(this.list[ind].prev) 
      && this.isInPolyList(this.list[ind].next)) {
    if (this.firstNode === ind) {
      this.firstNode = this.list[ind].next;
    }

    this.list[this.list[ind].next].prev = this.list[ind].prev;
    this.list[this.list[ind].prev].next = this.list[ind].next;
    this.list[ind].prev = this.list[ind].next = ind;
  }
}

Triangulator.prototype.rotateLinks = function(ind1, ind2) {
  var ind0 = this.list[ind1].next;
  var ind3 = this.list[ind2].next;
  var ind = this.list[ind1].next;
  this.list[ind1].next = this.list[ind2].next;
  this.list[ind2].next = ind;
  this.list[ind0].prev = ind2;
  this.list[ind3].prev = ind1;
}

Triangulator.prototype.storeChain = function(ind) {
  if (this.chains === null) {
    this.chains = [];
  }
  this.chains.push(ind);
}

Triangulator.prototype.getNextChain = function (done) {
  if (this.chains !== null 
      && this.chains.length > 0) {
    done[0] = true;
    return this.chains.pop();
  } else {
   done[0] = false;
   this.chains = null;
   return 0;
  }
}

Triangulator.prototype.splitSplice = function(ind1, ind2, ind3, ind4) {
  this.list[ind1].next = ind4;
  this.list[ind4].prev = ind1;
  this.list[ind2].prev = ind3;
  this.list[ind3].next = ind2;
}

Triangulator.prototype.makeHook = function() {
  var node = new ListNode(-1);
  node.prev = this.list.length;
  node.next = this.list.length;
  this.list.push(node);
  return this.list.length - 1;
}

Triangulator.prototype.makeLoopHeader = function() {
  this.loops.push(this.makeHook());
  return this.loops.length - 1;
}

Triangulator.prototype.makeNode = function(index) {
  this.list.push(new ListNode(index));
  return this.list.length - 1;
}

Triangulator.prototype.insertAfter = function(ind1, ind2) {
  if (this.isInPolyList(ind1) && this.isInPolyList(ind2)) {
    this.list[ind2].next = this.list[ind1].next;
    this.list[ind2].prev = ind1;
    this.list[ind1].next = ind2;
    var ind3 = this.list[ind2].next;

    if (this.isInPolyList(ind3)) {
      this.list[ind3].prev = ind2;
    }
  }
}

Triangulator.prototype.fetchNextData = function(ind1) {
  return this.list[ind1].next;
}

Triangulator.prototype.fetchData = function(ind1) {
  return this.list[ind1].index;
}

Triangulator.prototype.fetchPrevData = function(ind1) {
  return this.list[ind1].prev;
}

Triangulator.prototype.swapLinks = function(ind1) {
  var ind2 = this.list[ind1].next;
  this.list[ind1].next = this.list[ind1].prev;
  this.list[ind1].prev = ind2;
  var ind3 = ind2;
  while (ind2 !== ind1) {
    ind3 = this.list[ind2].next;
    this.list[ind2].next = this.list[ind2].prev;
    this.list[ind2].prev = ind3;
    ind2 = ind3;
  }
}

Triangulator.prototype.storeTriangle = function(i, j, k) {
  if (this.ccwLoop) {
    this.triangles.push(new Triangle(i,j,k));
  } else {
    this.triangles.push(new Triangle(j,i,k));
  }
}

Triangulator.prototype.initPoints = function(number) {
  this.points = [];
  for(var i = 0; i < number; i++) {
    this.points [i] = [0., 0.];
  }
  this.numPoints = 0;
}

Triangulator.prototype.inPointsList = function(index) {
  return index >= 0 && index < this.numPoints;
}

Triangulator.prototype.storePoint = function(x, y) {
  if (this.points === null)  {
    this.points = [];
  }
  this.points[this.numPoints] = [x, y];
  return this.numPoints++;
}

// From Simple class
Triangulator.prototype.simpleFace = function(ind1) {
  var ind0 = this.fetchPrevData(ind1);
  var i0 = this.fetchData(ind0);
  if (ind0 === ind1) {
    return true;
  }

  var ind2 = this.fetchNextData(ind1);
  var i2 = this.fetchData(ind2);
  if (ind0 === ind2) {
    return true;
  }

  var ind3 = this.fetchNextData(ind2);
  var i3 = this.fetchData(ind3);
  if (ind0 === ind3) {
    this.storeTriangle(ind1, ind2, ind3);
    return true;
  }

  var ind4 = this.fetchNextData(ind3);
  var i4 = this.fetchData(ind4);
  if (ind0 === ind4) {
    this.initPoints(5);
    var i1 = this.fetchData(ind1);

    var pq = vec3.subtract(vec3.create(), this.vertices[i1], this.vertices[i2]);
    var pr = vec3.subtract(vec3.create(), this.vertices[i3], this.vertices[i2]);
    var nr = vec3.cross(vec3.create(), pq, pr);

    var x = Math.abs(nr [0]);
    var y = Math.abs(nr [1]);
    var z = Math.abs(nr [2]);
    if (z >= x && z >= y) {
      this.points[1][0] = this.vertices[i1][0];
      this.points[1][1] = this.vertices[i1][1];
      this.points[2][0] = this.vertices[i2][0];
      this.points[2][1] = this.vertices[i2][1];
      this.points[3][0] = this.vertices[i3][0];
      this.points[3][1] = this.vertices[i3][1];
      this.points[4][0] = this.vertices[i4][0];
      this.points[4][1] = this.vertices[i4][1];
    } else if (x >= y && x >= z) {
      this.points[1][0] = this.vertices[i1][2];
      this.points[1][1] = this.vertices[i1][1];
      this.points[2][0] = this.vertices[i2][2];
      this.points[2][1] = this.vertices[i2][1];
      this.points[3][0] = this.vertices[i3][2];
      this.points[3][1] = this.vertices[i3][1];
      this.points[4][0] = this.vertices[i4][2];
      this.points[4][1] = this.vertices[i4][1];
    } else {
      this.points[1][0] = this.vertices[i1][0];
      this.points[1][1] = this.vertices[i1][2];
      this.points[2][0] = this.vertices[i2][0];
      this.points[2][1] = this.vertices[i2][2];
      this.points[3][0] = this.vertices[i3][0];
      this.points[3][1] = this.vertices[i3][2];
      this.points[4][0] = this.vertices[i4][0];
      this.points[4][1] = this.vertices[i4][2];
    }
    this.numPoints = 5;

    var ori2 = this.orientation(1, 2, 3);
    var ori4 = this.orientation(1, 3, 4);
    if ((ori2 > 0 && ori4 > 0) || (ori2 < 0 && ori4 < 0)) {
      this.storeTriangle(ind1, ind2, ind3);
      this.storeTriangle(ind1, ind3, ind4);
    } else {
      this.storeTriangle(ind2, ind3, ind4);
      this.storeTriangle(ind2, ind4, ind1);
    }
    return  true;
  }
  return false;
}

// From Numerics class
Triangulator.prototype.max3 = function(a, b, c) {
  return (a > b) 
      ? ((a > c) ? a : c) 
      : ((b > c) ? b : c);
}

Triangulator.prototype.min3 = function(a, b, c) {
  return (a < b) 
      ? ((a < c) ? a : c)
      : ((b < c) ? b : c);
}

Triangulator.prototype.baseLength = function(u, v) {
  return Math.abs(v[0] - u[0]) + Math.abs(v[1] - u[1]);
}

Triangulator.prototype.sideLength = function(u, v) {
  var x = v [0] - u [0];
  var y = v [1] - u [1];
  return x * x + y * y;
}

Triangulator.prototype.inBetween = function(i1, i2, i3) {
  return i1 <= i3  && i3 <= i2;
}

Triangulator.prototype.strictlyInBetween = function(i1, i2, i3) {
  return i1 < i3 && i3 < i2;
}

Triangulator.prototype.stableDet2D = function(i, j, k) {
  var det;
  if (i === j || i === k || j === k) {
    det = 0.0;
  } else {
    var numericsHP = this.points [i];
    var numericsHQ = this.points [j];
    var numericsHR = this.points [k];
    if (i < j) {
      if (j < k) {
        det =  this.det2D(numericsHP, numericsHQ, numericsHR);
      } else if (i < k) {
        det = -this.det2D(numericsHP, numericsHR, numericsHQ);
      } else {
        det =  this.det2D(numericsHR, numericsHP, numericsHQ);
      }
    } else {
      if (i < k) {
        det = -this.det2D(numericsHQ, numericsHP, numericsHR);
      } else if (j < k) {
        det =  this.det2D(numericsHQ, numericsHR, numericsHP);
      } else {
        det = -this.det2D(numericsHR, numericsHQ, numericsHP);
      }
    }
  }

  return det;
}

Triangulator.prototype.det2D = function(u, v, w) {
  return (u[0] - v[0]) * (v[1] - w[1]) + (v[1] - u[1]) * (v[0] - w[0]);
}

Triangulator.prototype.orientation = function(i, j, k) {
  var numericsHDet = this.stableDet2D(i, j, k);
  if (numericsHDet < -this.epsilon) {
    return -1;
  } else if (!(numericsHDet <= this.epsilon)) {
    return 1;
  } else {
    return 0;
  }
}

Triangulator.prototype.isInCone = function(i, j, k, l, convex) {
  if (convex) {
    if (i !== j) {
      var numericsHOri1 = this.orientation(i, j, l);
      if (numericsHOri1 < 0) {
        return false;
      } else if (numericsHOri1 === 0) {
        if (i < j) {
          if (!this.inBetween(i, j, l)) {
            return false;
          }
        } else if (!this.inBetween(j, i, l)) {
          return false;
        }
      }
    }
    if (j !== k) {
      var numericsHOri2 = this.orientation(j, k, l);
      if (numericsHOri2 < 0) {
        return false;
      } else if (numericsHOri2 === 0) {
        if (j < k) {
          if (!this.inBetween(j, k, l)) {
            return false;
          }
        } else if (!this.inBetween(k, j, l)) {
          return false;
        }
      }
    }
  } else if (this.orientation(i, j, l) <= 0
             && this.orientation(j, k, l) < 0) {
    return false;
  }
  return true;
}

Triangulator.prototype.isConvexAngle = function(i, j, k, ind) {
  if (i === j) {
    if (j === k) {
      return 1;
    } else {
      return 1;
    }
  } else if (j === k) {
    return -1;
  } else {
    var numericsHOri1 = this.orientation(i, j, k);
    if (numericsHOri1 > 0) {
      return  1;
    } else if (numericsHOri1 < 0) {
      return -1;
    } else {
      var numericsHP = [this.points[i][0] - this.points[j][0],
                        this.points[i][1] - this.points[j][1]];
      var numericsHQ = [this.points[k][0] - this.points[j][0],
                        this.points[k][1] - this.points[j][1]];
      var numericsHDot = numericsHP[0] * numericsHQ[0] + numericsHP[1] * numericsHQ[1];
      if (numericsHDot < 0.0) {
        return 0;
      } else {
        return this.spikeAngle(i, j, k, ind);
      }
    }
  }
}

Triangulator.prototype.isPointInTriangle = function(i1, i2, i3, i4) {
  var numericsHOri1 = this.orientation(i2, i3, i4);
  if (numericsHOri1 >= 0) {
    numericsHOri1 = this.orientation(i1, i2, i4);
    if (numericsHOri1 >= 0) {
      numericsHOri1 = this.orientation(i3, i1, i4);
      if (numericsHOri1 >= 0) {
        return true;
      }
    }
  }
  return false;
}

Triangulator.prototype.isVertexInTriangle = function(i1, i2, i3, i4, type) {
  var numericsHOri1 = this.orientation(i2, i3, i4);
  if (numericsHOri1 >= 0) {
    numericsHOri1 = this.orientation(i1, i2, i4);
    if (numericsHOri1 > 0) {
      numericsHOri1 = this.orientation(i3, i1, i4);
      if (numericsHOri1 > 0) {
        type[0] = 0;
        return true;
      } else if (numericsHOri1 === 0) {
        type[0] = 1;
        return true;
      }
    } else if (numericsHOri1 === 0) {
      numericsHOri1 = this.orientation(i3, i1, i4);
      if (numericsHOri1 > 0) {
        type[0] = 2;
        return true;
      } else if (numericsHOri1 === 0) {
        type[0] = 3;
        return true;
      }
    }
  }
  return false;
}

Triangulator.prototype.segmentsIntersect = function(i1, i2, i3, i4, i5) {
  if (i1 === i2 || i3 === i4) {
    return  false;
  }
  if (i1 === i3 && i2 === i4) {
    return  true;
  }
  if (i3 === i5 || i4 === i5) {
    ++this.identCntr;
  } 
  
  var ori3 = this.orientation(i1, i2, i3);
  var ori4 = this.orientation(i1, i2, i4);
  if ((ori3 ===  1 && ori4 ===  1)  ||
      (ori3 === -1 && ori4 === -1)) {
    return  false;
  }
  if (ori3 === 0) {
    if (this.strictlyInBetween(i1, i2, i3)) {
      return  true;
    }
    if (ori4 === 0) {
      if (this.strictlyInBetween(i1, i2, i4)) {
        return  true;
      }
    } else {
      return  false;
    } 
  } else if (ori4 === 0) {
    if (this.strictlyInBetween(i1, i2, i4)) {
      return  true;
    } else {
      return  false;
    }
  }
  
  var ori1 = this.orientation(i3, i4, i1);
  var ori2 = this.orientation(i3, i4, i2);
  if ((ori1 <= 0 && ori2 <= 0) 
      || (ori1 >= 0 && ori2 >= 0)) {
    return  false;
  }
  return  true;
}

Triangulator.prototype.getRatio = function(i, j, k) {
  var p = this.points[i];
  var q = this.points[j];
  var r = this.points[k];

  var a = this.baseLength(p, q);
  var b = this.baseLength(p, r);
  var c = this.baseLength(r, q);
  var base = this.max3(a, b, c);

  if ((10.0 * a) < Math.min(b, c)) {
    return 0.1;
  }
  
  var area = this.stableDet2D(i, j, k);
  if (area < -this.epsilon) {
    area = -area;
  } else if (area <= this.epsilon) {
    if (base > a) {
      return 0.1;
    } else {
      return Number.MAX_VALUE;
    }                         
  }

  var ratio = base * base / area;
  if (ratio < 10.0) {
    return ratio;
  } else {
    if (a < base) {
      return 0.1;
    } else {
      return ratio;
    }
  }
}

Triangulator.prototype.spikeAngle = function(i, j, k, ind) {
  var ind2 = ind;
  var i2 = this.fetchData(ind2);
  var ind1 = this.fetchPrevData(ind2);
  var i1 = this.fetchData(ind1);
  var ind3 = this.fetchNextData(ind2);
  var i3 = this.fetchData(ind3);
  return this.recSpikeAngle(i, j, k, ind1, ind3);
}

Triangulator.prototype.recSpikeAngle = function(i1, i2, i3, ind1, ind3) {
  if (ind1 === ind3) {
    return -2;
  }

  if (i1 !== i3) {
    var ii1;
    var ii2;
    if (i1 < i2) {
      ii1 = i1;
      ii2 = i2;
    } else {
      ii1 = i2;
      ii2 = i1;
    }
    if (this.inBetween(ii1, ii2, i3)) {
      i2 = i3;
      ind3 = this.fetchNextData(ind3);
      i3 = this.fetchData(ind3);
      if (ind1 === ind3) {
        return 2;
      }  
      var ori = this.orientation(i1, i2, i3);
      if (ori > 0) {
        return  2;
      } else if (ori < 0) {
        return -2;
      } else {
        return this.recSpikeAngle(i1, i2, i3, ind1, ind3);
      }
    } else {
      i2 = i1;
      ind1 = this.fetchPrevData(ind1);
      i1 = this.fetchData(ind1);
      if (ind1 === ind3) {
        return 2;
      }
      var ori = this.orientation(i1, i2, i3);
      if (ori > 0) {
        return  2;
      } else if (ori < 0)  {
        return -2;
      } else {
        return this.recSpikeAngle(i1, i2, i3, ind1, ind3);
      }
    }
  } else {
    var i0 = i2;
    i2   = i1;
    ind1 = this.fetchPrevData(ind1);
    i1 = this.fetchData(ind1);
    if (ind1 === ind3) {
      return 2;
    }
    ind3 = this.fetchNextData(ind3);
    i3 = this.fetchData(ind3);
    if (ind1 === ind3) {
      return 2;
    }
    ori = this.orientation(i1, i2, i3);
    if (ori > 0) {
      if (this.orientation(i1, i2, i0) > 0
          && this.orientation(i2, i3, i0) > 0) {
        return -2;
      }
      return 2;
    } else if (ori < 0)  {
      if (this.orientation(i2, i1, i0) > 0
          && this.orientation(i3, i2, i0) > 0) {
        return 2;
      }
      return -2;
    } else {
      var pq = [this.points[i1][0] - this.points[i2][0], 
                this.points[i1][1] - this.points[i2][1]];
      var pr = [this.points[i3][0] - this.points[i2][0], 
                this.points[i3][1] - this.points[i2][1]];
      var dot = pq [0] * pr [0] + pq [1] * pr [1];
      if (dot < 0.) {
        if (this.orientation(i2, i1, i0) > 0) {
          return  2;
        } else {
          return -2;
        }
      } else {
        return this.recSpikeAngle(i1, i2, i3, ind1, ind3);
      }
    }
  }
}

Triangulator.prototype.angle = function(p, p1, p2) {
  var det = (p2[0] - p[0]) * (p[1] - p1[1]) + (p[1] - p2[1]) * (p[0] - p1[0]);
  var sign = det <= this.epsilon  ? (det < -this.epsilon ? -1 : 0)  : 1;
  if (sign === 0) {
    return 0.0;
  }

  var v1 = [p1[0] - p[0], p1[1] - p[1]];
  var v2 = [p2[0] - p[0], p2[1] - p[1]];
  var angle1 = Math.atan2(v1[1], v1[0]);
  var angle2 = Math.atan2(v2[1], v2[0]);

  if (angle1 < 0.0) {
    angle1 += 2.0 * Math.PI;
  }
  if (angle2 < 0.0) {
    angle2 += 2.0 * Math.PI;
  }
  
  var angle = angle1 - angle2;
  if (angle > Math.PI) {
    angle  = 2.0 * Math.PI - angle;
  } else if (angle < -Math.PI) {
    angle  = 2.0 * Math.PI + angle;
  }
  
  if (sign === 1) {
    if (angle < 0.0) {
      return  -angle;
    } else {
      return   angle;
    }
  }
  else {
    if (angle > 0.0) {
      return  -angle;
    } else {
      return   angle;
    }
  }
}

// From Project class
Triangulator.prototype.projectFace = function(loopMin, loopMax) {
  var normal = vec3.create();
  var nr = vec3.create();
  this.determineNormal(this.loops[loopMin], normal);
  var j = loopMin + 1;
  if (j < loopMax) {
    for (var i = j;  i < loopMax; i++) {
      this.determineNormal(this.loops[i], nr);
      if (vec3.dot(normal, nr) < 0.0) {
        vec3.negate(nr, nr);
      }
      vec3.add(normal, nr, normal);
    }
    var d = vec3.length(normal);
    if (!(d <= Triangulator.ZERO)) {
      vec3.scale(normal, normal, 1 / d);
    } else {
      normal[0] = normal[1] = 0.;
      normal[2] = 1.;
    }
  }

  this.projectPoints(loopMin, loopMax, normal);
}

Triangulator.prototype.determineNormal = function(ind, normal) {
  var ind1 = ind;
  var i1 = this.fetchData(ind1);
  var ind0 = this.fetchPrevData(ind1);
  var i0 = this.fetchData(ind0);
  var ind2 = this.fetchNextData(ind1);
  var i2 = this.fetchData(ind2);
  var pq = vec3.subtract(vec3.create(), this.vertices[i0], this.vertices[i1]);
  var pr = vec3.subtract(vec3.create(), this.vertices[i2], this.vertices[i1]);
  var nr = vec3.cross(vec3.create(), pq, pr);
  var d = vec3.length(nr);
  if (!(d <= Triangulator.ZERO)) {
    vec3.scale(normal, nr, 1 / d);
  } else {
    normal[0] = normal[1] = normal[2] = 0.;
  }

  vec3.copy(pq, pr);
  ind1 = ind2;
  ind2 = this.fetchNextData(ind1);
  i2 = this.fetchData(ind2);
  while (ind1 !== ind) {
    vec3.subtract(pr, this.vertices[i2], this.vertices[i1]);
    vec3.cross(nr, pq, pr);
    d = vec3.length(nr);
    if (!(d <= Triangulator.ZERO))  {
      vec3.scale(nr, nr, 1 / d);
      if (vec3.dot(normal, nr) < 0.) {
        vec3.negate(nr, nr);
      }
      vec3.add(normal, nr, normal);
    }
    vec3.copy(pq, pr);
    ind1 = ind2;
    ind2 = this.fetchNextData(ind1);
    i2 = this.fetchData(ind2);
  }

  d = vec3.length(normal);
  if (!(d <= Triangulator.ZERO)) {
    vec3.scale(normal, normal, 1 / d);
  } else {
    normal[0] = normal[1] = 0.;
    normal[2] = 1.;
  }
}

Triangulator.prototype.projectPoints = function(i1, i2, n3) {
  var n1 = vec3.create();
  var n2 = vec3.create();
  if ((Math.abs(n3[0]) > 0.1)  || (Math.abs(n3[1]) > 0.1)) {
    n1[0] = -n3[1];
    n1[1] =  n3[0];
    n1[2] =  0.;
  } else {
    n1[0] =  n3[2];
    n1[2] = -n3[0];
    n1[1] =  0.;
  }
  var d = vec3.length(n1);
  vec3.scale(n1, n1, 1 / d);
  vec3.cross(n2, n1, n3);
  d = vec3.length(n2);
  vec3.scale(n2, n2, 1 / d);

  var matrix = mat4.create();
  matrix[0] = n1[0];
  matrix[4] = n1[1];
  matrix[8] = n1[2];
  matrix[1] = n2[0];
  matrix[5] = n2[1];
  matrix[9] = n2[2];
  matrix[2] = n3[0];
  matrix[6] = n3[1];
  matrix[10] = n3[2];

  var vertex = vec3.create();
  this.initPoints(20);
  for (var i = i1; i < i2; i++) {
    var ind = this.loops[i];
    var ind1 = ind;
    var j1 = this.fetchData(ind1);
    vec3.transformMat4(vertex, this.vertices[j1], matrix);
    j1 = this.storePoint(vertex[0], vertex[1]);
    this.updateIndex(ind1, j1);
    ind1 = this.fetchNextData(ind1);
    j1 = this.fetchData(ind1);
    while (ind1 !== ind) {
      vec3.transformMat4(vertex, this.vertices[j1], matrix);
      j1 = this.storePoint(vertex[0], vertex[1]);
      this.updateIndex(ind1, j1);
      ind1 = this.fetchNextData(ind1);
      j1 = this.fetchData(ind1);
    }
  }
}

// From Clean class
Triangulator.prototype.initPointsUnsorted = function(number) {
  if (number > this.maxNumPUnsorted) {
    this.maxNumPUnsorted = number;
    this.pointsUnsorted = new Array(this.maxNumPUnsorted);
    for (var i = 0; i < this.maxNumPUnsorted; i++) {
      this.pointsUnsorted [i] = [0., 0.];
    }
  }
}

Triangulator.prototype.cleanPolyhedralFace = function(i1, i2) {
  this.initPointsUnsorted(this.numPoints);

  for (var i = 0; i < this.numPoints; i++) {
    this.pointsUnsorted[i][0] = this.points[i][0];
    this.pointsUnsorted[i][1] = this.points[i][1];
  }

  this.sortPoints(this.points, this.numPoints);
  var i = 0;
  var j;
  for (j = 1; j < this.numPoints; j++) {
    if (this.comparePoints(this.points[i], this.points[j]) !== 0) {
      this.points[++i] = this.points[j];
    }
  }
  var numSorted = i + 1;
  var removed = this.numPoints - numSorted;
  for (i = i1;  i < i2;  ++i) {
    var ind1 = this.loops[i];
    var ind2 = this.fetchNextData(ind1);
    var index = this.fetchData(ind2);
    while (ind2 !== ind1) {
      j = this.findPointIndex(this.points, numSorted, this.pointsUnsorted[index]);
      this.updateIndex(ind2, j);
      ind2 = this.fetchNextData(ind2);
      index = this.fetchData(ind2);
    }
    j = this.findPointIndex(this.points, numSorted, this.pointsUnsorted[index]);
    this.updateIndex(ind2, j);
  }

  this.numPoints = numSorted;
  return removed;
}

Triangulator.prototype.sortPoints = function(points, numPoints) {
  for (var i = 0; i < numPoints; i++) {
    for (var j = i + 1; j < numPoints; j++) {
      if (this.comparePoints(points[i], points[j]) > 0) {
        var x = points[i][0];
        var y = points[i][1];
        points[i][0] = points[j][0];
        points[i][1] = points[j][1];
        points[j][0] = x;
        points[j][1] = y;
      }
    }
  }
}

Triangulator.prototype.findPointIndex = function(sorted, numPoints, points) {
  for (var i = 0; i < numPoints; i++) {
    if (points[0] === sorted[i][0] 
        && points[1] === sorted[i][1]) {
      return i;
    }
  }
  return -1;
}

Triangulator.prototype.comparePoints = function(a, b) {
  if (a[0] < b[0]) {
    return -1;
  } else if (a[0] > b[0]) {
    return  1;
  } else {
    if (a[1] < b[1]) {
      return -1;
    } else if (a[1] > b[1]) {
      return  1;
    } else {
      return  0;
    }
  }
}

// From Orientation class
Triangulator.prototype.polygonArea = function(ind) {
  var hook = 0;
  var ind1 = ind;
  var i1 = this.fetchData(ind1);
  var ind2 = this.fetchNextData(ind1);
  var i2 = this.fetchData(ind2);
  var area = this.stableDet2D(hook, i1, i2);

  ind1 = ind2;
  i1   = i2;
  while (ind1 !== ind) {
    ind2  = this.fetchNextData(ind1);
    i2 = this.fetchData(ind2);
    area += this.stableDet2D(hook, i1, i2);
    ind1  = ind2;
    i1    = i2;
  }

  return  area;
}

Triangulator.prototype.determineOrientation = function(ind) {
  if (this.polygonArea(ind) < 0.0)   {
    this.swapLinks(ind);
    this.ccwLoop = false;
  }
}

// From EarClip class
Triangulator.prototype.classifyAngles = function(ind) {
  var ind1 = ind;
  var i1 = this.fetchData(ind1);
  var ind0 = this.fetchPrevData(ind1);
  var i0 = this.fetchData(ind0);
  do {
    var ind2 = this.fetchNextData(ind1);
    var i2 = this.fetchData(ind2);
    var angle = this.isConvexAngle(i0, i1, i2, ind1);
    this.setAngle(ind1, angle);
    i0 = i1;
    i1 = i2;
    ind1 = ind2;
  } while (ind1 !== ind);
}

Triangulator.prototype.classifyEars = function(ind) {
  var ind0 = [0];
  var ind2 = [0];
  var ratio = [0.];
  this.initHeap();
  var ind1 = ind;
  var i1 = this.fetchData(ind1);

  do {
    if ((this.getAngle(ind1) > 0) 
        && this.isEar(ind1, ind0, ind2, ratio))   {
      this.dumpOnHeap(ratio[0], ind1, ind0[0], ind2[0]);
    }
    ind1 = this.fetchNextData(ind1);
    i1 = this.fetchData(ind1);
  } while (ind1 !== ind);
}

Triangulator.prototype.isEar = function(ind2, ind1, ind3, ratio) {
  var i2 = this.fetchData(ind2);
  ind3[0] = this.fetchNextData(ind2);
  var i3 = this.fetchData(ind3[0]);
  var ind4 = this.fetchNextData(ind3[0]);
  var i4 = this.fetchData(ind4);
  ind1[0] = this.fetchPrevData(ind2);
  var i1 = this.fetchData(ind1[0]);
  var ind0 = this.fetchPrevData(ind1[0]);
  var i0 = this.fetchData(ind0);

  if (i1 === i3 || i1 === i2 || i2 === i3 || this.getAngle(ind2) === 2) {
    ratio[0] = 0.0;
    return  true;
  }

  if (i0 === i3) {
    if (this.getAngle(ind0) < 0 || this.getAngle(ind3[0]) < 0) {
      ratio[0] = 0.0;
      return  true;
    }
    else {
      return  false;
    }
  }

  if (i1 === i4) {
    if ((this.getAngle(ind1[0]) < 0)  ||  (this.getAngle(ind4) < 0)) {
      ratio[0] = 0.0;
      return  true;
    }
    else {
      return  false;
    }
  }

  var convex = this.getAngle(ind1[0]) > 0;
  var coneOk = this.isInCone(i0, i1, i2, i3, convex);
  if (!coneOk) {
    return false;
  }
  var convex = this.getAngle(ind3[0]) > 0;
  var coneOk = this.isInCone(i2, i3, i4, i1, convex);
  if (coneOk) {
    var box = new BoundingBox(this, i1, i3);
    if (!this.noHashIntersectionExists(i2, ind2, i3, i1, box)) {
      ratio[0] = 1.0;
      return true;
    }
  }

  return  false;
}

Triangulator.prototype.clipEar = function(done) {
  var index1 = [0];
  var index3 = [0];
  var ind1;
  var ind2 = [0];
  var ind3;
  var i1;
  var i3;
  do {
    if (!this.deleteFromHeap(ind2, index1, index3)) {
      return false;
    }
    ind1 = this.fetchPrevData(ind2[0]);
    i1 = this.fetchData(ind1);
    ind3 = this.fetchNextData(ind2[0]);
    i3 = this.fetchData(ind3);
  } while (index1[0] !== ind1 || index3[0] !== ind3);

  var i2 = this.fetchData(ind2[0]);
  this.deleteLinks(ind2[0]);
  this.storeTriangle(ind1, ind2[0], ind3);

  var ind0 = this.fetchPrevData(ind1);
  var i0 = this.fetchData(ind0);
  if (ind0 === ind3) {
    done[0] = true;
    return  true;
  }

  var angle1 = this.isConvexAngle(i0, i1, i3, ind1);
  var ind4 = this.fetchNextData(ind3);
  var i4 = this.fetchData(ind4);
  var angle3 = this.isConvexAngle(i1, i3, i4, ind3);
  if (i1 !== i3) {
    if (angle1 >= 0 && this.getAngle(ind1) < 0) {
      this.deleteReflexVertex(ind1);
    }
    if (angle3 >= 0 && this.getAngle(ind3) < 0) {
      this.deleteReflexVertex(ind3);
    }
  } else {
    if (angle1 >= 0 && this.getAngle(ind1) < 0) {
      this.deleteReflexVertex(ind1);
    } else if (angle3 >= 0 && this.getAngle(ind3) < 0) {
      this.deleteReflexVertex(ind3);
    }
  }

  this.setAngle(ind1, angle1);
  this.setAngle(ind3, angle3);

  var ratio = [0.];
  var index0 = [0];
  var index2 = [0];
  if (angle1 > 0) {
    if (this.isEar(ind1, index0, index2, ratio)) {
      this.insertIntoHeap(ratio[0], ind1, index0[0], index2[0]);
    }
  }

  var index4 = [0];
  if (angle3 > 0) {
    if (this.isEar(ind3, index2, index4, ratio)) {
      this.insertIntoHeap(ratio[0], ind3, index2[0], index4[0]);
    }
  }

  ind0 = this.fetchPrevData(ind1);
  i0 = this.fetchData(ind0);
  ind4 = this.fetchNextData(ind3);
  i4 = this.fetchData(ind4);
  if (ind0 === ind4) {
    this.storeTriangle(ind1, ind3, ind4);
    done[0] = true;
  } else {
    done[0] = false;
  }

  return true;
}

// from NoHash class
Triangulator.prototype.insertAfterVertex = function(vertexIndex) {
  if (this.vertexList === null) {
    this.vertexList = [];
  }
  var node = new PointNode();
  node.pnt = vertexIndex;
  node.next = this.reflexVertices;
  this.vertexList.push(node);
  this.reflexVertices = this.vertexList.length - 1;
  ++this.numReflex;
}

Triangulator.prototype.deleteFromList = function(i) {
  if (this.numReflex === 0) {
    return;
  }
  var indPnt = this.reflexVertices;
  var indVtx = this.vertexList[indPnt].pnt;

  if (indVtx === i) {
    this.reflexVertices = this.vertexList[indPnt].next;
    --this.numReflex;
  } else {
    var indPnt1 = this.vertexList[indPnt].next;
    while (indPnt1 !== -1) {
      indVtx = this.vertexList[indPnt1].pnt;
      if (indVtx === i) {
        this.vertexList[indPnt].next = this.vertexList[indPnt1].next;
        indPnt1 = -1;
        --this.numReflex;
      } else {
        indPnt = indPnt1;
        indPnt1 = this.vertexList[indPnt].next;
      }
    }
  }
}

Triangulator.prototype.freeNoHash = function() {
  this.noHashingEdges = false;
  this.noHashingPnts  = false;
  this.vertexList = null;
}

Triangulator.prototype.prepareNoHashEdges = function(currLoopMin, currLoopMax) {
  this.loopMin = currLoopMin;
  this.loopMax = currLoopMax;
  this.noHashingEdges = true;
}

Triangulator.prototype.prepareNoHashPoints = function(currLoopMin) {
  this.vertexList = null;
  this.reflexVertices = -1;

  var ind = this.loops[currLoopMin];
  var ind1 = ind;
  this.numReflex = 0;
  var i1 = this.fetchData(ind1);

  do {
    if (this.getAngle(ind1) < 0) {
      this.insertAfterVertex(ind1);
    }
    ind1 = this.fetchNextData(ind1);
    i1 = this.fetchData(ind1);
  } while (ind1 !== ind);

  this.noHashingPnts = true;
}

Triangulator.prototype.noHashIntersectionExists = function(i1, ind1, i2, i3, box) {
  if (this.numReflex <= 0) {
    return false;
  }
  
  if (i1 < box.imin) {
    box.imin = i1;
  } else if (i1 > box.imax) {
    box.imax = i1;
  }
  var y = this.points[i1][1];
  if (y < box.ymin) {
    box.ymin = y;
  } else if (y > box.ymax) {
    box.ymax = y;
  }
  
  var indPoint = this.reflexVertices;
  var type = [0];
  do {
    var indVertex = this.vertexList[indPoint].pnt;
    var i4 = this.fetchData(indVertex);

    if (box.isInBoundingBox(this.points[i4], i4)) {
      var ind5 = this.fetchNextData(indVertex);
      var i5 = this.fetchData(ind5);
      if (indVertex !== ind1 && indVertex !== ind5) {
        if (i4 === i1) {
          if (this.handleDegeneracies(i1, ind1, i2, i3, i4, indVertex)) {
            return  true;
          }
        } else if (i4 !== i2 && i4 !== i3) {
          if (this.isVertexInTriangle(i1, i2, i3, i4, type)) {
            return true;
          }
        }
      }
    }
    indPoint = this.vertexList[indPoint].next;
  } while (indPoint !== -1);

  return false;
}

Triangulator.prototype.deleteReflexVertex = function(ind) {
  this.deleteFromList(ind);
}

Triangulator.prototype.noHashEdgeIntersectionExists = function(box, i1, i2, ind5, i5) {
  this.identCntr = 0;
  for (var i = this.loopMin; i < this.loopMax; i++) {
    var ind  = this.loops[i];
    var ind2 = ind;
    var i3 = this.fetchData(ind2);
    do {
      ind2 = this.fetchNextData(ind2);
      var i4 = this.fetchData(ind2);
      var box1 = new BoundingBox(this, i3, i4);
      if (box.overlaps(box1)) {
        if (this.segmentsIntersect(box.imin, box.imax, box1.imin, box1.imax, i5))
          return  true;
        }
        i3 = i4;
    } while (ind2 !== ind);
  }

  if (this.identCntr >= 4) {
    return this.checkBottleNeck(i5, i1, i2, ind5); 
  }
  return false;
}

// From Degenerate class
Triangulator.prototype.handleDegeneracies = function(i1, ind1, i2, i3, i4, ind4) {
  var ind5 = this.fetchPrevData(ind4);
  var i5 = this.fetchData(ind5);
  if (i5 !== i2 && i5 !== i3) {
    var type = [0];
    var flag = this.isVertexInTriangle(i1, i2, i3, i5, type);
    if (flag && type[0] === 0) {
      return true;
    }
    if (i2 <= i3) {
      if (i4 <= i5) {
        flag = this.segmentsIntersect(i2, i3, i4, i5, -1);
      } else {
        flag = this.segmentsIntersect(i2, i3, i5, i4, -1);
      }
    } else {
      if (i4 <= i5) {
        flag = this.segmentsIntersect(i3, i2, i4, i5, -1);
      } else {
        flag = this.segmentsIntersect(i3, i2, i5, i4, -1);
      }
    }
    if (flag) {
      return true;
    }
  }

  ind5 = this.fetchNextData(ind4);
  i5 = this.fetchData(ind5);
  if (i5 !== i2 && i5 !== i3) {
    var type = [0];
    var flag = this.isVertexInTriangle(i1, i2, i3, i5, type);
    if (flag && type[0] === 0) {
      return true;
    }
    if (i2 <= i3) {
      if (i4 <= i5) {
        flag = this.segmentsIntersect(i2, i3, i4, i5, -1);
      } else {               
        flag = this.segmentsIntersect(i2, i3, i5, i4, -1);
      }
    } else {
      if (i4 <= i5) {
        flag = this.segmentsIntersect(i3, i2, i4, i5, -1);
      } else {
        flag = this.segmentsIntersect(i3, i2, i5, i4, -1);
      }
    }
    if (flag) {
      return true;
    }
  }

  var i0   = i1;
  var ind0 = ind1;
  var aera = 0.; 
  var area1 = 0.;
  ind1 = this.fetchNextData(ind1);
  i1 = this.fetchData(ind1);
  while (ind1 !== ind4) {
    var ind2  = this.fetchNextData(ind1);
    i2 = this.fetchData(ind2);
    area = this.stableDet2D(i0, i1, i2);
    area1 += area;
    ind1   = ind2;
    i1     = i2;
  }

  var area2 = 0.;
  ind1 = this.fetchPrevData(ind0);
  i1 = this.fetchData(ind1);
  while (ind1 !== ind4) {
    var ind2  = this.fetchPrevData(ind1);
    i2 = this.fetchData(ind2);
    area = this.stableDet2D(i0, i1, i2);
    area2 += area;
    ind1   = ind2;
    i1     = i2;
  }

  if (area1 <= this.ZERO && area2 <= this.ZERO) {
    return false;
  } else if (!(area1 <= -this.ZERO) && !(area2 <= -this.ZERO)) { 
    return false;
  } else {
    return true;
  }
}

// From Desperate class
Triangulator.prototype.desperate = function(ind, i, splitted) {
  var i1 = [0];
  var i2 = [0];
  var i3 = [0];
  var i4 = [0];
  var ind1 = [0];
  var ind2 = [0];
  var ind3 = [0];
  var ind4 = [0];

  splitted[0] = false;
  if (this.existsCrossOver(ind, ind1, i1, ind2, i2, ind3, i3, ind4, i4)) {
    this.handleCrossOver(ind1[0], i1[0], ind2[0], i2[0], ind3[0], i3[0], ind4[0], i4[0]);
    return false;
  }

  this.prepareNoHashEdges(i, i+1);
  if (this.existsSplit(ind, ind1, i1, ind2, i2)) {
    this.handleSplit(ind1[0], i1[0], ind2[0], i2[0]);
    splitted[0] = true;
    return false;
  }

  return true;
}

Triangulator.prototype.existsCrossOver = function(ind, ind1, i1, ind2, i2, ind3, i3, ind4, i4) {
  ind1[0] = ind;
  i1[0] = this.fetchData(ind1[0]);
  ind2[0] = this.fetchNextData(ind1[0]);
  i2[0] = this.fetchData(ind2[0]);
  ind3[0] = this.fetchNextData(ind2[0]);
  i3[0] = this.fetchData(ind3[0]);
  ind4[0] = this.fetchNextData(ind3[0]);
  i4[0] = this.fetchData(ind4[0]);
  do {
    var box1 = new BoundingBox(this, i1[0], i2[0]);
    var box2 = new BoundingBox(this, i3[0], i4[0]);
    if (box1.overlaps(box2)) {
      if (this.segmentsIntersect(box1.imin, box1.imax, box2.imin, box2.imax, -1)) {
        return true;
      }
    }
    ind1[0] = ind2[0];
    i1[0]   = i2[0];
    ind2[0] = ind3[0];
    i2[0]   = i3[0];
    ind3[0] = ind4[0];
    i3[0]   = i4[0];
    ind4[0] = this.fetchNextData(ind3[0]);
    i4[0] = this.fetchData(ind4[0]);
  } while (ind1[0] !== ind);

  return false;
}

Triangulator.prototype.handleCrossOver = function(ind1, i1, ind2, i2, ind3, i3, ind4, i4) {
  var angle1 = this.getAngle(ind1);
  var angle4 = this.getAngle(ind4);
  var first;
  if (angle1 < angle4) {
    first = true;
  } else if (angle1 > angle4) {
    first = false;
  } else {
    first = true;
  }

  if (first) {
    this.deleteLinks(ind2);
    this.storeTriangle(ind1, ind2, ind3);
    this.setAngle(ind3, 1);
    this.insertIntoHeap(0.0, ind3, ind1, ind4);
  } else {
    this.deleteLinks(ind3);
    this.storeTriangle(ind2, ind3, ind4);
    this.setAngle(ind2, 1);
    this.insertIntoHeap(0.0, ind2, ind1, ind4);
  }
}

Triangulator.prototype.letsHope = function(ind) {
  var ind1 = ind;
  do  {
    if (this.getAngle(ind1) > 0) {
      var ind0 = this.fetchPrevData(ind1);
      var ind2 = this.fetchNextData(ind1);
      this.insertIntoHeap(0.0, ind1, ind0, ind2);
      return true;
    }
    ind1 = this.fetchNextData(ind1);
  } while (ind1 !== ind);

  this.setAngle(ind, 1);
  var ind0 = this.fetchPrevData(ind);
  var ind2 = this.fetchNextData(ind);
  this.insertIntoHeap(0.0, ind, ind0, ind2);
  return true;
}

Triangulator.prototype.existsSplit = function(ind, ind1, i1, ind2, i2) {
  if (this.numPoints > this.maxNumDist) {
    this.maxNumDist = this.numPoints;
    this.distances = new Array(this.maxNumDist);
    for (var k = 0; k < this.maxNumDist; k++) {
      this.distances[k] = new Distance();
    }
  }
  ind1[0] = ind;
  i1[0] = this.fetchData(ind1[0]);
  var ind4 = this.fetchNextData(ind1[0]);
  var i4 = this.fetchData(ind4);
  var ind5 = this.fetchNextData(ind4);
  var i5 = this.fetchData(ind5);
  var ind3  = this.fetchPrevData(ind1[0]);
  var i3 = this.fetchData(ind3);
  if (this.foundSplit(ind5, i5, ind3, ind1[0], i1[0], i3, i4, ind2, i2)) {
    return true;
  }
  i3      = i1[0];
  ind1[0] = ind4;
  i1[0]   = i4;
  ind4    = ind5;
  i4      = i5;
  ind5    = this.fetchNextData(ind4);
  i5      = this.fetchData(ind5);
  while (ind5 !== ind) {
    if (this.foundSplit(ind5, i5, ind, ind1[0], i1[0], i3, i4, ind2, i2)) {
      return true;
    }
    i3    = i1[0];
    ind1[0] = ind4;
    i1[0]   = i4;
    ind4  = ind5;
    i4    = i5;
    ind5  = this.fetchNextData(ind4);
    i5    = this.fetchData(ind5);
  }
  
  return false;
}

Triangulator.prototype.windingNumber = function(ind, p) {
  var i1 = this.fetchData(ind);
  var ind2 = this.fetchNextData(ind);
  var i2 = this.fetchData(ind2);
  var angle = this.angle(p, this.points[i1], this.points[i2]);
  while (ind2 !== ind) {
    i1   = i2;
    ind2 = this.fetchNextData(ind2);
    i2 = this.fetchData(ind2);
    angle += this.angle(p, this.points[i1], this.points[i2]);
  }

  angle += Math.PI;
  return Math.floor(angle / (Math.PI*2.0));
}

Triangulator.prototype.foundSplit = function(ind5, i5, ind, ind1, i1, i3, i4, ind2, i2) {
  var numDist = 0;
  do {
    this.distances[numDist].dist = this.baseLength(this.points[i1],
        this.points[i5]);
    this.distances[numDist].ind = ind5;
    ++numDist;
    ind5 = this.fetchNextData(ind5);
    i5 = this.fetchData(ind5);
  } while (ind5 !== ind);

  this.sortDistance(this.distances, numDist);

  for (var j = 0; j < numDist; j++) {
    ind2[0] = this.distances[j].ind;
    i2[0] = this.fetchData(ind2[0]);
    if (i1 !== i2[0]) {
      var ind6  = this.fetchPrevData(ind2[0]);
      var i6 = this.fetchData(ind6);
      var ind7  = this.fetchNextData(ind2[0]);
      var i7 = this.fetchData(ind7);
      var convex = this.getAngle(ind2[0]) > 0;
      if (this.isInCone(i6, i2[0], i7, i1, convex)) {
        convex = this.getAngle(ind1) > 0;
        if (this.isInCone(i3, i1, i4, i2[0], convex)) {
          var box = new BoundingBox(this, i1, i2[0]);
          if (!this.noHashEdgeIntersectionExists(box, -1, -1, ind1, -1)) {
            var center = [(this.points[i1][0] + this.points[i2[0]][0]) * 0.5, 
                          (this.points[i1][1] + this.points[i2[0]][1]) * 0.5];
            if (this.windingNumber(ind, center) === 1) {
              return true;
            }
          }
        }
      }
    }
  }

  return false;
}

Triangulator.prototype.handleSplit = function(ind1, i1, ind3, i3) {
  var ind2 = this.makeNode(i1);
  this.insertAfter(ind1, ind2);
  var comIndex = this.list[ind1].getCommonIndex();
  this.list[ind2].setCommonIndex(comIndex);
  
  var ind4 = this.makeNode(i3);
  this.insertAfter(ind3, ind4);
  comIndex = this.list[ind3].getCommonIndex();
  this.list[ind4].setCommonIndex(comIndex);
  
  this.splitSplice(ind1, ind2, ind3, ind4);

  this.storeChain(ind1);
  this.storeChain(ind3);
  
  var next = this.fetchNextData(ind1);
  var nxt = this.fetchData(next);
  var prev = this.fetchPrevData(ind1);
  var prv = this.fetchData(prev);
  var angle = this.isConvexAngle(prv, i1, nxt, ind1);
  this.setAngle(ind1, angle);
  
  next = this.fetchNextData(ind2);
  nxt = this.fetchData(next);
  prev = this.fetchPrevData(ind2);
  prv = this.fetchData(prev);
  angle = this.isConvexAngle(prv, i1, nxt, ind2);
  this.setAngle(ind2, angle);
  
  next = this.fetchNextData(ind3);
  nxt = this.fetchData(next);
  prev = this.fetchPrevData(ind3);
  prv = this.fetchData(prev);
  angle = this.isConvexAngle(prv, i3, nxt, ind3);
  this.setAngle(ind3, angle);
  
  next = this.fetchNextData(ind4);
  nxt = this.fetchData(next);
  prev = this.fetchPrevData(ind4);
  prv = this.fetchData(prev);
  angle = this.isConvexAngle(prv, i3, nxt, ind4);
  this.setAngle(ind4, angle);
}

// From Heap class
Triangulator.prototype.initHeap = function() {
  this.heap = [];
  this.numHeap = 0;
  this.numZero = 0;
}

Triangulator.prototype.storeHeapData = function(index, ratio, ind, prev, next) {
  this.heap[index] = new HeapNode();
  this.heap[index].ratio = ratio;
  this.heap[index].index = ind;
  this.heap[index].prev  = prev;
  this.heap[index].next  = next;
}

Triangulator.prototype.dumpOnHeap = function(ratio, ind, prev, next) {
  var index;
  if (ratio === 0.0) {
    if (this.numZero < this.numHeap) {
      if (this.heap[this.numHeap] === undefined) {
        this.storeHeapData(this.numHeap, this.heap[this.numZero].ratio,
            this.heap[this.numZero].index,
            this.heap[this.numZero].prev,
            this.heap[this.numZero].next);
      } else {
        this.heap[this.numHeap].copy(this.heap[this.numZero]);
      }
    }
    index = this.numZero;
    ++this.numZero;
  } else {
    index = this.numHeap;
  }

  this.storeHeapData(index, ratio, ind, prev, next);
  ++this.numHeap;
}

Triangulator.prototype.insertIntoHeap = function(ratio, ind, prev, next) {
  this.dumpOnHeap(ratio, ind, prev, next);
}

Triangulator.prototype.deleteFromHeap = function(ind, prev, next) {
  if (this.numZero > 0) {
    --this.numZero;
    --this.numHeap;
    ind[0]  = this.heap[this.numZero].index;
    prev[0] = this.heap[this.numZero].prev;
    next[0] = this.heap[this.numZero].next;
    if (this.numZero < this.numHeap) {
      this.heap[this.numZero].copy(this.heap[this.numHeap]);
    }
    return true;
  } else {
    if (this.numHeap <= 0) {
      this.numHeap = 0;
      return false;
    }
    --this.numHeap;
    ind[0]  = this.heap[this.numHeap].index;
    prev[0] = this.heap[this.numHeap].prev;
    next[0] = this.heap[this.numHeap].next;
    return true;
  }
}

// From Bridge class
Triangulator.prototype.sortDistance = function(distances, numPts) {
  var swap = new Distance();
  for (var i = 0; i < numPts; i++) {
    for (var j = i + 1; j < numPts; j++) {
      if (this.compareDistances(distances[i], distances[j]) > 0) {
        swap.copy(distances[i]);
        distances[i].copy(distances[j]);
        distances[j].copy(swap);
      }
    }
  }
}

Triangulator.prototype.compareDistances = function(a, b) {
  if (a.dist < b.dist) {
    return -1;
  } else if (a.dist > b.dist) {
    return  1;
  } else {
    return 0;
  }
}

// From BottleNeck class
Triangulator.prototype.checkArea = function(ind4, ind5) {
  var i0 = this.fetchData(ind4);
  var ind1 = this.fetchNextData(ind4);
  var i1 = this.fetchData(ind1);
  var area = 0.;
  var area1 = 0.;
  while (ind1 != ind5) {
    var ind2  = this.fetchNextData(ind1);
    var i2 = this.fetchData(ind2);
    area = this.stableDet2D(i0, i1, i2);
    area1 += area;
    ind1   = ind2;
    i1     = i2;
  }

  if (area1 <= this.ZERO) {
    return false;
  }

  ind1 = this.fetchNextData(ind5);
  i1 = this.fetchData(ind1);
  var area2 = 0.;
  while (ind1 != ind4) {
    var ind2  = this.fetchNextData(ind1);
    var i2 = this.fetchData(ind2);
    area = this.stableDet2D(i0, i1, i2);
    area2 += area;
    ind1   = ind2;
    i1     = i2;
  }

  return !(area2 <= this.ZERO);
}

Triangulator.prototype.checkBottleNeck = function(i1, i2, i3, ind4) {
  var i4 = i1;
  var ind5 = this.fetchPrevData(ind4);
  var i5 = this.fetchData(ind5);
  if (i5 !== i2 && i5 !== i3) {
    if (this.isPointInTriangle(i1, i2, i3, i5)) {
      return true;
    }
  }

  var flag;
  if (i2 <= i3) {
    if (i4 <= i5) {
      flag = this.segmentsIntersect(i2, i3, i4, i5, -1);
    } else {
      flag = this.segmentsIntersect(i2, i3, i5, i4, -1);
    }
  } else {
    if (i4 <= i5) {
      flag = this.segmentsIntersect(i3, i2, i4, i5, -1);
    } else {
      flag = this.segmentsIntersect(i3, i2, i5, i4, -1);
    }
  }
  if (flag) {
    return true;
  }

  ind5 = this.fetchNextData(ind4);
  i5 = this.fetchData(ind5);
  if (i5 !== i2 && i5 !== i3) {
    if (this.isPointInTriangle(i1, i2, i3, i5)) {
      return true;
    }
  }
  if (i2 <= i3) {
    if (i4 <= i5) {
      flag = this.segmentsIntersect(i2, i3, i4, i5, -1);
    } else {
      flag = this.segmentsIntersect(i2, i3, i5, i4, -1);
    }
  } else {
    if (i4 <= i5) {
      flag = this.segmentsIntersect(i3, i2, i4, i5, -1);
    } else {
      flag = this.segmentsIntersect(i3, i2, i5, i4, -1);
    }
  }
  if (flag) {
    return true;
  }

  ind5 = this.fetchNextData(ind4);
  i5 = this.fetchData(ind5);
  while (ind5 !== ind4) {
    if (i4 === i5) {
      if (this.checkArea(ind4, ind5)) {
        return true;
      }
    }
    ind5 = this.fetchNextData(ind5);
    i5 = this.fetchData(ind5);
  }

  return  false;
}


// ListNode class
function ListNode(ind) {
  this.index = ind;
  this.prev = -1;
  this.next = -1;
  this.convex = 0;
  this.vcntIndex = -1;
}

ListNode.prototype.setCommonIndex = function(comIndex) {
  this.vcntIndex = comIndex;
}

ListNode.prototype.getCommonIndex = function() {
  return this.vcntIndex;
}

// Triangle class
function Triangle(a, b, c) {
  this.v1 = a;
  this.v2 = b;
  this.v3 = c;
}

// BoundingBox class (from BBox class) 
function BoundingBox(triangulator, i, j) {
  this.imin = Math.min(i, j);
  this.imax = Math.max(i, j);
  this.ymin = Math.min(triangulator.points[this.imin][1], triangulator.points[this.imax][1]);
  this.ymax = Math.max(triangulator.points[this.imin][1], triangulator.points[this.imax][1]);
}

BoundingBox.prototype.isInBoundingBox = function(point, i) {
  return this.imax >= i 
      && this.imin <= i 
      && this.ymax >= point[1] 
      && this.ymin <= point[1];
}

BoundingBox.prototype.overlaps = function(box) {
  return this.imax >= box.imin 
      && this.imin <= box.imax 
      && this.ymax >= box.ymin 
      && this.ymin <= box.ymax;
}

// HeapNode class
function HeapNode() {
  this.index = 0;
  this.prev = 0;
  this.next = 0;
  this.ratio = 0.;
}

HeapNode.prototype.copy = function(hNode) {
  this.index = hNode.index;
  this.prev = hNode.prev;
  this.next = hNode.next;
  this.ratio = hNode.ratio;
}

// Distance class
function Distance() {
  this.ind = 0;
  this.dist = 0.;
}

Distance.prototype.copy = function(d) {
  this.ind = d.ind;
  this.dist = d.dist;
}

// PointNode class (from PntNode class)
function PointNode() {
  this.pnt = 0;
  this.next = 0;
}
