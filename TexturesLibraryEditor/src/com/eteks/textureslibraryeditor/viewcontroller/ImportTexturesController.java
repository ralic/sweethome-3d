/*
 * ImportTexturesController.java 11 sept. 2012
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

import java.util.concurrent.Callable;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskView;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import com.eteks.sweethome3d.viewcontroller.ViewFactoryAdapter;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;

/**
 * Controller used to import textures.
 * @author Emmanuel Puybaret
 */
public class ImportTexturesController implements Controller {
  private ThreadedTaskController threadedTaskController;
  
  public ImportTexturesController(final TexturesLibrary texturesLibrary,
                                   final String [] texturesNames, 
                                   final Runnable postImportTask,
                                   final TexturesLibraryUserPreferences preferences, 
                                   final EditorViewFactory editorViewFactory,
                                   final ContentManager contentManager) {
    Callable<Void> importTexturesTask = new Callable<Void>() {
        public Void call() throws InterruptedException {
          ImportTexturesTaskView importTexturesView = (ImportTexturesTaskView)threadedTaskController.getView();
          for (int i = 0; i < texturesNames.length; i++) {
            String texturesName = texturesNames [i];
            try {
              importTexturesView.setProgress(i, 0, texturesNames.length);
              final CatalogTexture texture = importTexturesView.readTexture(
                  contentManager.getContent(texturesName));
              if (texture != null) {
                importTexturesView.invokeLater(new Runnable() {
                  public void run() {
                    texturesLibrary.addTexture(texture);
                  }
                });
              }
            } catch (RecorderException ex) {
            }            
          }
          
          importTexturesView.invokeLater(postImportTask);
          return null;
        }
      };

    ThreadedTaskController.ExceptionHandler exceptionHandler = 
      new ThreadedTaskController.ExceptionHandler() {
        public void handleException(Exception ex) {
          if (!(ex instanceof InterruptedException)) {
            ex.printStackTrace();
          }
        }
      };
   
    ViewFactory threadedTaskViewFactory = new ViewFactoryAdapter() {
        public ThreadedTaskView createThreadedTaskView(String taskMessage, UserPreferences preferences,
                                                       ThreadedTaskController controller) {
          return editorViewFactory.createImportTexturesView(taskMessage, (TexturesLibraryUserPreferences)preferences, controller);
        }
      };

   String importTexturesMessage = preferences.getLocalizedString(ImportTexturesController.class, "importTexturesMessage");
   this.threadedTaskController = new ThreadedTaskController(importTexturesTask, importTexturesMessage, exceptionHandler, 
       preferences, threadedTaskViewFactory);
  }
  
  /**
   * Executes the import task.
   */
  public void executeTask(View view) {
    this.threadedTaskController.executeTask(view);
  }

  /**
   * Returns the view associated to this controller.
   */
  public View getView() {
    return this.threadedTaskController.getView();
  }
}
