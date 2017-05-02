/*
 * core.js
 *
 * Copyright (c) 2015 Emmanuel PUYBARET / eTeks <info@eteks.com>
 * 
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied OpenJDK 8 source code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// Various classes of OpenJDK 8 translated to Javascript

/**
 * Creates an IllegalArgumentException instance.
 * Adapted from java.lang.IllegalArgumentException
 * @constructor
 */
function IllegalArgumentException(message) {
  this.message = message;
}


/**
 * Creates an NullPointerException instance.
 * Adapted from java.lang.NullPointerException
 * @constructor
 */
function NullPointerException(message) {
  this.message = message;
}


/**
 * Creates an IllegalStateException instance.
 * Adapted from java.lang.IllegalStateException
 * @constructor
 */
function IllegalStateException(message) {
  this.message = message;
}


/**
 * Creates an UnsupportedOperationException instance.
 * Adapted from java.lang.UnsupportedOperationException
 * @constructor
 */
function UnsupportedOperationException(message) {
  this.message = message;
}


/**
 * Creates an InternalError instance.
 * Adapted from java.lang.InternalError
 * @constructor
 * @ignore
 */
function InternalError(message) {
  this.message = message;
}


/**
 * Creates an NoSuchElementException instance.
 * Adapted from java.util.NoSuchElementException
 * @constructor
 */
function NoSuchElementException(message) {
  this.message = message;
}


/**
 * System class.
 * @class
 * @ignore
 */
var System = {}

System.arraycopy = function(srcPts, srcOff, dstPts, dstOff, size) {
  if (srcPts !== dstPts
      || dstOff >= srcOff + size) {
    while (--size >= 0) {
      dstPts[dstOff++] = srcPts[srcOff++];
    }
  } else {
    // In case copied items overlap  
    var tmp = srcPts.slice(srcOff, srcOff + size);
    for (var i = 0; i < size; i++) {
      dstPts[dstOff++] = tmp[i];
    }
  }
}


/**
 * Creates a PropertyChangeEvent instance.
 * Adapted from java.beans.PropertyChangeEvent
 * @constructor
 */
function PropertyChangeEvent(source, propertyName, oldValue, newValue) {
  this.source = source;
  this.propertyName = propertyName;
  this.newValue = newValue;
  this.oldValue = oldValue;
}

/**
 * Returns the source of this event.
 * @return {Object}
 */
PropertyChangeEvent.prototype.getSource = function() {
  return this.source;
}

/**
 * Returns the name of the modified property.
 * @return {string}
 */
PropertyChangeEvent.prototype.getPropertyName = function() {
  return this.propertyName;
}
  
/**
 * Returns the new value of the property.
 */
PropertyChangeEvent.prototype.getNewValue = function() {
  return this.newValue;
}

/**
 * Returns the old value of the property.
 */
PropertyChangeEvent.prototype.getOldValue = function() {
  return this.oldValue;
}


/**
 * Creates a PropertyChangeSupport instance.
 * Adapted from java.beans.PropertyChangeSupport
 * @constructor
 */
function PropertyChangeSupport(source) {
  this.source = source;
  this.listeners = [];
}

/**
 * Adds the <code>listener</code> in parameter to the list of listeners that may be notified.
 * @param {string} [propertyName] the name of an optional property to listen
 * @param listener  a callback that will be called with a {@link PropertyChangeEvent} instance
 */
PropertyChangeSupport.prototype.addPropertyChangeListener = function(propertyName, listener) {
  if (listener === undefined) {
    // One parameter
    listener = propertyName;
    propertyName = null;
  }
  if (listener) {
    if (propertyName) {
      this.listeners.push({"listener":listener, "propertyName":propertyName});
    } else {
      this.listeners.push({"listener":listener});
    }
  }
}

/**
 * Removes the <code>listener</code> in parameter to the list of listeners that may be notified.
 * @param listener the listener to remove. If it doesn't exist, it's simply ignored.
 */
PropertyChangeSupport.prototype.removePropertyChangeListener = function(propertyName, listener) {
  if (listener === undefined) {
    // One parameter
    listener = propertyName;
    propertyName = undefined;
  }
  if (listener) {
    for (var i = this.listeners.length - 1; i >= 0; i--) {
      if (this.listeners [i].propertyName == propertyName
          && this.listeners [i].listener === listener) {
        this.listeners.splice(i, 1);
      }
    }
  }
}

/**
 * Returns an array of all the listeners that were added to the
 * PropertyChangeSupport object with addPropertyChangeListener().
 * @param {string} [propertyName] the name of an optional property to listen
 * @return [Array]
 */
PropertyChangeSupport.prototype.getPropertyChangeListeners = function(propertyName) {
  var listeners = [];
  for (var i = this.listeners.length - 1; i >= 0; i--) {
    if (this.listeners [i].propertyName == propertyName
        && this.listeners [i].listener === listener) {
      listeners.push(this.listeners [i]);
    }
  }
  return listeners;
}

/**
 * Fires a property change event.
 * @param propertyName {string} the name of the modified property
 * @param oldValue old value
 * @param newValue new value
 */
PropertyChangeSupport.prototype.firePropertyChange = function(propertyName, oldValue, newValue) {
  if (oldValue != newValue) {
    var ev = new PropertyChangeEvent(this.source, propertyName, oldValue, newValue);
    for (var i = 0; i < this.listeners.length; i++) {
      if (!("propertyName" in this.listeners [i])
          || this.listeners [i].propertyName == propertyName) {
        if (typeof(this.listeners [i].listener) === "function") {
          this.listeners [i].listener(ev);
        } else {
          this.listeners [i].listener.propertyChange(ev);
        }
      }
    }
  }
}
