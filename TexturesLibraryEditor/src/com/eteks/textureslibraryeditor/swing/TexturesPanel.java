/*
 * TexturesPanel.java 
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
package com.eteks.textureslibraryeditor.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.NullableSpinner;
import com.eteks.sweethome3d.swing.ScaledImageComponent;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.textureslibraryeditor.viewcontroller.TexturesController;

/**
 * Home textures editing panel.
 * @author Emmanuel Puybaret
 */
public class TexturesPanel extends JPanel implements DialogView {
  private final TexturesController controller;
  private JLabel                   idLabel;
  private JTextField               idTextField;
  private JLabel                   nameLabel;
  private JTextField               nameTextField;
  private JLabel                   categoryLabel;
  private JComboBox                categoryComboBox;
  private ScaledImageComponent     imageComponent;
  private JLabel                   widthLabel;
  private JSpinner                 widthSpinner;
  private JLabel                   heightLabel;
  private JSpinner                 heightSpinner;
  private JLabel                   creatorLabel;
  private JTextField               creatorTextField;
  private String                   dialogTitle;

  /**
   * Creates a panel that displays catalog textures data according to the units 
   * set in <code>preferences</code>.
   * @param preferences user preferences
   * @param controller the controller of this panel
   */
  public TexturesPanel(UserPreferences preferences,
                        TexturesController controller) {
    super(new GridBagLayout());
    this.controller = controller;
    createComponents(preferences, controller);
    setMnemonics(preferences);
    layoutComponents();
  }

  /**
   * Creates and initializes components and spinners model.
   */
  private void createComponents(final UserPreferences preferences, 
                                final TexturesController controller) {
    // Get unit name matching current unit 
    String unitName = preferences.getLengthUnit().getName();
    
    if (this.controller.isPropertyEditable(TexturesController.Property.ID)) {
      // Create id label and its text field bound to ID controller property
      this.idLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, TexturesPanel.class, "idLabel.text"));
      this.idTextField = new JTextField(controller.getId(), 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.idTextField);
      }
      final PropertyChangeListener idChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            idTextField.setText(controller.getId());
          }
        };
      controller.addPropertyChangeListener(TexturesController.Property.ID, idChangeListener);
      this.idTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(TexturesController.Property.ID, idChangeListener);
            String id = idTextField.getText(); 
            if (id == null || id.trim().length() == 0) {
              controller.setId(null);
            } else {
              controller.setId(id);
            }
            controller.addPropertyChangeListener(TexturesController.Property.ID, idChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }
        
    if (this.controller.isPropertyEditable(TexturesController.Property.NAME)) {
      // Create name label and its text field bound to NAME controller property
      this.nameLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, TexturesPanel.class, "nameLabel.text"));
      this.nameTextField = new JTextField(controller.getName(), 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.nameTextField);
      }
      final PropertyChangeListener nameChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            nameTextField.setText(controller.getName());
          }
        };
      controller.addPropertyChangeListener(TexturesController.Property.NAME, nameChangeListener);
      this.nameTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(TexturesController.Property.NAME, nameChangeListener);
            String name = nameTextField.getText(); 
            if (name == null || name.trim().length() == 0) {
              controller.setName(null);
            } else {
              controller.setName(name);
            }
            controller.addPropertyChangeListener(TexturesController.Property.NAME, nameChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(TexturesController.Property.CATEGORY)) {
      this.categoryLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          TexturesPanel.class, "categoryLabel.text")); 
      final List<TexturesCategory> categories = controller.getAvailableCategories();
      List<TexturesCategory> categoriesList = new ArrayList<TexturesCategory>(categories);
      final boolean nullableComboBox = controller.getCategory() == null;
      if (nullableComboBox) {
        categoriesList.add(0, null);
      }
      this.categoryComboBox = new JComboBox(categoriesList.toArray());
      this.categoryComboBox.setEditable(true); 
      final ComboBoxEditor defaultEditor = this.categoryComboBox.getEditor();
      // Change editor to edit category name
      this.categoryComboBox.setEditor(new ComboBoxEditor() {
          public Object getItem() {
            String name = (String)defaultEditor.getItem();
            name = name.trim();
            // If category is empty, replace it by the last selected item when the combo box isn't nullable
            if (name.length() == 0) {
              if (nullableComboBox) {
                controller.setCategory(null);
                return null;
              } else {
                Object selectedItem = categoryComboBox.getSelectedItem();
                setItem(selectedItem);
                return selectedItem;
              }
            } else {
              TexturesCategory category = new TexturesCategory(name);
              // Search an existing category
              int categoryIndex = Collections.binarySearch(categories, category);
              if (categoryIndex >= 0) {
                return categories.get(categoryIndex);
              }
              // If no existing category was found, return a new one          
              return category;
            }
          }
        
          public void setItem(Object value) {
            if (value != null) {
              defaultEditor.setItem(((TexturesCategory)value).getName());
            }
          }
  
          public void addActionListener(ActionListener l) {
            defaultEditor.addActionListener(l);
          }
  
          public Component getEditorComponent() {
            return defaultEditor.getEditorComponent();
          }
  
          public void removeActionListener(ActionListener l) {
            defaultEditor.removeActionListener(l);
          }
  
          public void selectAll() {
            defaultEditor.selectAll();
          }
        });
      this.categoryComboBox.setRenderer(new DefaultListCellRenderer() {
          public Component getListCellRendererComponent(JList list, Object value, int index, 
                                                        boolean isSelected, boolean cellHasFocus) {
            if (value == null) {
              value = " ";
            } else {
             value = ((TexturesCategory)value).getName();
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          }
        });
      this.categoryComboBox.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent ev) {
            controller.setCategory((TexturesCategory)ev.getItem());
          }
        });
      controller.addPropertyChangeListener(TexturesController.Property.CATEGORY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              // If category changes update category combo box
              TexturesCategory category = controller.getCategory();
              if (category != null) {
                categoryComboBox.setSelectedItem(category);
              }
            }
          });
      if (this.categoryComboBox.getItemCount() > 0) {
        this.categoryComboBox.setSelectedItem(controller.getCategory());
      }
      this.categoryComboBox.setMaximumRowCount(15);
    }
    
    final float minimumLength = preferences.getLengthUnit().getMinimumLength();
    final float maximumLength = preferences.getLengthUnit().getMaximumLength();
    if (this.controller.isPropertyEditable(TexturesController.Property.WIDTH)) {
      // Create width label and its spinner bound to WIDTH controller property
      this.widthLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          TexturesPanel.class, "widthLabel.text", unitName));
      final NullableSpinner.NullableSpinnerLengthModel widthSpinnerModel = 
          new NullableSpinner.NullableSpinnerLengthModel(preferences, minimumLength, maximumLength);
      this.widthSpinner = new NullableSpinner(widthSpinnerModel);
      final PropertyChangeListener widthChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            Float width = controller.getWidth();
            widthSpinnerModel.setNullable(width == null);
            widthSpinnerModel.setLength(width);
            if (width != null) {
              widthSpinnerModel.setMinimumLength(Math.min(width, minimumLength));
            }
          }
        };
      widthChangeListener.propertyChange(null);
      controller.addPropertyChangeListener(TexturesController.Property.WIDTH, widthChangeListener);
      widthSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(TexturesController.Property.WIDTH, widthChangeListener);
            controller.setWidth(widthSpinnerModel.getLength());
            controller.addPropertyChangeListener(TexturesController.Property.WIDTH, widthChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(TexturesController.Property.HEIGHT)) {
      // Create height label and its spinner bound to HEIGHT controller property
      this.heightLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          TexturesPanel.class, "heightLabel.text", unitName));
      final NullableSpinner.NullableSpinnerLengthModel heightSpinnerModel = 
          new NullableSpinner.NullableSpinnerLengthModel(preferences, minimumLength, maximumLength);
      this.heightSpinner = new NullableSpinner(heightSpinnerModel);
      final PropertyChangeListener heightChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            Float height = controller.getHeight();
            heightSpinnerModel.setNullable(height == null);
            heightSpinnerModel.setLength(height);
            if (height != null) {
              heightSpinnerModel.setMinimumLength(Math.min(height, minimumLength));
            }
          }
        };
      heightChangeListener.propertyChange(null);
      controller.addPropertyChangeListener(TexturesController.Property.HEIGHT, heightChangeListener);
      heightSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(TexturesController.Property.HEIGHT, heightChangeListener);
            controller.setHeight(heightSpinnerModel.getLength());
            controller.addPropertyChangeListener(TexturesController.Property.HEIGHT, heightChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(TexturesController.Property.CREATOR)) {
      // Create creator label and its text field bound to CREATOR controller property
      this.creatorLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, TexturesPanel.class, "creatorLabel.text"));
      this.creatorTextField = new JTextField(controller.getCreator(), 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.creatorTextField);
      }
      final PropertyChangeListener creatorChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            creatorTextField.setText(controller.getCreator());
          }
        };
      controller.addPropertyChangeListener(TexturesController.Property.CREATOR, creatorChangeListener);
      this.creatorTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(TexturesController.Property.CREATOR, creatorChangeListener);
            String creator = creatorTextField.getText(); 
            if (creator == null || creator.trim().length() == 0) {
              controller.setCreator(null);
            } else {
              controller.setCreator(creator);
            }
            controller.addPropertyChangeListener(TexturesController.Property.CREATOR, creatorChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }
  
    if (this.controller.isPropertyEditable(TexturesController.Property.IMAGE)) {
      this.imageComponent = new ScaledImageComponent();
      Insets insets = this.imageComponent.getInsets();
      this.imageComponent.setPreferredSize(
          new Dimension(128 + insets.left + insets.right, 128  + insets.top + insets.bottom));
      setImage(controller.getImage(), preferences);
      controller.addPropertyChangeListener(TexturesController.Property.IMAGE,
          new PropertyChangeListener () {
            public void propertyChange(PropertyChangeEvent ev) {
              setImage(controller.getImage(), preferences);
            }
          });
    }

    this.dialogTitle = preferences.getLocalizedString(TexturesPanel.class, "textures.title");
  }

  /**
   * Sets the image viewed by this texture.  
   */
  public void setImage(Content image, final UserPreferences preferences) {
    if (image == null) {
      this.imageComponent.setImage(null);            
    } else {
      InputStream in = null;
      try {
        in = controller.getImage().openStream();
        this.imageComponent.setImage(ImageIO.read(in));
      } catch (IOException e) {
        JOptionPane.showMessageDialog(SwingUtilities.getRootPane(TexturesPanel.this), 
            preferences.getLocalizedString(TexturesPanel.class, "textureError"),
            preferences.getLocalizedString(TexturesPanel.class, "errorTitle"),
            JOptionPane.ERROR_MESSAGE);
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Sets components mnemonics and label / component associations.
   */
  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      if (this.idLabel != null) {
        this.idLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            TexturesPanel.class, "idLabel.mnemonic")).getKeyCode());
        this.idLabel.setLabelFor(this.idTextField);
      }
      if (this.nameLabel != null) {
        this.nameLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            TexturesPanel.class, "nameLabel.mnemonic")).getKeyCode());
        this.nameLabel.setLabelFor(this.nameTextField);
      }
      if (this.categoryLabel != null) {
        this.categoryLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            TexturesPanel.class, "categoryLabel.mnemonic")).getKeyCode());
        this.categoryLabel.setLabelFor(this.categoryComboBox);
      }
      if (this.widthLabel != null) {
        this.widthLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            TexturesPanel.class, "widthLabel.mnemonic")).getKeyCode());
        this.widthLabel.setLabelFor(this.widthSpinner);
      }
      if (this.heightLabel != null) {
        this.heightLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            TexturesPanel.class, "heightLabel.mnemonic")).getKeyCode());
        this.heightLabel.setLabelFor(this.heightSpinner);
      }
      if (this.creatorLabel != null) {
        this.creatorLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            TexturesPanel.class, "creatorLabel.mnemonic")).getKeyCode());
        this.creatorLabel.setLabelFor(this.creatorTextField);
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
    if (this.controller.isPropertyEditable(TexturesController.Property.IMAGE)) {
      JPanel iconPanel = new JPanel(new GridBagLayout());
      // Add dummy labels with a vertical weight of 1 at top and bottom of iconPanel 
      // to keep imageComponent and rotationButtonsPanel in the middle
      // when grid bag fill constraint is BOTH for iconPanel.
      // If this constraint is set to HORIZONTAL only, iconPanel location may be lower
      // and may not be the first panel treated by the focus traversal algorithm 
      iconPanel.add(new JLabel(), new GridBagConstraints(
          0, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, 
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
      iconPanel.add(this.imageComponent, new GridBagConstraints(
          0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, 
          GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
      iconPanel.add(new JLabel(), new GridBagConstraints(
          0, 3, 1, 1, 0, 1, GridBagConstraints.CENTER, 
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

      add(iconPanel, new GridBagConstraints(
          0, 0, 1, 15, 0, 0, GridBagConstraints.CENTER, 
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 15), 0, 0));
    }
    if (this.controller.isPropertyEditable(TexturesController.Property.ID)) {
      add(this.idLabel, new GridBagConstraints(
          1, 0, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.idTextField, new GridBagConstraints(
          2, 0, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(TexturesController.Property.NAME)) {
      add(this.nameLabel, new GridBagConstraints(
          1, 1, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.nameTextField, new GridBagConstraints(
          2, 1, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(TexturesController.Property.CREATOR)) {
      add(this.creatorLabel, new GridBagConstraints(
          1, 3, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.creatorTextField, new GridBagConstraints(
          2, 3, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), 0, 0));
    }
    if (this.controller.isPropertyEditable(TexturesController.Property.CATEGORY)) {
      add(this.categoryLabel, new GridBagConstraints(
          3, 3, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.categoryComboBox, new GridBagConstraints(
          4, 3, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(TexturesController.Property.WIDTH)) {
      add(this.widthLabel, new GridBagConstraints(
          1, 5, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.widthSpinner, new GridBagConstraints(
          2, 5, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), -10, 0));
    }
    if (this.controller.isPropertyEditable(TexturesController.Property.HEIGHT)) {
      add(this.heightLabel, new GridBagConstraints(
          3, 5, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.heightSpinner, new GridBagConstraints(
          4, 5, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), -10, 0));
    }
  }

  /**
   * Displays this panel in a modal dialog box. 
   */
  public void displayView(View parentView) {
    final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (SwingTools.showConfirmDialog((JComponent)parentView, 
            this, this.dialogTitle, this.nameTextField) == JOptionPane.OK_OPTION) {
      this.controller.modifyTextures();
    }
    if (focusOwner != null) {
      focusOwner.requestFocusInWindow();
    }
  }
}
