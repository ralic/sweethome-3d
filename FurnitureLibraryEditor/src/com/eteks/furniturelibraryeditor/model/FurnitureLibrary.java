/*
 * FurnitureLibrary.java 18 déc. 2009
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
package com.eteks.furniturelibraryeditor.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.CollectionChangeSupport;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Library;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * A library of furniture catalog pieces.
 * @author Emmanuel Puybaret
 */
public class FurnitureLibrary implements Library {
  /**
   * The properties of this library that may change. <code>PropertyChangeListener</code>s added 
   * to a library will be notified under a property name equal to the name value of one these properties.
   */
  public enum Property {LOCATION, ID, NAME, MODIFIED, DESCRIPTION, VERSION, LICENSE, PROVIDER, LOCALIZED_DATA};
  
  public static final String DEFAULT_LANGUAGE = "";

  public static final String FURNITURE_ID_PROPERTY                           = "ID";
  public static final String FURNITURE_NAME_PROPERTY                         = "NAME";
  public static final String FURNITURE_DESCRIPTION_PROPERTY                  = "DESCRIPTION";
  public static final String FURNITURE_INFORMATION_PROPERTY                  = "INFORMATION";
  public static final String FURNITURE_TAGS_PROPERTY                         = "TAGS";
  public static final String FURNITURE_CREATION_DATE_PROPERTY                = "CREATION_DATE";
  public static final String FURNITURE_GRADE_PROPERTY                        = "GRADE";
  public static final String FURNITURE_CATEGORY_PROPERTY                     = "CATEGORY";
  public static final String FURNITURE_CREATOR_PROPERTY                      = "CREATOR";
  public static final String FURNITURE_PRICE_PROPERTY                        = "PRICE";
  public static final String FURNITURE_VALUE_ADDED_TAX_PERCENTAGE_PROPERTY   = "VALUE_ADDED_TAX_PERCENTAGE";
  public static final String FURNITURE_MODEL_PROPERTY                        = "MODEL";
  public static final String FURNITURE_ICON_PROPERTY                         = "ICON";
  public static final String FURNITURE_PLAN_ICON_PROPERTY                    = "PLAN_ICON";
  public static final String FURNITURE_WIDTH_PROPERTY                        = "WIDTH";
  public static final String FURNITURE_DEPTH_PROPERTY                        = "DEPTH";
  public static final String FURNITURE_HEIGHT_PROPERTY                       = "HEIGHT";
  public static final String FURNITURE_MOVABLE_PROPERTY                      = "MOVABLE";
  public static final String FURNITURE_DOOR_OR_WINDOW_PROPERTY               = "DOOR_OR_WINDOW";
  public static final String FURNITURE_DOOR_OR_WINDOW_CUT_OUT_SHAPE_PROPERTY = "DOOR_OR_WINDOW_CUT_OUT_SHAPE";
  public static final String FURNITURE_STAIRCASE_CUT_OUT_SHAPE_PROPERTY      = "STAIRCASE_CUT_OUT_SHAPE";
  public static final String FURNITURE_ELEVATION_PROPERTY                    = "ELEVATION";
  public static final String FURNITURE_MODEL_ROTATION_PROPERTY               = "MODEL_ROTATION";
  public static final String FURNITURE_RESIZABLE_PROPERTY                    = "RESIZABLE";
  public static final String FURNITURE_DEFORMABLE_PROPERTY                   = "DEFORMABLE";
  public static final String FURNITURE_TEXTURABLE_PROPERTY                   = "TEXTURABLE";
  
  private final PropertyChangeSupport                                    propertyChangeSupport;
  private List<CatalogPieceOfFurniture>                                  furniture;
  private Map<String, CatalogPieceOfFurniture>                           furnitureByIds;
  private Map<CatalogPieceOfFurniture, Map<String, Map<String, Object>>> furnitureLocalizedData;
  private Set<String>                                                    supportedLanguages;
  private CollectionChangeSupport<CatalogPieceOfFurniture>               furnitureChangeSupport;
  private boolean   noRequestSinceLastChange = true;
  private String    location;
  private boolean   modified;
  private String    id;
  private String    name;
  private String    description;
  private String    version;
  private String    license; 
  private String    provider;
  
  public FurnitureLibrary() {
    this.furniture = new ArrayList<CatalogPieceOfFurniture>();
    this.furnitureByIds = new HashMap<String, CatalogPieceOfFurniture>();
    // Use IdentityHashMap to ignore equality between two CatalogPieceOfFurniture instances with same name 
    this.furnitureLocalizedData = new IdentityHashMap<CatalogPieceOfFurniture, Map<String, Map<String, Object>>>();
    this.supportedLanguages = new HashSet<String>();
    this.supportedLanguages.add(DEFAULT_LANGUAGE);
    this.furnitureChangeSupport = new CollectionChangeSupport<CatalogPieceOfFurniture>(this);
    this.propertyChangeSupport = new PropertyChangeSupport(this);
  }

  /**
   * Adds the property change <code>listener</code> in parameter to this library.
   */
  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  /**
   * Removes the property change <code>listener</code> in parameter from this library.
   */
  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  /**
   * Adds the furniture <code>listener</code> in parameter to this library.
   */
  public void addListener(CollectionListener<CatalogPieceOfFurniture> listener) {
    this.furnitureChangeSupport.addCollectionListener(listener);
  }

  /**
   * Removes the furniture <code>listener</code> in parameter from this library.
   */
  public void removeListener(CollectionListener<CatalogPieceOfFurniture> listener) {
    this.furnitureChangeSupport.removeCollectionListener(listener);
  }

  /**
   * Returns an unmodifiable list of the furniture managed by this library. 
   */
  public List<CatalogPieceOfFurniture> getFurniture() {
    this.noRequestSinceLastChange = false;
    return Collections.unmodifiableList(this.furniture);
  }

  /**
   * Adds a <code>piece</code> in parameter.
   * Once the <code>piece</code> is added, furniture listeners added to this library will receive a
   * {@link CollectionListener#collectionChanged(CollectionEvent) collectionChanged}
   * notification.
   */
  public void addPieceOfFurniture(CatalogPieceOfFurniture piece) {
    addPieceOfFurniture(piece, this.furniture.size());
  }

  /**
   * Adds the <code>piece</code> in parameter at a given <code>index</code>.
   * Once the <code>piece</code> is added, furniture listeners added to this library will receive a
   * {@link CollectionListener#collectionChanged(CollectionEvent) collectionChanged}
   * notification.
   */
  public void addPieceOfFurniture(CatalogPieceOfFurniture piece, int index) {
    if (!this.noRequestSinceLastChange) {
      // Make a copy of the list to avoid conflicts in the list returned by getFurniture
      this.furniture = new ArrayList<CatalogPieceOfFurniture>(this.furniture);
      this.noRequestSinceLastChange = true;
    }
    this.furniture.add(index, piece);
    String pieceId = piece.getId();
    if (pieceId != null) {
      this.furnitureByIds.put(pieceId, piece);
    }
    this.furnitureChangeSupport.fireCollectionChanged(piece, index, CollectionEvent.Type.ADD);
  }

  /**
   * Deletes the <code>piece</code> in parameter from this library.
   * Once the <code>piece</code> is deleted, furniture listeners added to this library will receive a
   * {@link CollectionListener#collectionChanged(CollectionEvent) collectionChanged}
   * notification.
   */
  public void deletePieceOfFurniture(CatalogPieceOfFurniture piece) {
    for (int index = 0; index < this.furniture.size(); index++) {
      if (this.furniture.get(index) == piece) {
        if (!this.noRequestSinceLastChange) {
          // Make a copy of the list to avoid conflicts in the list returned by getFurniture
          this.furniture = new ArrayList<CatalogPieceOfFurniture>(this.furniture);
          this.noRequestSinceLastChange = true;
        }
        this.furniture.remove(index);
        String pieceId = piece.getId();
        if (pieceId != null) {
          this.furnitureByIds.remove(pieceId);
        }
        this.furnitureLocalizedData.remove(piece);
        if (this.furnitureLocalizedData.isEmpty()) {
          this.supportedLanguages.clear();
          this.supportedLanguages.add(DEFAULT_LANGUAGE);
        }
        this.furnitureChangeSupport.fireCollectionChanged(piece, index, CollectionEvent.Type.DELETE);
        break;
      }
    }
  }

  /**
   * Returns the index of the given <code>piece</code> or -1 if it was not found.
   * This method use strong reference equality to compare pieces.
   */
  public int getPieceOfFurnitureIndex(CatalogPieceOfFurniture piece) {
    for (int index = 0; index < this.furniture.size(); index++) {
      if (this.furniture.get(index) == piece) {
        return index;
      }
    }
    return -1;
  }
  
  /**
   * Returns the first piece of furniture with the given ID or <code>null</code>.
   */
  public CatalogPieceOfFurniture getPieceOfFurniture(String pieceId) {
    return this.furnitureByIds.get(pieceId);
  }
  
  /**
   * Sets localized data of a piece of furniture and fires a <code>PropertyChangeEvent</code>.
   */
  public void setPieceOfFurnitureLocalizedData(CatalogPieceOfFurniture piece, String language,
                                               String propertyKey, 
                                               Object propertyValue) {
    Map<String, Map<String, Object>> pieceLocalizedData = this.furnitureLocalizedData.get(piece);
    if (pieceLocalizedData == null) {
      pieceLocalizedData = new HashMap<String, Map<String,Object>>();
      this.furnitureLocalizedData.put(piece, pieceLocalizedData);
    }
    Map<String, Object> pieceData = pieceLocalizedData.get(language);
    if (pieceData == null) {
      pieceData = new HashMap<String, Object>();
      pieceLocalizedData.put(language, pieceData);
      this.supportedLanguages.add(language);
    }
    
    Object oldPropertyValue = pieceData.get(propertyKey);
    if (oldPropertyValue != propertyValue
        || propertyValue != null && !propertyValue.equals(oldPropertyValue)) {
      pieceData.put(propertyKey, propertyValue);
      this.propertyChangeSupport.firePropertyChange(Property.LOCALIZED_DATA.name(), oldPropertyValue, propertyValue);
    }
  }
  
  /**
   * Returns a localized data of a piece of furniture or <code>null</code> if it doesn't exist.
   */
  public Object getPieceOfFurnitureLocalizedData(CatalogPieceOfFurniture piece, String language,
                                                 String propertyKey) {
    return getPieceOfFurnitureLocalizedData(piece, language, propertyKey, null);
  }
  
  /**
   * Returns a localized data of a piece of furniture or <code>defaultValue</code> if it doesn't exist.
   */
  public Object getPieceOfFurnitureLocalizedData(CatalogPieceOfFurniture piece, 
                                                 String language,
                                                 String propertyKey,
                                                 Object defaultValue) {
    Map<String, Map<String, Object>> pieceLocalizedData = this.furnitureLocalizedData.get(piece);
    if (pieceLocalizedData != null) {
      Map<String, Object> pieceData = pieceLocalizedData.get(language);
      if (pieceData != null) {
        Object propertyValue = pieceData.get(propertyKey);
        if (propertyValue != null) {
          return propertyValue;
        }
      }
    }
    return defaultValue;
  }
  
  /**
   * Returns an unmodifiable list of the languages supported by this library.
   * This list is built from the localized data set on furniture.
   */
  public List<String> getSupportedLanguages() {
    return Collections.unmodifiableList(new ArrayList<String>(this.supportedLanguages));
  }

  /**
   * Returns the location where this library is stored.
   */
  public String getLocation() {
    return this.location;
  }

  /**
   * Sets the location where this library is stored and fires a <code>PropertyChangeEvent</code>.
   */
  public void setLocation(String location) {
    if (location != this.location
        || (location != null && !location.equals(this.location))) {
      String oldLocation = this.location;
      this.location = location;
      this.propertyChangeSupport.firePropertyChange(Property.LOCATION.name(), oldLocation, location);
    }
  }

  /**
   * Returns <code>true</code> if the library handled by this controller is modified.
   */
  public boolean isModified() {
    return this.modified;
  }
  
  /**
   * Sets whether the library is modified or not, and fires a <code>PropertyChangeEvent</code>.
   */
  public void setModified(boolean modified) {
    if (modified != this.modified) {
      this.modified = modified; 
      this.propertyChangeSupport.firePropertyChange(Property.MODIFIED.toString(), !modified, modified);
    }
  }
  
  /**
   * Returns the id of this library.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Sets the id of this library and fires a <code>PropertyChangeEvent</code>.
   */
  public void setId(String id) {
    if (id != this.id
        || (id != null && !id.equals(this.id))) {
      String oldId = this.id;
      this.id = id;
      this.propertyChangeSupport.firePropertyChange(Property.ID.name(), oldId, id);
    }
  }

  /**
   * Returns {@link UserPreferences#FURNITURE_LIBRARY_TYPE}.
   */
  public String getType() {
    return UserPreferences.FURNITURE_LIBRARY_TYPE;
  }

  /**
   * Returns the name of this library.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Sets the name of this library and fires a <code>PropertyChangeEvent</code>.
   */
  public void setName(String name) {
    if (name != this.name
        || (name != null && !name.equals(this.name))) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  /**
   * Returns the description of this library.
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Sets the description of this library and fires a <code>PropertyChangeEvent</code>.
   */
  public void setDescription(String description) {
    if (description != this.description
        || (description != null && !description.equals(this.description))) {
      String oldDescription = this.description;
      this.description = description;
      this.propertyChangeSupport.firePropertyChange(Property.DESCRIPTION.name(), oldDescription, description);
    }
  }

  /**
   * Returns the version of this library.
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * Sets the version of this library and fires a <code>PropertyChangeEvent</code>.
   */
  public void setVersion(String version) {
    if (version != this.version
        || (version != null && !version.equals(this.version))) {
      String oldVersion = this.version;
      this.version = version;
      this.propertyChangeSupport.firePropertyChange(Property.VERSION.name(), oldVersion, version);
    }
  }

  /**
   * Returns the license of this library.
   */
  public String getLicense() {
    return this.license;
  }

  /**
   * Sets the license of this library and fires a <code>PropertyChangeEvent</code>.
   */
  public void setLicense(String license) {
    if (license != this.license
        || (license != null && !license.equals(this.license))) {
      String oldLicense = this.license;
      this.license = license;
      this.propertyChangeSupport.firePropertyChange(Property.LICENSE.name(), oldLicense, license);
    }
  }

  /**
   * Returns the provider of this library.
   */
  public String getProvider() {
    return this.provider;
  }

  /**
   * Sets the provider of this library and fires a <code>PropertyChangeEvent</code>.
   */
  public void setProvider(String provider) {
    if (provider != this.provider
        || (provider != null && !provider.equals(this.provider))) {
      String oldProvider = this.provider;
      this.provider = provider;
      this.propertyChangeSupport.firePropertyChange(Property.PROVIDER.name(), oldProvider, provider);
    }
  }
}
