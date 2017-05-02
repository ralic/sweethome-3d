/*
 * URLContent.js
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA02111-1307USA
 */

/**
 * Content wrapper for strings used as URLs.
 * @param {string} url  the URL from which this content will be read
 * @constructor
 * @author Emmanuel Puybaret
 */
function URLContent(url) {
  this.url = url;
}

URLContent["__interfaces"] = ["com.eteks.sweethome3d.model.Content"];

/**
 * Returns the URL of this content.
 * @return {string}
 */
URLContent.prototype.getURL = function() {
  return this.url;
}

/**
 * Returns <code>true</code> if the URL stored by this content 
 * references an entry in a JAR.
 * @return {boolean}
 */
URLContent.prototype.isJAREntry = function() {
  return this.url.indexOf("jar:") === 0 && this.url.indexOf('!') !== -1; 
}

/**
 * Returns the URL base of a JAR entry.
 * @return {string}
 */
URLContent.prototype.getJAREntryURL = function() {
  if (!this.isJAREntry()) {
    throw new IllegalStateException("Content isn't a JAR entry");
  }
  return this.url.substring("jar:".length, this.url.indexOf('!'));
}

/**
 * Returns the name of a JAR entry. 
 * If the JAR entry in the URL given at creation time was encoded in application/x-www-form-urlencoded format,
 * this method will return it unchanged and not decoded.
 * @return {string}
 * @throws IllegalStateException if the URL of this content 
 *                    doesn't reference an entry in a JAR URL.
 */
URLContent.prototype.getJAREntryName = function() {
  if (!this.isJAREntry()) {
    throw new IllegalStateException("Content isn't a JAR entry");
  }
  return this.url.substring(this.url.indexOf('!') + 2);
}

/**
 * Returns <code>true</code> if the object in parameter is an URL content
 * that references the same URL as this object.
 * @return {boolean}
 */
URLContent.prototype.equals = function(obj) {
  if (obj === this) {
    return true;
  } else if (obj instanceof URLContent) {
    var urlContent = obj;
    return urlContent.url == this.url;
  } else {
    return false;
  }
}

/**
 * An URL content read from a home stream.
 * @param {string} url  the URL from which this content will be read
 * @constructor
 * @author Emmanuel Puybaret
 */
function HomeURLContent(url) {
  URLContent.call(this, url);
}
HomeURLContent.prototype = Object.create(URLContent.prototype);
HomeURLContent.prototype.constructor = HomeURLContent;

HomeURLContent["__interfaces"] = ["com.eteks.sweethome3d.model.Content"];

/**
 * ZIP reading utilities.
 * @class
 * @author Emmanuel Puybaret
 */
var ZIPTools = {};

ZIPTools.READING = "Reading";

ZIPTools.openedZips = {};
ZIPTools.runningRequests = [];

/**
 * Reads the ZIP data in the given URL.
 * @param url the URL of a zip file containing an OBJ entry that will be loaded
 *            or an URL noted as jar:url!/objEntry where objEntry will be loaded.
 * @param synchronous optional parameter equal to false by default
 * @param {{zipReady, zipError, progression}} zipObserver An observer containing zipReady(zip), 
 *            zipError(error), progression(part, info, percentage) methods that
 *            will called at various phases.
 */
ZIPTools.getZIP = function(url, synchronous, zipObserver) {
  if (zipObserver === undefined) {
    zipObserver = synchronous;
    synchronous = false;
  }
  if (url in ZIPTools.openedZips) {
    zipObserver.zipReady(ZIPTools.openedZips [url]); 
  } else {
    try {
      var request = new XMLHttpRequest();
      request.open('GET', url, !synchronous);
      request.responseType = "arraybuffer";
      request.overrideMimeType("application/octet-stream");
      request.addEventListener("readystatechange", 
          function(ev) {
            if (request.readyState === XMLHttpRequest.DONE) {
              if ((request.status === 200 || request.status === 0)
                  && request.response != null) {
                try {
                  ZIPTools.runningRequests.splice(ZIPTools.runningRequests.indexOf(request), 1);
                  var zip = new JSZip(request.response);
                  ZIPTools.openedZips [url] = zip;
                  zipObserver.zipReady(ZIPTools.openedZips [url]); 
                } catch (ex) {
                  zipObserver.zipError(ex);
                }
              } else {
                // Report error for requests that weren't aborted
                var index = ZIPTools.runningRequests.indexOf(request);              
                if (index >= 0) {
                  ZIPTools.runningRequests.splice(index, 1);                
                  zipObserver.zipError(new Error(request.status + " while requesting " + url)); 
                }
              }
            }
          });
      request.addEventListener("progress", 
          function(ev) {
            if (ev.lengthComputable
                && zipObserver.progression !== undefined) {
              zipObserver.progression(ZIPTools.READING, url, ev.loaded / ev.total);
            }
          });
      request.send();
      ZIPTools.runningRequests.push(request);
    } catch (ex) {
      zipObserver.zipError(ex);
    }
  }
}


/**
 * Clears cache and aborts running requests.
 */
ZIPTools.clear = function() {
  ZIPTools.openedZips = {};
  // Abort running requests
  while (ZIPTools.runningRequests.length > 0) {
    var request = ZIPTools.runningRequests [ZIPTools.runningRequests.length - 1];
    ZIPTools.runningRequests.splice(ZIPTools.runningRequests.length - 1, 1);
    request.abort();
  }
}

/**
 * Removes from cache the content matching the given <code>url</code>. 
 */
ZIPTools.disposeZIP = function(url) {
  delete ZIPTools.openedZips [url];
}

/**
 * Returns true if the given image data describes a transparent PNG file.
 * @package
 * @ignore
 */
ZIPTools.isTranparentImage = function(imageData) {
  return imageData.charAt(0).charCodeAt(0) === 0x89 
  && imageData.charAt(1).charCodeAt(0) === 0x50 
  && imageData.charAt(2).charCodeAt(0) === 0x4e 
  && imageData.charAt(3).charCodeAt(0) === 0x47 
  && imageData.charAt(4).charCodeAt(0) === 0x0d 
  && imageData.charAt(5).charCodeAt(0) === 0x0a 
  && imageData.charAt(6).charCodeAt(0) === 0x1a 
  && imageData.charAt(7).charCodeAt(0) === 0x0a
  && (imageData.charAt(25).charCodeAt(0) === 4
      || imageData.charAt(25).charCodeAt(0) === 6
      || (imageData.indexOf("PLTE") !== -1 && imageData.indexOf("tRNS") !== -1));
}

/**
 * Returns the folder where a given Javascript .js file was read from.  
 * @package
 * @ignore
 */
ZIPTools.getScriptFolder = function(script) {
  var baseUrl = "http://www.sweethome3d.com/libjs/"; 
  // Search the base URL of this script
  var scripts = document.getElementsByTagName("script");      
  for (var i = 0; i < scripts.length; i++) {
    if (scripts[i].src.indexOf(script) !== -1) {
      baseUrl = scripts[i].src.substring(0, scripts[i].src.lastIndexOf("/") + 1);
      break;
    }
  }
  return baseUrl;
}
