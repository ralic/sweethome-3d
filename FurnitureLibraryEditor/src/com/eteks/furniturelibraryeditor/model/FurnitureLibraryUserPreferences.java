/*
 * FurnitureLibraryUserPreferences.java 6 juin 2010
 *
 * Furniture Library Editor, Copyright (c) 2010 Emmanuel PUYBARET / eTeks <info@eteks.com>
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
package com.eteks.furniturelibraryeditor.model;

import java.beans.PropertyChangeSupport;

import com.eteks.sweethome3d.model.LengthUnit;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * User preferences with additional attributes for furniture libraries management.
 * @author Emmanuel Puybaret
 */
public abstract class FurnitureLibraryUserPreferences extends UserPreferences {
  /**
   * The properties of user preferences that may change. <code>PropertyChangeListener</code>s added 
   * to user preferences will be notified under a property name equal to the string value of one these properties.
   */
  public enum Property {DEFAULT_CREATOR, OFFLINE_FURNITURE_LIBRARY, FURNITURE_RESOURCES_LOCAL_DIRECTORY,
                        FURNITURE_RESOURCES_REMOTE_URL_BASE}
  
  private final PropertyChangeSupport propertyChangeSupport;
  private String [] editedProperties;
  private String    defaultCreator;
  private boolean   offlineFurnitureLibrary;
  private String    furnitureResourcesLocalDirectory;
  private String    furnitureResourcesRemoteUrlBase;

  public FurnitureLibraryUserPreferences() {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.editedProperties = new String [] {
        FurnitureLibrary.FURNITURE_MODEL_PROPERTY,
        FurnitureLibrary.FURNITURE_ICON_PROPERTY,
        FurnitureLibrary.FURNITURE_NAME_PROPERTY,
        FurnitureLibrary.FURNITURE_TAGS_PROPERTY,
        FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY,
        FurnitureLibrary.FURNITURE_CREATOR_PROPERTY,
        FurnitureLibrary.FURNITURE_WIDTH_PROPERTY,
        FurnitureLibrary.FURNITURE_DEPTH_PROPERTY,
        FurnitureLibrary.FURNITURE_HEIGHT_PROPERTY,
        FurnitureLibrary.FURNITURE_ELEVATION_PROPERTY,
        FurnitureLibrary.FURNITURE_MOVABLE_PROPERTY,
        FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_PROPERTY,
        FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_CUT_OUT_SHAPE_PROPERTY,
        FurnitureLibrary.FURNITURE_STAIRCASE_CUT_OUT_SHAPE_PROPERTY,
        FurnitureLibrary.FURNITURE_MODEL_ROTATION_PROPERTY};
    setUnit(LengthUnit.CENTIMETER);
    this.offlineFurnitureLibrary = true;
  }

  @Override
  public String [] getSupportedLanguages() {
    return new String [] {"en", "fr"};
  }
  
  /**
   * Returns the language used to retrieve the default localized values of the furniture.
   */
  public String getFurnitureDefaultLanguage() {
    return getLanguage();
  }

  /**
   * Returns the list of properties that the user may display and edit with the editor.
   */
  public String [] getEditedProperties() {
    return this.editedProperties;
  }
  
  /**
   * Returns <code>true</code> if model content should always converted to OBJ format 
   * at importation time. 
   */
  public boolean isModelContentAlwaysConvertedToOBJFormat() {
    return false;
  }
  
  /**
   * Returns the creator used by default for imported furniture or <code>null</code>.
   */
  public String getDefaultCreator() {
    return this.defaultCreator;
  }
  
  /**
   * Sets the creator used by default for imported furniture.
   */
  public void setDefaultCreator(String defaultCreator) {
    if (defaultCreator != this.defaultCreator
        || defaultCreator != null && !defaultCreator.equals(this.defaultCreator)) {
      String oldDefaultCreator = this.defaultCreator;
      this.defaultCreator = defaultCreator;
      this.propertyChangeSupport.firePropertyChange(Property.DEFAULT_CREATOR.toString(), oldDefaultCreator, defaultCreator);
    }
  }

  /**
   * Returns <code>true</code> if the user may edit online libraries,
   * and sets furniture resources local directory and remote URL base.   
   */
  public boolean isOnlineFurnitureLibrarySupported() {
    return false;
  }
  
  /**
   * Returns <code>true</code> if resources needed by the furniture of a library 
   * must be included with the library to let it work without connection. 
   */
  public boolean isFurnitureLibraryOffline() {
    return this.offlineFurnitureLibrary;
  }
  
  /**
   * Sets whether resources needed by the furniture of a library 
   * must be included with the library to let it work without connection or not.
   */
  public void setFurnitureLibraryOffline(boolean offlineFurnitureLibrary) {
    if (!isOnlineFurnitureLibrarySupported() && !offlineFurnitureLibrary) {
      throw new IllegalArgumentException("Furniture library doesn't support online libraries");
    }
    if (offlineFurnitureLibrary != this.offlineFurnitureLibrary) {
      this.offlineFurnitureLibrary = offlineFurnitureLibrary;
      this.propertyChangeSupport.firePropertyChange(Property.OFFLINE_FURNITURE_LIBRARY.toString(), 
          !offlineFurnitureLibrary, offlineFurnitureLibrary);
    }
  }

  /**
   * Returns the local directory where resources needed by the furniture of a library
   * will be saved before being deployed on server.
   */
  public String getFurnitureResourcesLocalDirectory() {
    return this.furnitureResourcesLocalDirectory;
  }
  
  /**
   * Sets the local directory where resources needed by the furniture of a library
   * will be saved before being deployed on server.
   */
  public void setFurnitureResourcesLocalDirectory(String furnitureResourcesLocalDirectory) {
    if (!isOnlineFurnitureLibrarySupported()) {
      throw new IllegalArgumentException("Furniture library doesn't support online libraries");
    }
    if (furnitureResourcesLocalDirectory != this.furnitureResourcesLocalDirectory
        || furnitureResourcesLocalDirectory != null && !furnitureResourcesLocalDirectory.equals(this.furnitureResourcesLocalDirectory)) {
      String oldValue = this.furnitureResourcesLocalDirectory;
      this.furnitureResourcesLocalDirectory = furnitureResourcesLocalDirectory;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_RESOURCES_LOCAL_DIRECTORY.toString(),
          oldValue, furnitureResourcesLocalDirectory);
    }
  }

  /**
   * Returns the URL base (relative or absolute) used to build the path to resources 
   * needed by the furniture of a library. 
   */
  public String getFurnitureResourcesRemoteURLBase() {
    return this.furnitureResourcesRemoteUrlBase;
  }
  
  /**
   * Sets the URL base (relative or absolute) used to build the path to resources 
   * needed by the furniture of a library. This base should be ended by a / character
   * if it's a directory. 
   */
  public void setFurnitureResourcesRemoteURLBase(String furnitureResourcesRemoteUrlBase) {
    if (!isOnlineFurnitureLibrarySupported()) {
      throw new IllegalArgumentException("Furniture library doesn't support online libraries");
    }
    if (furnitureResourcesRemoteUrlBase != this.furnitureResourcesRemoteUrlBase
        || furnitureResourcesRemoteUrlBase != null && !furnitureResourcesRemoteUrlBase.equals(this.furnitureResourcesRemoteUrlBase)) {
      Object oldValue = this.furnitureResourcesRemoteUrlBase;
      this.furnitureResourcesRemoteUrlBase = furnitureResourcesRemoteUrlBase;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_RESOURCES_REMOTE_URL_BASE.toString(),
          oldValue, furnitureResourcesRemoteUrlBase);
    }
  }

  /**
   * Returns <code>true</code> if the furniture content saved with the library should be named 
   * from the furniture name in the default language.
   */
  public boolean isContentMatchingFurnitureName() {
    return true;
  }
  
  /**
   * Returns <code>false</code>.
   */
  @Override
  public boolean isCheckUpdatesEnabled() {
    return false;
  }
}
