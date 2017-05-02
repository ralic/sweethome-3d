/*
 * UserPreferences.js
 *
 * Sweet Home 3D, Copyright (c) 2015 Emmanuel PUYBARET / eTeks <info@eteks.com>
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

// Requires core.js
//          LengthUnit.js

/**
 * User preferences.
 * @constructor
 * @author Emmanuel Puybaret
 */
function UserPreferences() {
  this.propertyChangeSupport = new PropertyChangeSupport(this);
  // TODO
  // this.classResourceBundles = new HashMap<Class<?>, ResourceBundle>();
  // this.resourceBundles = new HashMap<String, ResourceBundle>();

  this.supportedLanguages = UserPreferences.DEFAULT_SUPPORTED_LANGUAGES;
  this.defaultCountry = navigator.language.substring(navigator.language.indexOf("_") + 1, navigator.language.length);    
  var defaultLanguage = navigator.language.substring(0, navigator.language.indexOf("_"));
  // Find closest language among supported languages in Sweet Home 3D
  // For example, use simplified Chinese even for Chinese users (zh_?) not from China (zh_CN)
  // unless their exact locale is supported as in Taiwan (zh_TW)
  for (var i = 0; i < this.supportedLanguages; i++) {
    var supportedLanguage = this.supportedLanguages [i];
    if (supportedLanguage == defaultLanguage + "_" + this.defaultCountry) {
      this.language = supportedLanguage;
      break; // Found the exact supported language
    } else if (this.language === null 
               && supportedLanguage.indexOf(defaultLanguage) === 0) {
      this.language = supportedLanguage; // Found a supported language
    }
  }
  // If no language was found, let's use English by default
  if (this.language === null) {
    this.language = "en";
  }
  
  this.resourceBundles = {};
  this.furnitureCatalog = null;
  this.texturesCatalog = null;
  this.patternsCatalog = null;
  this.currency = null;
  this.unit = null;
  this.furnitureCatalogViewedInTree = true;
  this.aerialViewCenteredOnSelectionEnabled = false;
  this.navigationPanelVisible = true;
  this.magnetismEnabled = true;
  this.rulersVisible    = true;
  this.gridVisible      = true;
  this.defaultFontName  = null;
  this.furnitureViewedFromTop = true;
  this.roomFloorColoredOrTextured = true;
  this.wallPattern = null;
  this.newWallPattern = null;
  this.newWallThickness = 7.5;
  this.newWallHeight = 250;
  this.newWallBaseboardThickness = 1;
  this.newWallBaseboardHeight = 7;
  this.newFloorThickness = 12;
  this.recentHomes = [];
  this.autoSaveDelayForRecovery;
  this.autoCompletionStrings = {};
  this.recentColors = [];
  this.recentTextures = [];
}

UserPreferences.DEFAULT_SUPPORTED_LANGUAGES = ["en"]; 

UserPreferences.DEFAULT_TEXT_STYLE = new TextStyle(18);
UserPreferences.DEFAULT_ROOM_TEXT_STYLE = new TextStyle(24);


/**
 * Adds the <code>listener</code> in parameter to these preferences. 
 * The listener is a function that will receive in parameter an event of {@link PropertyChangeEvent} class.
 */
UserPreferences.prototype.addPropertyChangeListener = function(property,  listener) {
  this.propertyChangeSupport.addPropertyChangeListener(property, listener);
}

/**
 * Removes the <code>listener</code> in parameter from these preferences.
 */
UserPreferences.prototype.removePropertyChangeListener = function(property, listener) {
  this.propertyChangeSupport.removePropertyChangeListener(property, listener);
}

/**
 * Returns the furniture catalog.
 * @ignore
 */
UserPreferences.prototype.getFurnitureCatalog = function() {
  return this.furnitureCatalog;
}

/**
 * Sets furniture catalog.
 * @ignore
 */
UserPreferences.prototype.setFurnitureCatalog = function(catalog) {
  this.furnitureCatalog = catalog;
}

/**
 * Returns the textures catalog.
 * @ignore
 */
UserPreferences.prototype.getTexturesCatalog = function() {
  return this.texturesCatalog;
}

/**
 * Sets textures catalog.
 * @ignore
 */
UserPreferences.prototype.setTexturesCatalog = function(catalog) {
  this.texturesCatalog = catalog;
}

/**
 * Returns the patterns catalog available to fill plan areas. 
 * @ignore
 */
UserPreferences.prototype.getPatternsCatalog = function() {
  return this.patternsCatalog;
}

/**
 * Sets the patterns available to fill plan areas.
 * @ignore
 */
UserPreferences.prototype.setPatternsCatalog = function(catalog) {
  this.patternsCatalog = catalog;
}

/**
 * Returns the length unit currently in use.
 */
UserPreferences.prototype.getLengthUnit = function() {
  return this.unit;
}

/**
 * Changes the unit currently in use, and notifies listeners of this change. 
 * @param unit one of the values of Unit.
 */
UserPreferences.prototype.setUnit = function(unit) {
  if (this.unit !== unit) {
    var oldUnit = this.unit;
    this.unit = unit;
    this.propertyChangeSupport.firePropertyChange("UNIT", oldUnit, unit);
  }
}

/**
 * Returns the preferred language to display information, noted with an ISO 639 code
 * that may be followed by an underscore and an ISO 3166 code.
 * @ignore 
 */
UserPreferences.prototype.getLanguage = function() {
  return this.language;
}

/**
 * If language can be changed, sets the preferred language to display information, 
 * changes current default locale accordingly and notifies listeners of this change.
 * @param language an ISO 639 code that may be followed by an underscore and an ISO 3166 code
 *            (for example fr, de, it, en_US, zh_CN). 
 * @ignore 
 */
UserPreferences.prototype.setLanguage = function(language) {
  if (language != this.language
      && this.isLanguageEditable()) {
    var oldLanguage = this.language;
    this.language = language;
    // TODO
    // this.classResourceBundles.clear();
    // this.resourceBundles.clear();
    this.propertyChangeSupport.firePropertyChange("LANGUAGE", oldLanguage, language);
  }
}

/**
 * Returns <code>true</code> if the language in preferences can be set.
 * @return <code>true</code> except if <code>user.language</code> System property isn't writable.
 * @ignore 
 */
UserPreferences.prototype.isLanguageEditable = function() {
  return true;
}

/**
 * Returns the array of default available languages in Sweet Home 3D.
 * @ignore 
 */
UserPreferences.prototype.getDefaultSupportedLanguages = function() {
  return UserPreferences.DEFAULT_SUPPORTED_LANGUAGES.slice(0);
}

/**
 * Returns the array of available languages in Sweet Home 3D including languages in libraries.
 * @ignore 
 */
UserPreferences.prototype.getSupportedLanguages = function() {
  return this.supportedLanguages.slice(0);
}

/**
 * Returns the array of available languages in Sweet Home 3D.
 * @ignore 
 */
UserPreferences.prototype.setSupportedLanguages = function(supportedLanguages) {
  if (this.supportedLanguages != supportedLanguages) {
    var oldSupportedLanguages = this.supportedLanguages;
    this.supportedLanguages = supportedLanguages.slice(0);
    this.propertyChangeSupport.firePropertyChange("SUPPORTED_LANGUAGES", 
        oldSupportedLanguages, supportedLanguages);
  }
}

/**
 * Returns the string matching <code>resourceKey</code> in current language in the 
 * context of <code>resourceClass</code> or for a resource family if <code>resourceClass</code>
 * is a string.
 * If <code>resourceParameters</code> isn't empty the string is considered
 * as a format string, and the returned string will be formatted with these parameters. 
 * This implementation searches first the key in a properties file named as 
 * <code>resourceClass</code>, then if this file doesn't exist, it searches 
 * the key prefixed by <code>resourceClass</code> name and a dot in a package.properties file 
 * in the folder matching the package of <code>resourceClass</code>. 
 * @throws IllegalArgumentException if no string for the given key can be found
 * @throws UnsupportedOperationException Not implemented yet
 * @ignore
 */
UserPreferences.prototype.getLocalizedString = function(resourceClass, resourceKey, resourceParameters) {
  // TODO Implement
  throw new UnsupportedOperationException("Not implemented yet");
  
  if (typeof resourceClass !== "string") {
    var classResourceBundle = this.classResourceBundles [resourceClass];
    if (classResourceBundle === null) {
      try {      
        classResourceBundle = this.getResourceBundle(resourceClass.constructor.name);
        this.classResourceBundles [resourceClass.constructor.name] = classResourceBundle;
      } catch (ex) {
        try {
          var className = resourceClass.constructor.name;
          var lastIndex = className.lastIndexOf(".");
          var resourceFamily;
          if (lastIndex !== -1) {
            resourceFamily = className.substring(0, lastIndex) + ".package";
          } else {
            resourceFamily = "package";
          }
          classResourceBundle = new PrefixedResourceBundle(getResourceBundle(resourceFamily), 
              resourceClass.constructor.name + ".");
          this.classResourceBundles [resourceClass.constructor.name] = classResourceBundle;
        } catch (ex2) {
          throw new IllegalArgumentException(
              "Can't find resource bundle for " + resourceClass, ex);
        }
      }
    } 
    
    return this.getBundleLocalizedString(classResourceBundle, resourceKey, resourceParameters);
  } else {
    var resourceFamily = resourceClass;
    
    try {      
      var resourceBundle = this.getResourceBundle(resourceFamily);
      return this.getBundleLocalizedString(resourceBundle, resourceKey, resourceParameters);
    } catch (ex) {
      throw new IllegalArgumentException(
          "Can't find resource bundle for " + resourceFamily, ex);
    }
  }
}

/**
 * Returns a new resource bundle for the given <code>familyName</code> 
 * that matches current default locale. The search will be done
 * only among .properties files.
 * @throws IOException if no .properties file was found
 * @private
 */
UserPreferences.prototype.getResourceBundle = function(resourceFamily) {
  resourceFamily = resourceFamily.replace('.', '/');
  var resourceBundle = this.resourceBundles.get(resourceFamily);
  if (resourceBundle !== null) {
    return resourceBundle;
  }
  var defaultLocale = this.language;
  var language = defaultLocale.getLanguage();
  var country = defaultLocale.getCountry();
  var suffixes = [".properties",
                  "_" + language + ".properties",
                  "_" + language + "_" + country + ".properties"];
  for (var i = 0; i < suffixes.length; i++) {
    var suffix = suffixes [i];
    var classLoaders = this.getResourceClassLoaders();
    for (var j = 0; j < classLoaders.length; j++) {
      var classLoader = classLoaders [j];
      var input = classLoader.getResourceAsStream(resourceFamily + suffix);
      if (input !== null) {
        var parentResourceBundle = resourceBundle;
        try {
          resourceBundle = new PropertyResourceBundle(input);
          resourceBundle.setParent(parentResourceBundle);
          break;
        } catch (ex) {
          // May happen if the file contains some wrongly encoded characters
          ex.printStackTrace();
        } finally {
          input.close();
        }
      }
    }
  }
  if (resourceBundle === null) {
    throw new IOException("No available resource bundle for " + resourceFamily);
  }
  this.resourceBundles.put(resourceFamily, resourceBundle);
  return resourceBundle;
}

/**
 * Returns the string matching <code>resourceKey</code> for the given resource bundle.
 * @private
 */
UserPreferences.prototype.getBundleLocalizedString = function(resourceBundle, resourceKey, resourceParameters) {
  try {
    var localizedString = resourceBundle.getString(resourceKey);
    if (resourceParameters.length > 0) {
      localizedString = String.format(localizedString, resourceParameters);
    }      
    return localizedString;
  } catch (ex) {
    throw new IllegalArgumentException("Unknown key " + resourceKey);
  }
}

/**
 * Returns the class loaders through which localized strings returned by 
 * <code>getLocalizedString</code> might be loaded.
 * @throws UnsupportedOperationException Not implemented yet
 * @ignore
 */
UserPreferences.prototype.getResourceClassLoaders = function() {
  // TODO Implement ?
  throw new UnsupportedOperationException("Not implemented yet");
  
  return DEFAULT_CLASS_LOADER;
}

/**
 * Returns the default currency in use, noted with ISO 4217 code, or <code>null</code> 
 * if prices aren't used in application.
 * @ignore
 */
UserPreferences.prototype.getCurrency = function() {
  return this.currency;
}

/**
 * Sets the default currency in use.
 * @package
 * @ignore
 */
UserPreferences.prototype.setCurrency = function(currency) {
  this.currency = currency;
}
  
/**
 * Returns <code>true</code> if the furniture catalog should be viewed in a tree.
 * @ignore
 */
UserPreferences.prototype.isFurnitureCatalogViewedInTree = function() {
  return this.furnitureCatalogViewedInTree;
}

/**
 * Sets whether the furniture catalog should be viewed in a tree or a different way.
 * @ignore
 */
UserPreferences.prototype.setFurnitureCatalogViewedInTree = function(furnitureCatalogViewedInTree) {
  if (this.furnitureCatalogViewedInTree !== furnitureCatalogViewedInTree) {
    this.furnitureCatalogViewedInTree = furnitureCatalogViewedInTree;
    this.propertyChangeSupport.firePropertyChange("FURNITURE_CATALOG_VIEWED_IN_TREE", 
        !furnitureCatalogViewedInTree, furnitureCatalogViewedInTree);
  }
}

/**
 * Returns <code>true</code> if the navigation panel should be displayed.
 */
UserPreferences.prototype.isNavigationPanelVisible = function() {
  return this.navigationPanelVisible;
}

/**
 * Sets whether the navigation panel should be displayed or not.
 */
UserPreferences.prototype.setNavigationPanelVisible = function(navigationPanelVisible) {
  if (this.navigationPanelVisible !== navigationPanelVisible) {
    this.navigationPanelVisible = navigationPanelVisible;
    this.propertyChangeSupport.firePropertyChange("NAVIGATION_PANEL_VISIBLE", 
        !navigationPanelVisible, navigationPanelVisible);
  }
}

/**
 * Sets whether aerial view should be centered on selection or not.
 */
UserPreferences.prototype.setAerialViewCenteredOnSelectionEnabled = function(aerialViewCenteredOnSelectionEnabled) {
  if (aerialViewCenteredOnSelectionEnabled !== this.aerialViewCenteredOnSelectionEnabled) {
    this.aerialViewCenteredOnSelectionEnabled = aerialViewCenteredOnSelectionEnabled;
    this.propertyChangeSupport.firePropertyChange("AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED", 
        !aerialViewCenteredOnSelectionEnabled, aerialViewCenteredOnSelectionEnabled);
  }
}

/**
 * Returns whether aerial view should be centered on selection or not.
 */
UserPreferences.prototype.isAerialViewCenteredOnSelectionEnabled = function() {
  return this.aerialViewCenteredOnSelectionEnabled;
}

/**
 * Returns <code>true</code> if magnetism is enabled.
 * @return <code>true</code> by default.
 * @ignore
 */
UserPreferences.prototype.isMagnetismEnabled = function() {
  return this.magnetismEnabled;
}

/**
 * Sets whether magnetism is enabled or not, and notifies
 * listeners of this change. 
 * @param magnetismEnabled <code>true</code> if magnetism is enabled,
 *          <code>false</code> otherwise.
 * @ignore
 */
UserPreferences.prototype.setMagnetismEnabled = function(magnetismEnabled) {
  if (this.magnetismEnabled !== magnetismEnabled) {
    this.magnetismEnabled = magnetismEnabled;
    this.propertyChangeSupport.firePropertyChange("MAGNETISM_ENABLED", 
        !magnetismEnabled, magnetismEnabled);
  }
}

/**
 * Returns <code>true</code> if rulers are visible.
 * @return <code>true</code> by default.
 * @ignore
 */
UserPreferences.prototype.isRulersVisible = function() {
  return this.rulersVisible;
}

/**
 * Sets whether rulers are visible or not, and notifies
 * listeners of this change. 
 * @param rulersVisible <code>true</code> if rulers are visible,
 *          <code>false</code> otherwise.
 * @ignore
 */
UserPreferences.prototype.setRulersVisible = function(rulersVisible) {
  if (this.rulersVisible !== rulersVisible) {
    this.rulersVisible = rulersVisible;
    this.propertyChangeSupport.firePropertyChange("RULERS_VISIBLE", 
        !rulersVisible, rulersVisible);
  }
}

/**
 * Returns <code>true</code> if plan grid visible.
 * @return <code>true</code> by default.
 * @ignore
 */
UserPreferences.prototype.isGridVisible = function() {
  return this.gridVisible;
}

/**
 * Sets whether plan grid is visible or not, and notifies
 * listeners of this change. 
 * @param gridVisible <code>true</code> if grid is visible,
 *          <code>false</code> otherwise.
 * @ignore
 */
UserPreferences.prototype.setGridVisible = function(gridVisible) {
  if (this.gridVisible !== gridVisible) {
    this.gridVisible = gridVisible;
    this.propertyChangeSupport.firePropertyChange("GRID_VISIBLE", 
        !gridVisible, gridVisible);
  }
}

/**
 * Returns the name of the font that should be used by default or <code>null</code> 
 * if the default font should be the default one in the application.
 * @ignore
 */
UserPreferences.prototype.getDefaultFontName = function() {
  return this.defaultFontName;
}

/**
 * Sets the name of the font that should be used by default.
 * @ignore
 */
UserPreferences.prototype.setDefaultFontName = function(defaultFontName) {
  if (defaultFontName != this.defaultFontName) {
    var oldName = this.defaultFontName;
    this.defaultFontName = defaultFontName;
    this.propertyChangeSupport.firePropertyChange("DEFAULT_FONT_NAME", oldName, defaultFontName);
  }
}

/**
 * Returns <code>true</code> if furniture should be viewed from its top in plan.
 * @ignore
 */
UserPreferences.prototype.isFurnitureViewedFromTop = function() {
  return this.furnitureViewedFromTop;
}

/**
 * Sets how furniture icon should be displayed in plan, and notifies
 * listeners of this change. 
 * @param furnitureViewedFromTop if <code>true</code> the furniture 
 *    should be viewed from its top.
 * @ignore
 */
UserPreferences.prototype.setFurnitureViewedFromTop = function(furnitureViewedFromTop) {
  if (this.furnitureViewedFromTop !== furnitureViewedFromTop) {
    this.furnitureViewedFromTop = furnitureViewedFromTop;
    this.propertyChangeSupport.firePropertyChange("FURNITURE_VIEWED_FROM_TOP", 
        !furnitureViewedFromTop, furnitureViewedFromTop);
  }
}

/**
 * Returns <code>true</code> if room floors should be rendered with color or texture 
 * in plan.
 * @return <code>false</code> by default.
 * @ignore
 */
UserPreferences.prototype.isRoomFloorColoredOrTextured = function() {
  return this.roomFloorColoredOrTextured;
}

/**
 * Sets whether room floors should be rendered with color or texture, 
 * and notifies listeners of this change. 
 * @param roomFloorColoredOrTextured <code>true</code> if floor color 
 *          or texture is used, <code>false</code> otherwise.
 * @ignore
 */
UserPreferences.prototype.setFloorColoredOrTextured = function(roomFloorColoredOrTextured) {
  if (this.roomFloorColoredOrTextured !== roomFloorColoredOrTextured) {
    this.roomFloorColoredOrTextured = roomFloorColoredOrTextured;
    this.propertyChangeSupport.firePropertyChange("ROOM_FLOOR_COLORED_OR_TEXTURED", 
        !roomFloorColoredOrTextured, roomFloorColoredOrTextured);
  }
}

/**
 * Returns the wall pattern in plan used by default.
 * @ignore
 */
UserPreferences.prototype.getWallPattern = function() {
  return this.wallPattern;
}

/**
 * Sets how walls should be displayed in plan by default, and notifies
 * listeners of this change.
 * @ignore
 */
UserPreferences.prototype.setWallPattern = function(wallPattern) {
  if (this.wallPattern !== wallPattern) {
    var oldWallPattern = this.wallPattern;
    this.wallPattern = wallPattern;
    this.propertyChangeSupport.firePropertyChange("WALL_PATTERN", 
        oldWallPattern, wallPattern);
  }
}

/**
 * Returns the pattern used for new walls in plan or <code>null</code> if it's not set.
 * @ignore
 */
UserPreferences.prototype.getNewWallPattern = function() {
  return this.newWallPattern;
}

/**
 * Sets how new walls should be displayed in plan, and notifies
 * listeners of this change.
 * @ignore
 */
UserPreferences.prototype.setNewWallPattern = function(newWallPattern) {
  if (this.newWallPattern !== newWallPattern) {
    var oldWallPattern = this.newWallPattern;
    this.newWallPattern = newWallPattern;
    this.propertyChangeSupport.firePropertyChange("NEW_WALL_PATTERN", 
        oldWallPattern, newWallPattern);
  }
}

/**
 * Returns default thickness of new walls in home. 
 * @ignore
 */
UserPreferences.prototype.getNewWallThickness = function() {
  return this.newWallThickness;
}

/**
 * Sets default thickness of new walls in home, and notifies
 * listeners of this change.  
 * @ignore
 */
UserPreferences.prototype.setNewWallThickness = function(newWallThickness) {
  if (this.newWallThickness !== newWallThickness) {
    var oldDefaultThickness = this.newWallThickness;
    this.newWallThickness = newWallThickness;
    this.propertyChangeSupport.firePropertyChange("NEW_WALL_THICKNESS", 
        oldDefaultThickness, newWallThickness);
  }
}

/**
 * Returns default wall height of new home walls. 
 * @ignore
 */
UserPreferences.prototype.getNewWallHeight = function() {
  return this.newWallHeight;
}

/**
 * Sets default wall height of new walls, and notifies
 * listeners of this change. 
 * @ignore
 */
UserPreferences.prototype.setNewWallHeight = function(newWallHeight) {
  if (this.newWallHeight !== newWallHeight) {
    var oldWallHeight = this.newWallHeight;
    this.newWallHeight = newWallHeight;
    this.propertyChangeSupport.firePropertyChange("NEW_WALL_HEIGHT", 
        oldWallHeight, newWallHeight);
  }
}

/**
 * Returns default baseboard thickness of new walls in home. 
 * @ignore
 */
UserPreferences.prototype.getNewWallBaseboardThickness = function() {
  return this.newWallBaseboardThickness;
}

/**
 * Sets default baseboard thickness of new walls in home, and notifies
 * listeners of this change.  
 * @ignore
 */
UserPreferences.prototype.setNewWallBaseboardThickness = function(newWallBaseboardThickness) {
  if (this.newWallBaseboardThickness !== newWallBaseboardThickness) {
    var oldThickness = this.newWallBaseboardThickness;
    this.newWallBaseboardThickness = newWallBaseboardThickness;
    this.propertyChangeSupport.firePropertyChange("NEW_WALL_SIDEBOARD_THICKNESS", 
        oldThickness, newWallBaseboardThickness);
  }
}

/**
 * Returns default baseboard height of new home walls. 
 * @ignore
 */
UserPreferences.prototype.getNewWallBaseboardHeight = function() {
  return this.newWallBaseboardHeight;
}

/**
 * Sets default baseboard height of new walls, and notifies
 * listeners of this change. 
 * @ignore
 */
UserPreferences.prototype.setNewWallBaseboardHeight = function(newWallBaseboardHeight) {
  if (this.newWallBaseboardHeight !== newWallBaseboardHeight) {
    var oldHeight = this.newWallBaseboardHeight;
    this.newWallBaseboardHeight = newWallBaseboardHeight;
    this.propertyChangeSupport.firePropertyChange("NEW_WALL_SIDEBOARD_HEIGHT", 
        oldHeight, newWallBaseboardHeight);
  }
}

/**
 * Returns default thickness of the floor of new levels in home. 
 * @ignore
 */
UserPreferences.prototype.getNewFloorThickness = function() {
  return this.newFloorThickness;
}

/**
 * Sets default thickness of the floor of new levels in home, and notifies
 * listeners of this change.  
 * @ignore
 */
UserPreferences.prototype.setNewFloorThickness = function(newFloorThickness) {
  if (this.newFloorThickness !== newFloorThickness) {
    var oldFloorThickness = this.newFloorThickness;
    this.newFloorThickness = newFloorThickness;
    this.propertyChangeSupport.firePropertyChange("NEW_FLOOR_THICKNESS", 
        oldFloorThickness, newFloorThickness);
  }
}

/**
 * Returns the delay between two automatic save operations of homes for recovery purpose.
 * @return a delay in milliseconds or 0 to disable auto save.
 * @ignore
 */
UserPreferences.prototype.getAutoSaveDelayForRecovery = function() {
  return this.autoSaveDelayForRecovery;
}

/**
 * Sets the delay between two automatic save operations of homes for recovery purpose.
 * @ignore
 */
UserPreferences.prototype.setAutoSaveDelayForRecovery = function(autoSaveDelayForRecovery) {
  if (this.autoSaveDelayForRecovery !== autoSaveDelayForRecovery) {
    var oldAutoSaveDelayForRecovery = this.autoSaveDelayForRecovery;
    this.autoSaveDelayForRecovery = autoSaveDelayForRecovery;
    this.propertyChangeSupport.firePropertyChange("AUTO_SAVE_DELAY_FOR_RECOVERY", 
        oldAutoSaveDelayForRecovery, autoSaveDelayForRecovery);
  }
}

/**
 * Returns an unmodifiable list of the recent homes.
 * @ignore
 */
UserPreferences.prototype.getRecentHomes = function() {
  return Collections.unmodifiableList(this.recentHomes);
}

/**
 * Sets the recent homes list and notifies listeners of this change.
 * @ignore
 */
UserPreferences.prototype.setRecentHomes = function(recentHomes) {
  if (recentHomes != this.recentHomes) {
    var oldRecentHomes = this.recentHomes;
    this.recentHomes = new ArrayList<String>(recentHomes);
    this.propertyChangeSupport.firePropertyChange("RECENT_HOMES", 
        oldRecentHomes, this.getRecentHomes());
  }
}

/**
 * Returns the maximum count of homes that should be proposed to the user.
 * @ignore
 */
UserPreferences.prototype.getRecentHomesMaxCount = function() {
  return 10;
}

/**
 * Returns the maximum count of stored cameras in homes that should be proposed to the user.
 * @ignore
 */
UserPreferences.prototype.getStoredCamerasMaxCount = function() {
  return 50;
}

/**
 * Sets which action tip should be ignored.
 * <br>This method should be overridden to store the ignore information.
 * By default it just notifies listeners of this change. 
 * @ignore
 */
UserPreferences.prototype.setActionTipIgnored = function(actionKey) {    
  this.propertyChangeSupport.firePropertyChange("IGNORED_ACTION_TIP", null, actionKey);
}

/**
 * Returns whether an action tip should be ignored or not. 
 * <br>This method should be overridden to return the display information
 * stored in setActionTipIgnored.
 * By default it returns <code>true</code>. 
 * @ignore
 */
UserPreferences.prototype.isActionTipIgnored = function(actionKey) {
  return true;
}

/**
 * Resets the ignore flag of action tips.
 * <br>This method should be overridden to clear all the display flags.
 * By default it just notifies listeners of this change. 
 * @ignore
 */
UserPreferences.prototype.resetIgnoredActionTips = function() {    
  this.propertyChangeSupport.firePropertyChange("IGNORED_ACTION_TIP", null, null);
}

/**
 * Returns the default text style of a class of selectable item. 
 * @ignore
 */
UserPreferences.prototype.getDefaultTextStyle = function(selectableClass) {
  if (selectableClass.name == "Room") {
    return UserPreferences.DEFAULT_ROOM_TEXT_STYLE;
  } else {
    return UserPreferences.DEFAULT_TEXT_STYLE;
  }
}

/**
 * Returns the strings that may be used for the auto completion of the given <code>property</code>.
 * @ignore
 */
UserPreferences.prototype.getAutoCompletionStrings = function(property) {
  var propertyAutoCompletionStrings = this.autoCompletionStrings.get(property);
  if (propertyAutoCompletionStrings !== null) {
    return Collections.unmodifiableList(propertyAutoCompletionStrings);
  } else {
    return [];
  }
}

/**
 * Adds the given string to the list of the strings used in auto completion of a <code>property</code>
 * and notifies listeners of this change.
 * @ignore
 */
UserPreferences.prototype.addAutoCompletionString = function(property, autoCompletionString) {
  if (autoCompletionString !== null 
      && autoCompletionString.length() > 0) {
    var propertyAutoCompletionStrings = this.autoCompletionStrings [property];
    if (propertyAutoCompletionStrings === undefined) {
      propertyAutoCompletionStrings = [];
    } else if (!propertyAutoCompletionStrings.contains(autoCompletionString)) {
      propertyAutoCompletionStrings = new ArrayList<String>(propertyAutoCompletionStrings);
    } else {
      return;
    }
    propertyAutoCompletionStrings.splice(0, 0, autoCompletionString);
    this.setAutoCompletionStrings(property, propertyAutoCompletionStrings);
  }
}

/**
 * Sets the auto completion strings list of the given <code>property</code> and notifies listeners of this change.
 * @ignore
 */
UserPreferences.prototype.setAutoCompletionStrings = function(property, autoCompletionStrings) {
  var propertyAutoCompletionStrings = this.autoCompletionStrings [property];
  if (autoCompletionStrings != propertyAutoCompletionStrings) {
    this.autoCompletionStrings.put(property, autoCompletionStrings.slice(0));
    this.propertyChangeSupport.firePropertyChange("AUTO_COMPLETION_STRINGS", 
        null, property);
  }
}

/**
 * Returns the list of properties with auto completion strings. 
 * @ignore
 */
UserPreferences.prototype.getAutoCompletedProperties = function() {
  if (this.autoCompletionStrings !== null) {
    return Object.keys(this.autoCompletionStrings);
  } else {
    return Collections.emptyList();
  }
}

/**
 * Returns the list of the recent colors.
 * @ignore
 */
UserPreferences.prototype.getRecentColors = function() {
  return this.recentColors;
}

/**
 * Sets the recent colors list and notifies listeners of this change.
 * @ignore
 */
UserPreferences.prototype.setRecentColors = function(recentColors) {
  if (recentColors != this.recentColors) {
    var oldRecentColors = this.recentColors;
    this.recentColors = recentColors.slice(0);
    this.propertyChangeSupport.firePropertyChange("RECENT_COLORS", 
        oldRecentColors, this.getRecentColors());
  }
}

/**
 * Returns the list of the recent textures.
 * @ignore
 */
UserPreferences.prototype.getRecentTextures = function() {
  return this.recentTextures;
}

/**
 * Sets the recent colors list and notifies listeners of this change.
 * @ignore
 */
UserPreferences.prototype.setRecentTextures = function(recentTextures) {
  if (recentTextures != this.recentTextures) {
    var oldRecentTextures = this.recentTextures;
    this.recentTextures = recentTextures.slice(0);
    this.propertyChangeSupport.firePropertyChange("RECENT_TEXTURES", 
        oldRecentTextures, this.getRecentTextures());
  }
}

/**
 * A resource bundle with a prefix added to resource key.
 */
// TODO
//private static class PrefixedResourceBundle extends ResourceBundle {
//  private ResourceBundle resourceBundle;
//  private var         keyPrefix;
//
//  public PrefixedResourceBundle(ResourceBundle resourceBundle, 
//                                var keyPrefix) {
//    this.resourceBundle = resourceBundle;
//    this.keyPrefix = keyPrefix;
//  }
//  
////  public Locale getLocale() {
//    return this.resourceBundle.getLocale();
//  }
//  
////  protected Object handleGetObject(String key) {
//    key = this.keyPrefix + key;
//    return this.resourceBundle.getObject(key);
//  }    
//
////  public Enumeration<String> getKeys() {
//    return this.resourceBundle.getKeys();
//  }    
//}


/**
 * Default user preferences.
 * @constructor
 * @extends UserPreferences
 * @author Emmanuel Puybaret
 */
function DefaultUserPreferences() {
  UserPreferences.call(this);
  this.setUnit(LengthUnit.CENTIMETER);
  this.setNavigationPanelVisible(false);
}
DefaultUserPreferences.prototype = Object.create(UserPreferences.prototype);
DefaultUserPreferences.prototype.constructor = DefaultUserPreferences;
