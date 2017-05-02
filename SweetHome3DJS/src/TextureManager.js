/*
 * TextureManager.js
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

/**
 * Singleton managing texture image cache.
 * @constructor
 * @author Emmanuel Puybaret
 */
function TextureManager() {
  this.loadedTextureImages = {};
  this.loadingTextureObservers = {};
}

TextureManager.READING_TEXTURE = "Reading texture";

// Singleton
TextureManager.instance = null;

/**
 * Returns an instance of this singleton.
 * @return {TextureManager} 
 */
TextureManager.getInstance = function() {
  if (TextureManager.instance == null) {
    TextureManager.instance = new TextureManager();
  }
  return TextureManager.instance;
}

/**
 * Clears loaded texture images cache. 
 */
TextureManager.prototype.clear = function() {
  this.loadedTextureImages = {};
  this.loadingTextureObservers = {};
}


/**
 * Reads a texture image from <code>content</code> notified to <code>textureObserver</code>. 
 * If the texture isn't loaded in cache yet and <code>synchronous</code> is false, a one pixel 
 * white image texture will be notified immediately to the given <code>textureObserver</code>, 
 * then a second notification will be given in Event Dispatch Thread once the image texture is loaded. 
 * If the texture is in cache, it will be notified immediately to the given <code>textureObserver</code>.
 * @param {URLContent} content  an object containing an image
 * @param {number}  [angle]       the rotation angle applied to the image
 * @param {boolean} [synchronous] if <code>true</code>, this method will return only once image content is loaded.
 * @param textureObserver the observer that will be notified once the texture is available
 */
TextureManager.prototype.loadTexture = function(content, angle, synchronous, textureObserver) {
  if (synchronous === undefined) {
    // 2 parameters (content, textureObserver)
    textureObserver = angle;
    angle = 0;
    synchronous = false;
  } else if (textureObserver === undefined) {
    // 3 parameters (content, synchronous, textureObserver)
    textureObserver = synchronous;
    synchronous = false;
  }
  var contentUrl = content.getURL();
  if (contentUrl in this.loadedTextureImages) {
    textureObserver.textureUpdated(this.loadedTextureImages [contentUrl]);
  } else {
    if (contentUrl in this.loadingTextureObservers) {
      // If observers list exists, content texture is already being loaded
      // register observer for future notification
      this.loadingTextureObservers [contentUrl].push(textureObserver);
    } else {
      // Create a list of observers that will be notified once texture model is loaded
      var observers = [];
      observers.push(textureObserver);
      this.loadingTextureObservers [contentUrl] = observers;
      var textureManager = this;
      this.load(contentUrl, synchronous,
          {
            textureLoaded : function(textureImage) {
              var observers = textureManager.loadingTextureObservers [contentUrl];
              if (observers) {
                delete textureManager.loadingTextureObservers [contentUrl];
                // Note that angle is managed with appearance#setTextureTransform
                textureManager.loadedTextureImages [contentUrl] = textureImage;
                for (var i = 0; i < observers.length; i++) {
                  observers [i].textureUpdated(textureImage);
                }
              }
            },
            textureError : function(err) {
              var observers = textureManager.loadingTextureObservers [contentUrl];
              if (observers) {
                delete textureManager.loadingTextureObservers [contentUrl];
                for (var i = 0; i < observers.length; i++) {
                  observers [i].textureError(err);
                }
              }
            },
            progression : function(part, info, percentage) {
              var observers = textureManager.loadingTextureObservers [contentUrl];
              if (observers) {
                for (var i = 0; i < observers.length; i++) {
                  observers [i].progression(part, info, percentage);
                } 
              }
            }
          });
    }
  }
}

/**
 * @private
 */
TextureManager.prototype.load = function(url, synchronous, textureObserver) {
  textureObserver.progression(TextureManager.READING_TEXTURE, url, 0);
  var textureImage = new Image();
  textureImage.url = url;
  var imageErrorListener = function(ev) {
      textureImage.removeEventListener("load", imageLoadingListener);
      textureImage.removeEventListener("error", imageErrorListener);
      textureObserver.textureError("Can't load " + url);
    };
  var imageLoadingListener = function() {
      textureImage.removeEventListener("load", imageLoadingListener);
      textureImage.removeEventListener("error", imageErrorListener);
      textureObserver.progression(TextureManager.READING_TEXTURE, url, 1);
      textureObserver.textureLoaded(textureImage);
    };
  if (url.indexOf("jar:") === 0) {
    var entrySeparatorIndex = url.indexOf("!/");
    var imageEntryName = decodeURIComponent(url.substring(entrySeparatorIndex + 2));
    var jarUrl = url.substring(4, entrySeparatorIndex);
    ZIPTools.getZIP(jarUrl, synchronous,
        {
          zipReady : function(zip) {
            try {
              textureImage.addEventListener("load", imageLoadingListener);
              textureImage.addEventListener("error", imageErrorListener);
              var imageEntry = zip.file(imageEntryName);
              var imageData = imageEntry.asBinary();
              var base64Image = btoa(imageData);
              // Detect quickly if the image is a PNG using transparency
              textureImage.transparent = ZIPTools.isTranparentImage(imageData);
              textureImage.src = "data:image;base64," + base64Image;
              if (textureImage.width !== 0) {
                // Image is already here
                imageLoadingListener();
              }
            } catch (ex) {
              this.zipError(ex);
            }
          },
          zipError : function(error) {
            if (textureObserver.textureError !== undefined) {
              textureObserver.textureError("Can't load " + jarUrl); 
            }
          },
          progression : function(part, info, percentage) {
            if (textureObserver.progression !== undefined) {
              textureObserver.progression(part, info, percentage);
            }
          }
        });
  } else {
    textureImage.addEventListener("load", imageLoadingListener);
    textureImage.addEventListener("error", imageErrorListener);
    // Prepare download
    textureImage.src = url;
    if (textureImage.width !== 0) {
      // Image is already here
      textureImage.removeEventListener("load", imageLoadingListener);
      textureImage.removeEventListener("error", imageErrorListener);
      textureObserver.progression(TextureManager.READING_TEXTURE, url, 1);
      textureObserver.textureLoaded(textureImage);
    } 
  }
}

/**
 * Returns <code>true</code> if the texture is shared and its image contains 
 * at least one transparent pixel.
 * @return {boolean}
 */
TextureManager.prototype.isTextureTransparent = function(textureImage) {
  return textureImage.transparent === true;
}

/**
 * Returns the width of the given texture once its rotation angle is applied.
 * @return {number}
 */
TextureManager.prototype.getRotatedTextureWidth = function(texture) {
  var angle = texture.getAngle();
  if (angle !== 0) {
    return Math.round(Math.abs(texture.getWidth() * Math.cos(angle)) 
        + Math.abs(texture.getHeight() * Math.sin(angle)));
  } else {
    return texture.getWidth();
  }
}

/**
 * Returns the height of the given texture once its rotation angle is applied.
 * @return {number}
 */
TextureManager.prototype.getRotatedTextureHeight = function(texture) {
  var angle = texture.getAngle();
  if (angle !== 0) {
    return Math.round(Math.abs(texture.getWidth() * Math.sin(angle)) 
        + Math.abs(texture.getHeight() * Math.cos(angle)));
  } else {
    return texture.getHeight();
  }
}

/**
 * Returns an image for error purpose.
 * @package
 * @ignore
 */
TextureManager.prototype.getErrorImage = function() {
  if (TextureManager.errorImage === undefined) {
    // Create on the fly a red image of 2x2 pixels
    var canvas = document.createElement('canvas');
    canvas.width = 2;
    canvas.height = 2;
    var context = canvas.getContext('2d');
    context.fillStyle = "#FF0000";
    context.fillRect(0, 0, 2, 2);
    var errorImageUrl = canvas.toDataURL();
    var errorImage = new Image();
    errorImage.url = errorImageUrl;
    errorImage.src = errorImageUrl;
    TextureManager.errorImage = errorImage;
  }
  return TextureManager.errorImage;
}
