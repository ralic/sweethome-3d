/*
 * ImportFurnitureController.java 13 janv. 2010
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

import java.util.concurrent.Callable;

import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskView;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import com.eteks.sweethome3d.viewcontroller.ViewFactoryAdapter;

/**
 * Controller used to import furniture.
 * @author Emmanuel Puybaret
 */
public class ImportFurnitureController implements Controller {
  private ThreadedTaskController threadedTaskController;
  
  public ImportFurnitureController(final FurnitureLibrary furnitureLibrary,
                                   final String [] furnitureNames, 
                                   final Runnable postImportTask,
                                   final FurnitureLibraryUserPreferences preferences, 
                                   final EditorViewFactory editorViewFactory,
                                   final ContentManager contentManager) {
    Callable<Void> importFurnitureTask = new Callable<Void>() {
        public Void call() throws InterruptedException {
          ImportFurnitureTaskView importFurnitureView = (ImportFurnitureTaskView)threadedTaskController.getView();
          for (int i = 0; i < furnitureNames.length; i++) {
            String furnitureName = furnitureNames [i];
            try {
              importFurnitureView.setProgress(i, 0, furnitureNames.length);
              final CatalogPieceOfFurniture piece = importFurnitureView.readPieceOfFurniture(
                  contentManager.getContent(furnitureName));
              if (piece != null) {
                importFurnitureView.invokeLater(new Runnable() {
                  public void run() {
                    furnitureLibrary.addPieceOfFurniture(piece);
                  }
                });
              }
            } catch (RecorderException ex) {
            }            
          }
          
          importFurnitureView.invokeLater(postImportTask);
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
          return editorViewFactory.createImportFurnitureView(taskMessage, (FurnitureLibraryUserPreferences)preferences, controller);
        }
      };

   String importFurnitureMessage = preferences.getLocalizedString(ImportFurnitureController.class, "importFurnitureMessage");
   this.threadedTaskController = new ThreadedTaskController(importFurnitureTask, importFurnitureMessage, exceptionHandler, 
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
