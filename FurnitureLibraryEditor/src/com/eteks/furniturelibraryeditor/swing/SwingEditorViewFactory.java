/*
 * SwingEditorViewFactory.java 5 juin 2010
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
package com.eteks.furniturelibraryeditor.swing;

import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.furniturelibraryeditor.viewcontroller.EditorController;
import com.eteks.furniturelibraryeditor.viewcontroller.EditorView;
import com.eteks.furniturelibraryeditor.viewcontroller.EditorViewFactory;
import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureController;
import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureLanguageController;
import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureLibraryController;
import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureLibraryUserPreferencesController;
import com.eteks.furniturelibraryeditor.viewcontroller.ImportFurnitureTaskView;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.sweethome3d.viewcontroller.UserPreferencesController;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * A factory that specifies how to create the views displayed in Furniture Library Editor with Swing. 
 * @author Emmanuel Puybaret
 */
public class SwingEditorViewFactory extends SwingViewFactory implements EditorViewFactory {
  /**
   * Returns a pane that displays editor main view.
   */
  public EditorView createEditorView(FurnitureLibrary furnitureLibrary,
                                     FurnitureLibraryUserPreferences preferences,
                                     EditorController controller) {
    return new EditorPane(furnitureLibrary, preferences, controller);
  }
  
  /**
   * Returns a table that displays all the furniture in <code>furnitureLibrary</code>.
   */
  public View createFurnitureLibraryView(FurnitureLibrary furnitureLibrary,
                                         FurnitureLibraryUserPreferences preferences,
                                         FurnitureLibraryController furnitureLibraryController,
                                         FurnitureLanguageController furnitureLanguageController) {
    return new FurnitureLibraryTable(furnitureLibrary, preferences, furnitureLibraryController, furnitureLanguageController);
  }

  /**
   * Returns a panel that displays furniture importation progression.
   */
  public ImportFurnitureTaskView createImportFurnitureView(String taskMessage,
                                                  FurnitureLibraryUserPreferences preferences,
                                                  ThreadedTaskController controller) {
    return new ImportFurnitureTaskPanel(taskMessage, preferences, controller);
  }

  /**
   * Returns a combo box that lets the user choose the language in the furniture library.
   */
  public View createFurnitureLanguageView(FurnitureLibrary furnitureLibrary,
                                          FurnitureLibraryUserPreferences preferences,
                                          FurnitureLanguageController controller) {
    return new FurnitureLanguageComboBox(furnitureLibrary, preferences, controller);
  }

  /**
   * Returns a panel to edit furniture properties.
   */
  public DialogView createFurnitureView(FurnitureLibraryUserPreferences preferences,
                                        FurnitureController controller) {
    return new FurniturePanel(preferences, controller);
  }
  
  @Override
  public DialogView createUserPreferencesView(UserPreferences preferences,
                                              UserPreferencesController controller) {
    return new FurnitureLibraryUserPreferencesPanel(preferences, 
        (FurnitureLibraryUserPreferencesController)controller);
  }
}
