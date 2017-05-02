/*
 * TexturesLibraryUserPreferences.java 11 sept 2012
 *
 * Textures Library Editor, Copyright (c) 2012 Emmanuel PUYBARET / eTeks <info@eteks.com>
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
package com.eteks.textureslibraryeditor.model;

import java.beans.PropertyChangeSupport;

import com.eteks.sweethome3d.model.LengthUnit;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * User preferences with additional attributes for textures libraries management.
 * @author Emmanuel Puybaret
 */
public abstract class TexturesLibraryUserPreferences extends UserPreferences {
  /**
   * The properties of user preferences that may change. <code>PropertyChangeListener</code>s added 
   * to user preferences will be notified under a property name equal to the string value of one these properties.
   */
  public enum Property {DEFAULT_CREATOR, OFFLINE_TEXTURES_LIBRARY, TEXTURES_RESOURCES_LOCAL_DIRECTORY,
                        TEXTURES_RESOURCES_REMOTE_URL_BASE}
  
  private final PropertyChangeSupport propertyChangeSupport;
  private String [] editedProperties;
  private String    defaultCreator;
  private boolean   offlineTexturesLibrary;
  private String    texturesResourcesLocalDirectory;
  private String    texturesResourcesRemoteUrlBase;

  public TexturesLibraryUserPreferences() {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.editedProperties = new String [] {
        TexturesLibrary.TEXTURES_IMAGE_PROPERTY,
        TexturesLibrary.TEXTURES_NAME_PROPERTY,
        TexturesLibrary.TEXTURES_CATEGORY_PROPERTY,
        TexturesLibrary.TEXTURES_CREATOR_PROPERTY,
        TexturesLibrary.TEXTURES_WIDTH_PROPERTY,
        TexturesLibrary.TEXTURES_HEIGHT_PROPERTY};
    setUnit(LengthUnit.CENTIMETER);
    this.offlineTexturesLibrary = true;
  }

  @Override
  public String [] getSupportedLanguages() {
    return new String [] {"en", "fr"};
  }
  
  /**
   * Returns the language used to retrieve the default localized values of the textures.
   */
  public String getTexturesDefaultLanguage() {
    return getLanguage();
  }

  /**
   * Returns the list of properties that the user may display and edit with the editor.
   */
  public String [] getEditedProperties() {
    return this.editedProperties;
  }
  
  /**
   * Returns the creator used by default for imported textures or <code>null</code>.
   */
  public String getDefaultCreator() {
    return this.defaultCreator;
  }
  
  /**
   * Sets the creator used by default for imported textures.
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
   * and sets textures resources local directory and remote URL base.   
   */
  public boolean isOnlineTexturesLibrarySupported() {
    return false;
  }
  
  /**
   * Returns <code>true</code> if resources needed by the textures of a library 
   * must be included with the library to let it work without connection. 
   */
  public boolean isTexturesLibraryOffline() {
    return this.offlineTexturesLibrary;
  }
  
  /**
   * Sets whether resources needed by the textures of a library 
   * must be included with the library to let it work without connection or not.
   */
  public void setTexturesLibraryOffline(boolean offlineTexturesLibrary) {
    if (!isOnlineTexturesLibrarySupported() && !offlineTexturesLibrary) {
      throw new IllegalArgumentException("Textures library doesn't support online libraries");
    }
    if (offlineTexturesLibrary != this.offlineTexturesLibrary) {
      this.offlineTexturesLibrary = offlineTexturesLibrary;
      this.propertyChangeSupport.firePropertyChange(Property.OFFLINE_TEXTURES_LIBRARY.toString(), 
          !offlineTexturesLibrary, offlineTexturesLibrary);
    }
  }

  /**
   * Returns the local directory where resources needed by the textures of a library
   * will be saved before being deployed on server.
   */
  public String getTexturesResourcesLocalDirectory() {
    return this.texturesResourcesLocalDirectory;
  }
  
  /**
   * Sets the local directory where resources needed by the textures of a library
   * will be saved before being deployed on server.
   */
  public void setTexturesResourcesLocalDirectory(String texturesResourcesLocalDirectory) {
    if (!isOnlineTexturesLibrarySupported()) {
      throw new IllegalArgumentException("Textures library doesn't support online libraries");
    }
    if (texturesResourcesLocalDirectory != this.texturesResourcesLocalDirectory
        || texturesResourcesLocalDirectory != null && !texturesResourcesLocalDirectory.equals(this.texturesResourcesLocalDirectory)) {
      String oldValue = this.texturesResourcesLocalDirectory;
      this.texturesResourcesLocalDirectory = texturesResourcesLocalDirectory;
      this.propertyChangeSupport.firePropertyChange(Property.TEXTURES_RESOURCES_LOCAL_DIRECTORY.toString(),
          oldValue, texturesResourcesLocalDirectory);
    }
  }

  /**
   * Returns the URL base (relative or absolute) used to build the path to resources 
   * needed by the textures of a library. 
   */
  public String getTexturesResourcesRemoteURLBase() {
    return this.texturesResourcesRemoteUrlBase;
  }
  
  /**
   * Sets the URL base (relative or absolute) used to build the path to resources 
   * needed by the textures of a library. This base should be ended by a / character
   * if it's a directory. 
   */
  public void setTexturesResourcesRemoteURLBase(String texturesResourcesRemoteUrlBase) {
    if (!isOnlineTexturesLibrarySupported()) {
      throw new IllegalArgumentException("Textures library doesn't support online libraries");
    }
    if (texturesResourcesRemoteUrlBase != this.texturesResourcesRemoteUrlBase
        || texturesResourcesRemoteUrlBase != null && !texturesResourcesRemoteUrlBase.equals(this.texturesResourcesRemoteUrlBase)) {
      Object oldValue = this.texturesResourcesRemoteUrlBase;
      this.texturesResourcesRemoteUrlBase = texturesResourcesRemoteUrlBase;
      this.propertyChangeSupport.firePropertyChange(Property.TEXTURES_RESOURCES_REMOTE_URL_BASE.toString(),
          oldValue, texturesResourcesRemoteUrlBase);
    }
  }

  /**
   * Returns <code>true</code> if the textures content saved with the library should be named 
   * from the textures name in the default language.
   */
  public boolean isContentMatchingTexturesName() {
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
