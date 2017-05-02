/*
 * PropertyChangeEvent.java 
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

/**
 * A bridge to the JavaScript implementation defined in core.js
 * 
 * @author Renaud Pawlak
 */
public class PropertyChangeEvent {

  public PropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue) {
  }
  
  public native Object getSource();
  
  public native Object getNewValue();
  
  public native Object getOldValue();
  
  public native String getPropertyName();

}
