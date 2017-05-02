/*
 * FurnitureController.java 
 *
 * Furniture Library Editor, Copyright (c) 2009 Emmanuel PUYBARET / eTeks <info@eteks.com>
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
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.sweethome3d.model.CatalogDoorOrWindow;
import com.eteks.sweethome3d.model.CatalogLight;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.FurnitureCatalog;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Sash;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * A MVC controller for home furniture view.
 * @author Emmanuel Puybaret
 */
public class FurnitureController implements Controller {
  /**
   * The properties that may be edited by the view associated to this controller. 
   */
  public enum Property {ID, NAME, DESCRIPTION, INFORMATION, TAGS, GRADE, CREATION_DATE, CATEGORY, MODEL, ICON, 
      WIDTH, DEPTH,  HEIGHT, ELEVATION, MOVABLE, RESIZABLE, DEFORMABLE, TEXTURABLE, 
      DOOR_OR_WINDOW, DOOR_OR_WINDOW_CUT_OUT_SHAPE, STAIRCASE, STAIRCASE_CUT_OUT_SHAPE, 
      MODEL_ROTATION, CREATOR, PROPORTIONAL, BACK_FACE_SHOWN, PRICE, VALUE_ADDED_TAX_PERCENTAGE}
  
  private static final Map<String, Property> PROPERTIES_MAP = new HashMap<String, Property>();
  
  static {
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_ID_PROPERTY, Property.ID);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_NAME_PROPERTY, Property.NAME);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY, Property.DESCRIPTION);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY, Property.INFORMATION);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_TAGS_PROPERTY, Property.TAGS);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_GRADE_PROPERTY, Property.GRADE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_CREATION_DATE_PROPERTY, Property.CREATION_DATE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, Property.CATEGORY);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_CREATOR_PROPERTY, Property.CREATOR);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_MODEL_PROPERTY, Property.MODEL);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_ICON_PROPERTY, Property.ICON);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_WIDTH_PROPERTY, Property.WIDTH);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_DEPTH_PROPERTY, Property.DEPTH);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_HEIGHT_PROPERTY, Property.HEIGHT);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_ELEVATION_PROPERTY, Property.ELEVATION);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_MOVABLE_PROPERTY, Property.MOVABLE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_DEFORMABLE_PROPERTY, Property.DEFORMABLE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_RESIZABLE_PROPERTY, Property.RESIZABLE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_TEXTURABLE_PROPERTY, Property.TEXTURABLE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_PROPERTY, Property.DOOR_OR_WINDOW);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_CUT_OUT_SHAPE_PROPERTY, Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_STAIRCASE_CUT_OUT_SHAPE_PROPERTY, Property.STAIRCASE_CUT_OUT_SHAPE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_MODEL_ROTATION_PROPERTY, Property.MODEL_ROTATION);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_PRICE_PROPERTY, Property.PRICE);
    PROPERTIES_MAP.put(FurnitureLibrary.FURNITURE_VALUE_ADDED_TAX_PERCENTAGE_PROPERTY, Property.VALUE_ADDED_TAX_PERCENTAGE);
  }
  
  private static final String DEFAULT_CUT_OUT_SHAPE = "M0,0 v1 h1 v-1 z";

  private final FurnitureLibrary                furnitureLibrary;
  private final List<CatalogPieceOfFurniture>   modifiedFurniture;
  private final Set<Property>                   editableProperties;
  private final FurnitureLibraryUserPreferences preferences;
  private final FurnitureLanguageController     furnitureLanguageController;
  private final EditorViewFactory               viewFactory;
  private final ContentManager                  contentManager;
  private final PropertyChangeSupport           propertyChangeSupport;
  private DialogView                            homeFurnitureView;

  private String            id;
  private String            name;
  private String            description;
  private String            information;
  private String []         tags;
  private Long              creationDate;
  private Float             grade;
  private FurnitureCategory category;
  private Content           model;
  private Content           icon;
  private Float             width;
  private Float             proportionalWidth;
  private Float             depth;
  private Float             proportionalDepth;
  private Float             height;
  private Float             proportionalHeight;
  private Float             elevation;
  private Boolean           movable;
  private Boolean           doorOrWindow;
  private String            doorOrWindowCutOutShape;
  private Boolean           staircase;
  private String            staircaseCutOutShape;
  private Boolean           backFaceShown;
  private Boolean           resizable;
  private Boolean           deformable;
  private Boolean           texturable;
  private float [][]        modelRotation;
  private String            creator;
  private BigDecimal        price;
  private BigDecimal        valueAddedTaxPercentage;
  
  private boolean           proportional;

  private PropertyChangeListener widthChangeListener;
  private PropertyChangeListener depthChangeListener;
  private PropertyChangeListener heightChangeListener;
  
  /**
   * Creates the controller of catalog furniture view.
   */
  public FurnitureController(FurnitureLibrary furnitureLibrary, 
                             List<CatalogPieceOfFurniture> modifiedFurniture,
                             FurnitureLibraryUserPreferences preferences, 
                             FurnitureLanguageController furnitureLanguageController,
                             EditorViewFactory viewFactory,
                             ContentManager    contentManager) {
    this.furnitureLibrary = furnitureLibrary;
    this.modifiedFurniture = modifiedFurniture;
    this.preferences = preferences;
    this.furnitureLanguageController = furnitureLanguageController;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    this.editableProperties = new HashSet<Property>();
    for (String editedProperty : preferences.getEditedProperties()) {
      this.editableProperties.add(PROPERTIES_MAP.get(editedProperty));
    }

    setProportional(modifiedFurniture.size() == 1);
    updateProperties();
    addListeners();
  }

  /**
   * Returns the view associated with this controller.
   */
  public DialogView getView() {
    // Create view lazily only once it's needed
    if (this.homeFurnitureView == null) {
      this.homeFurnitureView = this.viewFactory.createFurnitureView(this.preferences, this); 
    }
    return this.homeFurnitureView;
  }
  
  /**
   * Displays the view controlled by this controller.
   */
  public void displayView(View parentView) {
    getView().displayView(parentView);
  }

  /**
   * Adds the property change <code>listener</code> in parameter to this controller.
   */
  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  /**
   * Removes the property change <code>listener</code> in parameter from this controller.
   */
  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  /**
   * Adds listeners to automatically update lengths when proportional check box is checked.
   */
  private void addListeners() {
    this.widthChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            removePropertyChangeListener(Property.DEPTH, depthChangeListener);
            removePropertyChangeListener(Property.HEIGHT, heightChangeListener);
            
            // If proportions should be kept, update depth and height
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setDepth(proportionalDepth * ratio, true); 
            setHeight(proportionalHeight * ratio, true);
            
            addPropertyChangeListener(Property.DEPTH, depthChangeListener);
            addPropertyChangeListener(Property.HEIGHT, heightChangeListener);
          }
        }
      };
    this.depthChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            removePropertyChangeListener(Property.WIDTH, widthChangeListener);
            removePropertyChangeListener(Property.HEIGHT, heightChangeListener);
            
            // If proportions should be kept, update width and height
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setWidth(proportionalWidth * ratio, true); 
            setHeight(proportionalHeight * ratio, true);
            
            addPropertyChangeListener(Property.WIDTH, widthChangeListener);
            addPropertyChangeListener(Property.HEIGHT, heightChangeListener);
          }
        }
      };
    this.heightChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            removePropertyChangeListener(Property.WIDTH, widthChangeListener);
            removePropertyChangeListener(Property.DEPTH, depthChangeListener);
            
            // If proportions should be kept, update width and depth
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setWidth(proportionalWidth * ratio, true); 
            setDepth(proportionalDepth * ratio, true);
            
            addPropertyChangeListener(Property.WIDTH, widthChangeListener);
            addPropertyChangeListener(Property.DEPTH, depthChangeListener);
          }
        }
      };

    addPropertyChangeListener(Property.WIDTH, this.widthChangeListener);
    addPropertyChangeListener(Property.DEPTH, this.depthChangeListener);
    addPropertyChangeListener(Property.HEIGHT, this.heightChangeListener);
  }

  /**
   * Returns the content manager of this controller.
   */
  public ContentManager getContentManager() {
    return this.contentManager;
  }
  
  /**
   * Returns <code>true</code> if the given <code>property</code> is editable.
   * Depending on whether a property is editable or not, the view associated to this controller
   * may render it differently.
   */
  public boolean isPropertyEditable(Property property) {
    if (this.modifiedFurniture.size() == 1) {
      return this.editableProperties.contains(property);
    } else {
      return this.editableProperties.contains(property)
          && property != Property.ID
          && property != Property.MODEL
          && property != Property.ICON
          && property != Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE;
    }
  }
  
  /**
   * Updates edited properties from selected furniture in the home edited by this controller.
   */
  protected void updateProperties() {
    if (this.modifiedFurniture.isEmpty()) {
      setId(null); // Nothing to edit
      setName(null);
      setDescription(null);
      setCategory(null);
      setModel((Content)null);
      setIcon(null);
      setWidth(null);
      setDepth(null);
      setHeight(null);
      setElevation(null);
      setMovable(null);
      setDoorOrWindow(null);
      setDoorOrWindowCutOutShape(null);
      setStaircase(null);
      setStaircaseCutOutShape(null);
      setBackFaceShown(null);
      setResizable(null);
      setDeformable(null);
      setTexturable(null);
      setModelRotation(null);
      setCreator(null);
      setPrice(null);
      setValueAddedTaxPercentage(null);
      this.editableProperties.remove(Property.PROPORTIONAL);
    } else {
      CatalogPieceOfFurniture firstPiece = this.modifiedFurniture.get(0);
      
      setBackFaceShown(false);
      if (this.modifiedFurniture.size() == 1) {
        setIcon(firstPiece.getIcon());
        setModel(firstPiece.getModel());
        this.editableProperties.add(Property.BACK_FACE_SHOWN);
      } else {
        setIcon(null);
        setModel((Content)null);
        this.editableProperties.remove(Property.BACK_FACE_SHOWN);
      }
      
      // Search the common properties among selected furniture
      String id = firstPiece.getId();
      if (id != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!id.equals(this.modifiedFurniture.get(i).getId())) {
            id = null;
            break;
          }
        }
      }
      setId(id);
      
      String furnitureLanguage = this.furnitureLanguageController.getFurnitureLangauge();
      String name = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          firstPiece, furnitureLanguage, FurnitureLibrary.FURNITURE_NAME_PROPERTY, firstPiece.getName());
      if (name != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!name.equals(this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, furnitureLanguage, FurnitureLibrary.FURNITURE_NAME_PROPERTY, piece.getName()))) {
            name = null;
            break;
          }
        }
      }
      setName(name);
      
      String description = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          firstPiece, furnitureLanguage, FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY, firstPiece.getDescription());
      if (description != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!description.equals(this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, furnitureLanguage, FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY, piece.getDescription()))) {
            description = null;
            break;
          }
        }
      }
      setDescription(description);

      String information = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          firstPiece, furnitureLanguage, FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY, firstPiece.getInformation());
      if (information != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!information.equals(this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, furnitureLanguage, FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY, piece.getInformation()))) {
            information = null;
            break;
          }
        }
      }
      setInformation(information);

      String [] tags = (String [])this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          firstPiece, furnitureLanguage, FurnitureLibrary.FURNITURE_TAGS_PROPERTY, firstPiece.getTags());
      if (tags != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!Arrays.equals(tags, (String [])this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, furnitureLanguage, FurnitureLibrary.FURNITURE_TAGS_PROPERTY, piece.getTags()))) {
            tags = null;
            break;
          }
        }
      }
      setTags(tags);

      Long creationDate = firstPiece.getCreationDate();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
        if (creationDate == null && piece.getCreationDate() != null
            || creationDate != null && !creationDate.equals(piece.getCreationDate())) {
          creationDate = null;
          break;
        }
      }
      setCreationDate(creationDate);

      Float grade = firstPiece.getGrade();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
        if (grade == null && piece.getGrade() != null
            || grade != null && !grade.equals(piece.getGrade())) {
          grade = null;
          break;
        }
      }
      setGrade(grade);

      FurnitureCategory category = firstPiece.getCategory();
      String categoryName = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          firstPiece, furnitureLanguage, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, category.getName());
      if (category != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!categoryName.equals(this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
              piece, furnitureLanguage, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, piece.getCategory().getName()))) {
            category = null;
            break;
          }
        }
      }
      setCategory(category == null ? null : new FurnitureCategory(categoryName));

      Float width = firstPiece.getWidth();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (width.floatValue() != this.modifiedFurniture.get(i).getWidth()) {
          width = null;
          break;
        }
      }
      setWidth(width);

      Float depth = firstPiece.getDepth();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (depth.floatValue() != this.modifiedFurniture.get(i).getDepth()) {
          depth = null;
          break;
        }
      }
      setDepth(depth);

      Float height = firstPiece.getHeight();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (height.floatValue() != this.modifiedFurniture.get(i).getHeight()) {
          height = null;
          break;
        }
      }
      setHeight(height);

      Float elevation = firstPiece.getElevation();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (elevation.floatValue() != this.modifiedFurniture.get(i).getElevation()) {
          elevation = null;
          break;
        }
      }
      setElevation(elevation);

      Boolean movable = firstPiece.isMovable();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (movable != this.modifiedFurniture.get(i).isMovable()) {
          movable = null;
          break;
        }
      }
      setMovable(movable);           

      Boolean resizable = firstPiece.isResizable();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (resizable.booleanValue() != this.modifiedFurniture.get(i).isResizable()) {
          resizable = null;
          break;
        }
      }
      setResizable(resizable);

      Boolean deformable = firstPiece.isDeformable();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (deformable.booleanValue() != this.modifiedFurniture.get(i).isDeformable()) {
          deformable = null;
          break;
        }
      }
      setDeformable(deformable);

      Boolean texturable = firstPiece.isTexturable();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (texturable.booleanValue() != this.modifiedFurniture.get(i).isTexturable()) {
          texturable = null;
          break;
        }
      }
      setTexturable(texturable);
      
      Boolean doorOrWindow = firstPiece.isDoorOrWindow();
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (doorOrWindow != this.modifiedFurniture.get(i).isDoorOrWindow()) {
          doorOrWindow = null;
          break;
        }
      }
      setDoorOrWindow(doorOrWindow);           

      String doorOrWindowCutOutShape = firstPiece instanceof CatalogDoorOrWindow
         ? ((CatalogDoorOrWindow)firstPiece).getCutOutShape()
         : null;
      if (doorOrWindowCutOutShape != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (!(piece instanceof CatalogDoorOrWindow)
              || !doorOrWindowCutOutShape.equals(((CatalogDoorOrWindow)piece).getCutOutShape())) {
            doorOrWindowCutOutShape = null;
            break;
          }
        }
      }
      setDoorOrWindowCutOutShape(doorOrWindowCutOutShape);

      Boolean staircase = firstPiece.getStaircaseCutOutShape() != null;
      for (int i = 1; i < this.modifiedFurniture.size(); i++) {
        if (staircase != (this.modifiedFurniture.get(i).getStaircaseCutOutShape() != null)) {
          staircase = null;
          break;
        }
      }
      setStaircase(staircase);           

      if (Boolean.TRUE.equals(staircase)) {
        String staircaseCutOutShape = firstPiece.getStaircaseCutOutShape();
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          CatalogPieceOfFurniture piece = this.modifiedFurniture.get(i);
          if (staircaseCutOutShape == null && piece.getStaircaseCutOutShape() != null
              || staircaseCutOutShape != null && !staircaseCutOutShape.equals(piece.getStaircaseCutOutShape())) {
            staircaseCutOutShape = null;
            break;
          }
        }
        setStaircaseCutOutShape(staircaseCutOutShape);
      } else if (Boolean.FALSE.equals(staircase)) {
        setStaircaseCutOutShape(DEFAULT_CUT_OUT_SHAPE);
      } else {
        setStaircaseCutOutShape(null);
      }

      float [][] modelRotation = firstPiece.getModelRotation();
      if (modelRotation != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!Arrays.deepEquals(modelRotation, this.modifiedFurniture.get(i).getModelRotation())) {
            modelRotation = null;
            break;
          }
        }
      }
      setModelRotation(modelRotation);

      String creator = firstPiece.getCreator();
      if (creator != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!creator.equals(this.modifiedFurniture.get(i).getCreator())) {
            creator = null;
            break;
          }
        }
      }
      setCreator(creator);

      BigDecimal price = firstPiece.getPrice();
      if (price != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!price.equals(this.modifiedFurniture.get(i).getPrice())) {
            price = null;
            break;
          }
        }
      }
      setPrice(price);

      BigDecimal valueAddedTaxPercentage = firstPiece.getValueAddedTaxPercentage();
      if (valueAddedTaxPercentage != null) {
        for (int i = 1; i < this.modifiedFurniture.size(); i++) {
          if (!valueAddedTaxPercentage.equals(this.modifiedFurniture.get(i).getValueAddedTaxPercentage())) {
            valueAddedTaxPercentage = null;
            break;
          }
        }
      }
      setValueAddedTaxPercentage(valueAddedTaxPercentage);
      
      if (this.editableProperties.contains(Property.WIDTH)
          && this.editableProperties.contains(Property.DEPTH)
          && this.editableProperties.contains(Property.HEIGHT)
          && getWidth() != null
          && getDepth() != null
          && getHeight() != null) {
        this.editableProperties.add(Property.PROPORTIONAL);
      }
    }
  }  
  
  /**
   * Sets the edited id.
   */
  public void setId(String id) {
    if (id != this.id) {
      String oldId = this.id;
      this.id = id;
      this.propertyChangeSupport.firePropertyChange(Property.ID.name(), oldId, id);
    }
  }

  /**
   * Returns the edited id.
   */
  public String getId() {
    return this.id;
  }
  
  /**
   * Sets the edited name.
   */
  public void setName(String name) {
    if (name != this.name) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  /**
   * Returns the edited name.
   */
  public String getName() {
    return this.name;
  }
  
  /**
   * Sets the edited description.
   */
  public void setDescription(String description) {
    if (description != this.description) {
      String oldDescription = this.description;
      this.description = description;
      this.propertyChangeSupport.firePropertyChange(Property.DESCRIPTION.name(), oldDescription, description);
    }
  }

  /**
   * Returns the edited description.
   */
  public String getDescription() {
    return this.description;
  }
  
  /**
   * Sets the edited information.
   */
  public void setInformation(String information) {
    if (information != this.information) {
      String oldInformation = this.information;
      this.information = information;
      this.propertyChangeSupport.firePropertyChange(Property.INFORMATION.name(), oldInformation, information);
    }
  }

  /**
   * Returns the edited information.
   */
  public String getInformation() {
    return this.information;
  }
  
  /**
   * Sets the edited tags.
   */
  public void setTags(String [] tags) {
    if (tags != this.tags) {
      String [] oldTags = this.tags;
      this.tags = tags;
      this.propertyChangeSupport.firePropertyChange(Property.TAGS.name(), oldTags, tags);
    }
  }

  /**
   * Returns the edited tags.
   */
  public String [] getTags() {
    return this.tags;
  }
  
  /**
   * Sets the edited creation date.
   */
  public void setCreationDate(Long creationDate) {
    if (creationDate != this.creationDate) {
      Long oldCreationDate = this.creationDate;
      this.creationDate = creationDate;
      this.propertyChangeSupport.firePropertyChange(Property.INFORMATION.name(), oldCreationDate, creationDate);
    }
  }

  /**
   * Returns the edited creation date.
   */
  public Long getCreationDate() {
    return this.creationDate;
  }
  
  /**
   * Sets the edited grade.
   */
  public void setGrade(Float grade) {
    if (grade != this.grade) {
      Float oldGrade = this.grade;
      this.grade = grade;
      this.propertyChangeSupport.firePropertyChange(Property.GRADE.name(), oldGrade, grade);
    }
  }

  /**
   * Returns the edited grade.
   */
  public Float getGrade() {
    return this.grade;
  }
  
  /**
   * Sets the edited category.
   */
  public void setCategory(FurnitureCategory category) {
    if (category != this.category) {
      FurnitureCategory oldCategory = this.category;
      this.category = category;
      this.propertyChangeSupport.firePropertyChange(Property.CATEGORY.name(), oldCategory, category);
    }
  }

  /**
   * Returns the edited category.
   */
  public FurnitureCategory getCategory() {
    return this.category;
  }
  
  /**
   * Returns the list of available categories in furniture library sorted in alphabetical order.
   */
  public List<FurnitureCategory> getAvailableCategories() {
    String furnitureLanguage = this.furnitureLanguageController.getFurnitureLangauge();
    Set<FurnitureCategory> categories = new TreeSet<FurnitureCategory>(getDefaultCategories(furnitureLanguage));
    for (CatalogPieceOfFurniture piece : this.furnitureLibrary.getFurniture()) {
      String categoryName = (String)this.furnitureLibrary.getPieceOfFurnitureLocalizedData(
          piece, furnitureLanguage, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, piece.getCategory().getName());
      categories.add(new FurnitureCategory(categoryName));
    }
    return new ArrayList<FurnitureCategory>(categories);
  }

  /**
   * Returns the list of available categories in furniture library in the given language.
   */
  public List<FurnitureCategory> getDefaultCategories(String language) {
    Locale locale;
    int underscoreIndex = language.indexOf('_');
    if (underscoreIndex != -1) {
      locale = new Locale(language.substring(0, underscoreIndex), language.substring(underscoreIndex + 1));
    } else {
      locale = new Locale(language.length() == 0
          ? this.preferences.getFurnitureDefaultLanguage()
          : language);
    }
    ResourceBundle resource = ResourceBundle.getBundle(
        "com.eteks.furniturelibraryeditor.viewcontroller.DefaultCategories", locale);
    List<FurnitureCategory> categories = new ArrayList<FurnitureCategory>();
    int i = 1;
    try {
      while (true) {
        categories.add(new FurnitureCategory(resource.getString("defaultFurnitureCategory#" + i++)));
      }
    } catch (MissingResourceException ex) {
      // Stop searching for next category
    }
    return categories;
  }

  /**
   * Sets the edited icon.
   */
  public void setIcon(Content icon) {
    if (icon != this.icon) {
      Content oldIcon = this.icon;
      this.icon = icon;
      this.propertyChangeSupport.firePropertyChange(Property.ICON.name(), oldIcon, icon);
    }
  }

  /**
   * Returns the edited icon.
   */
  public Content getIcon() {
    return this.icon;
  }

  /**
   * Sets the edited model.
   */
  public void setModel(Content model) {
    if (model != this.model) {
      Content oldModel = this.model;
      this.model = model;
      this.propertyChangeSupport.firePropertyChange(Property.MODEL.name(), oldModel, model);
    }
  }
  
  /**
   * Returns the model of the edited piece of furniture.
   */
  public Content getModel() {
    return this.model;
  }

  /**
   * Reads the 3D model from the given parameter and updates the model if it's valid.
   */
  public void setModel(String modelName) {
    final FurnitureLibrary furnitureLibrary = new FurnitureLibrary();
    Runnable postImportTask = new Runnable() {
        public void run() {
          List<CatalogPieceOfFurniture> furniture = furnitureLibrary.getFurniture();
          if (!furniture.isEmpty()) {
            CatalogPieceOfFurniture piece = furniture.get(0);
            setModel(piece.getModel());
            setModelRotation(piece.getModelRotation());
            // Reset object size 
            boolean proportional = isProportional();
            setProportional(false);
            setWidth(piece.getWidth());
            setDepth(piece.getDepth());
            setHeight(piece.getHeight());
            setProportional(proportional);
          }
        }
      };
    new ImportFurnitureController(furnitureLibrary, new String [] {modelName}, postImportTask, 
        this.preferences, this.viewFactory, this.contentManager).executeTask(getView());
  }

  /**
   * Sets the edited width.
   */
  public void setWidth(Float width) {
    setWidth(width, false);
  }

  private void setWidth(Float width, boolean keepProportionalWidthUnchanged) {
    Float adjustedWidth = width != null 
        ? Math.max(width, 0.001f)
        : null;
    if (adjustedWidth == width 
        || adjustedWidth != null && adjustedWidth.equals(width)
        || !keepProportionalWidthUnchanged) {
      this.proportionalWidth = width;
    }
    if (adjustedWidth == null && this.width != null
        || adjustedWidth != null && !adjustedWidth.equals(this.width)) {
      Float oldWidth = this.width;
      this.width = adjustedWidth;
      this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, adjustedWidth);
    }
  }

  /**
   * Returns the edited width.
   */
  public Float getWidth() {
    return this.width;
  }
  
  /**
   * Sets the edited depth.
   */
  public void setDepth(Float depth) {
    setDepth(depth, false);
  }

  private void setDepth(Float depth, boolean keepProportionalDepthUnchanged) {
    Float adjustedDepth = depth != null 
        ? Math.max(depth, 0.001f)
        : null;

    if (adjustedDepth == depth 
        || adjustedDepth != null && adjustedDepth.equals(depth)
        || !keepProportionalDepthUnchanged) {
      this.proportionalDepth = depth;
    }
    if (adjustedDepth == null && this.depth != null
        || adjustedDepth != null && !adjustedDepth.equals(this.depth)) {
      Float oldDepth = this.depth;
      this.depth = adjustedDepth;
      this.propertyChangeSupport.firePropertyChange(Property.DEPTH.name(), oldDepth, adjustedDepth);
    }
  }

  /**
   * Returns the edited depth.
   */
  public Float getDepth() {
    return this.depth;
  }
  
  /**
   * Sets the edited height.
   */
  public void setHeight(Float height) {
    setHeight(height, false);
  }

  private void setHeight(Float height, boolean keepProportionalHeightUnchanged) {
    Float adjustedHeight = height != null 
        ? Math.max(height, 0.001f)
        : null;
    if (adjustedHeight == height 
        || adjustedHeight != null && adjustedHeight.equals(height)
        || !keepProportionalHeightUnchanged) {
      this.proportionalHeight = height;
    }
    if (adjustedHeight == null && this.height != null
        || adjustedHeight != null && !adjustedHeight.equals(this.height)) {
      Float oldHeight = this.height;
      this.height = adjustedHeight;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, adjustedHeight);
    }
  }

  /**
   * Returns the edited height.
   */
  public Float getHeight() {
    return this.height;
  }
  
  /**
   * Returns <code>true</code> if piece proportions should be kept.
   */
  public boolean isProportional() {
    return this.proportional;
  }
  
  /**
   * Sets whether piece proportions should be kept or not.
   */
  public void setProportional(boolean proportional) {
    if (proportional != this.proportional) {
      this.proportional = proportional;
      this.propertyChangeSupport.firePropertyChange(Property.PROPORTIONAL.name(), !proportional, proportional);
    }
  }

  /**
   * Multiplies width, depth and height by the given <code>factor</code>.
   */
  public void multiplySize(float factor) {
    if (isProportional()) {
      setWidth(getWidth() * factor);
    } else {
      setProportional(true);
      setWidth(getWidth() * factor);
      setProportional(false);
    }
  }
  
  /**
   * Sets the edited elevation.
   */
  public void setElevation(Float elevation) {
    if (elevation != this.elevation) {
      Float oldElevation = this.elevation;
      this.elevation = elevation;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION.name(), oldElevation, elevation);
    }
  }

  /**
   * Returns the edited elevation.
   */
  public Float getElevation() {
    return this.elevation;
  }
  
  /**
   * Sets whether furniture is movable or not.
   */
  public void setMovable(Boolean movable) {
    if (movable != this.movable) {
      Boolean oldVisible = this.movable;
      this.movable = movable;
      this.propertyChangeSupport.firePropertyChange(Property.MOVABLE.name(), oldVisible, movable);
    }
  }

  /**
   * Returns whether furniture is movable or not.
   */
  public Boolean getMovable() {
    return this.movable;
  }

  /**
   * Sets whether furniture model is a door or a window.
   */
  public void setDoorOrWindow(Boolean doorOrWindow) {
    if (doorOrWindow != this.doorOrWindow) {
      Boolean oldDoorOrWindow = this.doorOrWindow;
      this.doorOrWindow = doorOrWindow;
      this.propertyChangeSupport.firePropertyChange(Property.DOOR_OR_WINDOW.name(), oldDoorOrWindow, doorOrWindow);
    }
  }

  /**
   * Returns whether furniture model is a door or a window.
   */
  public Boolean getDoorOrWindow() {
    return this.doorOrWindow;
  }
   
  /**
   * Sets the shape used to cut out walls at its intersection with a door or a window.
   */
  public void setDoorOrWindowCutOutShape(String doorOrWindowCutOutShape) {
    if (doorOrWindowCutOutShape != this.doorOrWindowCutOutShape
        || (doorOrWindowCutOutShape != null && !doorOrWindowCutOutShape.equals(this.doorOrWindowCutOutShape))) {
      String oldDoorOrWindowCutOutShape = this.doorOrWindowCutOutShape;
      this.doorOrWindowCutOutShape = doorOrWindowCutOutShape;
      this.propertyChangeSupport.firePropertyChange(Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE.name(), oldDoorOrWindowCutOutShape, doorOrWindowCutOutShape);
    }
  }
  
  /**
   * Returns the shape used to cut out walls at its intersection with a door or a window.
   */
  public String getDoorOrWindowCutOutShape() {
    return this.doorOrWindowCutOutShape;
  }

  /**
   * Sets whether furniture model is a staircase.
   */
  public void setStaircase(Boolean staircase) {
    if (staircase != this.staircase) {
      Boolean oldStaircase = this.staircase;
      this.staircase = staircase;
      this.propertyChangeSupport.firePropertyChange(Property.STAIRCASE.name(), oldStaircase, staircase);
    }
  }

  /**
   * Returns whether furniture model is a staircase.
   */
  public Boolean getStaircase() {
    return this.staircase;
  }
   
  /**
   * Sets the shape used to cut out upper levels at its intersection with a staircase.
   */
  public void setStaircaseCutOutShape(String staircaseCutOutShape) {
    if (staircaseCutOutShape != this.staircaseCutOutShape
        || (staircaseCutOutShape != null && !staircaseCutOutShape.equals(this.staircaseCutOutShape))) {
      String oldStaircaseCutOutShape = this.staircaseCutOutShape;
      this.staircaseCutOutShape = staircaseCutOutShape;
      this.propertyChangeSupport.firePropertyChange(Property.STAIRCASE_CUT_OUT_SHAPE.name(), oldStaircaseCutOutShape, staircaseCutOutShape);
    }
  }
  
  /**
   * Returns the shape used to cut out upper levels at its intersection with a staircase.
   */
  public String getStaircaseCutOutShape() {
    return this.staircaseCutOutShape;
  }

  /**
   * Sets whether the back face of the furniture model should be shown or not.
   */
  public void setBackFaceShown(Boolean backFaceShown) {
    if (backFaceShown != this.backFaceShown) {
      Boolean oldBackFaceShown = this.backFaceShown;
      this.backFaceShown = backFaceShown;
      this.propertyChangeSupport.firePropertyChange(Property.BACK_FACE_SHOWN.name(), oldBackFaceShown, backFaceShown);
    }
  }
  
  /**
   * Returns whether the back face of the furniture model should be shown or not.
   */
  public Boolean getBackFaceShown() {
    return this.backFaceShown;
  }
  
  /**
   * Sets whether furniture model can be resized or not.
   */
  public void setResizable(Boolean resizable) {
    if (resizable != this.resizable) {
      Boolean oldResizable = this.resizable;
      this.resizable = resizable;
      this.propertyChangeSupport.firePropertyChange(Property.RESIZABLE.name(), oldResizable, resizable);
    }
  }
  
  /**
   * Returns whether furniture model can be resized or not.
   */
  public Boolean getResizable() {
    return this.resizable;
  }
  
  /**
   * Sets whether furniture model can be deformed or not.
   */
  public void setDeformable(Boolean deformable) {
    if (deformable != this.deformable) {
      Boolean oldDeformable = this.deformable;
      this.deformable = deformable;
      this.propertyChangeSupport.firePropertyChange(Property.DEFORMABLE.name(), oldDeformable, deformable);
    }
  }
  
  /**
   * Returns whether furniture model can be deformed or not.
   */
  public Boolean getDeformable() {
    return this.deformable;
  }
  
  /**
   * Sets whether furniture model color or texture can be changed or not.
   */
  public void setTexturable(Boolean texturable) {
    if (texturable != this.texturable) {
      Boolean oldTexturable = this.texturable;
      this.texturable = texturable;
      this.propertyChangeSupport.firePropertyChange(Property.TEXTURABLE.name(), oldTexturable, texturable);
    }
  }
  
  /**
   * Returns whether furniture model color or texture can be changed or not.
   */
  public Boolean getTexturable() {
    return this.texturable;
  }
  
  /**
   * Sets the edited model rotation.
   */
  public void setModelRotation(float [][] modelRotation) {
    if (modelRotation != this.modelRotation) {
      float [][] oldModelRotation = this.modelRotation;
      this.modelRotation = modelRotation;
      this.propertyChangeSupport.firePropertyChange(Property.MODEL_ROTATION.name(), oldModelRotation, modelRotation);
    }
  }

  /**
   * Returns the edited model rotation.
   */
  public float [][] getModelRotation() {
    return this.modelRotation;
  }
  
  /**
   * Sets the edited creator.
   */
  public void setCreator(String creator) {
    if (creator != this.creator) {
      String oldCreator = this.creator;
      this.creator = creator;
      this.propertyChangeSupport.firePropertyChange(Property.CREATOR.name(), oldCreator, creator);
    }
  }

  /**
   * Returns the edited creator.
   */
  public String getCreator() {
    return this.creator;
  }
  
  /**
   * Sets the edited price.
   */
  public void setPrice(BigDecimal price) {
    if (price != this.price) {
      BigDecimal oldPrice = this.price;
      this.price = price;
      this.propertyChangeSupport.firePropertyChange(Property.ICON.name(), oldPrice, price);
    }
  }

  /**
   * Returns the edited price.
   */
  public BigDecimal getPrice() {
    return this.price;
  }
  
  /**
   * Sets the edited value added tax percentage.
   */
  public void setValueAddedTaxPercentage(BigDecimal valueAddedTaxPercentage) {
    if (valueAddedTaxPercentage != this.valueAddedTaxPercentage) {
      BigDecimal oldValueAddedTaxPercentage = this.valueAddedTaxPercentage;
      this.valueAddedTaxPercentage = valueAddedTaxPercentage;
      this.propertyChangeSupport.firePropertyChange(Property.VALUE_ADDED_TAX_PERCENTAGE.name(), oldValueAddedTaxPercentage, valueAddedTaxPercentage);
    }
  }

  /**
   * Returns the edited value added tax percentage.
   */
  public BigDecimal getValueAddedTaxPercentage() {
    return this.valueAddedTaxPercentage;
  }

  /**
   * Controls the modification of selected furniture in the edited home.
   */
  public void modifyFurniture() {
    if (!this.modifiedFurniture.isEmpty()) {
      String id = getId(); 
      String name = getName();
      String description = getDescription();
      String information = getInformation();
      String [] tags = getTags();
      Long creationDate = getCreationDate();
      Float grade = getGrade();
      FurnitureCategory category = getCategory();
      Content model = getModel();
      Content icon = getIcon();
      Float width = getWidth();
      Float depth = getDepth();
      Float height = getHeight();
      Float elevation = getElevation();
      Boolean movable = getMovable();
      Boolean resizable = getResizable();
      Boolean deformable = getDeformable();
      Boolean texturable = getTexturable();
      Boolean doorOrWindow = getDoorOrWindow();
      String doorOrWindowCutOutShape = getDoorOrWindowCutOutShape();
      Boolean staircase = getStaircase();
      String staircaseCutOutShape = getStaircaseCutOutShape();
      float [][] modelRotation = getModelRotation();
      String creator = getCreator();
      BigDecimal price = getPrice();
      BigDecimal valueAddedTaxPercentage = getValueAddedTaxPercentage();
      boolean defaultFurnitureLanguage = FurnitureLibrary.DEFAULT_LANGUAGE.equals(this.furnitureLanguageController.getFurnitureLangauge());
      
      // Apply modification
      int piecesCount = this.modifiedFurniture.size();
      for (CatalogPieceOfFurniture piece : this.modifiedFurniture) {
        int index = this.furnitureLibrary.getPieceOfFurnitureIndex(piece);
        // Retrieve piece data
        String pieceId = piece.getId();
        String pieceName = piece.getName();
        String pieceDescription = piece.getDescription();
        String pieceInformation = piece.getInformation();
        String [] pieceTags = piece.getTags();
        Long pieceCreationDate = piece.getCreationDate();
        Float pieceGrade = piece.getGrade();
        FurnitureCategory pieceCategory = piece.getCategory();
        Content pieceModel = piece.getModel();
        Content pieceIcon = piece.getIcon();
        Content piecePlanIcon = piece.getPlanIcon();
        float pieceWidth = piece.getWidth();
        float pieceDepth = piece.getDepth();
        float pieceHeight = piece.getHeight();
        float pieceDropOnTopElevation = piece.getDropOnTopElevation();
        float pieceElevation = piece.getElevation();
        boolean pieceMovable = piece.isMovable();
        float [][] pieceModelRotation = piece.getModelRotation();
        String pieceDoorOrWindowCutOutShape = piece instanceof CatalogDoorOrWindow
            ? ((CatalogDoorOrWindow)piece).getCutOutShape()
            : null;
        String pieceStaircaseCutOutShape = piece.getStaircaseCutOutShape();
        boolean pieceBackFaceShown = piece.isBackFaceShown();
        String pieceCreator = piece.getCreator();
        boolean pieceResizable = piece.isResizable();
        boolean pieceDeformable = piece.isDeformable();
        boolean pieceTexturable = piece.isTexturable();
        BigDecimal piecePrice = piece.getPrice();
        BigDecimal pieceValueAddedTaxPercentage = piece.getValueAddedTaxPercentage();
        String pieceCurrency = piece.getCurrency();
        // Retrieve localized data
        Map<String, Object> localizedNames = new HashMap<String, Object>();
        retrieveLocalizedData(piece, localizedNames, FurnitureLibrary.FURNITURE_NAME_PROPERTY);
        Map<String, Object> localizedDescriptions = new HashMap<String, Object>();
        retrieveLocalizedData(piece, localizedDescriptions, FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY);
        Map<String, Object> localizedInformation = new HashMap<String, Object>();
        retrieveLocalizedData(piece, localizedInformation, FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY);
        Map<String, Object> localizedTags = new HashMap<String, Object>();
        retrieveLocalizedData(piece, localizedTags, FurnitureLibrary.FURNITURE_TAGS_PROPERTY);
        Map<String, Object> localizedCategories = new HashMap<String, Object>();
        retrieveLocalizedData(piece, localizedCategories, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY);

        // Update mandatory not localizable data
        if (model != null) {
          pieceModel = model;
        }
        if (icon != null) {
          pieceIcon = icon;
        }
        if (width != null) {
          pieceWidth = width;
        }
        if (depth != null) {
          pieceDepth = depth;
        }
        if (height != null) {
          pieceHeight = height;
        }
        if (movable != null) {
          pieceMovable = movable;
        }
        // Update not mandatory and not localizable data
        // When only one piece is updated, data can be reset to empty 
        if (id != null || piecesCount == 1) {
          pieceId = id;
        }
        if (creationDate != null || piecesCount == 1) {
          pieceCreationDate = creationDate;
        }
        if (grade != null || piecesCount == 1) {
          pieceGrade = grade;
        }
        if (elevation != null || piecesCount == 1) {
          pieceElevation = elevation;
        }
        if (modelRotation != null || piecesCount == 1) {
          pieceModelRotation = modelRotation;
        }
        if (pieceDoorOrWindowCutOutShape != null || piecesCount == 1) {
          pieceDoorOrWindowCutOutShape = doorOrWindowCutOutShape;
        }
        if (staircase != null) { // Always Boolean.TRUE or Boolean.FALSE (not null) if piecesCount == 1
          if (staircase) {
            if (staircaseCutOutShape != null) {
              pieceStaircaseCutOutShape = staircaseCutOutShape;
            } else if (piecesCount == 1) {
              pieceStaircaseCutOutShape = DEFAULT_CUT_OUT_SHAPE;
            }
          } else {
            pieceStaircaseCutOutShape = null;
          }
        } else if (staircaseCutOutShape != null && pieceStaircaseCutOutShape != null) {
          pieceStaircaseCutOutShape = staircaseCutOutShape;
        }
        if (creator != null || piecesCount == 1) {
          pieceCreator = creator;
        }
        if (resizable != null || piecesCount == 1) {
          pieceResizable = resizable;
        }
        if (deformable != null || piecesCount == 1) {
          pieceDeformable = deformable;
        }
        if (texturable != null || piecesCount == 1) {
          pieceTexturable = texturable;
        }
        if (price != null || piecesCount == 1) {
          piecePrice = price;
        }
        if (valueAddedTaxPercentage != null || piecesCount == 1) {
          pieceValueAddedTaxPercentage = valueAddedTaxPercentage;
        }
        // Update mandatory localizable data
        if (name != null) {
          if (defaultFurnitureLanguage) {
            pieceName = name;
          } else {
            localizedNames.put(this.furnitureLanguageController.getFurnitureLangauge(), name);
          }
        }
        if (category != null) {
          if (defaultFurnitureLanguage) {
            pieceCategory = category;
          } else {
            localizedCategories.put(this.furnitureLanguageController.getFurnitureLangauge(), category.getName());
          }
        }
        // Update not mandatory localizable data
        // When only one piece is updated, data can be reset to empty 
        if (description != null || piecesCount == 1) {
          if (defaultFurnitureLanguage) {
            pieceDescription = description;
          } else {
            localizedDescriptions.put(this.furnitureLanguageController.getFurnitureLangauge(), description);
          }
        }
        if (information != null || piecesCount == 1) {
          if (defaultFurnitureLanguage) {
            pieceInformation = information;
          } else {
            localizedInformation.put(this.furnitureLanguageController.getFurnitureLangauge(), information);
          }
        }
        if (tags != null || piecesCount == 1) {
          if (defaultFurnitureLanguage) {
            if (tags == null) {
              pieceTags = new String [0];
            } else {
              pieceTags = tags;
            }
          } else {
            localizedTags.put(this.furnitureLanguageController.getFurnitureLangauge(), tags);
          }
        }
       
        // Create updated piece
        CatalogPieceOfFurniture updatedPiece;
        if (piece instanceof CatalogDoorOrWindow
            && (doorOrWindow == null || doorOrWindow)) {
          CatalogDoorOrWindow opening = (CatalogDoorOrWindow)piece;
          updatedPiece = new CatalogDoorOrWindow(pieceId, pieceName, pieceDescription, 
              pieceInformation, pieceTags, pieceCreationDate, pieceGrade, 
              pieceIcon, piecePlanIcon, pieceModel, pieceWidth, pieceDepth, pieceHeight, 
              pieceElevation, pieceDropOnTopElevation, pieceMovable, 
              pieceDoorOrWindowCutOutShape, opening.getWallThickness(), opening.getWallDistance(), opening.getSashes(),
              pieceModelRotation, pieceBackFaceShown, pieceCreator, pieceResizable, pieceDeformable, pieceTexturable, 
              piecePrice, pieceValueAddedTaxPercentage, pieceCurrency);
        } else if (piece instanceof CatalogLight) {
          CatalogLight light = (CatalogLight)piece;
          updatedPiece = new CatalogLight(pieceId, pieceName, pieceDescription, 
              pieceInformation, pieceTags, pieceCreationDate, pieceGrade, 
              pieceIcon, piecePlanIcon, pieceModel, pieceWidth, pieceDepth, pieceHeight, 
              pieceElevation, pieceDropOnTopElevation, pieceMovable, 
              light.getLightSources(), pieceStaircaseCutOutShape, 
              pieceModelRotation, pieceBackFaceShown, pieceCreator, pieceResizable, pieceDeformable, pieceTexturable, 
              piecePrice, pieceValueAddedTaxPercentage, pieceCurrency);
        } else {
          if (doorOrWindow != null && doorOrWindow) {
            updatedPiece = new CatalogDoorOrWindow(pieceId, pieceName, pieceDescription, 
                pieceInformation, pieceTags, pieceCreationDate, pieceGrade,
                pieceIcon, piecePlanIcon, pieceModel, pieceWidth, pieceDepth, pieceHeight, 
                pieceElevation, pieceDropOnTopElevation, pieceMovable, 
                pieceDoorOrWindowCutOutShape, 1, 0, new Sash [0], pieceModelRotation, pieceBackFaceShown, pieceCreator, 
                pieceResizable, pieceDeformable, pieceTexturable, 
                piecePrice, pieceValueAddedTaxPercentage, pieceCurrency);
          } else {
            updatedPiece = new CatalogPieceOfFurniture(pieceId, pieceName, pieceDescription, 
                pieceInformation, pieceTags, pieceCreationDate, pieceGrade, 
                pieceIcon, piecePlanIcon, pieceModel, pieceWidth, pieceDepth, pieceHeight, 
                pieceElevation, pieceDropOnTopElevation, pieceMovable, 
                pieceStaircaseCutOutShape, pieceModelRotation, pieceBackFaceShown, pieceCreator,  
                pieceResizable, pieceDeformable, pieceTexturable, 
                piecePrice, pieceValueAddedTaxPercentage, pieceCurrency);
          }
        }
        new FurnitureCatalog().add(pieceCategory, updatedPiece);
        this.furnitureLibrary.addPieceOfFurniture(updatedPiece, index);
        Set<String> supportedLanguages = new HashSet<String>(this.furnitureLibrary.getSupportedLanguages());
        supportedLanguages.add(this.furnitureLanguageController.getFurnitureLangauge());
        for (String language : supportedLanguages) {
          if (!FurnitureLibrary.DEFAULT_LANGUAGE.equals(language)) {
            Object localizedPieceName = localizedNames.get(language);
            if (localizedPieceName != null) {
              this.furnitureLibrary.setPieceOfFurnitureLocalizedData(
                  updatedPiece, language, FurnitureLibrary.FURNITURE_NAME_PROPERTY, localizedPieceName);
            }
            Object localizedPieceDescription = localizedDescriptions.get(language);
            if (localizedPieceDescription != null) {
              this.furnitureLibrary.setPieceOfFurnitureLocalizedData(
                  updatedPiece, language, FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY, localizedPieceDescription);
            }          
            Object localizedPieceInformation = localizedInformation.get(language);
            if (localizedPieceInformation != null) {
              this.furnitureLibrary.setPieceOfFurnitureLocalizedData(
                  updatedPiece, language, FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY, localizedPieceInformation);
            }          
            Object localizedPieceTags = localizedTags.get(language);
            if (localizedPieceTags != null) {
              this.furnitureLibrary.setPieceOfFurnitureLocalizedData(
                  updatedPiece, language, FurnitureLibrary.FURNITURE_TAGS_PROPERTY, localizedPieceTags);
            }          
            Object localizedPieceCategory = localizedCategories.get(language);
            if (localizedPieceCategory != null) {
              this.furnitureLibrary.setPieceOfFurnitureLocalizedData(
                  updatedPiece, language, FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY, localizedPieceCategory);
            }
          }
        }
        
        // Remove old piece from library
        this.furnitureLibrary.deletePieceOfFurniture(piece);
      }
    }
  }

  private void retrieveLocalizedData(CatalogPieceOfFurniture piece,
                                     Map<String, Object> localizedNames,
                                     String propertyKey) {
    for (String language : this.furnitureLibrary.getSupportedLanguages()) {
      Object pieceData = this.furnitureLibrary.getPieceOfFurnitureLocalizedData(piece, language, propertyKey);
      if (pieceData != null) {
        localizedNames.put(language, pieceData);
      }
    }
  }
}
