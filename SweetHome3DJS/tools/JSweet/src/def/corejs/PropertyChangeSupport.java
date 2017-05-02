/*
 * PropertyChangeSupport.java 
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
package def.corejs;

import java.util.List;

/**
 * A bridge to the JavaScript implementation defined in core.js
 * 
 * @author Renaud Pawlak
 */
public class PropertyChangeSupport {

  public PropertyChangeSupport(Object source) {
  }

  public native void addPropertyChangeListener(PropertyChangeListener listener);

  public native void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

  public native void removePropertyChangeListener(PropertyChangeListener listener);

  public native void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

  public native List<PropertyChangeListener> getPropertyChangeListeners(String propertyName);

  public native void firePropertyChange(String propertyName, Object oldValue, Object newValue);

}
