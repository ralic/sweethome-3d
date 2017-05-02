/*
 * EditorController.java 12 sept. 2012
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
import com.eteks.sweethome3d.model.InterruptedRecorderException;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryRecorder;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;

/**
 * A MVC controller for the editor.
 * @author Emmanuel Puybaret
 */
public class EditorController implements Controller {
  private final TexturesLibrary                texturesLibrary;
  private final TexturesLibraryRecorder        recorder;
  private final TexturesLibraryUserPreferences preferences;
  private final ContentManager                 contentManager;
  private final EditorViewFactory              viewFactory;

  private TexturesLibraryController            texturesLibraryController;
  private TexturesLanguageController           texturesLanguageController;
  private EditorView                           editorView;

  /**
   * Creates a controller of the editor view.
   */
  public EditorController(final TexturesLibrary texturesLibrary,
                          TexturesLibraryRecorder recorder,
                          TexturesLibraryUserPreferences preferences, 
                          EditorViewFactory viewFactory,
                          ContentManager  contentManager) {
    this.texturesLibrary = texturesLibrary;
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
      this.editorView = this.viewFactory.createEditorView(this.texturesLibrary, this.preferences, this);
    }
    return this.editorView;
  }
  
  /**
   * Returns the textures library controller managed by this controller.
   */
  public TexturesLibraryController getTexturesLibraryController() {
    // Create sub controller lazily only once it's needed
    if (this.texturesLibraryController == null) {
      this.texturesLibraryController = new TexturesLibraryController(
          this.texturesLibrary, this.preferences, getTexturesLanguageController(),
          this.viewFactory, this.contentManager);
    }
    return this.texturesLibraryController;
  }

  /**
   * Returns the textures language controller managed by this controller.
   */
  public TexturesLanguageController getTexturesLanguageController() {
    // Create sub controller lazily only once it's needed
    if (this.texturesLanguageController == null) {
      this.texturesLanguageController = new TexturesLanguageController(
          this.texturesLibrary, this.preferences, this.viewFactory);
    }
    return this.texturesLanguageController;
  }

  /**
   * Empties the textures library after saving and deleting the current one.
   */
  public void newLibrary() {
    // Create a task that resets textures library
    Runnable newLibraryTask = new Runnable() {
        public void run() {
          for (CatalogTexture texture : texturesLibrary.getTextures()) {
            texturesLibrary.deleteTexture(texture);
          }
          getTexturesLanguageController().setTexturesLanguage(TexturesLibrary.DEFAULT_LANGUAGE);
          texturesLibrary.setId(null);
          texturesLibrary.setName(null);
          texturesLibrary.setDescription(null);
          texturesLibrary.setProvider(null);
          texturesLibrary.setLicense(null);
          texturesLibrary.setVersion(null);
          texturesLibrary.setLocation(null);
          texturesLibrary.setModified(false);
        }
      };
      
    if (this.texturesLibrary.isModified()) {
      switch (getView().confirmSave(this.texturesLibrary.getLocation())) {
        case SAVE   : save(newLibraryTask); // Falls through
        case CANCEL : return;
      }  
    }
    newLibraryTask.run();
  }

  /**
   * Opens a textures library chosen by user after saving and deleting the current one.
   */
  public void open() {
    // Create a task that opens textures library
    Runnable openTask = new Runnable() {
        public void run() {
          String openTitle = preferences.getLocalizedString(EditorController.class, "openTitle");
          String texturesLibraryLocation = contentManager.showOpenDialog(null, openTitle, 
              ContentManager.ContentType.TEXTURES_LIBRARY);
          if (texturesLibraryLocation != null) {
            open(texturesLibraryLocation);
          }
        }
      };
      
    if (this.texturesLibrary.isModified()) {
      switch (getView().confirmSave(this.texturesLibrary.getLocation())) {
        case SAVE   : save(openTask); // Falls through
        case CANCEL : return;
      }  
    }
    openTask.run();
  }

  /**
   * Merges the current library with a textures library chosen by user.
   */
  public void merge() {
    String mergeTitle = preferences.getLocalizedString(EditorController.class, "mergeTitle");
    final String texturesLibraryLocation = contentManager.showOpenDialog(null, mergeTitle, 
        ContentManager.ContentType.TEXTURES_LIBRARY);
    if (texturesLibraryLocation != null) {
      Callable<Void> saveTask = new Callable<Void>() {
          public Void call() throws RecorderException {
            recorder.mergeTexturesLibrary(texturesLibrary, texturesLibraryLocation, preferences);
            texturesLibrary.setModified(true);
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
   * Opens the textures library in the given location.
   */
  public void open(final String texturesLibraryLocation) {
    Callable<Void> saveTask = new Callable<Void>() {
        public Void call() throws RecorderException {
          recorder.readTexturesLibrary(texturesLibrary, texturesLibraryLocation, preferences);
          getTexturesLanguageController().setTexturesLanguage(TexturesLibrary.DEFAULT_LANGUAGE);
          texturesLibrary.setLocation(texturesLibraryLocation);
          texturesLibrary.setModified(false);
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
   * Saves the textures library.
   */
  public void save() {
    save(null);
  }

  /**
   * Saves the library managed by this controller and executes <code>postSaveTask</code> 
   * if it's not <code>null</code>.
   */
  private void save(Runnable postSaveTask) {
    if (this.texturesLibrary.getLocation() == null) {
      saveAs(postSaveTask);
    } else {
      save(this.texturesLibrary.getLocation(), postSaveTask);
    }
  }
  
  /**
   * Saves the textures library under a different location.
   */
  public void saveAs() {
    saveAs(null);
  }

  /**
   * Saves the textures library under a different location and executes <code>postSaveTask</code> 
   * if it's not <code>null</code>.
   */
  private void saveAs(Runnable postSaveTask) {
    String saveTitle = this.preferences.getLocalizedString(EditorController.class, "saveTitle");
    String texturesLibraryLocation = this.contentManager.showSaveDialog(null, saveTitle, 
        ContentManager.ContentType.TEXTURES_LIBRARY, this.texturesLibrary.getLocation());
    if (texturesLibraryLocation != null) {
      save(texturesLibraryLocation, postSaveTask);
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
          recorder.writeTexturesLibrary(texturesLibrary, location, preferences);
          getView().invokeLater(new Runnable() {
              public void run() {
                texturesLibrary.setLocation(location);
                texturesLibrary.setModified(false);
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
      
    if (this.texturesLibrary.isModified()) {
      switch (getView().confirmSave(this.texturesLibrary.getLocation())) {
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
      new TexturesLibraryUserPreferencesController(this.preferences,
          this.viewFactory, this.contentManager).displayView(getView());
      this.preferences.write();
    } catch (RecorderException ex) {
      getView().showError(preferences.getLocalizedString(EditorController.class, "errorTitle"), 
          preferences.getLocalizedString(EditorController.class, "savePreferencesError"));
    }
  }
}
