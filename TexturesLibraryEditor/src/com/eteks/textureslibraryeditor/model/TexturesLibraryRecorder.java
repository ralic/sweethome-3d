/*
 * TexturesLibraryRecorder.java 11 sept. 2012
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

import com.eteks.sweethome3d.model.RecorderException;

/**
 * The recorder able to read and write textures libraries.
 * @author Emmanuel Puybaret
 */
public interface TexturesLibraryRecorder {
  /**
   * Merges a textures library with the one at the given location.
   */
  public abstract void mergeTexturesLibrary(TexturesLibrary texturesLibrary,
                                             String texturesLibraryLocation,
                                             TexturesLibraryUserPreferences preferences) throws RecorderException;
  /**
   * Reads a textures library from the given location.
   */
  public abstract void readTexturesLibrary(TexturesLibrary texturesLibrary,
                                           String texturesLibraryLocation,
                                           TexturesLibraryUserPreferences preferences) throws RecorderException;

  /**
   * Writes textures library at the given location.  
   */
  public abstract void writeTexturesLibrary(TexturesLibrary texturesLibrary,
                                            String texturesLibraryLocation,
                                            TexturesLibraryUserPreferences preferences) throws RecorderException;
}