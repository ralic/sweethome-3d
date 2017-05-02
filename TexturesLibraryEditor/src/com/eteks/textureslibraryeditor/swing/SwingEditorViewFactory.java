/*
 * SwingEditorViewFactory.java 5 juin 2010
 *
 * Textures Library Editor, Copyright (c) 2010 Emmanuel PUYBARET / eTeks <info@eteks.com>
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
package com.eteks.textureslibraryeditor.swing;

import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.sweethome3d.viewcontroller.UserPreferencesController;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;
import com.eteks.textureslibraryeditor.viewcontroller.EditorController;
import com.eteks.textureslibraryeditor.viewcontroller.EditorView;
import com.eteks.textureslibraryeditor.viewcontroller.EditorViewFactory;
import com.eteks.textureslibraryeditor.viewcontroller.TexturesController;
import com.eteks.textureslibraryeditor.viewcontroller.TexturesLanguageController;
import com.eteks.textureslibraryeditor.viewcontroller.TexturesLibraryController;
import com.eteks.textureslibraryeditor.viewcontroller.TexturesLibraryUserPreferencesController;
import com.eteks.textureslibraryeditor.viewcontroller.ImportTexturesTaskView;

/**
 * A factory that specifies how to create the views displayed in Textures Library Editor with Swing. 
 * @author Emmanuel Puybaret
 */
public class SwingEditorViewFactory extends SwingViewFactory implements EditorViewFactory {
  /**
   * Returns a pane that displays editor main view.
   */
  public EditorView createEditorView(TexturesLibrary texturesLibrary,
                                     TexturesLibraryUserPreferences preferences,
                                     EditorController controller) {
    return new EditorPane(texturesLibrary, preferences, controller);
  }
  
  /**
   * Returns a table that displays all the textures in <code>texturesLibrary</code>.
   */
  public View createTexturesLibraryView(TexturesLibrary texturesLibrary,
                                        TexturesLibraryUserPreferences preferences,
                                        TexturesLibraryController texturesLibraryController,
                                        TexturesLanguageController texturesLanguageController) {
    return new TexturesLibraryTable(texturesLibrary, preferences, texturesLibraryController, texturesLanguageController);
  }

  /**
   * Returns a panel that displays textures importation progression.
   */
  public ImportTexturesTaskView createImportTexturesView(String taskMessage,
                                                         TexturesLibraryUserPreferences preferences,
                                                         ThreadedTaskController controller) {
    return new ImportTexturesTaskPanel(taskMessage, preferences, controller);
  }

  /**
   * Returns a combo box that lets the user choose the language in the textures library.
   */
  public View createTexturesLanguageView(TexturesLibrary texturesLibrary,
                                         TexturesLibraryUserPreferences preferences,
                                         TexturesLanguageController controller) {
    return new TexturesLanguageComboBox(texturesLibrary, preferences, controller);
  }

  /**
   * Returns a panel to edit textures properties.
   */
  public DialogView createTexturesView(TexturesLibraryUserPreferences preferences,
                                       TexturesController controller) {
    return new TexturesPanel(preferences, controller);
  }
  
  @Override
  public DialogView createUserPreferencesView(UserPreferences preferences,
                                              UserPreferencesController controller) {
    return new TexturesLibraryUserPreferencesPanel(preferences, 
        (TexturesLibraryUserPreferencesController)controller);
  }
}
