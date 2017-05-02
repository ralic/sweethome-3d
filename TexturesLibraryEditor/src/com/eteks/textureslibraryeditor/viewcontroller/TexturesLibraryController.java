/*
 * TexturesLibraryController.java 12 sept. 2012
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;

/**
 * A MVC controller for the textures library.
 * @author Emmanuel Puybaret
 */
public class TexturesLibraryController implements Controller {
  private final TexturesLibrary                texturesLibrary;
  private final TexturesLibraryUserPreferences preferences;
  private final ContentManager                 contentManager;
  private final EditorViewFactory              viewFactory;
  private final List<SelectionListener>        selectionListeners;
  
  private TexturesLanguageController           texturesLanguageController;
  private List<CatalogTexture>                 selectedTextures;
  private View                                 view;

  /**
   * Creates a controller of the textures library view.
   */
  public TexturesLibraryController(final TexturesLibrary texturesLibrary,
                                    TexturesLibraryUserPreferences preferences, 
                                    TexturesLanguageController texturesLanguageController,
                                    EditorViewFactory viewFactory,
                                    ContentManager  contentManager) {
    this.texturesLibrary = texturesLibrary;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.texturesLanguageController = texturesLanguageController;
    this.selectionListeners = new ArrayList<SelectionListener>();
    this.selectedTextures  = Collections.emptyList();
    
    this.texturesLibrary.addListener(new CollectionListener<CatalogTexture>() {
        public void collectionChanged(CollectionEvent<CatalogTexture> ev) {
          if (ev.getType() == CollectionEvent.Type.DELETE) {
            deselectTexture(ev.getItem());
          }
          texturesLibrary.setModified(true);
        }
      });
    this.texturesLanguageController.addPropertyChangeListener(TexturesLanguageController.Property.TEXTURES_LANGUAGE, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            if (ev.getOldValue() != null) {
              translateTexturesCategories((String)ev.getOldValue(), (String)ev.getNewValue());
            }
          }
        });
  }

  /**
   * Returns the view associated with this controller.
   */
  public View getView() {
    // Create view lazily only once it's needed
    if (this.view == null) {
      this.view = this.viewFactory.createTexturesLibraryView(this.texturesLibrary,  
          this.preferences, this, this.texturesLanguageController);
    }
    return this.view;
  }
  
  /**
   * Adds the selection <code>listener</code> in parameter to this controller.
   */
  public void addSelectionListener(SelectionListener listener) {
    this.selectionListeners.add(listener);
  }

  /**
   * Removes the selection <code>listener</code> in parameter from this controller.
   */
  public void removeSelectionListener(SelectionListener listener) {
    this.selectionListeners.remove(listener);
  }
  
  /**
   * Returns an unmodifiable list of the selected textures in library.
   */
  public List<CatalogTexture> getSelectedTextures() {
    return Collections.unmodifiableList(this.selectedTextures);
  }
  
  /**
   * Updates the selected textures in library and notifies listeners selection change.
   */
  public void setSelectedTextures(List<CatalogTexture> selectedTextures) {
    this.selectedTextures = new ArrayList<CatalogTexture>(selectedTextures);
    if (!this.selectionListeners.isEmpty()) {
      SelectionEvent selectionEvent = new SelectionEvent(this, getSelectedTextures());
      // Work on a copy of selectionListeners to ensure a listener 
      // can modify safely listeners list
      SelectionListener [] listeners = this.selectionListeners.
          toArray(new SelectionListener [this.selectionListeners.size()]);
      for (SelectionListener listener : listeners) {
        listener.selectionChanged(selectionEvent);
      }
    }
  }

  /**
   * Removes <code>texture</code> from selected textures.
   */
  private void deselectTexture(CatalogTexture texture) {
    for (int i = 0; i < this.selectedTextures.size(); i++) {
      CatalogTexture selectedTexture = this.selectedTextures.get(i);
      if (texture == selectedTexture) {
        List<CatalogTexture> selectedItems = 
            new ArrayList<CatalogTexture>(getSelectedTextures());
        selectedItems.remove(i);
        setSelectedTextures(selectedItems);
        break;
      }
    }
  }
  
  /**
   * Lets the user choose some texture images and imports the compatible textures to the library. 
   */
  public void importTextures() {
    String importTexturesTitle = this.preferences.getLocalizedString(TexturesLibraryController.class, "importTexturesTitle");
    String texturesNames = this.contentManager.showOpenDialog(null, importTexturesTitle, 
        ContentManager.ContentType.USER_DEFINED);
    if (texturesNames != null) {
      importTextures(texturesNames.split(File.pathSeparator));
    }
  }

  /**
   * Imports the given textures to the library. 
   */
  public void importTextures(final String [] texturesNames) {
    final AddedTexturesSelector addedTexturesListener = new AddedTexturesSelector();
    this.texturesLibrary.addListener(addedTexturesListener);
    Runnable postImportTask = new Runnable() {
        public void run() {
          addedTexturesListener.selectAddedTextures();
          texturesLibrary.removeListener(addedTexturesListener);
        }
      };
    new ImportTexturesController(this.texturesLibrary, texturesNames, postImportTask, 
        this.preferences, this.viewFactory, this.contentManager).executeTask(getView());
  }

  /**
   * Displays the dialog that helps to change the selected textures of textures. 
   */
  public void modifySelectedTextures() {
    if (this.selectedTextures.size() > 0) {
      AddedTexturesSelector addedTexturesListener = new AddedTexturesSelector();
      this.texturesLibrary.addListener(addedTexturesListener);
      new TexturesController(this.texturesLibrary, this.selectedTextures, this.preferences, 
          this.texturesLanguageController, this.viewFactory).displayView(getView());
      addedTexturesListener.selectAddedTextures();
      this.texturesLibrary.removeListener(addedTexturesListener);
    }
  }

  /**
   * Listener that keeps track of the textures added to library.
   */
  private class AddedTexturesSelector implements CollectionListener<CatalogTexture> {
    private List<CatalogTexture> addedTextures = new ArrayList<CatalogTexture>();

    public void collectionChanged(CollectionEvent<CatalogTexture> ev) {
      if (ev.getType() == CollectionEvent.Type.ADD) {
        this.addedTextures.add(ev.getItem());
      }
    }
    
    public void selectAddedTextures() {
      if (this.addedTextures.size() > 0) {
        setSelectedTextures(this.addedTextures);
      }
    }
  }

  /**
   * Deletes selected library textures. 
   */
  public void deleteSelectedTextures() {
    for (CatalogTexture texture : this.selectedTextures) {
      this.texturesLibrary.deleteTexture(texture);
    }
  }
  
  /**
   * Selects all textures of textures.
   */
  public void selectAll() {
    setSelectedTextures(this.texturesLibrary.getTextures());
  }

  /**
   * Translates the category name of each texture of textures from <code>language</code> 
   * to <code>translationLanguage</code> when possible.
   */
  private void translateTexturesCategories(String language,
                                           String translationLanguage) {
    if (translationLanguage.length() > 0) {
      List<TexturesCategory> categories = null;
      List<TexturesCategory> translatedCategories = null;
      final List<CatalogTexture> selectedTextures = new ArrayList<CatalogTexture>();
      for (CatalogTexture texture : this.texturesLibrary.getTextures()) {
        final boolean selected = this.selectedTextures.contains(texture);
        // If texture category wasn't translated yet
        if (this.texturesLibrary.getTextureLocalizedData(
                texture, translationLanguage, TexturesLibrary.TEXTURES_CATEGORY_PROPERTY) == null) {
          TexturesController texturesController = new TexturesController(this.texturesLibrary, 
              Arrays.asList(new CatalogTexture [] {texture}), 
              this.preferences, this.texturesLanguageController, this.viewFactory);
          if (categories == null) {
            categories = texturesController.getDefaultCategories(language);
            translatedCategories = texturesController.getDefaultCategories(translationLanguage);
          }
          String categoryName = (String)this.texturesLibrary.getTextureLocalizedData(texture, language, 
              TexturesLibrary.TEXTURES_CATEGORY_PROPERTY, texture.getCategory().getName());
          int i = categories.indexOf(new TexturesCategory(categoryName));
          if (i >= 0) {
            texturesController.setCategory(translatedCategories.get(i));
            // Retrieve the new modified texture that replaces the old one to add it to selection
            this.texturesLibrary.addListener(new CollectionListener<CatalogTexture>() {
                public void collectionChanged(CollectionEvent<CatalogTexture> ev) {
                  if (ev.getType() == CollectionEvent.Type.ADD) {
                    if (selected) {
                      selectedTextures.add(ev.getItem());
                    }
                    texturesLibrary.removeListener(this);
                  }
                }
              });
            texturesController.modifyTextures();
            continue;
          }
        } 
        if (selected) {
          selectedTextures.add(texture);
        }
      }    
      setSelectedTextures(selectedTextures);
    }
  }
}
