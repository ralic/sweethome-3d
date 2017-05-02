/*
 * TexturesLibraryUserPreferencesController.java 12 sept. 2012
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
package com.eteks.textureslibraryeditor.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.UserPreferencesController;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;

/**
 * A MVC controller for user preferences view.
 * @author Emmanuel Puybaret
 */
public class TexturesLibraryUserPreferencesController extends UserPreferencesController {
  /**
   * The properties that may be edited by the view associated to this controller. 
   */
  public enum Property {DEFAULT_CREATOR, OFFLINE_TEXTURES_LIBRARY, TEXTURES_RESOURCES_LOCAL_DIRECTORY,
      TEXTURES_RESOURCES_REMOTE_URL_BASE}

  private final TexturesLibraryUserPreferences preferences;
  private final PropertyChangeSupport          propertyChangeSupport;
  
  private String    defaultCreator;
  private boolean   offlineTexturesLibrary;
  private String    texturesResourcesLocalDirectory;
  private String    texturesResourcesRemoteUrlBase;


  public TexturesLibraryUserPreferencesController(TexturesLibraryUserPreferences preferences,
                                                   ViewFactory viewFactory,
                                                   ContentManager contentManager) {
    super(preferences, viewFactory, contentManager);
    this.preferences = preferences;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    updateTexturesLibraryProperties();
  }
  
  /**
   * Adds the property change <code>listener</code> in parameter to this controller.
   */
  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  /**
   * Removes the property change <code>listener</code> in parameter from this controller.
   */
  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  /**
   * Updates textures library preferences properties edited by this controller.
   */
  private void updateTexturesLibraryProperties() {
    setDefaultCreator(this.preferences.getDefaultCreator());
    setTexturesLibraryOffline(this.preferences.isTexturesLibraryOffline());
    setTexturesResourcesLocalDirectory(this.preferences.getTexturesResourcesLocalDirectory());
    setTexturesResourcesRemoteURLBase(this.preferences.getTexturesResourcesRemoteURLBase());
  }

  @Override
  public boolean isPropertyEditable(UserPreferencesController.Property property) {
    switch (property) {
      case UNIT :
      case LANGUAGE :
        return true;
      default :        
        return false;
    }
  }

  public boolean isPropertyEditable(Property property) {    
    return this.preferences.isOnlineTexturesLibrarySupported()
        || property == Property.DEFAULT_CREATOR;
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
   * needed by the textures of a library. 
   */
  public void setTexturesResourcesRemoteURLBase(String texturesResourcesRemoteUrlBase) {
    if (texturesResourcesRemoteUrlBase != this.texturesResourcesRemoteUrlBase
        || texturesResourcesRemoteUrlBase != null && !texturesResourcesRemoteUrlBase.equals(this.texturesResourcesRemoteUrlBase)) {
      Object oldValue = this.texturesResourcesRemoteUrlBase;
      this.texturesResourcesRemoteUrlBase = texturesResourcesRemoteUrlBase;
      this.propertyChangeSupport.firePropertyChange(Property.TEXTURES_RESOURCES_REMOTE_URL_BASE.toString(),
          oldValue, texturesResourcesRemoteUrlBase);
    }
  }
 
  /**
   * Updates user preferences and saves them.
   */
  @Override
  public void modifyUserPreferences() {
    super.modifyUserPreferences();
    this.preferences.setDefaultCreator(getDefaultCreator());
    if (this.preferences.isOnlineTexturesLibrarySupported()) {
      this.preferences.setTexturesLibraryOffline(isTexturesLibraryOffline());
      this.preferences.setTexturesResourcesLocalDirectory(getTexturesResourcesLocalDirectory());
      this.preferences.setTexturesResourcesRemoteURLBase(getTexturesResourcesRemoteURLBase());
    }
  }
}
