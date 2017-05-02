/*
 * EditorViewFactory.java 05 juin 2010
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
package com.eteks.furniturelibraryeditor.viewcontroller;

import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;

/**
 * A factory that specifies how to create the views displayed in Furniture Library Editor. 
 * @author Emmanuel Puybaret
 */
public interface EditorViewFactory extends ViewFactory {
  /**
   * Returns a view that displays editor main view.
   */
  public EditorView createEditorView(FurnitureLibrary furnitureLibrary,
                                     FurnitureLibraryUserPreferences preferences,
                                     EditorController controller);
  /**
   * Returns a view that displays all the furniture in <code>furnitureLibrary</code>.
   */
  public View createFurnitureLibraryView(FurnitureLibrary furnitureLibrary,
                                         FurnitureLibraryUserPreferences preferences,
                                         FurnitureLibraryController furnitureLibraryController,
                                         FurnitureLanguageController furnitureLanguageController);

  /**
   * Returns a view that displays furniture importation progression.
   */
  public ImportFurnitureTaskView createImportFurnitureView(String taskMessage,
                                                  FurnitureLibraryUserPreferences preferences,
                                                  ThreadedTaskController controller);

  /**
   * Returns a view that lets the user choose the language in the furniture library.
   */
  public View createFurnitureLanguageView(FurnitureLibrary furnitureLibrary,
                                          FurnitureLibraryUserPreferences preferences,
                                          FurnitureLanguageController controller);

  /**
   * Returns a dialog view to edit furniture properties.
   */
  public DialogView createFurnitureView(FurnitureLibraryUserPreferences preferences,
                                        FurnitureController controller);
}
