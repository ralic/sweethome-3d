/*
 * HTMLCanvas3D.js
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

/**
 * Creates a canvas 3D bound to HTML canvas with the given id.
 * @param {string}  canvasId  the value of the id attribute of the canvas bound to this component
 * @constructor
 * @author Emmanuel Puybaret
 */
function HTMLCanvas3D(canvasId) {
  this.scene = null;
  this.textures = [];
  this.sceneGeometries = [];
  this.backgroundGeometries = [];
  this.lights = [];
  this.fieldOfView = Math.PI * 45 / 180;
  this.frontClipDistance = 0.1;
  this.backClipDistance = 100; 
  
  // Initialize WebGL
  this.canvas = document.getElementById(canvasId);
  this.gl = this.canvas.getContext("webgl");
  if (!this.gl) {
    this.gl = this.canvas.getContext("experimental-webgl");
    if (!this.gl) {
      throw "No WebGL";
    }
  }
  this.updateViewportSize();
  
  // Initialize shader
  this.shaderProgram = this.gl.createProgram();
  var vertexShader = this.createShader(this.gl.VERTEX_SHADER,
      "attribute vec3 vertexPosition;" 
    + "attribute vec3 vertexNormal;"
    + "attribute vec2 vertexTextureCoord;"
    + "uniform mat4 modelViewTransform;"
    + "uniform mat4 projectionTransform;"
    + "uniform bool textureCoordinatesGenerated;"
    + "uniform vec4 planeS;"
    + "uniform vec4 planeT;"
    + "uniform mat3 textureCoordTransform;"
    + "uniform mat3 normalTransform;"
    + "uniform bool backFaceNormalFlip;"
    + "uniform bool lightingEnabled;"
    + "uniform bool useTextures;"
    + "varying vec2 varTextureCoord;"
    + "varying vec4 varVertexPosition;"
    + "varying vec3 varTransformedNormal;"
    + "void main(void) {"
    + "  varVertexPosition = modelViewTransform * vec4(vertexPosition, 1.0);"
    + "  gl_Position = projectionTransform * varVertexPosition;"
    + "  if (useTextures) {"
    + "    if (textureCoordinatesGenerated) {"
    + "      varTextureCoord = vec2(vertexPosition.x * planeS.x + vertexPosition.y * planeS.y"
    + "          + vertexPosition.z * planeS.z + planeS.w,"
    + "            vertexPosition.x * planeT.x + vertexPosition.y * planeT.y"
    + "          + vertexPosition.z * planeT.z + planeT.w);"
    + "    } else {"
    + "      varTextureCoord = vec2(vertexTextureCoord);"
    + "    }"
    + "    varTextureCoord = vec2(textureCoordTransform * vec3(varTextureCoord, 1));"
    + "  }"
    + "  if (lightingEnabled) {"
    + "    vec3 normal = vertexNormal;"
    + "    if (backFaceNormalFlip) {" 
    + "      normal = -normal;"
    +	"    }"
    + "    varTransformedNormal = normalize(normalTransform * normal);"
    + "  }"
    + "}");
  this.gl.attachShader(this.shaderProgram, vertexShader);
  var fragmentShader = this.createShader(this.gl.FRAGMENT_SHADER,
      "precision mediump float;"
    + "uniform vec3  vertexDiffuseColor;"
    + "uniform vec3  vertexSpecularColor;"
    + "uniform float shininess;"
    + "uniform sampler2D sampler;"
    + "uniform float alpha;"
    + "uniform bool  useTextures;"
    + "uniform bool  lightingEnabled;"
    + "uniform vec3  ambientColor;"
    + "uniform int   directionalLightCount;"
    + "uniform vec3  lightDirections[" + HTMLCanvas3D.MAX_DIRECTIONAL_LIGHT + "];"
    + "uniform vec3  directionalLightColors[" + HTMLCanvas3D.MAX_DIRECTIONAL_LIGHT + "];"
    + "varying vec2  varTextureCoord;"
    + "varying vec4  varVertexPosition;"
    + "varying vec3  varTransformedNormal;"
    + "void main(void) {" 
    + "  vec3 lightWeight;"
    + "  if (lightingEnabled) {"
    + "    lightWeight = ambientColor;"
    + ""
    + "    if (directionalLightCount > 0) {"
    + "      vec3 diffuseLightWeight = vec3(0., 0., 0.);"
    + "      vec3 specularLightWeight = vec3(0., 0., 0.);"
    + "      vec3 eyeDirection = vec3(0., 0., 0.);"
    + "      bool computeSpecularLightWeight = false;"
    + "      if (vertexSpecularColor.r > 0." 
    + "          && vertexSpecularColor.g > 0."
    + "          && vertexSpecularColor.b > 0.) {"
    + "        eyeDirection = normalize(-varVertexPosition.xyz);"
    + "        computeSpecularLightWeight = length(eyeDirection) <= 1.0001;"  // May happen under iOS even after a normalization
    + "      }"
    + ""
    + "      for (int i = 0; i < " + HTMLCanvas3D.MAX_DIRECTIONAL_LIGHT + "; i++) {" 
    + "        if (i >= directionalLightCount) {" 
    + "          break;" 
    + "        }" 
    + "        float directionalLightWeight = max(dot(varTransformedNormal, lightDirections[i]), 0.);"
    + "        diffuseLightWeight += directionalLightColors[i] * directionalLightWeight;"
    + "        if (computeSpecularLightWeight) {"
    + "          vec3 reflectionDirection = reflect(-lightDirections[i], varTransformedNormal);"
    + "          specularLightWeight += directionalLightColors[i] * pow(max(dot(reflectionDirection, eyeDirection), 0.), shininess);"
    + "        }"
    + "      }"
    + ""
    + "      lightWeight += vertexDiffuseColor * diffuseLightWeight;"
    + "      if (computeSpecularLightWeight) {"
    + "        lightWeight += vertexSpecularColor * specularLightWeight;"
    + "      }"
    + "    }"
    + "  } else {"
    + "    lightWeight = vertexDiffuseColor;"
    + "  }"
    + ""
    + "  vec4 fragmentColor;"
    + "  if (useTextures) {"
    + "    fragmentColor = texture2D(sampler, vec2(varTextureCoord.s, varTextureCoord.t));"
    + "  } else {"
    + "    fragmentColor = vec4(1., 1., 1., 1.);"
    + "  }"
    + "  gl_FragColor = vec4(fragmentColor.rgb * lightWeight * alpha, fragmentColor.a * alpha);"
    + "}");
  this.gl.attachShader(this.shaderProgram, fragmentShader);
  this.gl.linkProgram(this.shaderProgram);
  this.shaderProgram.vertexPositionAttribute = this.gl.getAttribLocation(this.shaderProgram, "vertexPosition");
  this.shaderProgram.normalAttribute = this.gl.getAttribLocation(this.shaderProgram, "vertexNormal");
  this.shaderProgram.textureCoordAttribute = this.gl.getAttribLocation(this.shaderProgram, "vertexTextureCoord");
  this.shaderProgram.projectionTransform = this.gl.getUniformLocation(this.shaderProgram, "projectionTransform");
  this.shaderProgram.modelViewTransform = this.gl.getUniformLocation(this.shaderProgram, "modelViewTransform");
  this.shaderProgram.textureCoordinatesGenerated = this.gl.getUniformLocation(this.shaderProgram, "textureCoordinatesGenerated");
  this.shaderProgram.planeS = this.gl.getUniformLocation(this.shaderProgram, "planeS");
  this.shaderProgram.planeT = this.gl.getUniformLocation(this.shaderProgram, "planeT");
  this.shaderProgram.textureCoordTransform = this.gl.getUniformLocation(this.shaderProgram, "textureCoordTransform");
  this.shaderProgram.normalTransform = this.gl.getUniformLocation(this.shaderProgram, "normalTransform");
  this.shaderProgram.backFaceNormalFlip = this.gl.getUniformLocation(this.shaderProgram, "backFaceNormalFlip");
  this.shaderProgram.ambientColor = this.gl.getUniformLocation(this.shaderProgram, "ambientColor");
  this.shaderProgram.lightingEnabled = this.gl.getUniformLocation(this.shaderProgram, "lightingEnabled");
  this.shaderProgram.directionalLightCount = this.gl.getUniformLocation(this.shaderProgram, "directionalLightCount");
  this.shaderProgram.lightDirections = this.gl.getUniformLocation(this.shaderProgram, "lightDirections");
  this.shaderProgram.directionalLightColors = this.gl.getUniformLocation(this.shaderProgram, "directionalLightColors");
  this.shaderProgram.vertexDiffuseColor = this.gl.getUniformLocation(this.shaderProgram, "vertexDiffuseColor");
  this.shaderProgram.vertexSpecularColor = this.gl.getUniformLocation(this.shaderProgram, "vertexSpecularColor");
  this.shaderProgram.shininess = this.gl.getUniformLocation(this.shaderProgram, "shininess");
  this.shaderProgram.alpha = this.gl.getUniformLocation(this.shaderProgram, "alpha");
  this.shaderProgram.useTextures = this.gl.getUniformLocation(this.shaderProgram, "useTextures");
  this.gl.useProgram(this.shaderProgram);

  // Set default transformation
  this.viewPlatformTransform = mat4.create();
  mat4.translate(this.viewPlatformTransform, this.viewPlatformTransform, [0.0, 0.0, -2.4]);

  // Instantiate objects used in drawGeometry to avoid to GC them
  this.geometryAmbientColor = vec3.create();
  this.geometrySpecularColor = vec3.create();
  this.geometryModelViewTransform = mat4.create();
  this.geometryNormalTransform = mat3.create();
  
  // Set default shader colors, matrices and other values
  this.shaderAmbientColor = vec3.create();
  this.gl.uniform3fv(this.shaderProgram.ambientColor, this.shaderAmbientColor);
  this.shaderSpecularColor = vec3.create();
  this.gl.uniform3fv(this.shaderProgram.vertexSpecularColor, this.shaderSpecularColor);
  this.geometryDiffuseColor = vec3.create();
  this.shaderDiffuseColor = vec3.fromValues(1, 1, 1);
  this.gl.uniform3fv(this.shaderProgram.vertexDiffuseColor, this.shaderDiffuseColor);
  this.shaderModelViewTransform = mat4.create();
  this.gl.uniformMatrix4fv(this.shaderProgram.modelViewTransform, false, this.shaderModelViewTransform);
  this.shaderNormalTransform = mat3.create();
  this.gl.uniformMatrix3fv(this.shaderProgram.normalTransform, false, this.shaderNormalTransform);
  this.shaderTextureTransform = mat3.create();
  this.gl.uniformMatrix3fv(this.shaderProgram.textureCoordTransform, false, this.shaderTextureTransform);
  this.shaderLightingEnabled = true;
  this.gl.uniform1i(this.shaderProgram.lightingEnabled, this.shaderLightingEnabled);
  this.shaderShininess = 1.;
  this.gl.uniform1f(this.shaderProgram.shininess, this.shaderShininess);
  this.shaderBackFaceNormalFlip = false;
  this.gl.uniform1i(this.shaderProgram.backFaceNormalFlip, this.shaderBackFaceNormalFlip);
  this.shaderTextureCoordinatesGenerated = false;
  this.gl.uniform1i(this.shaderProgram.textureCoordinatesGenerated, false);
  this.shaderUseTextures = false;
  this.gl.uniform1i(this.shaderProgram.useTextures, false);
  this.shaderAlpha = 1;
  this.gl.uniform1f(this.shaderProgram.alpha, this.shaderAlpha);
  this.gl.enable(this.gl.CULL_FACE);
  this.gl.cullFace(this.gl.BACK);

  this.canvasNeededRepaint = false;
  this.pickingFrameBufferNeededRepaint = true;
}

HTMLCanvas3D.MAX_DIRECTIONAL_LIGHT = 16;
HTMLCanvas3D.VEC4_DEFAULT_PLANE_S = vec4.fromValues(1, 0, 0, 0);
HTMLCanvas3D.VEC4_DEFAULT_PLANE_T = vec4.fromValues(0, 1, 0, 0);
HTMLCanvas3D.MAT3_IDENTITY = mat3.create();

/**
 * Returns a shader from the given source code.
 * @private
 */
HTMLCanvas3D.prototype.createShader = function(type, source) {
  var shader = this.gl.createShader(type);
  this.gl.shaderSource(shader, source);
  this.gl.compileShader(shader);
  if (!this.gl.getShaderParameter(shader, this.gl.COMPILE_STATUS)) {
    throw "Invalid shader: " + source; 
  }
  return shader;
}

/**
 * Returns the HTML canvas associated to this component.
 */
HTMLCanvas3D.prototype.getCanvas = function() {
  return this.canvas;
}

/**
 * Sets the field of view of this canvas.
 * @param {number} fieldOfView
 */
HTMLCanvas3D.prototype.setFieldOfView = function(fieldOfView) {
  if (this.fieldOfView != fieldOfView) {
    this.fieldOfView = fieldOfView;
    this.repaint();
  }
}

/**
 * Sets the front clip distance of the fustrum.
 * @param {number} frontClipDistance
 */
HTMLCanvas3D.prototype.setFrontClipDistance = function(frontClipDistance) {
  if (this.frontClipDistance != frontClipDistance) {
    this.frontClipDistance = frontClipDistance;
    this.repaint();
  }
}

/**
 * Sets the back clip distance of the fustrum.
 * @param {number} frontClipDistance
 */
HTMLCanvas3D.prototype.setBackClipDistance = function(backClipDistance) {
  if (this.backClipDistance != backClipDistance) {
    this.backClipDistance = backClipDistance;
    this.repaint();
  }
}

/**
 * Updates the transformation used to view the scene and redraws it.
 */
HTMLCanvas3D.prototype.setViewPlatformTransform = function(viewPlatformTransform) {
  if (this.viewPlatformTransform != viewPlatformTransform) {
    this.viewPlatformTransform = viewPlatformTransform;
    this.repaint();
  }
}

/**
 * Updates the viewport size from HTML canvas size.
 * @package
 * @ignore
 */
HTMLCanvas3D.prototype.updateViewportSize = function() {
  var canvasBounds = this.canvas.getBoundingClientRect();
  if (this.viewportWidth != canvasBounds.width
      || this.viewportHeight != canvasBounds.height) {
    this.viewportWidth = canvasBounds.width;
    this.viewportHeight = canvasBounds.height;
    
    if (this.pickingFrameBuffer !== undefined) {
      this.gl.deleteFramebuffer(this.pickingFrameBuffer);
      delete this.pickingFrameBuffer;
    }
    this.repaint();
  }
}

/**
 * Displays the given scene in the canvas.
 * @param {Node3D} scene  the scene to view in this canvas
 * @param onprogression   progression call back
 */
HTMLCanvas3D.prototype.setScene = function(scene, onprogression) {
  var sceneGeometries = [];
  var backgroundGeometries = [];
  var lights = [];
  var sceneGeometryCount = this.countDisplayedGeometries(scene);
  this.prepareScene(scene, sceneGeometries, backgroundGeometries, false, [], lights, mat4.create(), onprogression, sceneGeometryCount);
  this.scene = scene;
  
  var canvas3D = this;
  setTimeout(
      function() {
        if (onprogression !== undefined) {
          onprogression(Node3D.BINDING_MODEL, "", 1);
        }
        canvas3D.sceneGeometries = sceneGeometries;
        canvas3D.backgroundGeometries = backgroundGeometries;
        canvas3D.lights = lights;
        canvas3D.drawScene();
      }, 0);
}

/**
 * Returns the scene viewed in this canvas.
 * @return {Node3D} 
 * @package
 * @ignore
 */
HTMLCanvas3D.prototype.getScene = function() {
  return this.scene;
}

/**
 * Returns the count of geometries in all the shapes children of the given node.
 * @param {Node3D} node
 * @private
 */
HTMLCanvas3D.prototype.countDisplayedGeometries = function(node) {
  if (node instanceof Group3D) {
    var sceneGeometryCount = 0;
    var children = node.getChildren();
    for (var i = 0; i < children.length; i++) {
      sceneGeometryCount += this.countDisplayedGeometries(children [i]);
    }
    return sceneGeometryCount;
  } else if (node instanceof Link3D) {
    return this.countDisplayedGeometries(node.getSharedGroup());
  } else if (node instanceof Shape3D) {
    return node.getGeometries().length;
  } else {
    return 0;
  }
}

HTMLCanvas3D.DEFAULT_APPEARANCE = new Appearance3D();

/**
 * Prepares the scene to be rendered, creating the required buffers and textures in WebGL.  
 * @param {Node3D}  node
 * @param {Array}   sceneGeometries
 * @param {Array}   backgroundGeometries
 * @param {boolean} background
 * @param [Link3D]  parentLinks
 * @param [Array]   lights
 * @param {mat4}    parentTransforms
 * @param onprogression
 * @param {number}  sceneGeometryCount
 * @private
 */
HTMLCanvas3D.prototype.prepareScene = function(node, sceneGeometries, backgroundGeometries, background, parentLinks, lights, parentTransforms, 
                                               onprogression, sceneGeometryCount) {
  var canvas3D = this;
  if (node instanceof Group3D) {
    if (node instanceof TransformGroup3D) {
      var nodeTransform = mat4.create();
      node.getTransform(nodeTransform);
      parentTransforms = mat4.mul(mat4.create(), parentTransforms, nodeTransform);
      if (node.getCapability(TransformGroup3D.ALLOW_TRANSFORM_WRITE)) {
        // Add listener to update the scene when transformation changes
        node.addPropertyChangeListener("TRANSFORM", 
            function(ev) {
              var oldInvert = mat4.invert(mat4.create(), ev.getOldValue());
              mat4.mul(parentTransforms, parentTransforms, oldInvert);
              mat4.mul(parentTransforms, parentTransforms, ev.getNewValue());
              var children = node.getChildren();
              for (var i = 0; i < children.length; i++) {
                canvas3D.updateChildrenTransformation(children [i], background ? backgroundGeometries : sceneGeometries, 
                    parentLinks, lights, parentTransforms);
              }
              canvas3D.repaint();
            });
      }
    }

    var children = node.getChildren();
    for (var i = 0; i < children.length; i++) {
      this.prepareScene(children [i], sceneGeometries, backgroundGeometries, background, parentLinks, lights, parentTransforms, onprogression, sceneGeometryCount);
    }
    if (node.getCapability(Group3D.ALLOW_CHILDREN_EXTEND)) {
      // Add listener to group to update the scene when children change
      node.addChildrenListener(
          {  
            childAdded : function(ev) {
              canvas3D.prepareScene(ev.child, sceneGeometries, backgroundGeometries, background, parentLinks, lights, parentTransforms);
              canvas3D.repaint();
            },
            childRemoved : function(ev) {
              canvas3D.removeDisplayedItems(ev.child, background ? backgroundGeometries : sceneGeometries, lights);
              // TODO Should remove listeners on deleted item
              canvas3D.repaint();
            }
          });
    }
  } else if (node instanceof Link3D) {
    parentLinks = parentLinks.slice(0);
    parentLinks.push(node);
    this.prepareScene(node.getSharedGroup(), sceneGeometries, backgroundGeometries, background, parentLinks, lights, parentTransforms, onprogression, sceneGeometryCount);
  } else if (node instanceof Shape3D) {
    // Log each time 10% more shape geometries are bound
    if (onprogression !== undefined
        && !background
        && Math.floor((sceneGeometries.length - 1) / sceneGeometryCount * 10) < Math.floor(sceneGeometries.length / sceneGeometryCount * 10)) {
      onprogression(Node3D.BINDING_MODEL, "", sceneGeometries.length / sceneGeometryCount);
    }
    var nodeAppearance = node.getAppearance();
    if (!nodeAppearance) {
      nodeAppearance = HTMLCanvas3D.DEFAULT_APPEARANCE;
    }
    var texture = null;
    if (nodeAppearance.getTextureImage()) {
      texture = this.prepareTexture(nodeAppearance.getTextureImage());
    }
    
    var nodeGeometries = node.getGeometries();
    for (var i = 0; i < nodeGeometries.length; i++) {
      this.prepareGeometry(nodeGeometries [i], nodeAppearance, texture, 
          node, sceneGeometries, backgroundGeometries, background, parentLinks, parentTransforms);
    }
    if (node.getCapability(Shape3D.ALLOW_GEOMETRY_WRITE)) {
      node.addPropertyChangeListener(
          function(ev) {
            if ("GEOMETRY" == ev.getPropertyName()) {
              if (ev.getOldValue()) {
                removedGeometry = ev.getOldValue();
                for (var i = 0; i < sceneGeometries.length; i++) {
                  var geometry = sceneGeometries [i];
                  if (geometry.nodeGeometry === removedGeometry) {
                    // Free geometry buffers
                    canvas3D.gl.deleteBuffer(geometry.vertexBuffer);
                    if (geometry.textureCoordinatesBuffer !== null
                        && geometry.texture !== undefined) {
                      canvas3D.gl.deleteBuffer(geometry.textureCoordinatesBuffer);
                    }
                    if (geometry.normalBuffer !== undefined) {
                      canvas3D.gl.deleteBuffer(geometry.normalBuffer);
                    }
                    sceneGeometries.splice(i, 1);
                    break;
                  }
                }
              }
              if (ev.getNewValue()) {
                addedGeometry = ev.getNewValue();
                // Retrieve possibly updated appearance and texture
                var nodeAppearance = node.getAppearance();
                if (!nodeAppearance) {
                  nodeAppearance = HTMLCanvas3D.DEFAULT_APPEARANCE;
                }
                var texture = null;
                if (nodeAppearance.getTextureImage()) {
                  texture = canvas3D.prepareTexture(nodeAppearance.getTextureImage());
                }
                canvas3D.prepareGeometry(addedGeometry, nodeAppearance, texture, 
                    node, sceneGeometries, backgroundGeometries, background, parentLinks, parentTransforms);
              }
            }
          });
    }

    if (nodeAppearance !== HTMLCanvas3D.DEFAULT_APPEARANCE) {
      nodeAppearance.addPropertyChangeListener(
          function(ev) {
            var geometries = background ? backgroundGeometries : sceneGeometries;
            for (var i = 0; i < geometries.length; i++) {
              var geometry = geometries [i];
              if (geometry.node === node) {
                var newValue = ev.getNewValue();
                switch (ev.getPropertyName()) {
                  case "AMBIENT_COLOR" : 
                    geometry.ambientColor = newValue;
                    break;
                  case "DIFFUSE_COLOR" : 
                    geometry.diffuseColor = newValue;
                    break;
                  case "SPECULAR_COLOR" : 
                    geometry.specularColor = newValue;
                    break;
                  case "SHININESS" :
                    geometry.shininess = newValue;
                    break;
                  case "TRANSPARENCY" : 
                    geometry.transparency = newValue !== undefined 
                        ? 1 - newValue
                        : 1;
                    break;
                  case "ILLUMINATION" :
                    geometry.lightingEnabled = (newValue === undefined || newValue >= 1)
                        && geometry.mode === canvas3D.gl.TRIANGLES;
                    break;
                  case "TEXTURE_IMAGE" : 
                    geometry.texture = newValue !== null
                        ? canvas3D.prepareTexture(newValue)
                        : undefined;
                    break;
                  case "TEXTURE_COORDINATES_GENERATION" :
                    var textureCoordinatesGeneration = newValue;
                    geometry.textureCoordinatesGeneration = textureCoordinatesGeneration;
                    break;
                  case "TEXTURE_TRANSFORM" :
                    geometry.textureTransform = newValue;
                    break;
                  case "VISIBLE" : 
                    geometry.visible = newValue !== false;
                    break;
                  case "CULL_FACE" : 
                    geometry.cullFace = newValue;
                    break;
                  case "BACK_FACE_NORMAL_FLIP" : 
                    geometry.backFaceNormalFlip = newValue === true;
                    break;
                }
              }
            }
            canvas3D.repaint();
          });
    }
  } else if (node instanceof Background3D) {
    this.prepareScene(node.getGeometry(), sceneGeometries, backgroundGeometries, true, parentLinks, lights, parentTransforms);
  } else if (node instanceof Light3D) {
    var light = {node  : node,
                 color : node.getColor()};
    if (node instanceof DirectionalLight3D) {
      light.direction = node.getDirection();
      light.transform = parentTransforms;
    }
    lights.push(light);
    
    node.addPropertyChangeListener(
        function(ev) {
          for (var i = 0; i < lights.length; i++) {
            var light = lights [i];
            if (lights [i].node === node) {
              var newValue = ev.getNewValue();
              switch (ev.getPropertyName()) {
                case "COLOR" : 
                  light.color = newValue;
                  break;
              }
              break;
            }
          }
          canvas3D.repaint();
        });
  }
}

/**
 * Prepares the geometry to be rendered.  
 * @param {IndexedGeometryArray3D} nodeGeometry
 * @param {Appearance3D} nodeAppearance
 * @param {WebGLTexture} texture
 * @param {Node3D}  node
 * @param {Array}   sceneGeometries
 * @param {Array}   backgroundGeometries
 * @param {boolean} background
 * @param [Link3D]  parentLinks
 * @param {mat4}    transform
 * @private
 */
HTMLCanvas3D.prototype.prepareGeometry = function(nodeGeometry, nodeAppearance, texture, 
                                                  node, sceneGeometries, backgroundGeometries, background, 
                                                  parentLinks, transform) {
  var geometries = background ? backgroundGeometries : sceneGeometries;
  var geometry = null;
  if (!node.getCapability(Shape3D.ALLOW_GEOMETRY_WRITE)) {
    // Search if node geometry is already used
    for (var i = 0; i < geometries.length; i++) {
      if (geometries [i].nodeGeometry === nodeGeometry) {
        geometry = {node : node,
                    nodeGeometry : nodeGeometry,
                    vertexCount  : geometries [i].vertexCount, 
                    vertexBuffer : geometries [i].vertexBuffer, 
                    textureCoordinatesBuffer : geometries [i].textureCoordinatesBuffer,
                    normalBuffer : geometries [i].normalBuffer,
                    mode : geometries [i].mode};
        break;
      }
    }
  }
  
  if (geometry === null) {
    geometry = {node : node,
                nodeGeometry : nodeGeometry,
                vertexCount  : nodeGeometry.vertexIndices.length};
    geometry.vertexBuffer = this.prepareBuffer(nodeGeometry.vertices, nodeGeometry.vertexIndices);
    geometry.textureCoordinatesBuffer = this.prepareBuffer(nodeGeometry.textureCoordinates, nodeGeometry.textureCoordinateIndices);
    if (nodeGeometry instanceof IndexedTriangleArray3D) {
      geometry.mode = this.gl.TRIANGLES;
      geometry.normalBuffer = this.prepareBuffer(nodeGeometry.normals, nodeGeometry.normalIndices);
    } else {
      geometry.mode = this.gl.LINES;
    } 
  } 
  // Set parameters not shared
  geometry.transform = transform;
  if (parentLinks.length > 0) {
    geometry.parentLinks = parentLinks;
  }
  var ambientColor = nodeAppearance.getAmbientColor();
  if (ambientColor !== undefined) {
    geometry.ambientColor = ambientColor;
  }
  var diffuseColor = nodeAppearance.getDiffuseColor();
  if (diffuseColor !== undefined) {
    geometry.diffuseColor = diffuseColor;
  }
  var specularColor = nodeAppearance.getSpecularColor();
  if (specularColor !== undefined) {
    geometry.specularColor = specularColor;
  }
  var shininess = nodeAppearance.getShininess();
  if (shininess !== undefined) {
    geometry.shininess = shininess;
  }
  var textureCoordinatesGeneration = nodeAppearance.getTextureCoordinatesGeneration();
  if (textureCoordinatesGeneration !== undefined) {
    geometry.textureCoordinatesGeneration = textureCoordinatesGeneration;
  }
  var textureTransform = nodeAppearance.getTextureTransform();
  if (textureTransform !== undefined) {
    geometry.textureTransform = textureTransform;
  }
  if (texture !== null) {
    geometry.texture = texture;
  }
  geometry.backFaceNormalFlip = nodeAppearance.isBackFaceNormalFlip();
  if (nodeAppearance.getCullFace() !== undefined) {
    geometry.cullFace = nodeAppearance.getCullFace();
  }
  var illumination = nodeAppearance.getIllumination();
  geometry.lightingEnabled =  
         (illumination === undefined || illumination >= 1)
      && geometry.normalBuffer !== null;
  geometry.transparency = nodeAppearance.getTransparency() !== undefined 
      ? 1 - nodeAppearance.getTransparency()
      : 1;
      geometry.visible = nodeAppearance.isVisible();
  geometries.push(geometry);
}

/**
 * Updates the transformation applied to the children of the given node.
 * @param {Node3D}  node
 * @param [Array]   geometries
 * @param [Link3D]  parentLinks
 * @param {Array}   lights
 * @param {mat4}    parentTransforms
 * @private  
 */
HTMLCanvas3D.prototype.updateChildrenTransformation = function(node, geometries, parentLinks, lights, parentTransforms) {
  var canvas3D = this;
  if (node instanceof Group3D) {
    if (node instanceof TransformGroup3D) {
      var nodeTransform = mat4.create();
      node.getTransform(nodeTransform);
      parentTransforms = mat4.mul(mat4.create(), parentTransforms, nodeTransform);
    }
    var children = node.getChildren();
    for (var i = 0; i < children.length; i++) {
      this.updateChildrenTransformation(children [i], geometries, parentLinks, lights, parentTransforms);
    }
  } else if (node instanceof Link3D) {
    parentLinks = parentLinks.slice(0);
    parentLinks.push(node);
    this.updateChildrenTransformation(node.getSharedGroup(), geometries, parentLinks, lights, parentTransforms);
  } else if (node instanceof Shape3D) {
    for (var i = 0; i < geometries.length; i++) {
      if (geometries [i].node === node) {
        var updateNode = geometries [i].parentLinks === undefined; 
        if (!updateNode) {
          // Check if the node of the geometry references the same parent links
          if (geometries [i].parentLinks.length === parentLinks.length) {
            var j;
            for (j = 0; j < parentLinks.length; j++) {
              if (geometries [i].parentLinks [j] !== parentLinks [j]) {
                break;
              }
            }
            updateNode = j === parentLinks.length;
          } 
        }
        if (updateNode) {
          geometries [i].transform = parentTransforms;
          break;
        }
      }
    }
  } else if (node instanceof Light3D) {
    for (var i = 0; i < lights.length; i++) {
      if (lights [i].node === node) {
        lights [i].transform = parentTransforms;
        break;
      }
    }
  }
}

/**
 * Removes the tree with the given root node.  
 * @param {Node3D}  node
 * @param {Array}   geometries
 * @param {Array}   lights
 * @private  
 */
HTMLCanvas3D.prototype.removeDisplayedItems = function(node, geometries, lights) {
  var canvas3D = this;
  if (node instanceof Group3D) {
    var children = node.getChildren();
    for (var i = 0; i < children.length; i++) {
      this.removeDisplayedItems(children [i], geometries, lights);
    }
  } else if (node instanceof Link3D) {
    this.removeDisplayedItems(node.getSharedGroup(), geometries, lights);
  } else if (node instanceof Shape3D) {
    for (var i = 0; i < geometries.length; i++) {
      if (geometries [i].node === node) {
        geometries.splice(i, 1);
        break;
      }
    }
  } else if (node instanceof Light3D) {
    for (var i = 0; i < lights.length; i++) {
      if (lights [i].node === node) {
        lights.splice(i, 1);
        break;
      }
    }
  }
}

/**
 * Returns the WebGL texture that will be bound to the given image. 
 * @param textureImage  a HTML image element
 * @return {WebGLTexture} a texture object
 * @private
 */
HTMLCanvas3D.prototype.prepareTexture = function(textureImage) {
  if (textureImage.url !== undefined) {
    // Search whether texture already exists
    for (var i = 0; i < this.textures.length; i++) {
      if (this.textures [i].image.url == textureImage.url) {
        return this.textures [i];
      }
    }
  }
  // Create texture
  var texture = this.gl.createTexture();
  texture.image = textureImage;
  if (textureImage.width != 0) {
    this.bindTexture(texture);
  } else {
    var canvas3D = this;
    // If texture image isn't loaded yet, add a listener to follow its loading
    var loadListener = function() {
        textureImage.removeEventListener("load", loadListener);
        canvas3D.bindTexture(texture);
        // Redraw scene
        canvas3D.repaint();
      };
    textureImage.addEventListener("load", loadListener);
  }
  this.textures.push(texture);
  return texture;
}

HTMLCanvas3D.resizeTransparentTextures = true;

/**
 * @return {WebGLTexture}
 * @private
 */
HTMLCanvas3D.prototype.bindTexture = function(texture) {
  this.gl.bindTexture(this.gl.TEXTURE_2D, texture);
  this.gl.pixelStorei(this.gl.UNPACK_FLIP_Y_WEBGL, true);
  if ((!Appearance3D.isPowerOfTwo(texture.image.width) || !Appearance3D.isPowerOfTwo(texture.image.height)) 
      && (!texture.image.transparent || HTMLCanvas3D.resizeTransparentTextures)) {
    // From https://www.khronos.org/webgl/wiki/WebGL_and_OpenGL_Differences#Non-Power_of_Two_Texture_Support
    // Scale up the texture image to the next highest power of two dimensions
    var canvas = document.createElement("canvas");
    canvas.width = Appearance3D.getNextHighestPowerOfTwo(texture.image.width);
    canvas.height = Appearance3D.getNextHighestPowerOfTwo(texture.image.height);
    var context = canvas.getContext("2d");
    context.drawImage(texture.image, 0, 0, texture.image.width, texture.image.height, 0, 0, canvas.width, canvas.height);
    canvas.transparent = texture.image.transparent; 
    texture.image = canvas;
  }
  this.gl.texImage2D(this.gl.TEXTURE_2D, 0, this.gl.RGBA, this.gl.RGBA, this.gl.UNSIGNED_BYTE, texture.image);
  this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MAG_FILTER, this.gl.LINEAR);
  if (Appearance3D.isPowerOfTwo(texture.image.width) && Appearance3D.isPowerOfTwo(texture.image.height)) {
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MIN_FILTER, this.gl.LINEAR_MIPMAP_NEAREST);
    this.gl.generateMipmap(this.gl.TEXTURE_2D);
  } else {
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MIN_FILTER, this.gl.LINEAR);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_WRAP_S, this.gl.CLAMP_TO_EDGE);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_WRAP_T, this.gl.CLAMP_TO_EDGE);
  }
  this.gl.bindTexture(this.gl.TEXTURE_2D, null);
  // Free image data
  delete texture.image.src;
}

/**
 * @private
 */
HTMLCanvas3D.prototype.prepareBuffer = function(data, indices) {
  if (indices.length > 0 && data.length > 0) {
    // Create buffer from data without indices
    var buffer = this.gl.createBuffer();
    this.gl.bindBuffer(this.gl.ARRAY_BUFFER, buffer);
    var itemSize = data [0].length;
    var dataArray = new Float32Array(indices.length * itemSize);
    for (var i = 0, index = 0; i < indices.length; i++, index += itemSize) {
      dataArray.set(data [indices [i]], index);
    }
    this.gl.bufferData(this.gl.ARRAY_BUFFER, dataArray, this.gl.STATIC_DRAW);
    return buffer;
  } else {
    return null;
  }
}

/**
 * Draws the prepared scene at screen.
 * @private
 */
HTMLCanvas3D.prototype.drawScene = function() {
  this.gl.viewport(0, 0, this.viewportWidth, this.viewportHeight);
  this.gl.clearColor(0.9, 0.9, 0.9, 1.0);
  this.gl.clear(this.gl.COLOR_BUFFER_BIT | this.gl.DEPTH_BUFFER_BIT);
  
  // Set lights
  var ambientLightColor = vec3.create();
  var directionalLightCount = 0;
  var directionalLightColors = [];
  var lightDirections = [];
  var viewPlatformInvertedTransform = mat4.invert(mat4.create(), this.viewPlatformTransform);
  var transform = mat4.create();
  for (var i = 0; i < this.lights.length; i++) {
    var light = this.lights [i];
    if (light.direction !== undefined) {
      // Adjust direction (if lights should be a fixed place, use an identity transform instead of viewPlatformTransform)
      var lightDirection = vec3.transformMat3(vec3.create(), light.direction, 
          mat3.fromMat4(mat3.create(), mat4.mul(transform, viewPlatformInvertedTransform, light.transform)));
      vec3.normalize(lightDirection, lightDirection);
      vec3.negate(lightDirection, lightDirection);
      directionalLightColors.push.apply(directionalLightColors, light.color);
      lightDirections.push.apply(lightDirections, lightDirection);
      directionalLightCount++;
    } else {
      // Compute total ambient light
      vec3.add(ambientLightColor, ambientLightColor, light.color); 
    }
  }
  this.gl.uniform1i(this.shaderProgram.directionalLightCount, directionalLightCount);
  if (directionalLightCount < HTMLCanvas3D.MAX_DIRECTIONAL_LIGHT) {
    // Complete arrays to HTMLCanvas3D.MAX_DIRECTIONAL_LIGHT
    directionalLightColors.push.apply(directionalLightColors, new Array((HTMLCanvas3D.MAX_DIRECTIONAL_LIGHT - directionalLightCount) * 3));
    lightDirections.push.apply(lightDirections, new Array((HTMLCanvas3D.MAX_DIRECTIONAL_LIGHT - directionalLightCount) * 3));
  }
  this.gl.uniform3fv(this.shaderProgram.directionalLightColors, directionalLightColors);
  this.gl.uniform3fv(this.shaderProgram.lightDirections, lightDirections);
  
  // Convert horizontal field of view to vertical
  var verticalFieldOfView = 2 * Math.atan(this.viewportHeight / this.viewportWidth * Math.tan(this.fieldOfView / 2));
  // First draw background geometries (contained in a unit sphere)
  var projectionTransform = mat4.create();
  mat4.perspective(projectionTransform, verticalFieldOfView, this.viewportWidth / this.viewportHeight, 
                   0.001, 1.0);
  this.gl.uniformMatrix4fv(this.shaderProgram.projectionTransform, false, projectionTransform);
  // Translate to center
  var backgroundTransform = mat4.clone(this.viewPlatformTransform);
  backgroundTransform[12] = 0.
  backgroundTransform[13] = 0;
  backgroundTransform[14] = 0;
  var backgroundInvertedTransform = mat4.invert(mat4.create(), backgroundTransform);
  for (var i = 0; i < this.backgroundGeometries.length; i++) {
    var backgroundGeometry = this.backgroundGeometries [i];
    this.drawGeometry(backgroundGeometry, backgroundInvertedTransform, ambientLightColor, backgroundGeometry.lightingEnabled, true, true);
  }

  // Reset depth buffer to draw the scene above background
  this.gl.clear(this.gl.DEPTH_BUFFER_BIT);
  mat4.perspective(projectionTransform, verticalFieldOfView, this.viewportWidth / this.viewportHeight,  
                   this.frontClipDistance, this.backClipDistance); 
  this.gl.uniformMatrix4fv(this.shaderProgram.projectionTransform, false, projectionTransform);

  // Second draw opaque geometries
  this.gl.enable(this.gl.DEPTH_TEST);
  for (var i = 0; i < this.sceneGeometries.length; i++) {
    var geometry = this.sceneGeometries [i];
    if (geometry.transparency === 1 
        && (geometry.texture === undefined
            || !geometry.texture.image.transparent)) {
      this.drawGeometry(geometry, viewPlatformInvertedTransform, ambientLightColor, geometry.lightingEnabled, true, true);
    }
  }
  // Then draw geometries which use a transparent texture 
  this.gl.enable(this.gl.BLEND);
  this.gl.blendFunc(this.gl.SRC_ALPHA, this.gl.ONE_MINUS_SRC_ALPHA);
  for (var i = 0; i < this.sceneGeometries.length; i++) {
    var geometry = this.sceneGeometries [i];
    if (geometry.texture !== undefined
        && geometry.texture.image.transparent) {
      this.drawGeometry(geometry, viewPlatformInvertedTransform, ambientLightColor, geometry.lightingEnabled, true, true);
    }
  }
  // And other geometries which are transparent 
  for (var i = 0; i < this.sceneGeometries.length; i++) {
    var geometry = this.sceneGeometries [i];
    if (geometry.transparency < 1 
        && (geometry.texture === undefined
            || !geometry.texture.image.transparent)) {
      this.drawGeometry(geometry, viewPlatformInvertedTransform, ambientLightColor, geometry.lightingEnabled, true, true);
    }
  }
  
  // Keep track of the number of frames drawn per second
  var now = Date.now();
  if (Math.floor(now / 1000) > Math.floor(this.lastDrawSceneTime / 1000)) {
    this.framesPerSecond = Math.round(this.drawnFrameCount / (now - this.previousFramesPerSecondTime) * 10000) / 10;
    this.previousFramesPerSecondTime = now;
    this.drawnFrameCount = 0;
  } 
  if (this.drawnFrameCount === undefined) {
    this.previousFramesPerSecondTime = now;
    this.drawnFrameCount = 0;
  }
  this.drawnFrameCount++;
  this.lastDrawSceneTime = now;
}

/**
 * Draws the given shape geometry.
 * @private
 */
HTMLCanvas3D.prototype.drawGeometry = function(geometry, viewPlatformInvertedTransform, ambientLightColor, 
                                               lightingEnabled, textureEnabled, transparencyEnabled) {
  if (geometry.visible
      && (geometry.transparency === undefined 
          || geometry.transparency > 0)) {
    // Call face management methods only if cullFace changed from previous geometry  
    if (geometry.cullFace !== this.shaderCullFace) {
      this.shaderCullFace = geometry.cullFace;
      if (this.shaderCullFace !== undefined) {
        if (this.shaderCullFace === Appearance3D.CULL_NONE) {
          this.gl.disable(this.gl.CULL_FACE);
        } else {
          this.gl.enable(this.gl.CULL_FACE);
          this.gl.cullFace(this.shaderCullFace === Appearance3D.CULL_BACK
              ? this.gl.BACK : this.gl.FRONT);
        }
      } else {
        this.gl.enable(this.gl.CULL_FACE);
        this.gl.cullFace(this.gl.BACK);
      }
    }

    mat4.copy(this.geometryModelViewTransform, viewPlatformInvertedTransform);
    mat4.mul(this.geometryModelViewTransform, this.geometryModelViewTransform, geometry.transform);
    // Call uniformMatrix4fv only if geometryModelViewTransform changed from previous geometry
    if (!mat4.exactEquals(this.shaderModelViewTransform, this.geometryModelViewTransform)) {
      mat4.copy(this.shaderModelViewTransform, this.geometryModelViewTransform);
      this.gl.uniformMatrix4fv(this.shaderProgram.modelViewTransform, false, this.shaderModelViewTransform);
    }

    // Call uniform1i only if lightingEnabled changed from previous geometry
    if (this.shaderLightingEnabled !== lightingEnabled) {
      this.shaderLightingEnabled = lightingEnabled;
      this.gl.uniform1i(this.shaderProgram.lightingEnabled, this.shaderLightingEnabled);
    }
    if (lightingEnabled) {
      if (geometry.ambientColor !== undefined
          && geometry.texture === undefined) {
        vec3.mul(this.geometryAmbientColor, geometry.ambientColor, ambientLightColor);
      } else {
        vec3.set(this.geometryAmbientColor, 0, 0, 0);
      }
      // Call uniform3fv only if geometryAmbientColor changed from previous geometry
      if (!vec3.exactEquals(this.shaderAmbientColor, this.geometryAmbientColor)) {
        vec3.copy(this.shaderAmbientColor, this.geometryAmbientColor);
        this.gl.uniform3fv(this.shaderProgram.ambientColor, this.shaderAmbientColor);
      }

      if (!this.ignoreShininess
          && geometry.specularColor !== undefined 
          && geometry.shininess !== undefined) {
        // Call uniform1f only if shininess changed from previous geometry
        if (this.shaderShininess !== geometry.shininess) {
          this.shaderShininess = geometry.shininess;
          this.gl.uniform1f(this.shaderProgram.shininess, this.shaderShininess);
        }
        vec3.copy(this.geometrySpecularColor, geometry.specularColor);
      } else {
        vec3.set(this.geometrySpecularColor, 0, 0, 0);
      }
      // Call uniform3fv only if geometrySpecularColor changed from previous geometry
      if (!vec3.exactEquals(this.shaderSpecularColor, this.geometrySpecularColor)) {
        vec3.copy(this.shaderSpecularColor, this.geometrySpecularColor);
        this.gl.uniform3fv(this.shaderProgram.vertexSpecularColor, this.shaderSpecularColor);
      }

      mat3.fromMat4(this.geometryNormalTransform, this.geometryModelViewTransform);
      // Call uniformMatrix3fv only if geometryNormalTransform changed from previous geometry
      if (!mat3.exactEquals(this.shaderNormalTransform, this.geometryNormalTransform)) {
        mat3.copy(this.shaderNormalTransform, this.geometryNormalTransform);
        this.gl.uniformMatrix3fv(this.shaderProgram.normalTransform, false, this.shaderNormalTransform);
      }
      // Call uniform1i only if backFaceNormalFlip changed from previous geometry
      if (this.shaderBackFaceNormalFlip !== geometry.backFaceNormalFlip) {
        this.shaderBackFaceNormalFlip = geometry.backFaceNormalFlip;
        this.gl.uniform1i(this.shaderProgram.backFaceNormalFlip, this.shaderBackFaceNormalFlip);
      }
    } 
    
    vec3.set(this.geometryDiffuseColor, 1, 1, 1);
    if (textureEnabled 
        && geometry.texture !== undefined) {
      this.gl.activeTexture(this.gl.TEXTURE0);
      if (geometry.textureCoordinatesGeneration) {
        this.gl.uniform4fv(this.shaderProgram.planeS, geometry.textureCoordinatesGeneration.planeS);
        this.gl.uniform4fv(this.shaderProgram.planeT, geometry.textureCoordinatesGeneration.planeT);
        // Call uniform1i only if textureCoordinatesGenerated changed from previous geometry
        if (!this.shaderTextureCoordinatesGenerated) {
          this.shaderTextureCoordinatesGenerated = true;
          this.gl.uniform1i(this.shaderProgram.textureCoordinatesGenerated, true);
        }
      } else if (geometry.textureCoordinatesBuffer === null) {
        // Default way to generate missing texture coordinates
        this.gl.uniform4fv(this.shaderProgram.planeS, HTMLCanvas3D.VEC4_DEFAULT_PLANE_S);
        this.gl.uniform4fv(this.shaderProgram.planeT, HTMLCanvas3D.VEC4_DEFAULT_PLANE_T);
        // Call uniform1i only if textureCoordinatesGenerated changed from previous geometry
        if (!this.shaderTextureCoordinatesGenerated) {
          this.shaderTextureCoordinatesGenerated = true;
          this.gl.uniform1i(this.shaderProgram.textureCoordinatesGenerated, true);
        }
      } else {
        // Call uniform1i only if textureCoordinatesGenerated changed from previous geometry
        if (this.shaderTextureCoordinatesGenerated) {
          this.shaderTextureCoordinatesGenerated = false;
          this.gl.uniform1i(this.shaderProgram.textureCoordinatesGenerated, false);
        }
      }
      var geometryTextureTransform = geometry.textureTransform !== undefined 
          ? geometry.textureTransform 
          : HTMLCanvas3D.MAT3_IDENTITY;
      // Call uniformMatrix3fv only if geometryTextureTransform changed from previous geometry
      if (!mat3.exactEquals(this.shaderTextureTransform, geometryTextureTransform)) {
        mat3.copy(this.shaderTextureTransform, geometryTextureTransform);
        this.gl.uniformMatrix3fv(this.shaderProgram.textureCoordTransform, false, this.shaderTextureTransform);
      }
      this.gl.bindTexture(this.gl.TEXTURE_2D, geometry.texture);
      // Call uniform1i only if useTextures changed from previous geometry
      if (!this.shaderUseTextures) {
        this.shaderUseTextures = true;
        this.gl.uniform1i(this.shaderProgram.useTextures, true);
      }
    } else {
      // Call uniform1i only if textureCoordinatesGenerated changed from previous geometry
      if (this.shaderTextureCoordinatesGenerated) {
        this.shaderTextureCoordinatesGenerated = false;
        this.gl.uniform1i(this.shaderProgram.textureCoordinatesGenerated, false);
      }
      // Call uniform1i only if useTextures changed from previous geometry
      if (this.shaderUseTextures) {
        this.shaderUseTextures = false;
        this.gl.uniform1i(this.shaderProgram.useTextures, false);
      }
      if (geometry.diffuseColor !== undefined) {
        vec3.copy(this.geometryDiffuseColor, geometry.diffuseColor);
      }
    }
    // Call uniform3fv only if geometryDiffuseColor changed from previous geometry
    if (!vec3.exactEquals(this.shaderDiffuseColor, this.geometryDiffuseColor)) {
      vec3.copy(this.shaderDiffuseColor, this.geometryDiffuseColor);
      this.gl.uniform3fv(this.shaderProgram.vertexDiffuseColor, this.shaderDiffuseColor);
    }
    
    this.gl.enableVertexAttribArray(this.shaderProgram.vertexPositionAttribute);
    this.gl.bindBuffer(this.gl.ARRAY_BUFFER, geometry.vertexBuffer);
    this.gl.vertexAttribPointer(this.shaderProgram.vertexPositionAttribute, 3, this.gl.FLOAT, false, 0, 0);
    if (lightingEnabled 
        && geometry.mode === this.gl.TRIANGLES) {
      this.gl.enableVertexAttribArray(this.shaderProgram.normalAttribute);
      this.gl.bindBuffer(this.gl.ARRAY_BUFFER, geometry.normalBuffer);
      this.gl.vertexAttribPointer(this.shaderProgram.normalAttribute, 3, this.gl.FLOAT, false, 0, 0);
    } else {
      this.gl.disableVertexAttribArray(this.shaderProgram.normalAttribute);
    }
    if (textureEnabled
        && geometry.textureCoordinatesBuffer !== null
        && geometry.texture !== undefined) {
      this.gl.enableVertexAttribArray(this.shaderProgram.textureCoordAttribute);
      this.gl.bindBuffer(this.gl.ARRAY_BUFFER, geometry.textureCoordinatesBuffer);
      this.gl.vertexAttribPointer(this.shaderProgram.textureCoordAttribute, 2, this.gl.FLOAT, false, 0, 0);
    } else {
      this.gl.disableVertexAttribArray(this.shaderProgram.textureCoordAttribute);
    }
  
    // Manage transparency
    var alpha = geometry.transparency && transparencyEnabled  ? geometry.transparency  : 1;
    // Call uniform1f only if alpha changed from previous geometry
    if (this.shaderAlpha !== alpha) {
      this.shaderAlpha = alpha;
      this.gl.uniform1f(this.shaderProgram.alpha, this.shaderAlpha);
    }
    
    this.gl.drawArrays(geometry.mode, 0, geometry.vertexCount);
  }
}

/**
 * Returns the last measured number of frames drawn by second by this component.
 * @return {number}
 */
HTMLCanvas3D.prototype.getFramesPerSecond = function() {
  return this.framesPerSecond;
}

/**
 * Repaints as soon as possible the scene of this canvas.
 */
HTMLCanvas3D.prototype.repaint = function() {
  if (!this.canvasNeededRepaint) {
    this.canvasNeededRepaint = true;
    var canvas3D = this;
    requestAnimationFrame(
        function () {
          if (canvas3D.canvasNeededRepaint) {
            canvas3D.drawScene(); 
            canvas3D.canvasNeededRepaint = false;
            canvas3D.pickingFrameBufferNeededRepaint = true;
          }
        });
  }
}

/**
 * Frees buffers and other resources used by this canvas.
 */
HTMLCanvas3D.prototype.clear = function() {
  for (var i = 0; i < this.textures.length; i++) {
    delete this.textures [i].src;
    this.gl.deleteTexture(this.textures [i]);
  }
  this.textures = [];
  
  this.clearGeometries(this.sceneGeometries);
  this.clearGeometries(this.backgroundGeometries);
  
  this.lights = [];
  if (this.pickingFrameBuffer !== undefined) {
    this.gl.deleteFramebuffer(this.pickingFrameBuffer);
    delete this.pickingFrameBuffer;
  }
  this.repaint();
}

/**
 * Frees buffers used by the given geometries.
 * @private
 */
HTMLCanvas3D.prototype.clearGeometries = function(geometries) {
  for (var i = 0; i < geometries.length; i++) {
    var geometry = geometries [i];
    this.gl.deleteBuffer(geometry.vertexBuffer);
    if (geometry.textureCoordinatesBuffer !== null
        && geometry.texture !== undefined) {
      this.gl.deleteBuffer(geometry.textureCoordinatesBuffer);
    }
    if (geometry.normalBuffer !== undefined) {
      this.gl.deleteBuffer(geometry.normalBuffer);
    }
  }
  geometries.length = 0;
}
  
/**
 * Sets whether shininess should be taken into account by the shader or not.
 */
HTMLCanvas3D.prototype.setIgnoreShininess = function(ignoreShininess) {
  this.ignoreShininess = ignoreShininess;
  this.repaint();
}

/**
 * Returns the closest shape displayed at client coordinates (x, y) among the displayed objects. 
 * @param {number} x
 * @param {number} y
 * @returns {Node3D}
 */
HTMLCanvas3D.prototype.getClosestShapeAt = function(x, y) {
  // Inspired from http://coffeesmudge.blogspot.fr/2013/08/implementing-picking-in-webgl.html
  if (this.pickingFrameBuffer === undefined) {
    this.pickingFrameBuffer = this.gl.createFramebuffer();
    this.gl.bindFramebuffer(this.gl.FRAMEBUFFER, this.pickingFrameBuffer);
    this.pickingFrameBuffer.width = Appearance3D.isPowerOfTwo(this.canvas.width) 
        ? this.canvas.width 
        : Appearance3D.getNextHighestPowerOfTwo(this.canvas.width) / 2;
    this.pickingFrameBuffer.height = Appearance3D.isPowerOfTwo(this.canvas.height) 
        ? this.canvas.height 
        : Appearance3D.getNextHighestPowerOfTwo(this.canvas.height) / 2;
    this.pickingFrameBuffer.colorMap = new Uint8Array(this.pickingFrameBuffer.width * this.pickingFrameBuffer.height * 4);

    var renderedTexture = this.gl.createTexture();
    this.gl.bindTexture(this.gl.TEXTURE_2D, renderedTexture);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MAG_FILTER, this.gl.NEAREST);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MIN_FILTER, this.gl.NEAREST);
    this.gl.texImage2D(this.gl.TEXTURE_2D, 0, this.gl.RGBA, this.pickingFrameBuffer.width, this.pickingFrameBuffer.height, 
        0, this.gl.RGBA, this.gl.UNSIGNED_BYTE, null);
    var renderBuffer = this.gl.createRenderbuffer();
    this.gl.bindRenderbuffer(this.gl.RENDERBUFFER, renderBuffer);
    this.gl.framebufferTexture2D(this.gl.FRAMEBUFFER, this.gl.COLOR_ATTACHMENT0, this.gl.TEXTURE_2D, renderedTexture, 0);
    this.gl.renderbufferStorage(this.gl.RENDERBUFFER, this.gl.DEPTH_COMPONENT16, this.pickingFrameBuffer.width, this.pickingFrameBuffer.height);
    this.gl.framebufferRenderbuffer(this.gl.FRAMEBUFFER, this.gl.DEPTH_ATTACHMENT, this.gl.RENDERBUFFER, renderBuffer);
  }

  if (this.pickingFrameBufferNeededRepaint) {
    this.gl.bindFramebuffer(this.gl.FRAMEBUFFER, this.pickingFrameBuffer);
    this.gl.viewport(0, 0, this.pickingFrameBuffer.width, this.pickingFrameBuffer.height);
    this.gl.clearColor(1., 1., 1., 1.);
    this.gl.clear(this.gl.COLOR_BUFFER_BIT | this.gl.DEPTH_BUFFER_BIT);
    
    // Convert horizontal field of view to vertical
    var projectionTransform = mat4.create();
    var verticalFieldOfView = 2 * Math.atan(this.canvas.height / this.canvas.width * Math.tan(this.fieldOfView / 2));
    mat4.perspective(projectionTransform, verticalFieldOfView, this.canvas.width / this.canvas.height,  
        this.frontClipDistance, this.backClipDistance); 
    this.gl.uniformMatrix4fv(this.shaderProgram.projectionTransform, false, projectionTransform);
    
    // Draw not background and opaque geometries without light and textures
    this.gl.enable(this.gl.DEPTH_TEST);
    var viewPlatformInvertedTransform = mat4.invert(mat4.create(), this.viewPlatformTransform);
    var geometryColor = vec3.create();
    for (var i = 0; i < this.sceneGeometries.length; i++) {
      var geometry = this.sceneGeometries [i];
      if (geometry.node.isPickable()) {
        var defaultColor = geometry.diffuseColor;
        // Change diffuse color by geometry index
        vec3.set(geometryColor, 
            ((i >>> 16) & 0xFF) / 255.,
            ((i >>> 8) & 0xFF) / 255.,
            (i & 0xFF) / 255.);
        geometry.diffuseColor = geometryColor;
        this.drawGeometry(geometry, viewPlatformInvertedTransform, null, false, false, false);
        if (defaultColor !== undefined) {
          geometry.diffuseColor = defaultColor;
        }
      }
    }
    
    this.gl.readPixels(0, 0, this.pickingFrameBuffer.width, this.pickingFrameBuffer.height, this.gl.RGBA, this.gl.UNSIGNED_BYTE,
        this.pickingFrameBuffer.colorMap);
    this.gl.bindFramebuffer(this.gl.FRAMEBUFFER, null);
    this.pickingFrameBufferNeededRepaint = false;
  }
  
  var canvasBounds = this.canvas.getBoundingClientRect();
  if (x >= canvasBounds.left && y >= canvasBounds.top && x < canvasBounds.right && y < canvasBounds.bottom) {
    x -= canvasBounds.left;
    y -= canvasBounds.top;
    // Find pixel index in the color map taking into the ratio between the size of the canvas at screen and the poser of two of the texture attached to the frame buffer
    var pixelIndex = (this.pickingFrameBuffer.height - 1 - Math.floor(y / canvasBounds.height * this.pickingFrameBuffer.height)) * this.pickingFrameBuffer.width 
        + Math.floor(x / canvasBounds.width * this.pickingFrameBuffer.width);
    pixelIndex *= 4;
    var geometryIndex = 
        this.pickingFrameBuffer.colorMap[pixelIndex] * 65536
      + this.pickingFrameBuffer.colorMap[pixelIndex + 1] * 256
      + this.pickingFrameBuffer.colorMap[pixelIndex + 2];
    if (geometryIndex != 0xFFFFFF) {
      return this.sceneGeometries [geometryIndex].node;
    }
  }
  
  return null;
} 
