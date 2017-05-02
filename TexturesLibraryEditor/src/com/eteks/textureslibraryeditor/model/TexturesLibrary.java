/*
 * TexturesLibrary.java 11 sept. 2012
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
package com.eteks.textureslibraryeditor.model;

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

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.CollectionChangeSupport;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Library;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * A library of catalog textures.
 * @author Emmanuel Puybaret
 */
public class TexturesLibrary implements Library {
  /**
   * The properties of this library that may change. <code>PropertyChangeListener</code>s added 
   * to a library will be notified under a property name equal to the name value of one these properties.
   */
  public enum Property {LOCATION, ID, NAME, MODIFIED, DESCRIPTION, VERSION, LICENSE, PROVIDER, LOCALIZED_DATA};
  
  public static final String DEFAULT_LANGUAGE = "";

  public static final String TEXTURES_ID_PROPERTY       = "ID";
  public static final String TEXTURES_NAME_PROPERTY     = "NAME";
  public static final String TEXTURES_CATEGORY_PROPERTY = "CATEGORY";
  public static final String TEXTURES_CREATOR_PROPERTY  = "CREATOR";
  public static final String TEXTURES_IMAGE_PROPERTY    = "IMAGE";
  public static final String TEXTURES_WIDTH_PROPERTY    = "WIDTH";
  public static final String TEXTURES_HEIGHT_PROPERTY   = "HEIGHT";
  
  private final PropertyChangeSupport                           propertyChangeSupport;
  private List<CatalogTexture>                                  textures;
  private Map<CatalogTexture, Map<String, Map<String, Object>>> texturesLocalizedData;
  private Set<String>                                           supportedLanguages;
  private CollectionChangeSupport<CatalogTexture>               texturesChangeSupport;
  private boolean   noRequestSinceLastChange = true;
  private String    location;
  private boolean   modified;
  private String    id;
  private String    name;
  private String    description;
  private String    version;
  private String    license; 
  private String    provider;
  
  public TexturesLibrary() {
    this.textures = new ArrayList<CatalogTexture>();
    // Use IdentityHashMap to ignore equality between two CatalogTexture instances with same name 
    this.texturesLocalizedData = new IdentityHashMap<CatalogTexture, Map<String, Map<String, Object>>>();
    this.supportedLanguages = new HashSet<String>();
    this.supportedLanguages.add(DEFAULT_LANGUAGE);
    this.texturesChangeSupport = new CollectionChangeSupport<CatalogTexture>(this);
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
   * Adds the textures <code>listener</code> in parameter to this library.
   */
  public void addListener(CollectionListener<CatalogTexture> listener) {
    this.texturesChangeSupport.addCollectionListener(listener);
  }

  /**
   * Removes the textures <code>listener</code> in parameter from this library.
   */
  public void removeListener(CollectionListener<CatalogTexture> listener) {
    this.texturesChangeSupport.removeCollectionListener(listener);
  }

  /**
   * Returns an unmodifiable list of the textures managed by this library. 
   */
  public List<CatalogTexture> getTextures() {
    this.noRequestSinceLastChange = false;
    return Collections.unmodifiableList(this.textures);
  }

  /**
   * Adds a <code>texture</code> in parameter.
   * Once the <code>texture</code> is added, textures listeners added to this library will receive a
   * {@link CollectionListener#collectionChanged(CollectionEvent) collectionChanged}
   * notification.
   */
  public void addTexture(CatalogTexture texture) {
    addTexture(texture, this.textures.size());
  }

  /**
   * Adds the <code>texture</code> in parameter at a given <code>index</code>.
   * Once the <code>texture</code> is added, textures listeners added to this library will receive a
   * {@link CollectionListener#collectionChanged(CollectionEvent) collectionChanged}
   * notification.
   */
  public void addTexture(CatalogTexture texture, int index) {
    if (!this.noRequestSinceLastChange) {
      // Make a copy of the list to avoid conflicts in the list returned by getTextures
      this.textures = new ArrayList<CatalogTexture>(this.textures);
      this.noRequestSinceLastChange = true;
    }
    this.textures.add(index, texture);
    this.texturesChangeSupport.fireCollectionChanged(texture, index, CollectionEvent.Type.ADD);
  }

  /**
   * Deletes the <code>texture</code> in parameter from this library.
   * Once the <code>texture</code> is deleted, textures listeners added to this library will receive a
   * {@link CollectionListener#collectionChanged(CollectionEvent) collectionChanged}
   * notification.
   */
  public void deleteTexture(CatalogTexture texture) {
    for (int index = 0; index < this.textures.size(); index++) {
      if (this.textures.get(index) == texture) {
        if (!this.noRequestSinceLastChange) {
          // Make a copy of the list to avoid conflicts in the list returned by getTextures
          this.textures = new ArrayList<CatalogTexture>(this.textures);
          this.noRequestSinceLastChange = true;
        }
        this.textures.remove(index);
        this.texturesLocalizedData.remove(texture);
        if (this.texturesLocalizedData.isEmpty()) {
          this.supportedLanguages.clear();
          this.supportedLanguages.add(DEFAULT_LANGUAGE);
        }
        this.texturesChangeSupport.fireCollectionChanged(texture, index, CollectionEvent.Type.DELETE);
        break;
      }
    }
  }

  /**
   * Returns the index of the given <code>texture</code> or -1 if it was not found.
   * This method use strong reference equality to compare textures.
   */
  public int getTextureIndex(CatalogTexture texture) {
    for (int index = 0; index < this.textures.size(); index++) {
      if (this.textures.get(index) == texture) {
        return index;
      }
    }
    return -1;
  }
  
  /**
   * Sets localized data of a texture of textures and fires a <code>PropertyChangeEvent</code>.
   */
  public void setTextureLocalizedData(CatalogTexture texture, String language,
                                               String propertyKey, 
                                               Object propertyValue) {
    Map<String, Map<String, Object>> textureLocalizedData = this.texturesLocalizedData.get(texture);
    if (textureLocalizedData == null) {
      textureLocalizedData = new HashMap<String, Map<String,Object>>();
      this.texturesLocalizedData.put(texture, textureLocalizedData);
    }
    Map<String, Object> textureData = textureLocalizedData.get(language);
    if (textureData == null) {
      textureData = new HashMap<String, Object>();
      textureLocalizedData.put(language, textureData);
      this.supportedLanguages.add(language);
    }
    
    Object oldPropertyValue = textureData.get(propertyKey);
    if (oldPropertyValue != propertyValue
        || propertyValue != null && !propertyValue.equals(oldPropertyValue)) {
      textureData.put(propertyKey, propertyValue);
      this.propertyChangeSupport.firePropertyChange(Property.LOCALIZED_DATA.name(), oldPropertyValue, propertyValue);
    }
  }
  
  /**
   * Returns a localized data of a texture of textures or <code>null</code> if it doesn't exist.
   */
  public Object getTextureLocalizedData(CatalogTexture texture, String language,
                                                 String propertyKey) {
    return getTextureLocalizedData(texture, language, propertyKey, null);
  }
  
  /**
   * Returns a localized data of a texture of textures or <code>defaultValue</code> if it doesn't exist.
   */
  public Object getTextureLocalizedData(CatalogTexture texture, 
                                        String language,
                                        String propertyKey,
                                        Object defaultValue) {
    Map<String, Map<String, Object>> textureLocalizedData = this.texturesLocalizedData.get(texture);
    if (textureLocalizedData != null) {
      Map<String, Object> textureData = textureLocalizedData.get(language);
      if (textureData != null) {
        Object propertyValue = textureData.get(propertyKey);
        if (propertyValue != null) {
          return propertyValue;
        }
      }
    }
    return defaultValue;
  }
  
  /**
   * Returns an unmodifiable list of the languages supported by this library.
   * This list is built from the localized data set on textures.
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
   * Returns {@link UserPreferences#TEXTURES_LIBRARY_TYPE}.
   */
  public String getType() {
    return UserPreferences.TEXTURES_LIBRARY_TYPE;
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
