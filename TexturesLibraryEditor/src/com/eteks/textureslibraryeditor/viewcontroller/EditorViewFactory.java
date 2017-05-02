/*
 * EditorViewFactory.java 12 sept. 2012
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

import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;

/**
 * A factory that specifies how to create the views displayed in Textures Library Editor. 
 * @author Emmanuel Puybaret
 */
public interface EditorViewFactory extends ViewFactory {
  /**
   * Returns a view that displays editor main view.
   */
  public EditorView createEditorView(TexturesLibrary texturesLibrary,
                                     TexturesLibraryUserPreferences preferences,
                                     EditorController controller);
  /**
   * Returns a view that displays all the textures in <code>texturesLibrary</code>.
   */
  public View createTexturesLibraryView(TexturesLibrary texturesLibrary,
                                        TexturesLibraryUserPreferences preferences,
                                        TexturesLibraryController texturesLibraryController,
                                        TexturesLanguageController texturesLanguageController);

  /**
   * Returns a view that displays textures importation progression.
   */
  public ImportTexturesTaskView createImportTexturesView(String taskMessage,
                                                         TexturesLibraryUserPreferences preferences,
                                                         ThreadedTaskController controller);

  /**
   * Returns a view that lets the user choose the language in the textures library.
   */
  public View createTexturesLanguageView(TexturesLibrary texturesLibrary,
                                         TexturesLibraryUserPreferences preferences,
                                         TexturesLanguageController controller);

  /**
   * Returns a dialog view to edit textures properties.
   */
  public DialogView createTexturesView(TexturesLibraryUserPreferences preferences,
                                       TexturesController controller);
}
