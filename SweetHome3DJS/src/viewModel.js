/*
 * viewModel.js
 *
 * Sweet Home 3D, Copyright (c) 2016 Emmanuel PUYBARET / eTeks <info@eteks.com>
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

/**
 * Loads the model at given URL and displays it in a 3D canvas.
 * @param {string} modelUrl the URL of the model to load
 * @param {string} modelRotation the 9 values of a 3x3 matrix 
 */
function viewModelInOverlay(modelUrl, modelRotation) {
    // Place canvas in the middle of the screen
    var windowWidth  = window.innerWidth;
    var windowHeight = window.innerHeight;
    var pageWidth = document.documentElement.clientWidth;
    var pageHeight = document.documentElement.clientHeight;
    var bodyElement = document.getElementsByTagName("body").item(0);
    if (bodyElement && bodyElement.scrollWidth) {
      if (bodyElement.scrollWidth > pageWidth) {
        pageWidth = bodyElement.scrollWidth;
      }
      if (bodyElement.scrollHeight > pageHeight) {
        pageHeight = bodyElement.scrollHeight;
      }
    }
    var pageXOffset = window.pageXOffset ? window.pageXOffset : 0;
    var pageYOffset = window.pageYOffset ? window.pageYOffset : 0;

    var canvas = document.getElementById("canvas3D");
    if (!canvas) {
      createModel3DOverlay();
      canvas = document.getElementById("canvas3D");
    }

    var overlayDiv = document.getElementById("modelViewerOverlay");
    overlayDiv.style.height = Math.max(pageHeight, windowHeight) + "px";
    overlayDiv.style.width = pageWidth <= windowWidth
        ? "100%"
        : pageWidth + "px";
    overlayDiv.style.display = "block";

    var canvasSize = Math.min(800, windowWidth, windowHeight);
    canvasSize *= 0.90;
    canvas.width = canvasSize;
    canvas.style.width = canvas.width + "px";
    canvas.height = canvasSize;
    canvas.style.height = canvas.height + "px";
    var canvasLeft = pageXOffset + (windowWidth - canvasSize - 10) / 2;
    canvas.style.left = canvasLeft + "px";
    var canvasTop = pageYOffset + (windowHeight - canvasSize - 10) / 2;
    canvas.style.top = canvasTop + "px";
        
    var closeButtonImage = document.getElementById("modelViewerCloseButton");
    closeButtonImage.style.left = (canvasLeft + canvasSize - 5) + "px";
    closeButtonImage.style.top = (canvasTop - 10) + "px";
    
    var progressDiv = document.getElementById("modelViewerProgressDiv");
    progressDiv.style.left = (canvasLeft + (canvasSize - 300) / 2) + "px";
    progressDiv.style.top = (canvasTop + (canvasSize - 50) / 2) + "px";
    progressDiv.style.visibility = "visible";
    document.getElementById("modelViewerProgress").value = 0;
      
    // Show model in canvas
    try {
      if (canvas.modelPreviewComponent === undefined) {
        canvas.modelPreviewComponent = new ModelPreviewComponent("canvas3D", true);
      }
      var modelPreviewComponent = canvas.modelPreviewComponent;
      var modelRotationMatrix = undefined;
      if (modelRotation) {
        var values = modelRotation.split(/\s+/);
        modelRotationMatrix = [[parseFloat(values [0]), parseFloat(values [1]), parseFloat(values [2])], 
                               [parseFloat(values [3]), parseFloat(values [4]), parseFloat(values [5])], 
                               [parseFloat(values [6]), parseFloat(values [7]), parseFloat(values [8])]];
      }
      modelPreviewComponent.setModel(new URLContent(modelUrl), modelRotationMatrix,
          function(err) {
            console.log(err); 
            alert(err); 
          },
          function(part, info, percentage) {
            var progress = document.getElementById("modelViewerProgress");
            if (part === Node3D.READING_MODEL) {
              progress.value = percentage * 100;
              info = info.substring(info.lastIndexOf('/') + 1);
            } else if (part === Node3D.PARSING_MODEL) {
              progress.value = 210 + percentage * 100;
            } else if (part === Node3D.BUILDING_MODEL) {
              progress.value = 310 + percentage * 100;
            } else if (part === Node3D.BINDING_MODEL) {
              progress.value = 410 + percentage * 50;
            }
            
            var progressLabel = document.getElementById("modelViewerProgressLabel");
            if (part === Node3D.BUILDING_MODEL && percentage === 1) {
              progressLabel.innerHTML = "Preparing display...";
            } else {
              progressLabel.innerHTML = (percentage ? Math.floor(percentage * 100) + "% " : "") 
                  + part + " " + info;
            }
            
            if (part === Node3D.BINDING_MODEL && percentage === 1) {
              document.getElementById("modelViewerProgressDiv").style.visibility = "hidden";
              modelPreviewComponent.startRotationAnimation();
            }
          });
    } catch (ex) {
      hideModel3DOverlay();
      if (ex == "No WebGL") {
        alert("Sorry, your browser doesn't support WebGL.");
      } else {
        alert("Error: " + ex);
      }
    }
}

/**
 * Creates an overlay containing a canvas with <code>modelViewerOverlay</code> as id.
 * @private
 */
function createModel3DOverlay() {
  var overlayDiv = document.createElement("div");
  overlayDiv.setAttribute("id", "modelViewerOverlay");
  overlayDiv.style.display = "none";
  overlayDiv.style.position = "absolute";
  overlayDiv.style.left = "0";
  overlayDiv.style.top = "0";
  overlayDiv.style.zIndex = "100";
  overlayDiv.style.background = "rgba(127, 127, 127, .5)";
    
  var bodyElement = document.getElementsByTagName("body").item(0);
  bodyElement.insertBefore(overlayDiv, bodyElement.firstChild);

  var modelViewDiv = document.createElement("div");
  modelViewDiv.innerHTML =
        '<canvas id="canvas3D" style="background-color: #CCCCCC; border: 1px solid gray; position: absolute; touch-action: none"></canvas>'
      + '<div id="modelViewerProgressDiv" style="position:absolute; width: 300px;">'
      + '  <progress id="modelViewerProgress" value="0" max="460" style="width: 300px;"></progress>'
      + '  <label id="modelViewerProgressLabel" style="margin-top: 2px; font-family: Sans-serif; margin-left: 10px; margin-right: 0px; display: block;"></label>'
      + '</div>';  
  
  // Create close button image
  var closeButtonImage = new Image();
  closeButtonImage.src = ZIPTools.getScriptFolder("jszip.min.js") + "/close.png";
  closeButtonImage.id = "modelViewerCloseButton";
  closeButtonImage.style.position = "absolute";

  overlayDiv.appendChild(modelViewDiv);  
  overlayDiv.appendChild(closeButtonImage);
 
  var hide = function(ev) {
      hideModel3DOverlay("canvas3D");
    }; 
  window.addEventListener("keydown", 
      function(ev) {
        if (ev.keyCode === 27) {
          hide();
        }
      });
  closeButtonImage.addEventListener("click", hide);
  var mouseActionsListener = {
      mousePressed : function(ev) {
        mouseActionsListener.mousePressedInCanvas = true;
      },
      mouseClicked : function(ev) {
        if (mouseActionsListener.mousePressedInCanvas) {
          delete mouseActionsListener.mousePressedInCanvas;
          hide();
        }
      }
    }
  overlayDiv.addEventListener("mousedown", mouseActionsListener.mousePressed); 
  overlayDiv.addEventListener("click", mouseActionsListener.mouseClicked); 
  overlayDiv.addEventListener("touchmove", 
      function(ev) {
        ev.preventDefault();
      });
}

/**
 * Hides the overlay and clears resources.
 * @private
 */
function hideModel3DOverlay() {
  document.getElementById("modelViewerOverlay").style.display = "none";
  var modelPreviewComponent = document.getElementById("canvas3D").modelPreviewComponent;
  if (modelPreviewComponent) {
    modelPreviewComponent.clear();
    ModelManager.getInstance().clear();
    ZIPTools.clear();
  }
}

/**
 * Replaces the href attribute of the element matching the given regular expression
 * by a call to <code>viewModel3D</code> with the link in parameter.
 * @param linkRegex
 * @ignore
 */
function bindAnchorsToModel3DViewer(linkRegex) {
  var anchors = document.getElementsByTagName("a");
  for (var i = 0; i < anchors.length; i++) {
    var anchor = anchors[i];
    var url = anchor.getAttribute("href");
    if (url !== null && url.match(linkRegex)) {
      anchor.onclick = function () {
          viewModelInOverlay(this.getAttribute("href"), this.getAttribute("data-model-rotation")); 
          return false;
        };
    }
  }
}

