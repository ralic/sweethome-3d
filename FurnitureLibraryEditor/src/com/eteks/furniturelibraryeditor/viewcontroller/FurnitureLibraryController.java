/*
 * FurnitureLibraryController.java 15 mai 2006
 *
 * Furniture Library Editor, Copyright (c) 2006 Emmanuel PUYBARET / eTeks <info@eteks.com>
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * A MVC controller for the furniture library.
 * @author Emmanuel Puybaret
 */
public class FurnitureLibraryController implements Controller {
  private final FurnitureLibrary                furnitureLibrary;
  private final FurnitureLibraryUserPreferences preferences;
  private final ContentManager                  contentManager;
  private final EditorViewFactory               viewFactory;
  private final List<SelectionListener>         selectionListeners;
  
  private FurnitureLanguageController           furnitureLanguageController;
  private List<CatalogPieceOfFurniture>         selectedFurniture;
  private View                                  view;

  /**
   * Creates a controller of the furniture library view.
   */
  public FurnitureLibraryController(final FurnitureLibrary furnitureLibrary,
                                    FurnitureLibraryUserPreferences preferences, 
                                    FurnitureLanguageController furnitureLanguageController,
                                    EditorViewFactory viewFactory,
                                    ContentManager  contentManager) {
    this.furnitureLibrary = furnitureLibrary;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.furnitureLanguageController = furnitureLanguageController;
    this.selectionListeners = new ArrayList<SelectionListener>();
    this.selectedFurniture  = Collections.emptyList();
    
    this.furnitureLibrary.addListener(new CollectionListener<CatalogPieceOfFurniture>() {
        public void collectionChanged(CollectionEvent<CatalogPieceOfFurniture> ev) {
          if (ev.getType() == CollectionEvent.Type.DELETE) {
            deselectPieceOfFurniture(ev.getItem());
          }
          furnitureLibrary.setModified(true);
        }
      });
    this.furnitureLanguageController.addPropertyChangeListener(FurnitureLanguageController.Property.FURNITURE_LANGUAGE, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            if (ev.getOldValue() != null) {
              translateFurnitureCategories((String)ev.getOldValue(), (String)ev.getNewValue());
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
      this.view = this.viewFactory.createFurnitureLibraryView(this.furnitureLibrary,  
          this.preferences, this, this.furnitureLanguageController);
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
   * Returns an unmodifiable list of the selected furniture in library.
   */
  public List<CatalogPieceOfFurniture> getSelectedFurniture() {
    return Collections.unmodifiableList(this.selectedFurniture);
  }
  
  /**
   * Updates the selected furniture in library and notifies listeners selection change.
   */
  public void setSelectedFurniture(List<CatalogPieceOfFurniture> selectedFurniture) {
    this.selectedFurniture = new ArrayList<CatalogPieceOfFurniture>(selectedFurniture);
    if (!this.selectionListeners.isEmpty()) {
      SelectionEvent selectionEvent = new SelectionEvent(this, getSelectedFurniture());
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
   * Removes <code>piece</code> from selected furniture.
   */
  private void deselectPieceOfFurniture(CatalogPieceOfFurniture piece) {
    for (int i = 0; i < this.selectedFurniture.size(); i++) {
      CatalogPieceOfFurniture selectedPiece = this.selectedFurniture.get(i);
      if (piece == selectedPiece) {
        List<CatalogPieceOfFurniture> selectedItems = 
            new ArrayList<CatalogPieceOfFurniture>(getSelectedFurniture());
        selectedItems.remove(i);
        setSelectedFurniture(selectedItems);
        break;
      }
    }
  }
  
  /**
   * Lets the user choose some 3D models and imports the compatible furniture to the library. 
   */
  public void importFurniture() {
    String importFurnitureTitle = this.preferences.getLocalizedString(FurnitureLibraryController.class, "importFurnitureTitle");
    String furnitureNames = this.contentManager.showOpenDialog(null, importFurnitureTitle, 
        ContentManager.ContentType.USER_DEFINED);
    if (furnitureNames != null) {
      importFurniture(furnitureNames.split(File.pathSeparator));
    }
  }

  /**
   * Imports the given 3D models to the library. 
   */
  public void importFurniture(final String [] furnitureNames) {
    final AddedFurnitureSelector addedFurnitureListener = new AddedFurnitureSelector();
    this.furnitureLibrary.addListener(addedFurnitureListener);
    Runnable postImportTask = new Runnable() {
        public void run() {
          addedFurnitureListener.selectAddedFurniture();
          furnitureLibrary.removeListener(addedFurnitureListener);
        }
      };
    new ImportFurnitureController(this.furnitureLibrary, furnitureNames, postImportTask, 
        this.preferences, this.viewFactory, this.contentManager).executeTask(getView());
  }

  /**
   * Displays the dialog that helps to change the selected pieces of furniture. 
   */
  public void modifySelectedFurniture() {
    if (this.selectedFurniture.size() > 0) {
      AddedFurnitureSelector addedFurnitureListener = new AddedFurnitureSelector();
      this.furnitureLibrary.addListener(addedFurnitureListener);
      new FurnitureController(this.furnitureLibrary, this.selectedFurniture, this.preferences, 
          this.furnitureLanguageController, this.viewFactory, this.contentManager).displayView(getView());
      addedFurnitureListener.selectAddedFurniture();
      this.furnitureLibrary.removeListener(addedFurnitureListener);
    }
  }

  /**
   * Listener that keeps track of the furniture added to library.
   */
  private class AddedFurnitureSelector implements CollectionListener<CatalogPieceOfFurniture> {
    private List<CatalogPieceOfFurniture> addedFurniture = new ArrayList<CatalogPieceOfFurniture>();

    public void collectionChanged(CollectionEvent<CatalogPieceOfFurniture> ev) {
      if (ev.getType() == CollectionEvent.Type.ADD) {
        this.addedFurniture.add(ev.getItem());
      }
    }
    
    public void selectAddedFurniture() {
      if (this.addedFurniture.size() > 0) {
        setSelectedFurniture(this.addedFurniture);
      }
    }
  }

  /**
   * Deletes selected library furniture. 
   */
  public void deleteSelectedFurniture() {
    for (CatalogPieceOfFurniture piece : this.selectedFurniture) {
      this.furnitureLibrary.deletePieceOfFurniture(piece);
    }
  }
  
  /**
   * Selects all pieces of furniture.
   */
  public void selectAll() {
    setSelectedFurniture(this.furnitureLibrary.getFurniture());
  }

  /**
   * Translates the category name of each piece of furniture from <code>language</code> 
   * to <code>translationLanguage</code> when possible.
   */
  private void translateFurnitureCategories(String language,
                                            String translationLanguage) {
    if (translationLanguage.length() > 0) {
      List<FurnitureCategory> categories = null;
      List<FurnitureCategory> translatedCategories = null;
      final List<CatalogPieceOfFurniture> selectedFurniture = new ArrayList<CatalogPieceOfFurniture>();
      for (CatalogPieceOfFurniture piece : this.furnitureLibrary.getFurniture()) {
        final boolean selected = this.selectedFurniture.contains(piece);
        // If piece category wasn't translated yet
        if (this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
                piece, translationLanguage, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY) == null) {
          FurnitureController furnitureController = new FurnitureController(this.furnitureLibrary, 
              Arrays.asList(new CatalogPieceOfFurniture [] {piece}), 
              this.preferences, this.furnitureLanguageController, this.viewFactory, this.contentManager);
          if (categories == null) {
            categories = furnitureController.getDefaultCategories(language);
            translatedCategories = furnitureController.getDefaultCategories(translationLanguage);
          }
          String categoryName = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(piece, language, 
              FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, piece.getCategory().getName());
          int i = categories.indexOf(new FurnitureCategory(categoryName));
          if (i >= 0) {
            furnitureController.setCategory(translatedCategories.get(i));
            // Retrieve the new modified piece that replaces the old one to add it to selection
            this.furnitureLibrary.addListener(new CollectionListener<CatalogPieceOfFurniture>() {
                public void collectionChanged(CollectionEvent<CatalogPieceOfFurniture> ev) {
                  if (ev.getType() == CollectionEvent.Type.ADD) {
                    if (selected) {
                      selectedFurniture.add(ev.getItem());
                    }
                    furnitureLibrary.removeListener(this);
                  }
                }
              });
            furnitureController.modifyFurniture();
            continue;
          }
        } 
        if (selected) {
          selectedFurniture.add(piece);
        }
      }    
      setSelectedFurniture(selectedFurniture);
    }
  }
}
