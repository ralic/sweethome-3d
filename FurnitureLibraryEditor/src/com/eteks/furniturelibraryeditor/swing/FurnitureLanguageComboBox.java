/*
 * FurnitureLanguageComboBox.java 4 juin 2010
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
package com.eteks.furniturelibraryeditor.swing;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureLanguageController;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * A combo box showing languages to localize furniture.
 * @author Emmanuel Puybaret
 */
public class FurnitureLanguageComboBox extends JComboBox implements View {
  private static final TreeSet<Locale> LOCALES;
  
  static {
    LOCALES = new TreeSet<Locale>(new Comparator<Locale>() {
        public int compare(Locale l1, Locale l2) {
          int languageComparison = l1.getLanguage().compareTo(l2.getLanguage());
          if (languageComparison != 0) {
            return languageComparison;
          } else 
            return l1.getCountry().compareTo(l2.getCountry());
          }
        });
    LOCALES.add(new Locale(FurnitureLibrary.DEFAULT_LANGUAGE));
    for (Locale locale : Locale.getAvailableLocales()) {
      LOCALES.add(locale);
    }
    // Remove from locales the one with only one country
    for (Iterator<Locale> it = LOCALES.iterator(); it.hasNext(); ) {
      Locale locale = it.next();
      if (locale.getCountry().length() > 0) {
        int languageCount = 0;
        for (Locale otherLocale : LOCALES) {
          if (otherLocale.getLanguage().equals(locale.getLanguage())) {
            languageCount++;
          }
        }
        if (languageCount == 2) {
          it.remove();
        }
      }
    }
  }

  public FurnitureLanguageComboBox(final FurnitureLibrary library,
                                   final FurnitureLibraryUserPreferences  preferences,
                                   final FurnitureLanguageController controller) {
    super(LOCALES.toArray());
    setRenderer(new DefaultListCellRenderer() {
        private Font defaultFont;
        private Font supportedLanguageFont;
        
        @Override
        public Component getListCellRendererComponent(JList list, 
            Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Locale locale = (Locale)value;
          String displayedValue = locale.getDisplayLanguage(locale);
          if (FurnitureLibrary.DEFAULT_LANGUAGE.equals(displayedValue)) {
            displayedValue = preferences.getLocalizedString(FurnitureLanguageComboBox.class, "defaultLanguage");
          } else {
            displayedValue = Character.toUpperCase(displayedValue.charAt(0)) + displayedValue.substring(1);
            if (locale.getCountry().length() > 0) {
              displayedValue += " - " + locale.getDisplayCountry(locale); 
            }
          }
          super.getListCellRendererComponent(list, displayedValue, index, isSelected,
              cellHasFocus);
          // Initialize fonts if not done
          if (this.defaultFont == null) {
            this.defaultFont = getFont();
            this.supportedLanguageFont = 
                new Font(this.defaultFont.getFontName(), Font.BOLD, this.defaultFont.getSize());
            
          }
          setFont(library.getSupportedLanguages().contains(locale.toString())
              ? supportedLanguageFont : defaultFont);
          return this;
        }
      });
    final PropertyChangeListener furnitureLanguageChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          String furnitureLangauge = controller.getFurnitureLangauge();
          int underscoreIndex = furnitureLangauge.indexOf('_');
          if (underscoreIndex != -1) {
            setSelectedItem(new Locale(furnitureLangauge.substring(0, underscoreIndex), 
                furnitureLangauge.substring(underscoreIndex + 1)));
          } else {
            setSelectedItem(new Locale(furnitureLangauge));
          }
        }
      };
    controller.addPropertyChangeListener(FurnitureLanguageController.Property.FURNITURE_LANGUAGE, 
        furnitureLanguageChangeListener);
    addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          controller.removePropertyChangeListener(FurnitureLanguageController.Property.FURNITURE_LANGUAGE, 
              furnitureLanguageChangeListener);
          controller.setFurnitureLanguage(getSelectedItem().toString());
          controller.addPropertyChangeListener(FurnitureLanguageController.Property.FURNITURE_LANGUAGE, 
              furnitureLanguageChangeListener);
        }
      });
    preferences.addPropertyChangeListener(
        UserPreferences.Property.LANGUAGE, new UserPreferencesChangeListener(this));
    setMaximumRowCount(20);
  }

  /**
   * Preferences property listener bound to this combo box with a weak reference to avoid
   * strong link between user preferences and this combo box.  
   */
  private static class UserPreferencesChangeListener implements PropertyChangeListener {
    private WeakReference<FurnitureLanguageComboBox> furnitureLanguageComboBox;

    public UserPreferencesChangeListener(FurnitureLanguageComboBox furnitureLanguageComboBox) {
      this.furnitureLanguageComboBox = new WeakReference<FurnitureLanguageComboBox>(furnitureLanguageComboBox);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      // If furniture table was garbage collected, remove this listener from preferences
      FurnitureLanguageComboBox furnitureLanguageComboBox = this.furnitureLanguageComboBox.get();
      if (furnitureLanguageComboBox == null) {
        ((UserPreferences)ev.getSource()).removePropertyChangeListener(
            UserPreferences.Property.valueOf(ev.getPropertyName()), this);
      } else {
        furnitureLanguageComboBox.repaint();
      }
    }
  }
}
