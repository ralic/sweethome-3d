/*
 * FurnitureLibraryRecorder.java 22 déc. 2009
 *
 * Furniture Library Editor, Copyright (c) 2009 Emmanuel PUYBARET / eTeks <info@eteks.com>
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

import com.eteks.sweethome3d.model.RecorderException;

/**
 * The recorder able to read and write furniture libraries.
 * @author Emmanuel Puybaret
 */
public interface FurnitureLibraryRecorder {
  /**
   * Merges a furniture library with the one at the given location.
   */
  public abstract void mergeFurnitureLibrary(FurnitureLibrary furnitureLibrary,
                                             String furnitureLibraryLocation,
                                             FurnitureLibraryUserPreferences preferences) throws RecorderException;
  /**
   * Reads a furniture library from the given location.
   */
  public abstract void readFurnitureLibrary(FurnitureLibrary furnitureLibrary,
                                            String furnitureLibraryLocation,
                                            FurnitureLibraryUserPreferences preferences) throws RecorderException;

  /**
   * Writes a furniture library at the given location.  
   */
  public abstract void writeFurnitureLibrary(FurnitureLibrary furnitureLibrary,
                                             String furnitureLibraryLocation,
                                             FurnitureLibraryUserPreferences preferences) throws RecorderException;
}