/*
 * EditorView.java 12 sept. 2012
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

import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.sweethome3d.viewcontroller.HomeView.SaveAnswer;

/**
 * Textures Library Editor main view.
 * @author Emmanuel Puybaret
 */
public interface EditorView extends View {
  /**
   * The actions proposed by this view.
   */
  public enum ActionType {NEW_LIBRARY, OPEN, MERGE, SAVE, SAVE_AS, PREFERENCES, EXIT, 
                          IMPORT_TEXTURES, MODIFY_TEXTURES, DELETE, SELECT_ALL, ABOUT}

  /**
   * Displays the error message in parameter.
   */
  void showError(String title, String message);

  /**
   * Displays a dialog that lets user choose whether he wants to save before closing or not.
   */
  SaveAnswer confirmSave(String homeName);

  /**
   * Displays an about dialog.
   */
  void showAboutDialog();

  /**
   * Runs the given <code>runnable</code> in the thread that runs event loop.
   */
  void invokeLater(Runnable runnable);
}
