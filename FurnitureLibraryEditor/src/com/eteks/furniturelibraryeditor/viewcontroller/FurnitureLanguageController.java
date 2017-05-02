/*
 * FurnitureLanguageController.java 4 juin 2010
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.sweethome3d.viewcontroller.Controller;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * A MVC controller for furniture language management.  
 * @author Emmanuel Puybaret
 */
public class FurnitureLanguageController implements Controller {
  /**
   * The properties of this controller that may change. <code>PropertyChangeListener</code>s added 
   * to this controller will be notified under a property name equal to the name value of one these properties.
   */
  public enum Property {FURNITURE_LANGUAGE}

  private final FurnitureLibrary                furnitureLibrary;
  private final FurnitureLibraryUserPreferences preferences;
  private final EditorViewFactory               viewFactory;
  private final PropertyChangeSupport           propertyChangeSupport;
  private View                                  furnitureLanguageView;
  private String                                furnitureLanguage;

  public FurnitureLanguageController(FurnitureLibrary furnitureLibrary,
                                     FurnitureLibraryUserPreferences  preferences,
                                     EditorViewFactory viewFactory) {
    this.furnitureLibrary = furnitureLibrary;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.furnitureLanguage = FurnitureLibrary.DEFAULT_LANGUAGE;
  }
  
  /**
   * Returns the view associated with this controller.
   */
  public View getView() {
    // Create view lazily only once it's needed
    if (this.furnitureLanguageView == null) {
      this.furnitureLanguageView = viewFactory.createFurnitureLanguageView(this.furnitureLibrary, this.preferences, this);
    }
    return this.furnitureLanguageView;
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
   * Sets the language displayed in furniture view.
   */
  public void setFurnitureLanguage(String furnitureLanguage) {
    if (!furnitureLanguage.equals(this.furnitureLanguage)) {
      String oldFurnitureLanguage = this.furnitureLanguage;
      this.furnitureLanguage = furnitureLanguage;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_LANGUAGE.name(), oldFurnitureLanguage, furnitureLanguage);
    }
  }
  
  /**
   * Returns the language displayed in the furniture view.
   */
  public String getFurnitureLangauge() {
    return this.furnitureLanguage;
  }
}
