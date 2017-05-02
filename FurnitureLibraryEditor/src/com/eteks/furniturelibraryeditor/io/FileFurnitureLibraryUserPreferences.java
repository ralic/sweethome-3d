/*
 * FileFurnitureLibraryUserPreferences.java 6 juin 2010
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
package com.eteks.furniturelibraryeditor.io;

import java.io.File;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.sweethome3d.model.LengthUnit;
import com.eteks.sweethome3d.model.Library;
import com.eteks.sweethome3d.model.RecorderException;

/**
 * Editor user preferences stored in file.
 * @author Emmanuel Puybaret
 */
public class FileFurnitureLibraryUserPreferences extends FurnitureLibraryUserPreferences {
  private static final String LANGUAGE                              = "language";
  private static final String UNIT                                  = "unit";
  private static final String DEFAULT_CREATOR                       = "defaultCreator";    
  private static final String OFFLINE_FURNITURE_LIBRARY             = "offlineFurnitureLibrary";
  private static final String FURNITURE_RESOURCES_LOCAL_DIRECTORY   = "furnitureResourcesLocalDirectory";
  private static final String FURNITURE_RESOURCES_REMOTE_URL_BASE   = "furnitureResourcesRemoteUrlBase";

  /**
   * Creates user preferences read from Java preferences.
   */
  public FileFurnitureLibraryUserPreferences() {
    Preferences preferences = getPreferences();
    setLanguage(preferences.get(LANGUAGE, getLanguage()));    
    setUnit(LengthUnit.valueOf(preferences.get(UNIT, getLengthUnit().name())));
    setDefaultCreator(preferences.get(DEFAULT_CREATOR, getDefaultCreator()));
    boolean offlineFurnitureLibrary = preferences.getBoolean(OFFLINE_FURNITURE_LIBRARY, isFurnitureLibraryOffline());
    if (isOnlineFurnitureLibrarySupported()) {
      setFurnitureLibraryOffline(offlineFurnitureLibrary);
      setFurnitureResourcesLocalDirectory(preferences.get(FURNITURE_RESOURCES_LOCAL_DIRECTORY, 
          getFurnitureResourcesLocalDirectory()));
      setFurnitureResourcesRemoteURLBase(preferences.get(FURNITURE_RESOURCES_REMOTE_URL_BASE, 
          getFurnitureResourcesRemoteURLBase()));
    }
  }
  
  @Override
  public void write() throws RecorderException {
    Preferences preferences = getPreferences();
    preferences.put(LANGUAGE, getLanguage());
    preferences.put(UNIT, getLengthUnit().name());   
    if (getDefaultCreator() != null) {
      preferences.put(DEFAULT_CREATOR, getDefaultCreator());
    } else {
      preferences.remove(DEFAULT_CREATOR);
    }
    preferences.putBoolean(OFFLINE_FURNITURE_LIBRARY, isFurnitureLibraryOffline());
    if (getFurnitureResourcesLocalDirectory() != null) {
      preferences.put(FURNITURE_RESOURCES_LOCAL_DIRECTORY, getFurnitureResourcesLocalDirectory());
    } else {
      preferences.remove(FURNITURE_RESOURCES_LOCAL_DIRECTORY);
    }
    if (getFurnitureResourcesRemoteURLBase() != null) {
      preferences.put(FURNITURE_RESOURCES_REMOTE_URL_BASE, getFurnitureResourcesRemoteURLBase());
    } else {
      preferences.remove(FURNITURE_RESOURCES_REMOTE_URL_BASE);
    }
    
    try {
      // Write preferences 
      preferences.sync();
    } catch (BackingStoreException ex) {
      throw new RecorderException("Couldn't write preferences", ex);
    }
  }
  
  /**
   * Returns Java preferences for current system user.
   */
  protected Preferences getPreferences() {
    return Preferences.userNodeForPackage(FileFurnitureLibraryUserPreferences.class);
  }

  @Override
  public void addFurnitureLibrary(String furnitureLibraryName) throws RecorderException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addLanguageLibrary(String languageLibraryName) throws RecorderException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addTexturesLibrary(String texturesLibraryName) throws RecorderException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean furnitureLibraryExists(String furnitureLibraryName) throws RecorderException {
    return new File(furnitureLibraryName).exists();
  }

  @Override
  public boolean languageLibraryExists(String languageLibraryName) throws RecorderException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean texturesLibraryExists(String texturesLibraryName) throws RecorderException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Library> getLibraries() {
    throw new UnsupportedOperationException();
  }
}
