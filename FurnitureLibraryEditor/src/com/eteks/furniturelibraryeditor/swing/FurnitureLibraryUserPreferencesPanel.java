/*
 * FurnitureLibraryUserPreferencesPanel.java 7 juin 2010
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

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureLibraryUserPreferencesController;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.UserPreferencesPanel;
import com.eteks.sweethome3d.tools.OperatingSystem;

/**
 * User preferences panel able to edit the additional preferences used by the editor.
 * @author Emmanuel Puybaret
 */
public class FurnitureLibraryUserPreferencesPanel extends UserPreferencesPanel {
  private JLabel      defaultCreatorLabel;
  private JTextField  defaultCreatorTextField;
  private JLabel      offlineFurnitureLibraryLabel;
  private JCheckBox   offlineFurnitureLibraryCheckBox;
  private JLabel      furnitureResourcesLocalDirectoryLabel;
  private JTextField  furnitureResourcesLocalDirectoryTextField;
  private JLabel      furnitureResourcesRemoteUrlBaseLabel;
  private JTextField  furnitureResourcesRemoteUrlBaseTextField;

  public FurnitureLibraryUserPreferencesPanel(UserPreferences preferences,
                                              FurnitureLibraryUserPreferencesController controller) {
    super(preferences, controller);
    // Remove Reset tips button
    remove(getComponentCount() - 1);

    createComponents(preferences, controller);
    setMnemonics(preferences);
    layoutComponents();
  }

  /**
   * Creates and initializes components and their models.
   */
  private void createComponents(UserPreferences preferences, 
                                final FurnitureLibraryUserPreferencesController controller) {
    abstract class DocumentChangeListener implements DocumentListener {
      public void insertUpdate(DocumentEvent ev) {
        changedUpdate(ev);
      }

      public void removeUpdate(DocumentEvent ev) {
        changedUpdate(ev);
      }
    };
    
    if (controller.isPropertyEditable(FurnitureLibraryUserPreferencesController.Property.DEFAULT_CREATOR)) {
      // Create default author label and its text field bound to DEFAULT_CREATOR controller property
      this.defaultCreatorLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurnitureLibraryUserPreferencesPanel.class, "defaultCreatorLabel.text"));
      this.defaultCreatorTextField = new JTextField(controller.getDefaultCreator(), 10);
      if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
        SwingTools.addAutoSelectionOnFocusGain(this.defaultCreatorTextField);
      }
      final PropertyChangeListener creatorChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            defaultCreatorTextField.setText(controller.getDefaultCreator());
          }
        };
      controller.addPropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.DEFAULT_CREATOR, creatorChangeListener);
      this.defaultCreatorTextField.getDocument().addDocumentListener(new DocumentChangeListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.DEFAULT_CREATOR, creatorChangeListener);
            String defaultCreator = defaultCreatorTextField.getText(); 
            if (defaultCreator == null || defaultCreator.trim().length() == 0) {
              controller.setDefaultCreator(null);
            } else {
              controller.setDefaultCreator(defaultCreator);
            }
            controller.addPropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.DEFAULT_CREATOR, creatorChangeListener);
          }
        });
    }
    
    if (controller.isPropertyEditable(FurnitureLibraryUserPreferencesController.Property.OFFLINE_FURNITURE_LIBRARY)) {
      // Create offline label and check box bound to controller OFFLINE_FURNITURE_LIBRARY property
      this.offlineFurnitureLibraryLabel = new JLabel(preferences.getLocalizedString(
          FurnitureLibraryUserPreferencesPanel.class, "offlineFurnitureLibraryLabel.text"));
      this.offlineFurnitureLibraryCheckBox = new JCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurnitureLibraryUserPreferencesPanel.class, "offlineFurnitureLibraryCheckBox.text"), controller.isFurnitureLibraryOffline());
      this.offlineFurnitureLibraryCheckBox.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent ev) {
            controller.setFurnitureLibraryOffline(offlineFurnitureLibraryCheckBox.isSelected());
          }
        });
      controller.addPropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.OFFLINE_FURNITURE_LIBRARY, 
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              boolean furnitureLibraryOffline = controller.isFurnitureLibraryOffline();
              offlineFurnitureLibraryCheckBox.setSelected(furnitureLibraryOffline);
              if (furnitureResourcesLocalDirectoryTextField != null) {
                furnitureResourcesLocalDirectoryTextField.setEnabled(!furnitureLibraryOffline);
              }
              if (furnitureResourcesRemoteUrlBaseTextField != null) {
                furnitureResourcesRemoteUrlBaseTextField.setEnabled(!furnitureLibraryOffline);
              }
            }
          });
    }

    if (controller.isPropertyEditable(FurnitureLibraryUserPreferencesController.Property.FURNITURE_RESOURCES_LOCAL_DIRECTORY)) {
      // Create local directory  label and its text field bound to FURNITURE_RESOURCES_LOCAL_DIRECTORY controller property
      this.furnitureResourcesLocalDirectoryLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurnitureLibraryUserPreferencesPanel.class, "furnitureResourcesLocalDirectoryLabel.text"));
      this.furnitureResourcesLocalDirectoryTextField = new JTextField(controller.getFurnitureResourcesLocalDirectory(), 20);
      this.furnitureResourcesLocalDirectoryTextField.setEnabled(!controller.isFurnitureLibraryOffline());
      if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
        SwingTools.addAutoSelectionOnFocusGain(this.furnitureResourcesLocalDirectoryTextField);
      }
      final PropertyChangeListener localDirectoryChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            furnitureResourcesLocalDirectoryTextField.setText(controller.getFurnitureResourcesLocalDirectory());
          }
        };
      controller.addPropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.FURNITURE_RESOURCES_LOCAL_DIRECTORY, localDirectoryChangeListener);
      this.furnitureResourcesLocalDirectoryTextField.getDocument().addDocumentListener(new DocumentChangeListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.FURNITURE_RESOURCES_LOCAL_DIRECTORY, localDirectoryChangeListener);
            String furnitureResourcesLocalDirectory = furnitureResourcesLocalDirectoryTextField.getText(); 
            if (furnitureResourcesLocalDirectory == null || furnitureResourcesLocalDirectory.trim().length() == 0) {
              controller.setFurnitureResourcesLocalDirectory(null);
            } else {
              controller.setFurnitureResourcesLocalDirectory(furnitureResourcesLocalDirectory);
            }
            controller.addPropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.FURNITURE_RESOURCES_LOCAL_DIRECTORY, localDirectoryChangeListener);
          }
        });
    }

    if (controller.isPropertyEditable(FurnitureLibraryUserPreferencesController.Property.FURNITURE_RESOURCES_REMOTE_URL_BASE)) {
      // Create URL base label and its text field bound to FURNITURE_RESOURCES_REMOTE_URL_BASE controller property
      this.furnitureResourcesRemoteUrlBaseLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurnitureLibraryUserPreferencesPanel.class, "furnitureResourcesRemoteUrlBaseLabel.text"));
      this.furnitureResourcesRemoteUrlBaseTextField = new JTextField(controller.getFurnitureResourcesRemoteURLBase(), 20);
      this.furnitureResourcesRemoteUrlBaseTextField.setEnabled(!controller.isFurnitureLibraryOffline());
      if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
        SwingTools.addAutoSelectionOnFocusGain(this.furnitureResourcesRemoteUrlBaseTextField);
      }
      final PropertyChangeListener urlBaseChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            furnitureResourcesRemoteUrlBaseTextField.setText(controller.getFurnitureResourcesRemoteURLBase());
          }
        };
      controller.addPropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.FURNITURE_RESOURCES_REMOTE_URL_BASE, urlBaseChangeListener);
      this.furnitureResourcesRemoteUrlBaseTextField.getDocument().addDocumentListener(new DocumentChangeListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.FURNITURE_RESOURCES_REMOTE_URL_BASE, urlBaseChangeListener);
            String furnitureResourcesRemoteUrlBase = furnitureResourcesRemoteUrlBaseTextField.getText(); 
            if (furnitureResourcesRemoteUrlBase == null || furnitureResourcesRemoteUrlBase.trim().length() == 0) {
              controller.setFurnitureResourcesRemoteURLBase(null);
            } else {
              controller.setFurnitureResourcesRemoteURLBase(furnitureResourcesRemoteUrlBase);
            }
            controller.addPropertyChangeListener(FurnitureLibraryUserPreferencesController.Property.FURNITURE_RESOURCES_REMOTE_URL_BASE, urlBaseChangeListener);
          }
        });
    }
  }        
  
  /**
   * Sets components mnemonics and label / component associations.
   */
  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      if (this.defaultCreatorLabel != null) {
        this.defaultCreatorLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurnitureLibraryUserPreferencesPanel.class, "defaultCreatorLabel.mnemonic")).getKeyCode());
        this.defaultCreatorLabel.setLabelFor(this.defaultCreatorTextField);
      }
      if (this.offlineFurnitureLibraryLabel != null) {
        this.offlineFurnitureLibraryCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurnitureLibraryUserPreferencesPanel.class, "offlineFurnitureLibraryCheckBox.mnemonic")).getKeyCode());
      }
      if (this.furnitureResourcesLocalDirectoryLabel != null) {
        this.furnitureResourcesLocalDirectoryLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurnitureLibraryUserPreferencesPanel.class, "furnitureResourcesLocalDirectoryLabel.mnemonic")).getKeyCode());
        this.furnitureResourcesLocalDirectoryLabel.setLabelFor(this.furnitureResourcesLocalDirectoryTextField);
      }
      if (this.furnitureResourcesRemoteUrlBaseLabel != null) {
        this.furnitureResourcesRemoteUrlBaseLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurnitureLibraryUserPreferencesPanel.class, "furnitureResourcesRemoteUrlBaseLabel.mnemonic")).getKeyCode());
        this.furnitureResourcesRemoteUrlBaseLabel.setLabelFor(this.furnitureResourcesRemoteUrlBaseTextField);
      }
    }
  }
  
  /**
   * Layouts panel components in panel with their labels. 
   */
  private void layoutComponents() {
    int labelAlignment = OperatingSystem.isMacOSX() 
        ? GridBagConstraints.LINE_END
        : GridBagConstraints.LINE_START;
    Insets labelInsets = new Insets(0, 0, 5, 5);
    Insets componentInsets = new Insets(0, 0, 5, 0);
    if (this.defaultCreatorLabel != null) {
      add(this.defaultCreatorLabel, new GridBagConstraints(
          0, 100, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.defaultCreatorTextField, new GridBagConstraints(
          1, 100, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.offlineFurnitureLibraryLabel != null) {
      add(this.offlineFurnitureLibraryLabel, new GridBagConstraints(
          0, 101, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.offlineFurnitureLibraryCheckBox, new GridBagConstraints(
          1, 101, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
    }
    if (this.furnitureResourcesLocalDirectoryLabel != null) {
      add(this.furnitureResourcesLocalDirectoryLabel, new GridBagConstraints(
          0, 102, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.furnitureResourcesLocalDirectoryTextField, new GridBagConstraints(
          1, 102, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.furnitureResourcesRemoteUrlBaseLabel != null) {
      add(this.furnitureResourcesRemoteUrlBaseLabel, new GridBagConstraints(
          0, 103, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
      add(this.furnitureResourcesRemoteUrlBaseTextField, new GridBagConstraints(
          1, 103, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }
  }
}
