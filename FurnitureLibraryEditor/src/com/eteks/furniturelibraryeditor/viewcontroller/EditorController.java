/*
 * EditorController.java 06 juin 2006
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
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryRecorder;
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.InterruptedRecorderException;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;

/**
 * A MVC controller for the editor.
 * @author Emmanuel Puybaret
 */
public class EditorController implements Controller {
  private final FurnitureLibrary                furnitureLibrary;
  private final FurnitureLibraryRecorder        recorder;
  private final FurnitureLibraryUserPreferences preferences;
  private final ContentManager                  contentManager;
  private final EditorViewFactory               viewFactory;

  private FurnitureLibraryController            furnitureLibraryController;
  private FurnitureLanguageController           furnitureLanguageController;
  private EditorView                            editorView;

  /**
   * Creates a controller of the editor view.
   */
  public EditorController(final FurnitureLibrary furnitureLibrary,
                          FurnitureLibraryRecorder recorder,
                          FurnitureLibraryUserPreferences preferences, 
                          EditorViewFactory viewFactory,
                          ContentManager  contentManager) {
    this.furnitureLibrary = furnitureLibrary;
    this.recorder = recorder;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
  }

  /**
   * Returns the view associated with this controller.
   */
  public EditorView getView() {
    // Create view lazily only once it's needed
    if (this.editorView == null) {
      this.editorView = this.viewFactory.createEditorView(this.furnitureLibrary, this.preferences, this);
    }
    return this.editorView;
  }
  
  /**
   * Returns the furniture library controller managed by this controller.
   */
  public FurnitureLibraryController getFurnitureLibraryController() {
    // Create sub controller lazily only once it's needed
    if (this.furnitureLibraryController == null) {
      this.furnitureLibraryController = new FurnitureLibraryController(
          this.furnitureLibrary, this.preferences, getFurnitureLanguageController(),
          this.viewFactory, this.contentManager);
    }
    return this.furnitureLibraryController;
  }

  /**
   * Returns the furniture language controller managed by this controller.
   */
  public FurnitureLanguageController getFurnitureLanguageController() {
    // Create sub controller lazily only once it's needed
    if (this.furnitureLanguageController == null) {
      this.furnitureLanguageController = new FurnitureLanguageController(
          this.furnitureLibrary, this.preferences, this.viewFactory);
    }
    return this.furnitureLanguageController;
  }

  /**
   * Empties the furniture library after saving and deleting the current one.
   */
  public void newLibrary() {
    // Create a task that resets furniture library
    Runnable newLibraryTask = new Runnable() {
        public void run() {
          for (CatalogPieceOfFurniture piece : furnitureLibrary.getFurniture()) {
            furnitureLibrary.deletePieceOfFurniture(piece);
          }
          getFurnitureLanguageController().setFurnitureLanguage(FurnitureLibrary.DEFAULT_LANGUAGE);
          furnitureLibrary.setId(null);
          furnitureLibrary.setName(null);
          furnitureLibrary.setProvider(null);
          furnitureLibrary.setLicense(null);
          furnitureLibrary.setVersion(null);
          furnitureLibrary.setDescription(null);
          furnitureLibrary.setLocation(null);
          furnitureLibrary.setModified(false);
        }
      };
      
    if (this.furnitureLibrary.isModified()) {
      switch (getView().confirmSave(this.furnitureLibrary.getLocation())) {
        case SAVE   : save(newLibraryTask); // Falls through
        case CANCEL : return;
      }  
    }
    newLibraryTask.run();
  }

  /**
   * Opens a furniture library chosen by user after saving and deleting the current one.
   */
  public void open() {
    // Create a task that opens furniture library
    Runnable openTask = new Runnable() {
        public void run() {
          String openTitle = preferences.getLocalizedString(EditorController.class, "openTitle");
          String furnitureLibraryLocation = contentManager.showOpenDialog(null, openTitle, 
              ContentManager.ContentType.FURNITURE_LIBRARY);
          if (furnitureLibraryLocation != null) {
            open(furnitureLibraryLocation);
          }
        }
      };
      
    if (this.furnitureLibrary.isModified()) {
      switch (getView().confirmSave(this.furnitureLibrary.getLocation())) {
        case SAVE   : save(openTask); // Falls through
        case CANCEL : return;
      }  
    }
    openTask.run();
  }

  /**
   * Opens the furniture library at the given location.
   */
  public void open(final String furnitureLibraryLocation) {
    Callable<Void> saveTask = new Callable<Void>() {
        public Void call() throws RecorderException {
          recorder.readFurnitureLibrary(furnitureLibrary, furnitureLibraryLocation, preferences);
          getFurnitureLanguageController().setFurnitureLanguage(FurnitureLibrary.DEFAULT_LANGUAGE);
          furnitureLibrary.setLocation(furnitureLibraryLocation);
          furnitureLibrary.setModified(false);
          return null;
        }
      };
    ThreadedTaskController.ExceptionHandler exceptionHandler = 
        new ThreadedTaskController.ExceptionHandler() {
          public void handleException(Exception ex) {
            if (!(ex instanceof InterruptedRecorderException)) {
              ex.printStackTrace();
              if (ex instanceof RecorderException) {
                getView().showError(preferences.getLocalizedString(EditorController.class, "errorTitle"), 
                    preferences.getLocalizedString(EditorController.class, "invalidFile"));
              }
            }
          }
        };
    new ThreadedTaskController(saveTask, 
        this.preferences.getLocalizedString(EditorController.class, "openMessage"), exceptionHandler, 
        this.preferences, this.viewFactory).executeTask(getView());
  }
  
  /**
   * Merges the current library with a furniture library chosen by user.
   */
  public void merge() {
    String mergeTitle = preferences.getLocalizedString(EditorController.class, "mergeTitle");
    final String furnitureLibraryLocation = contentManager.showOpenDialog(null, mergeTitle, 
        ContentManager.ContentType.FURNITURE_LIBRARY);
    if (furnitureLibraryLocation != null) {
      Callable<Void> saveTask = new Callable<Void>() {
          public Void call() throws RecorderException {
            recorder.mergeFurnitureLibrary(furnitureLibrary, furnitureLibraryLocation, preferences);
            furnitureLibrary.setModified(true);
            return null;
          }
        };
      ThreadedTaskController.ExceptionHandler exceptionHandler = 
          new ThreadedTaskController.ExceptionHandler() {
            public void handleException(Exception ex) {
              if (!(ex instanceof InterruptedRecorderException)) {
                ex.printStackTrace();
                if (ex instanceof RecorderException) {
                  getView().showError(preferences.getLocalizedString(EditorController.class, "errorTitle"), 
                      preferences.getLocalizedString(EditorController.class, "invalidFile"));
                }
              }
            }
          };
      new ThreadedTaskController(saveTask, 
          this.preferences.getLocalizedString(EditorController.class, "mergeMessage"), exceptionHandler, 
          this.preferences, this.viewFactory).executeTask(getView());
    }
  }

  /**
   * Saves the furniture library.
   */
  public void save() {
    save(null);
  }

  /**
   * Saves the library managed by this controller and executes <code>postSaveTask</code> 
   * if it's not <code>null</code>.
   */
  private void save(Runnable postSaveTask) {
    if (this.furnitureLibrary.getLocation() == null) {
      saveAs(postSaveTask);
    } else {
      save(this.furnitureLibrary.getLocation(), postSaveTask);
    }
  }
  
  /**
   * Saves the furniture library under a different name.
   */
  public void saveAs() {
    saveAs(null);
  }

  /**
   * Saves the furniture library under a different name and executes <code>postSaveTask</code> 
   * if it's not <code>null</code>.
   */
  private void saveAs(Runnable postSaveTask) {
    String saveTitle = this.preferences.getLocalizedString(EditorController.class, "saveTitle");
    String furnitureLibraryLocation = this.contentManager.showSaveDialog(null, saveTitle, 
        ContentManager.ContentType.FURNITURE_LIBRARY, this.furnitureLibrary.getLocation());
    if (furnitureLibraryLocation != null) {
      save(furnitureLibraryLocation, postSaveTask);
    }
  }
  
  /**
   * Actually saves the library managed by this controller and executes <code>postSaveTask</code> 
   * if it's not <code>null</code>.
   */
  private void save(final String location, 
                    final Runnable postSaveTask) {
    Callable<Void> saveTask = new Callable<Void>() {
        public Void call() throws RecorderException {
          recorder.writeFurnitureLibrary(furnitureLibrary, location, preferences);
          getView().invokeLater(new Runnable() {
              public void run() {
                furnitureLibrary.setLocation(location);
                furnitureLibrary.setModified(false);
                if (postSaveTask != null) {
                  postSaveTask.run();
                }
              }
            });
          return null;
        }
      };
    ThreadedTaskController.ExceptionHandler exceptionHandler = 
        new ThreadedTaskController.ExceptionHandler() {
          public void handleException(Exception ex) {
            if (!(ex instanceof InterruptedRecorderException)) {
              ex.printStackTrace();
              if (ex instanceof RecorderException) {
                getView().showError(preferences.getLocalizedString(EditorController.class, "errorTitle"), 
                    preferences.getLocalizedString(EditorController.class, "saveError"));
              }
            }
          }
        };
    new ThreadedTaskController(saveTask, 
        this.preferences.getLocalizedString(EditorController.class, "saveMessage"), exceptionHandler, 
        this.preferences, this.viewFactory).executeTask(getView());
  }

  /**
   * Exits program.
   */
  public void exit() {
    // Create a task that deletes home and run postCloseTask
    Runnable exitTask = new Runnable() {
        public void run() {
          System.exit(0);
        }
      };
      
    if (this.furnitureLibrary.isModified()) {
      switch (getView().confirmSave(this.furnitureLibrary.getLocation())) {
        case SAVE   : save(exitTask); // Falls through
        case CANCEL : return;
      }  
    }
    exitTask.run();
  }

  /**
   * Shows information about the program.
   */
  public void about() {
    getView().showAboutDialog();
  }

  /**
   * Edits the preferences of the program.
   */
  public void editPreferences() {
    try {
      new FurnitureLibraryUserPreferencesController(this.preferences,
          this.viewFactory, this.contentManager).displayView(getView());
      this.preferences.write();
    } catch (RecorderException ex) {
      getView().showError(preferences.getLocalizedString(EditorController.class, "errorTitle"), 
          preferences.getLocalizedString(EditorController.class, "savePreferencesError"));
    }
  }
}
