/*
 * TexturesController.java 
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
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.TexturesCatalog;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;

/**
 * A MVC controller for textures view.
 * @author Emmanuel Puybaret
 */
public class TexturesController implements Controller {
  /**
   * The properties that may be edited by the view associated to this controller. 
   */
  public enum Property {ID, NAME, CATEGORY, IMAGE, WIDTH, HEIGHT, CREATOR}
  
  private static final Map<String, Property> PROPERTIES_MAP = new HashMap<String, Property>();
  
  static {
    PROPERTIES_MAP.put(TexturesLibrary.TEXTURES_ID_PROPERTY, Property.ID);
    PROPERTIES_MAP.put(TexturesLibrary.TEXTURES_NAME_PROPERTY, Property.NAME);
    PROPERTIES_MAP.put(TexturesLibrary.TEXTURES_CATEGORY_PROPERTY, Property.CATEGORY);
    PROPERTIES_MAP.put(TexturesLibrary.TEXTURES_CREATOR_PROPERTY, Property.CREATOR);
    PROPERTIES_MAP.put(TexturesLibrary.TEXTURES_IMAGE_PROPERTY, Property.IMAGE);
    PROPERTIES_MAP.put(TexturesLibrary.TEXTURES_WIDTH_PROPERTY, Property.WIDTH);
    PROPERTIES_MAP.put(TexturesLibrary.TEXTURES_HEIGHT_PROPERTY, Property.HEIGHT);
  }
  
  private final TexturesLibrary                texturesLibrary;
  private final List<CatalogTexture>           modifiedTextures;
  private final Set<Property>                  editableProperties;
  private final TexturesLibraryUserPreferences preferences;
  private final TexturesLanguageController     texturesLanguageController;
  private final EditorViewFactory              viewFactory;
  private final PropertyChangeSupport          propertyChangeSupport;
  private DialogView                           homeTexturesView;

  private String            id;
  private String            name;
  private TexturesCategory  category;
  private Content           image;
  private Float             width;
  private Float             proportionalWidth;
  private Float             height;
  private Float             proportionalHeight;
  private String            creator;
  
  private PropertyChangeListener widthChangeListener;
  private PropertyChangeListener heightChangeListener;
  
  /**
   * Creates the controller of catalog textures view.
   */
  public TexturesController(TexturesLibrary texturesLibrary, 
                             List<CatalogTexture> modifiedTextures,
                             TexturesLibraryUserPreferences preferences, 
                             TexturesLanguageController texturesLanguageController,
                             EditorViewFactory viewFactory) {
    this.texturesLibrary = texturesLibrary;
    this.modifiedTextures = modifiedTextures;
    this.preferences = preferences;
    this.texturesLanguageController = texturesLanguageController;
    this.viewFactory = viewFactory;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    this.editableProperties = new HashSet<Property>();
    for (String editedProperty : preferences.getEditedProperties()) {
      this.editableProperties.add(PROPERTIES_MAP.get(editedProperty));
    }

    updateProperties();
    addListeners();
  }

  /**
   * Returns the view associated with this controller.
   */
  public DialogView getView() {
    // Create view lazily only once it's needed
    if (this.homeTexturesView == null) {
      this.homeTexturesView = this.viewFactory.createTexturesView(this.preferences, this); 
    }
    return this.homeTexturesView;
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
          removePropertyChangeListener(Property.HEIGHT, heightChangeListener);
          
          if (ev.getNewValue() != null 
              && ev.getOldValue() != null
              && proportionalHeight != null) {
            // If proportions should be kept, update height
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setHeight(proportionalHeight * ratio, true);
          }
          
          addPropertyChangeListener(Property.HEIGHT, heightChangeListener);
        }
      };
    this.heightChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          removePropertyChangeListener(Property.WIDTH, widthChangeListener);
          
          if (ev.getNewValue() != null 
              && ev.getOldValue() != null
              && proportionalWidth != null) {
            // If proportions should be kept, update width 
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setWidth(proportionalWidth * ratio, true);
          }
          
          addPropertyChangeListener(Property.WIDTH, widthChangeListener);
        }
      };

    addPropertyChangeListener(Property.WIDTH, this.widthChangeListener);
    addPropertyChangeListener(Property.HEIGHT, this.heightChangeListener);
  }

  /**
   * Returns <code>true</code> if the given <code>property</code> is editable.
   * Depending on whether a property is editable or not, the view associated to this controller
   * may render it differently.
   */
  public boolean isPropertyEditable(Property property) {
    if (this.modifiedTextures.size() == 1) {
      return this.editableProperties.contains(property);
    } else {
      return this.editableProperties.contains(property)
          && property != Property.ID
          && property != Property.IMAGE;
    }
  }
  
  /**
   * Updates edited properties from selected textures in the home edited by this controller.
   */
  protected void updateProperties() {
    if (this.modifiedTextures.isEmpty()) {
      setId(null); // Nothing to edit
      setName(null);
      setCategory(null);
      setImage(null);
      setWidth(null);
      setHeight(null);
      setCreator(null);
    } else {
      CatalogTexture firstTexture = this.modifiedTextures.get(0);
      
      if (this.modifiedTextures.size() == 1) {
        setImage(firstTexture.getImage());
      } else {
        setImage(null);
      }
      
      // Search the common properties among selected textures
      String id = firstTexture.getId();
      if (id != null) {
        for (int i = 1; i < this.modifiedTextures.size(); i++) {
          if (!id.equals(this.modifiedTextures.get(i).getId())) {
            id = null;
            break;
          }
        }
      }
      setId(id);
      
      String texturesLanguage = this.texturesLanguageController.getTexturesLangauge();
      String name = (String)this.texturesLibrary.getTextureLocalizedData(
          firstTexture, texturesLanguage, TexturesLibrary.TEXTURES_NAME_PROPERTY, firstTexture.getName());
      if (name != null) {
        for (int i = 1; i < this.modifiedTextures.size(); i++) {
          CatalogTexture texture = this.modifiedTextures.get(i);
          if (!name.equals(this.texturesLibrary.getTextureLocalizedData(
              texture, texturesLanguage, TexturesLibrary.TEXTURES_NAME_PROPERTY, texture.getName()))) {
            name = null;
            break;
          }
        }
      }
      setName(name);
      
      TexturesCategory category = firstTexture.getCategory();
      String categoryName = (String)this.texturesLibrary.getTextureLocalizedData(
          firstTexture, texturesLanguage, TexturesLibrary.TEXTURES_CATEGORY_PROPERTY, category.getName());
      if (category != null) {
        for (int i = 1; i < this.modifiedTextures.size(); i++) {
          CatalogTexture texture = this.modifiedTextures.get(i);
          if (!categoryName.equals(this.texturesLibrary.getTextureLocalizedData(
              texture, texturesLanguage, TexturesLibrary.TEXTURES_CATEGORY_PROPERTY, texture.getCategory().getName()))) {
            category = null;
            break;
          }
        }
      }
      setCategory(category == null ? null : new TexturesCategory(categoryName));

      Float width = firstTexture.getWidth();
      for (int i = 1; i < this.modifiedTextures.size(); i++) {
        if (width.floatValue() != this.modifiedTextures.get(i).getWidth()) {
          width = null;
          break;
        }
      }
      setWidth(width);

      Float height = firstTexture.getHeight();
      for (int i = 1; i < this.modifiedTextures.size(); i++) {
        if (height.floatValue() != this.modifiedTextures.get(i).getHeight()) {
          height = null;
          break;
        }
      }
      setHeight(height);

      String creator = firstTexture.getCreator();
      if (creator != null) {
        for (int i = 1; i < this.modifiedTextures.size(); i++) {
          if (!creator.equals(this.modifiedTextures.get(i).getCreator())) {
            creator = null;
            break;
          }
        }
      }
      setCreator(creator);
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
   * Sets the edited category.
   */
  public void setCategory(TexturesCategory category) {
    if (category != this.category) {
      TexturesCategory oldCategory = this.category;
      this.category = category;
      this.propertyChangeSupport.firePropertyChange(Property.CATEGORY.name(), oldCategory, category);
    }
  }

  /**
   * Returns the edited category.
   */
  public TexturesCategory getCategory() {
    return this.category;
  }
  
  /**
   * Returns the list of available categories in textures library sorted in alphabetical order.
   */
  public List<TexturesCategory> getAvailableCategories() {
    String texturesLanguage = this.texturesLanguageController.getTexturesLangauge();
    Set<TexturesCategory> categories = new TreeSet<TexturesCategory>(getDefaultCategories(texturesLanguage));
    for (CatalogTexture texture : this.texturesLibrary.getTextures()) {
      String categoryName = (String)this.texturesLibrary.getTextureLocalizedData(
          texture, texturesLanguage, TexturesLibrary.TEXTURES_CATEGORY_PROPERTY, texture.getCategory().getName());
      categories.add(new TexturesCategory(categoryName));
    }
    return new ArrayList<TexturesCategory>(categories);
  }

  /**
   * Returns the list of available categories in textures library in the given language.
   */
  public List<TexturesCategory> getDefaultCategories(String language) {
    Locale locale;
    int underscoreIndex = language.indexOf('_');
    if (underscoreIndex != -1) {
      locale = new Locale(language.substring(0, underscoreIndex), language.substring(underscoreIndex + 1));
    } else {
      locale = new Locale(language.length() == 0
          ? this.preferences.getTexturesDefaultLanguage()
          : language);
    }
    ResourceBundle resource = ResourceBundle.getBundle(
        "com.eteks.textureslibraryeditor.viewcontroller.DefaultCategories", locale);
    List<TexturesCategory> categories = new ArrayList<TexturesCategory>();
    int i = 1;
    try {
      while (true) {
        categories.add(new TexturesCategory(resource.getString("defaultTexturesCategory#" + i++)));
      }
    } catch (MissingResourceException ex) {
      // Stop searching for next category
    }
    return categories;
  }

  /**
   * Sets the edited image.
   */
  public void setImage(Content image) {
    if (image != this.image) {
      Content oldImage = this.image;
      this.image = image;
      this.propertyChangeSupport.firePropertyChange(Property.IMAGE.name(), oldImage, image);
    }
  }

  /**
   * Returns the edited image.
   */
  public Content getImage() {
    return this.image;
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
   * Controls the modification of selected textures in the edited home.
   */
  public void modifyTextures() {
    if (!this.modifiedTextures.isEmpty()) {
      String id = getId(); 
      String name = getName();
      TexturesCategory category = getCategory();
      Content image = getImage();
      Float width = getWidth();
      Float height = getHeight();
      String creator = getCreator();
      boolean defaultTexturesLanguage = TexturesLibrary.DEFAULT_LANGUAGE.equals(this.texturesLanguageController.getTexturesLangauge());
      
      // Apply modification
      int texturesCount = this.modifiedTextures.size();
      for (CatalogTexture texture : this.modifiedTextures) {
        int index = this.texturesLibrary.getTextureIndex(texture);
        // Retrieve texture data
        String textureId = texture.getId();
        String textureName = texture.getName();
        TexturesCategory textureCategory = texture.getCategory();
        Content textureImage = texture.getImage();
        float textureWidth = texture.getWidth();
        float textureHeight = texture.getHeight();
        String textureCreator = texture.getCreator();
        // Retrieve localized data
        Map<String, Object> localizedNames = new HashMap<String, Object>();
        retrieveLocalizedData(texture, localizedNames, TexturesLibrary.TEXTURES_NAME_PROPERTY);
        Map<String, Object> localizedCategories = new HashMap<String, Object>();
        retrieveLocalizedData(texture, localizedCategories, TexturesLibrary.TEXTURES_CATEGORY_PROPERTY);
        
        // Update mandatory not localizable data
        if (image != null) {
          textureImage = image;
        }
        if (width != null) {
          textureWidth = width;
        } else if (height != null) {
          textureWidth = texture.getWidth() * height / texture.getHeight();
        }
        if (height != null) {
          textureHeight = height;
        } else if (width != null) {
          textureHeight = texture.getHeight() * width / texture.getWidth();
        }
        // Update not mandatory and not localizable data
        // When only one texture is updated, data can be reset to empty 
        if (id != null || texturesCount == 1) {
          textureId = id;
        }
        if (creator != null || texturesCount == 1) {
          textureCreator = creator;
        }
        // Update mandatory localizable data
        if (name != null) {
          if (defaultTexturesLanguage) {
            textureName = name;
          } else {
            localizedNames.put(this.texturesLanguageController.getTexturesLangauge(), name);
          }
        }
        if (category != null) {
          if (defaultTexturesLanguage) {
            textureCategory = category;
          } else {
            localizedCategories.put(this.texturesLanguageController.getTexturesLangauge(), category.getName());
          }
        }
       
        // Create update texture
        CatalogTexture updatedTexture = new CatalogTexture(textureId, textureName, textureImage, textureWidth, textureHeight, textureCreator);
        
        new TexturesCatalog().add(textureCategory, updatedTexture);
        this.texturesLibrary.addTexture(updatedTexture, index);
        Set<String> supportedLanguages = new HashSet<String>(this.texturesLibrary.getSupportedLanguages());
        supportedLanguages.add(this.texturesLanguageController.getTexturesLangauge());
        for (String language : supportedLanguages) {
          if (!TexturesLibrary.DEFAULT_LANGUAGE.equals(language)) {
            Object localizedTextureName = localizedNames.get(language);
            if (localizedTextureName != null) {
              this.texturesLibrary.setTextureLocalizedData(
                  updatedTexture, language, TexturesLibrary.TEXTURES_NAME_PROPERTY, localizedTextureName);
            }
            Object localizedTextureCategory = localizedCategories.get(language);
            if (localizedTextureCategory != null) {
              this.texturesLibrary.setTextureLocalizedData(
                  updatedTexture, language, TexturesLibrary.TEXTURES_CATEGORY_PROPERTY, localizedTextureCategory);
            }
          }
        }
        
        // Remove old texture from library
        this.texturesLibrary.deleteTexture(texture);
      }
    }
  }

  private void retrieveLocalizedData(CatalogTexture texture,
                                     Map<String, Object> localizedNames,
                                     String propertyKey) {
    for (String language : this.texturesLibrary.getSupportedLanguages()) {
      Object textureData = this.texturesLibrary.getTextureLocalizedData(texture, language, propertyKey);
      if (textureData != null) {
        localizedNames.put(language, textureData);
      }
    }
  }
}
