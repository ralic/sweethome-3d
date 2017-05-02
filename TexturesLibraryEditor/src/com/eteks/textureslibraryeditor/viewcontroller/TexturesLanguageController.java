/*
 * TexturesLanguageController.java 12 sept. 2012
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;

/**
 * A MVC controller for textures language management.  
 * @author Emmanuel Puybaret
 */
public class TexturesLanguageController implements Controller {
  /**
   * The properties of this controller that may change. <code>PropertyChangeListener</code>s added 
   * to this controller will be notified under a property name equal to the name value of one these properties.
   */
  public enum Property {TEXTURES_LANGUAGE}

  private final TexturesLibrary                texturesLibrary;
  private final TexturesLibraryUserPreferences preferences;
  private final EditorViewFactory              viewFactory;
  private final PropertyChangeSupport          propertyChangeSupport;
  private View                                 texturesLanguageView;
  private String                               texturesLanguage;

  public TexturesLanguageController(TexturesLibrary texturesLibrary,
                                     TexturesLibraryUserPreferences  preferences,
                                     EditorViewFactory viewFactory) {
    this.texturesLibrary = texturesLibrary;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.texturesLanguage = TexturesLibrary.DEFAULT_LANGUAGE;
  }
  
  /**
   * Returns the view associated with this controller.
   */
  public View getView() {
    // Create view lazily only once it's needed
    if (this.texturesLanguageView == null) {
      this.texturesLanguageView = viewFactory.createTexturesLanguageView(this.texturesLibrary, this.preferences, this);
    }
    return this.texturesLanguageView;
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
   * Sets the language displayed in textures view.
   */
  public void setTexturesLanguage(String texturesLanguage) {
    if (!texturesLanguage.equals(this.texturesLanguage)) {
      String oldTexturesLanguage = this.texturesLanguage;
      this.texturesLanguage = texturesLanguage;
      this.propertyChangeSupport.firePropertyChange(Property.TEXTURES_LANGUAGE.name(), oldTexturesLanguage, texturesLanguage);
    }
  }
  
  /**
   * Returns the language displayed in the textures view.
   */
  public String getTexturesLangauge() {
    return this.texturesLanguage;
  }
}
