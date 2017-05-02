/*
 * FurniturePanel.java 
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
package com.eteks.furniturelibraryeditor.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.vecmath.Matrix3f;

import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathParser;

import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureController;
import com.eteks.sweethome3d.j3d.HomePieceOfFurniture3D;
import com.eteks.sweethome3d.j3d.ModelManager;
import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.AutoCommitSpinner;
import com.eteks.sweethome3d.swing.ModelPreviewComponent;
import com.eteks.sweethome3d.swing.NullableCheckBox;
import com.eteks.sweethome3d.swing.NullableSpinner;
import com.eteks.sweethome3d.swing.ResourceAction;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * Home furniture editing panel.
 * @author Emmanuel Puybaret
 */
public class FurniturePanel extends JPanel implements DialogView {
  private final FurnitureController controller;
  private final UserPreferences     preferences;
  private JLabel                    idLabel;
  private JTextField                idTextField;
  private JLabel                    nameLabel;
  private JTextField                nameTextField;
  private JLabel                    descriptionLabel;
  private JTextField                descriptionTextField;
  private JLabel                    informationLabel;
  private JTextField                informationTextField;
  private JLabel                    tagsLabel;
  private JTextField                tagsTextField;
  private JLabel                    creatorLabel;
  private JTextField                creatorTextField;
  private JLabel                    categoryLabel;
  private JComboBox                 categoryComboBox;
  private JLabel                    creationDateLabel;
  private JSpinner                  creationDateSpinner;
  private JLabel                    gradeLabel;
  private JSpinner                  gradeSpinner;
  private IconPreviewComponent      iconComponent;
  private JButton                   changeModelButton;
  private JButton                   turnLeftButton;
  private JButton                   turnRightButton;
  private JButton                   turnUpButton;
  private JButton                   turnDownButton;
  private JLabel                    widthLabel;
  private JSpinner                  widthSpinner;
  private JLabel                    depthLabel;
  private JSpinner                  depthSpinner;
  private JLabel                    heightLabel;
  private JSpinner                  heightSpinner;
  private JCheckBox                 keepProportionsCheckBox;
  private JButton                   enlargeTenTimesButton;
  private JButton                   reduceTenTimesButton;
  private JButton                   enlargeInchTimesButton;
  private JLabel                    elevationLabel;
  private JSpinner                  elevationSpinner;
  private NullableCheckBox          movableCheckBox;
  private NullableCheckBox          doorOrWindowCheckBox;
  private JCheckBox                 doorOrWindowCustomizedCutOutShapeCheckBox;
  private JTextField                doorOrWindowCustomizedCutOutShapeTextField;
  private NullableCheckBox          staircaseCheckBox;
  private JLabel                    staircaseCutOutShapeLabel;
  private JTextField                staircaseCutOutShapeTextField;
  private NullableCheckBox          backFaceShownCheckBox;
  private NullableCheckBox          resizableCheckBox;
  private NullableCheckBox          deformableCheckBox;
  private NullableCheckBox          texturableCheckBox;
  private JLabel                    priceLabel;
  private JSpinner                  priceSpinner;
  private JLabel                    valueAddedTaxPercentageLabel;
  private JSpinner                  valueAddedTaxPercentageSpinner;
  private String                    dialogTitle;

  /**
   * Creates a panel that displays catalog furniture data according to the units 
   * set in <code>preferences</code>.
   * @param preferences user preferences
   * @param controller the controller of this panel
   */
  public FurniturePanel(UserPreferences preferences,
                        FurnitureController controller) {
    super(new GridBagLayout());
    this.preferences = preferences;
    this.controller = controller;
    createComponents(preferences, controller);
    setMnemonics(preferences);
    layoutComponents();
  }

  /**
   * Creates and initializes components and spinners model.
   */
  private void createComponents(final UserPreferences preferences, 
                                final FurnitureController controller) {
    // Get unit name matching current unit 
    String unitName = preferences.getLengthUnit().getName();
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.ID)) {
      // Create id label and its text field bound to ID controller property
      this.idLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurniturePanel.class, "idLabel.text"));
      this.idTextField = new JTextField(controller.getId(), 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.idTextField);
      }
      final PropertyChangeListener idChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            idTextField.setText(controller.getId());
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.ID, idChangeListener);
      this.idTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.ID, idChangeListener);
            String id = idTextField.getText(); 
            if (id == null || id.trim().length() == 0) {
              controller.setId(null);
            } else {
              controller.setId(id);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.ID, idChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }
        
    if (this.controller.isPropertyEditable(FurnitureController.Property.NAME)) {
      // Create name label and its text field bound to NAME controller property
      this.nameLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurniturePanel.class, "nameLabel.text"));
      this.nameTextField = new JTextField(controller.getName(), 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.nameTextField);
      }
      final PropertyChangeListener nameChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            nameTextField.setText(controller.getName());
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.NAME, nameChangeListener);
      this.nameTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.NAME, nameChangeListener);
            String name = nameTextField.getText(); 
            if (name == null || name.trim().length() == 0) {
              controller.setName(null);
            } else {
              controller.setName(name);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.NAME, nameChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.DESCRIPTION)) {
      // Create description label and its text field bound to DESCRIPTION controller property
      this.descriptionLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurniturePanel.class, "descriptionLabel.text"));
      this.descriptionTextField = new JTextField(controller.getDescription(), 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.descriptionTextField);
      }
      final PropertyChangeListener descriptionChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            descriptionTextField.setText(controller.getDescription());
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.DESCRIPTION, descriptionChangeListener);
      this.descriptionTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.DESCRIPTION, descriptionChangeListener);
            String description = descriptionTextField.getText(); 
            if (description == null || description.trim().length() == 0) {
              controller.setDescription(null);
            } else {
              controller.setDescription(description);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.DESCRIPTION, descriptionChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }

    if (this.controller.isPropertyEditable(FurnitureController.Property.INFORMATION)) {
      // Create description label and its text field bound to INFORMATION controller property
      this.informationLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurniturePanel.class, "informationLabel.text"));
      this.informationTextField = new JTextField(controller.getInformation(), 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.informationTextField);
      }
      final PropertyChangeListener informationChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            informationTextField.setText(controller.getInformation());
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.INFORMATION, informationChangeListener);
      this.informationTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.INFORMATION, informationChangeListener);
            String information = informationTextField.getText(); 
            if (information == null || information.trim().length() == 0) {
              controller.setInformation(null);
            } else {
              controller.setInformation(information);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.INFORMATION, informationChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }

    if (this.controller.isPropertyEditable(FurnitureController.Property.TAGS)) {
      // Create tags label and its text field bound to TAGS controller property
      this.tagsLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurniturePanel.class, "tagsLabel.text"));
      String tags = null;
      if (controller.getTags() != null) {
        tags = Arrays.toString(controller.getTags());
        tags = tags.substring(1, tags.length() - 1);
      }
      this.tagsTextField = new JTextField(tags, 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.tagsTextField);
      }
      final PropertyChangeListener tagsChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            String tags = null;
            if (controller.getTags() != null) {
              tags = Arrays.toString(controller.getTags());
              tags = tags.substring(1, tags.length() - 1);
            }
            tagsTextField.setText(tags);
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.TAGS, tagsChangeListener);
      this.tagsTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.TAGS, tagsChangeListener);
            String tags = tagsTextField.getText(); 
            if (tags == null || tags.trim().length() == 0) {
              controller.setTags(null);
            } else {
              controller.setTags(tags.split("\\s*,\\s*"));
            }
            controller.addPropertyChangeListener(FurnitureController.Property.TAGS, tagsChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }

    if (this.controller.isPropertyEditable(FurnitureController.Property.CREATOR)) {
      // Create creator label and its text field bound to CREATOR controller property
      this.creatorLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurniturePanel.class, "creatorLabel.text"));
      this.creatorTextField = new JTextField(controller.getCreator(), 10);
      if (!OperatingSystem.isMacOSX()) {
        SwingTools.addAutoSelectionOnFocusGain(this.creatorTextField);
      }
      final PropertyChangeListener creatorChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            creatorTextField.setText(controller.getCreator());
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.CREATOR, creatorChangeListener);
      this.creatorTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.CREATOR, creatorChangeListener);
            String creator = creatorTextField.getText(); 
            if (creator == null || creator.trim().length() == 0) {
              controller.setCreator(null);
            } else {
              controller.setCreator(creator);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.CREATOR, creatorChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }
  
    if (this.controller.isPropertyEditable(FurnitureController.Property.CATEGORY)) {
      this.categoryLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "categoryLabel.text")); 
      final List<FurnitureCategory> categories = controller.getAvailableCategories();
      List<FurnitureCategory> categoriesList = new ArrayList<FurnitureCategory>(categories);
      final boolean nullableComboBox = controller.getCategory() == null;
      if (nullableComboBox) {
        categoriesList.add(0, null);
      }
      this.categoryComboBox = new JComboBox(categoriesList.toArray());
      this.categoryComboBox.setEditable(true); 
      final ComboBoxEditor defaultEditor = this.categoryComboBox.getEditor();
      // Change editor to edit category name when the combo box isn't nullable
      this.categoryComboBox.setEditor(new ComboBoxEditor() {
          public Object getItem() {
            String name = (String)defaultEditor.getItem();
            name = name.trim();
            // If category is empty, replace it by the last selected item
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
              FurnitureCategory category = new FurnitureCategory(name);
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
              defaultEditor.setItem(((FurnitureCategory)value).getName());
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
             value = ((FurnitureCategory)value).getName();
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          }
        });
      this.categoryComboBox.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent ev) {
            controller.setCategory((FurnitureCategory)ev.getItem());
          }
        });
      controller.addPropertyChangeListener(FurnitureController.Property.CATEGORY,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              // If category changes update category combo box
              FurnitureCategory category = controller.getCategory();
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
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.CREATION_DATE)) {
      // Create creation date label and spinner bound to CREATION_DATE controller property
      this.creationDateLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurniturePanel.class, "creationDateLabel.text"));
      final NullableSpinner.NullableSpinnerDateModel creationDateSpinnerModel = new NullableSpinner.NullableSpinnerDateModel();
      if (controller.getCreationDate() != null) {
        Date time = new Date(Camera.convertTimeToTimeZone(controller.getCreationDate(), TimeZone.getDefault().getID()));
        creationDateSpinnerModel.setValue(time);
      } else {
        creationDateSpinnerModel.setNullable(true);
        creationDateSpinnerModel.setValue(null);
      }
      this.creationDateSpinner = new AutoCommitSpinner(creationDateSpinnerModel);
      String datePattern = ((SimpleDateFormat)DateFormat.getDateInstance(DateFormat.SHORT)).toPattern();
      if (datePattern.indexOf("yyyy") == -1) {
        datePattern = datePattern.replace("yy", "yyyy");
      }
      JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(this.creationDateSpinner, datePattern);
      this.creationDateSpinner.setEditor(dateEditor);
      SwingTools.addAutoSelectionOnFocusGain(dateEditor.getTextField());
      
      final PropertyChangeListener dateChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (controller.getCreationDate() != null) {
            Date date = new Date(Camera.convertTimeToTimeZone(controller.getCreationDate(), TimeZone.getDefault().getID()));
            creationDateSpinnerModel.setNullable(false);
            creationDateSpinnerModel.setValue(date);
          } else {
            creationDateSpinnerModel.setNullable(true);
            creationDateSpinnerModel.setValue(null);
          }
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.CREATION_DATE, dateChangeListener);
      final ChangeListener dateTimeChangeListener = new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.CREATION_DATE, dateChangeListener);
            // Merge date and time
            GregorianCalendar dateCalendar = new GregorianCalendar();
            if (creationDateSpinnerModel.getValue() != null) {
              dateCalendar.setTime((Date)creationDateSpinnerModel.getValue());
              Calendar utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
              utcCalendar.set(GregorianCalendar.YEAR, dateCalendar.get(GregorianCalendar.YEAR));
              utcCalendar.set(GregorianCalendar.MONTH, dateCalendar.get(GregorianCalendar.MONTH));
              utcCalendar.set(GregorianCalendar.DAY_OF_MONTH, dateCalendar.get(GregorianCalendar.DAY_OF_MONTH));
              controller.setCreationDate(utcCalendar.getTimeInMillis());
            } else {
              controller.setCreationDate(null);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.CREATION_DATE, dateChangeListener);
          }
        };
      creationDateSpinnerModel.addChangeListener(dateTimeChangeListener);
    }
  
    if (this.controller.isPropertyEditable(FurnitureController.Property.GRADE)) {
      // Create grade label and spinner bound to GRADE controller property
      this.gradeLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "gradeLabel.text", unitName));
      final NullableSpinner.NullableSpinnerNumberModel gradeSpinnerModel = 
          new NullableSpinner.NullableSpinnerNumberModel(0, 0f, 1f, 0.1f);
      this.gradeSpinner = new AutoCommitSpinner(gradeSpinnerModel);
      final PropertyChangeListener gradeChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            Float grade = controller.getGrade();
            gradeSpinnerModel.setNullable(grade == null);
            gradeSpinnerModel.setValue(grade);
            if (grade != null) {
              gradeSpinnerModel.setMinimum(Math.min(grade, 0));
            }
          }
        };
      gradeChangeListener.propertyChange(null);
      controller.addPropertyChangeListener(FurnitureController.Property.GRADE, gradeChangeListener);
      gradeSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.GRADE, gradeChangeListener);
            controller.setGrade(((Number)gradeSpinnerModel.getValue()).floatValue());
            controller.addPropertyChangeListener(FurnitureController.Property.GRADE, gradeChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.PRICE)) {
      // Create price label and its spinner bound to PRICE controller property
      this.priceLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "priceLabel.text"));
      final NullableSpinner.NullableSpinnerNumberModel priceSpinnerModel = 
          new NullableSpinner.NullableSpinnerNumberModel(1, 0.00999f, 1000000f, 1f);
      this.priceSpinner = new NullableSpinner(priceSpinnerModel);
      priceSpinnerModel.setNullable(controller.getPrice() == null);
      priceSpinnerModel.setValue(controller.getPrice());
      final PropertyChangeListener priceChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          priceSpinnerModel.setNullable(ev.getNewValue() == null);
          priceSpinnerModel.setValue((Float)ev.getNewValue());
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.PRICE, priceChangeListener);
      priceSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.PRICE, priceChangeListener);
            Object value = priceSpinnerModel.getValue();
            controller.setPrice(value != null 
                ? new BigDecimal((Float)value).setScale(2, BigDecimal.ROUND_HALF_UP)
                : (BigDecimal)value);
            controller.addPropertyChangeListener(FurnitureController.Property.PRICE, priceChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.VALUE_ADDED_TAX_PERCENTAGE)) {
      // Create VAT % label and its spinner bound to VALUE_ADDED_TAX_PERCENTAGE controller property
      this.valueAddedTaxPercentageLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "valueAddedTaxPercentageLabel.text"));
      final NullableSpinner.NullableSpinnerNumberModel valueAddedTaxPercentageSpinnerModel = 
          new NullableSpinner.NullableSpinnerNumberModel(0, 0, 100f, 0.1f);
      this.valueAddedTaxPercentageSpinner = new NullableSpinner(valueAddedTaxPercentageSpinnerModel);
      valueAddedTaxPercentageSpinnerModel.setNullable(controller.getValueAddedTaxPercentage() == null);
      valueAddedTaxPercentageSpinnerModel.setValue(controller.getValueAddedTaxPercentage() == null
          ? null
          : controller.getValueAddedTaxPercentage().floatValue() * 100);
      final PropertyChangeListener valueAddedTaxPercentageChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          valueAddedTaxPercentageSpinnerModel.setNullable(ev.getNewValue() == null);
          valueAddedTaxPercentageSpinnerModel.setValue(ev.getNewValue() == null
              ? null
              : ((Float)ev.getNewValue()) * 100);
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.VALUE_ADDED_TAX_PERCENTAGE, valueAddedTaxPercentageChangeListener);
      valueAddedTaxPercentageSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.VALUE_ADDED_TAX_PERCENTAGE, valueAddedTaxPercentageChangeListener);
            Object value = valueAddedTaxPercentageSpinnerModel.getValue();
            controller.setValueAddedTaxPercentage(value != null 
                ? new BigDecimal((Float)value / 100).setScale(3, BigDecimal.ROUND_HALF_UP)
                : (BigDecimal)value);
            controller.addPropertyChangeListener(FurnitureController.Property.VALUE_ADDED_TAX_PERCENTAGE, valueAddedTaxPercentageChangeListener);
          }
        });
    }
    
    final float minimumLength = preferences.getLengthUnit().getMinimumLength();
    final float maximumLength = preferences.getLengthUnit().getMaximumLength();
    if (this.controller.isPropertyEditable(FurnitureController.Property.WIDTH)) {
      // Create width label and its spinner bound to WIDTH controller property
      this.widthLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "widthLabel.text", unitName));
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
      controller.addPropertyChangeListener(FurnitureController.Property.WIDTH, widthChangeListener);
      widthSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.WIDTH, widthChangeListener);
            controller.setWidth(widthSpinnerModel.getLength());
            if (!controller.isProportional()) {
              resetIcon(false);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.WIDTH, widthChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.DEPTH)) {
      // Create depth label and its spinner bound to DEPTH controller property
      this.depthLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "depthLabel.text", unitName));
      final NullableSpinner.NullableSpinnerLengthModel depthSpinnerModel = 
          new NullableSpinner.NullableSpinnerLengthModel(preferences, minimumLength, maximumLength);
      this.depthSpinner = new NullableSpinner(depthSpinnerModel);
      final PropertyChangeListener depthChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            Float depth = controller.getDepth();
            depthSpinnerModel.setNullable(depth == null);
            depthSpinnerModel.setLength(depth);
            if (depth != null) {
              depthSpinnerModel.setMinimumLength(Math.min(depth, minimumLength));
            }
          }
        };
      depthChangeListener.propertyChange(null);
      controller.addPropertyChangeListener(FurnitureController.Property.DEPTH, depthChangeListener);
      depthSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.DEPTH, depthChangeListener);
            controller.setDepth(depthSpinnerModel.getLength());
            if (!controller.isProportional()) {
              resetIcon(false);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.DEPTH, depthChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.HEIGHT)) {
      // Create height label and its spinner bound to HEIGHT controller property
      this.heightLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "heightLabel.text", unitName));
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
      controller.addPropertyChangeListener(FurnitureController.Property.HEIGHT, heightChangeListener);
      heightSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.HEIGHT, heightChangeListener);
            controller.setHeight(heightSpinnerModel.getLength());
            if (!controller.isProportional()) {
              resetIcon(false);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.HEIGHT, heightChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.PROPORTIONAL)) {
      this.keepProportionsCheckBox = new JCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "keepProportionsCheckBox.text"), controller.isProportional());
      this.keepProportionsCheckBox.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent ev) {
            controller.setProportional(keepProportionsCheckBox.isSelected());
          }
        });
      controller.addPropertyChangeListener(FurnitureController.Property.PROPORTIONAL,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              // If proportional property changes update keep proportions check box
              keepProportionsCheckBox.setSelected(controller.isProportional());
            }
          });
      
      this.enlargeTenTimesButton = new JButton(new ResourceAction(preferences, FurniturePanel.class, "ENLARGE_TEN_TIMES", true) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            controller.multiplySize(10);
          }
        });
      this.reduceTenTimesButton = new JButton(new ResourceAction(preferences, FurniturePanel.class, "REDUCE_TEN_TIMES", true) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            controller.multiplySize(0.1f);
          }
        });
      this.enlargeInchTimesButton = new JButton(new ResourceAction(preferences, FurniturePanel.class, "ENLARGE_INCH_TIMES", true) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            controller.multiplySize(2.54f);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.ELEVATION)) {
      // Create elevation label and its spinner bound to ELEVATION controller property
      this.elevationLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "elevationLabel.text", unitName));
      final NullableSpinner.NullableSpinnerLengthModel elevationSpinnerModel = 
          new NullableSpinner.NullableSpinnerLengthModel(preferences, 0f, preferences.getLengthUnit().getMaximumElevation());
      this.elevationSpinner = new NullableSpinner(elevationSpinnerModel);
      elevationSpinnerModel.setNullable(controller.getElevation() == null);
      elevationSpinnerModel.setLength(controller.getElevation());
      final PropertyChangeListener elevationChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          elevationSpinnerModel.setNullable(ev.getNewValue() == null);
          elevationSpinnerModel.setLength((Float)ev.getNewValue());
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.ELEVATION, 
          elevationChangeListener);
      elevationSpinnerModel.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.ELEVATION, 
                elevationChangeListener);
            controller.setElevation(elevationSpinnerModel.getLength());
            controller.addPropertyChangeListener(FurnitureController.Property.ELEVATION, 
                elevationChangeListener);
          }
        });
    }

    if (this.controller.isPropertyEditable(FurnitureController.Property.MOVABLE)) {
      // Create movable check box bound to MOVABLE controller property
      this.movableCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "movableCheckBox.text"));
      this.movableCheckBox.setNullable(controller.getMovable() == null);
      this.movableCheckBox.setValue(controller.getMovable());
      final PropertyChangeListener movableChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          movableCheckBox.setNullable(ev.getNewValue() == null);
          movableCheckBox.setValue((Boolean)ev.getNewValue());
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.MOVABLE, movableChangeListener);
      this.movableCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.MOVABLE, movableChangeListener);
            controller.setMovable(movableCheckBox.getValue());
            controller.addPropertyChangeListener(FurnitureController.Property.MOVABLE, movableChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.DOOR_OR_WINDOW)) {
      // Create doorOrWindow check box bound to DOOR_OR_WINDOW controller property
      this.doorOrWindowCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "doorOrWindowCheckBox.text"));
      this.doorOrWindowCheckBox.setNullable(controller.getDoorOrWindow() == null);
      this.doorOrWindowCheckBox.setValue(controller.getDoorOrWindow());
      final PropertyChangeListener doorOrWindowChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          doorOrWindowCheckBox.setNullable(ev.getNewValue() == null);
          doorOrWindowCheckBox.setValue((Boolean)ev.getNewValue());
          if (doorOrWindowCustomizedCutOutShapeCheckBox != null) {
            doorOrWindowCustomizedCutOutShapeCheckBox.setEnabled(Boolean.TRUE.equals(ev.getNewValue()));
            doorOrWindowCustomizedCutOutShapeTextField.setEnabled(Boolean.TRUE.equals(ev.getNewValue())
                && controller.getDoorOrWindowCutOutShape() != null);
          }
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.DOOR_OR_WINDOW, doorOrWindowChangeListener);
      this.doorOrWindowCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.DOOR_OR_WINDOW, doorOrWindowChangeListener);
            controller.setDoorOrWindow(doorOrWindowCheckBox.getValue());
            if (doorOrWindowCustomizedCutOutShapeCheckBox != null) {
              doorOrWindowCustomizedCutOutShapeCheckBox.setEnabled(Boolean.TRUE.equals(doorOrWindowCheckBox.getValue()));
              doorOrWindowCustomizedCutOutShapeTextField.setEnabled(Boolean.TRUE.equals(doorOrWindowCheckBox.getValue())
                  && controller.getDoorOrWindowCutOutShape() != null);
            }
            controller.addPropertyChangeListener(FurnitureController.Property.DOOR_OR_WINDOW, doorOrWindowChangeListener);
          }
        });

      if (this.controller.isPropertyEditable(FurnitureController.Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE)) {
        // Create cut out shape label and text field bound to DOOR_OR_WINDOW_CUT_OUT_SHAPE controller property
        this.doorOrWindowCustomizedCutOutShapeCheckBox = new JCheckBox(SwingTools.getLocalizedLabelText(preferences, 
            FurniturePanel.class, "doorOrWindowCustomizedCutOutShapeCheckBox.text"));
        this.doorOrWindowCustomizedCutOutShapeCheckBox.setEnabled(Boolean.TRUE.equals(controller.getDoorOrWindow()));
        this.doorOrWindowCustomizedCutOutShapeCheckBox.setSelected(controller.getDoorOrWindowCutOutShape() != null);
        final PropertyChangeListener customizedCutOutShapeChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            doorOrWindowCustomizedCutOutShapeCheckBox.setSelected(controller.getDoorOrWindowCutOutShape() != null);
            doorOrWindowCustomizedCutOutShapeTextField.setText(controller.getDoorOrWindowCutOutShape());
            doorOrWindowCustomizedCutOutShapeTextField.setEnabled(controller.getDoorOrWindowCutOutShape() != null);
          }
        };
        controller.addPropertyChangeListener(FurnitureController.Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE, customizedCutOutShapeChangeListener);
        this.doorOrWindowCustomizedCutOutShapeCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
              controller.removePropertyChangeListener(FurnitureController.Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE, customizedCutOutShapeChangeListener);
              doorOrWindowCustomizedCutOutShapeTextField.setEnabled(doorOrWindowCustomizedCutOutShapeCheckBox.isSelected());
              if (doorOrWindowCustomizedCutOutShapeCheckBox.isSelected()) {
                String customizedCutOutShape = doorOrWindowCustomizedCutOutShapeTextField.getText();
                controller.setDoorOrWindowCutOutShape(customizedCutOutShape != null && customizedCutOutShape.trim().length() > 0
                    ? customizedCutOutShape
                    : null);
              } else {
                controller.setDoorOrWindowCutOutShape(null);
              }
              controller.addPropertyChangeListener(FurnitureController.Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE, customizedCutOutShapeChangeListener);
            }
          });

        this.doorOrWindowCustomizedCutOutShapeTextField = new JTextField(controller.getDoorOrWindowCutOutShape(), 15);
        this.doorOrWindowCustomizedCutOutShapeTextField.setEnabled(controller.getDoorOrWindowCutOutShape() != null);
        final Color defaultTextFieldColor = this.doorOrWindowCustomizedCutOutShapeTextField.getForeground();
        this.doorOrWindowCustomizedCutOutShapeTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent ev) {
              controller.removePropertyChangeListener(FurnitureController.Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE, customizedCutOutShapeChangeListener);
              String customizedCutOutShape = doorOrWindowCustomizedCutOutShapeTextField.getText(); 
              if (customizedCutOutShape == null || customizedCutOutShape.trim().length() == 0) {
                controller.setDoorOrWindowCutOutShape(null);
              } else {
                controller.setDoorOrWindowCutOutShape(customizedCutOutShape);
                try {
                  PathParser pathParser = new PathParser();
                  pathParser.setPathHandler(new AWTPathProducer());
                  pathParser.parse(customizedCutOutShape);
                  doorOrWindowCustomizedCutOutShapeTextField.setForeground(defaultTextFieldColor);
                } catch (ParseException ex) {
                  doorOrWindowCustomizedCutOutShapeTextField.setForeground(Color.RED);
                } catch (NullPointerException ex) {
                  doorOrWindowCustomizedCutOutShapeTextField.setForeground(Color.RED);
                }
              }
              controller.addPropertyChangeListener(FurnitureController.Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE, customizedCutOutShapeChangeListener);
            }
      
            public void insertUpdate(DocumentEvent ev) {
              changedUpdate(ev);
            }
      
            public void removeUpdate(DocumentEvent ev) {
              changedUpdate(ev);
            }
          });
      }
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.STAIRCASE_CUT_OUT_SHAPE)) {
      // Create staircase check box and text field bound to STAIRCASE_CUT_OUT_SHAPE controller property
      this.staircaseCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "staircaseCheckBox.text"));
      this.staircaseCheckBox.setNullable(controller.getStaircase() == null);
      this.staircaseCheckBox.setValue(controller.getStaircase());
      final PropertyChangeListener staircaseChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          staircaseCheckBox.setNullable(ev.getNewValue() == null);
          staircaseCheckBox.setValue((Boolean)ev.getNewValue());
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.STAIRCASE, staircaseChangeListener);
      this.staircaseCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.STAIRCASE, staircaseChangeListener);
            Boolean value = staircaseCheckBox.getValue();
            staircaseCutOutShapeLabel.setEnabled(!Boolean.FALSE.equals(value));
            staircaseCutOutShapeTextField.setEnabled(!Boolean.FALSE.equals(value));
            controller.setStaircase(value);
            controller.addPropertyChangeListener(FurnitureController.Property.STAIRCASE, staircaseChangeListener);
          }
        });

      this.staircaseCutOutShapeLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, FurniturePanel.class, "staircaseCutOutShapeLabel.text"));
      this.staircaseCutOutShapeLabel.setEnabled(!Boolean.FALSE.equals(controller.getStaircase()));
      this.staircaseCutOutShapeTextField = new JTextField(controller.getStaircaseCutOutShape(), 15);
      this.staircaseCutOutShapeTextField.setEnabled(!Boolean.FALSE.equals(controller.getStaircase()));
      final PropertyChangeListener staircaseCutOutShapeChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            staircaseCutOutShapeTextField.setText(controller.getStaircaseCutOutShape());
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.STAIRCASE_CUT_OUT_SHAPE, staircaseCutOutShapeChangeListener);
      final Color defaultTextFieldColor = this.staircaseCutOutShapeTextField.getForeground();
      this.staircaseCutOutShapeTextField.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.STAIRCASE_CUT_OUT_SHAPE, staircaseCutOutShapeChangeListener);
            String staircaseCutOutShape = staircaseCutOutShapeTextField.getText(); 
            if (staircaseCutOutShape == null || staircaseCutOutShape.trim().length() == 0) {
              controller.setStaircaseCutOutShape(null);
            } else {
              controller.setStaircaseCutOutShape(staircaseCutOutShape);
              try {
                PathParser pathParser = new PathParser();
                pathParser.setPathHandler(new AWTPathProducer());
                pathParser.parse(staircaseCutOutShape);
                staircaseCutOutShapeTextField.setForeground(defaultTextFieldColor);
              } catch (ParseException ex) {
                staircaseCutOutShapeTextField.setForeground(Color.RED);
              } catch (NullPointerException ex) {
                staircaseCutOutShapeTextField.setForeground(Color.RED);
              }
            }
            controller.addPropertyChangeListener(FurnitureController.Property.STAIRCASE_CUT_OUT_SHAPE, staircaseCutOutShapeChangeListener);
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.BACK_FACE_SHOWN)) {
      // Create back face shown check box bound to BACK_FACE_SHOWN controller property
      this.backFaceShownCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "backFaceShownCheckBox.text"));
      this.backFaceShownCheckBox.setNullable(controller.getBackFaceShown() == null);
      this.backFaceShownCheckBox.setValue(controller.getBackFaceShown());
      final PropertyChangeListener backFaceShownChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          backFaceShownCheckBox.setNullable(ev.getNewValue() == null);
          backFaceShownCheckBox.setValue((Boolean)ev.getNewValue());
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.BACK_FACE_SHOWN, backFaceShownChangeListener);
      this.backFaceShownCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.BACK_FACE_SHOWN, backFaceShownChangeListener);
            controller.setBackFaceShown(backFaceShownCheckBox.getValue());
            resetIcon(false);
            controller.addPropertyChangeListener(FurnitureController.Property.BACK_FACE_SHOWN, backFaceShownChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.RESIZABLE)) {
      // Create resizable check box bound to RESIZABLE controller property
      this.resizableCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "resizableCheckBox.text"));
      this.resizableCheckBox.setNullable(controller.getResizable() == null);
      this.resizableCheckBox.setValue(controller.getResizable());
      final PropertyChangeListener resizableChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          resizableCheckBox.setNullable(ev.getNewValue() == null);
          resizableCheckBox.setValue((Boolean)ev.getNewValue());
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.RESIZABLE, resizableChangeListener);
      this.resizableCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.RESIZABLE, resizableChangeListener);
            controller.setResizable(resizableCheckBox.getValue());
            controller.addPropertyChangeListener(FurnitureController.Property.RESIZABLE, resizableChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.DEFORMABLE)) {
      // Create deformable check box bound to DEFORMABLE controller property
      this.deformableCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "deformableCheckBox.text"));
      this.deformableCheckBox.setNullable(controller.getDeformable() == null);
      this.deformableCheckBox.setValue(controller.getDeformable());
      final PropertyChangeListener deformableChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          deformableCheckBox.setNullable(ev.getNewValue() == null);
          deformableCheckBox.setValue((Boolean)ev.getNewValue());
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.DEFORMABLE, deformableChangeListener);
      this.deformableCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.DEFORMABLE, deformableChangeListener);
            controller.setDeformable(deformableCheckBox.getValue());
            controller.addPropertyChangeListener(FurnitureController.Property.DEFORMABLE, deformableChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.TEXTURABLE)) {
      // Create texturable check box bound to TEXTURABLE controller property
      this.texturableCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(preferences, 
          FurniturePanel.class, "texturableCheckBox.text"));
      this.texturableCheckBox.setNullable(controller.getTexturable() == null);
      this.texturableCheckBox.setValue(controller.getTexturable());
      final PropertyChangeListener texturableChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          texturableCheckBox.setNullable(ev.getNewValue() == null);
          texturableCheckBox.setValue((Boolean)ev.getNewValue());
        }
      };
      controller.addPropertyChangeListener(FurnitureController.Property.TEXTURABLE, texturableChangeListener);
      this.texturableCheckBox.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            controller.removePropertyChangeListener(FurnitureController.Property.TEXTURABLE, texturableChangeListener);
            controller.setTexturable(texturableCheckBox.getValue());
            controller.addPropertyChangeListener(FurnitureController.Property.TEXTURABLE, texturableChangeListener);
          }
        });
    }
    
    if (this.controller.isPropertyEditable(FurnitureController.Property.ICON)) {
      this.iconComponent = new IconPreviewComponent(controller, preferences);
      this.turnLeftButton = new JButton(new ResourceAction(preferences, FurniturePanel.class, "TURN_LEFT", true) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            Transform3D oldTransform = getModelRotationTransform();
            Transform3D leftRotation = new Transform3D();
            leftRotation.rotY(-Math.PI / 2);
            leftRotation.mul(oldTransform);
            updateModelRotation(leftRotation);
          }
        });
      this.turnRightButton = new JButton(new ResourceAction(preferences, FurniturePanel.class, "TURN_RIGHT", true) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            Transform3D oldTransform = getModelRotationTransform();
            Transform3D rightRotation = new Transform3D();
            rightRotation.rotY(Math.PI / 2);
            rightRotation.mul(oldTransform);
            updateModelRotation(rightRotation);
          }
        });
      this.turnUpButton = new JButton(new ResourceAction(preferences, FurniturePanel.class, "TURN_UP", true) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            Transform3D oldTransform = getModelRotationTransform();
            Transform3D upRotation = new Transform3D();
            upRotation.rotX(-Math.PI / 2);
            upRotation.mul(oldTransform);
            updateModelRotation(upRotation);
          }
        });
      this.turnDownButton = new JButton(new ResourceAction(preferences, FurniturePanel.class, "TURN_DOWN", true) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            Transform3D oldTransform = getModelRotationTransform();
            Transform3D downRotation = new Transform3D();
            downRotation.rotX(Math.PI / 2);
            downRotation.mul(oldTransform);
            updateModelRotation(downRotation);
          }
        });
    }

    if (this.controller.isPropertyEditable(FurnitureController.Property.MODEL)) {
      if (this.iconComponent != null) {
        controller.addPropertyChangeListener(FurnitureController.Property.MODEL, 
            new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent ev) {
                resetIcon(true);
              }
            });
        // Add a transfer handler to model preview component to let user drag and drop a file in component
        this.iconComponent.setTransferHandler(new TransferHandler() {
              @Override
              public boolean canImport(JComponent comp, DataFlavor [] flavors) {
                return Arrays.asList(flavors).contains(DataFlavor.javaFileListFlavor);
              }
              
              @Override
              public boolean importData(JComponent comp, Transferable transferedFiles) {
                boolean success = false;
                try {
                  List<File> files = (List<File>)transferedFiles.getTransferData(DataFlavor.javaFileListFlavor);
                  for (File file : files) {
                    final String modelName = file.getAbsolutePath();
                    // Try to import the first file that would be accepted by content manager
                    if (controller.getContentManager().isAcceptable(modelName, ContentManager.ContentType.MODEL)) {
                      EventQueue.invokeLater(new Runnable() {
                          public void run() {
                            controller.setModel(modelName);
                          }
                        });
                      success = true;
                      break;
                    }
                  }
                } catch (UnsupportedFlavorException ex) {
                  // No success
                } catch (IOException ex) {
                  // No success
                }
                if (!success) {
                  EventQueue.invokeLater(new Runnable() {
                      public void run() {
                        JOptionPane.showMessageDialog(SwingUtilities.getRootPane(FurniturePanel.this), 
                            preferences.getLocalizedString(FurniturePanel.class, "modelError"),
                            preferences.getLocalizedString(FurniturePanel.class, "errorTitle"),
                            JOptionPane.ERROR_MESSAGE);
                      }
                    });
                }
                return success;
              }
            });
        this.iconComponent.setBorder(SwingTools.getDropableComponentBorder());
      }
      
      this.changeModelButton = new JButton(new ResourceAction(preferences, FurniturePanel.class, "CHANGE_MODEL", true) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            String modelName = controller.getContentManager().showOpenDialog(FurniturePanel.this, 
                preferences.getLocalizedString(FurniturePanel.class, "modelChoiceDialog.title"), 
                ContentManager.ContentType.MODEL);
            if (modelName != null) {
              controller.setModel(modelName);
            }
          }
        });
    }

    this.dialogTitle = preferences.getLocalizedString(FurniturePanel.class, "homeFurniture.title");
  }
  
  /**
   * Returns the transformation matching current model rotation.
   */
  private Transform3D getModelRotationTransform() {
    float [][] modelRotation = this.controller.getModelRotation();
    Matrix3f modelRotationMatrix = new Matrix3f(modelRotation [0][0], modelRotation [0][1], modelRotation [0][2],
        modelRotation [1][0], modelRotation [1][1], modelRotation [1][2],
        modelRotation [2][0], modelRotation [2][1], modelRotation [2][2]);
    Transform3D transform = new Transform3D();
    transform.setRotation(modelRotationMatrix);
    return transform;
  }
  
  /**
   * Updates model rotation from the values of <code>transform</code>.
   */
  private void updateModelRotation(Transform3D transform) {
    Matrix3f modelRotationMatrix = new Matrix3f();
    transform.getRotationScale(modelRotationMatrix);
    boolean proportional = this.controller.isProportional();
    this.controller.setProportional(false);
    this.controller.setModelRotation(new float [][] {{modelRotationMatrix.m00, modelRotationMatrix.m01, modelRotationMatrix.m02},
                                                     {modelRotationMatrix.m10, modelRotationMatrix.m11, modelRotationMatrix.m12},
                                                     {modelRotationMatrix.m20, modelRotationMatrix.m21, modelRotationMatrix.m22}});
    this.controller.setProportional(proportional);
    resetIcon(true);
  }

  /**
   * Resets the model icon. 
   */
  private void resetIcon(boolean resetView) {
    if (this.controller.isPropertyEditable(FurnitureController.Property.ICON)) {
      try {
        if (resetView) {
          this.iconComponent.resetView();
        }
        this.controller.setIcon(this.iconComponent.getIcon(400));
      } catch (IOException ex) {
        ex.printStackTrace();
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
            FurniturePanel.class, "idLabel.mnemonic")).getKeyCode());
        this.idLabel.setLabelFor(this.idTextField);
      }
      if (this.nameLabel != null) {
        this.nameLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "nameLabel.mnemonic")).getKeyCode());
        this.nameLabel.setLabelFor(this.nameTextField);
      }
      if (this.descriptionLabel != null) {
        this.descriptionLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "descriptionLabel.mnemonic")).getKeyCode());
        this.descriptionLabel.setLabelFor(this.descriptionTextField);
      }
      if (this.informationLabel != null) {
        this.informationLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "informationLabel.mnemonic")).getKeyCode());
        this.informationLabel.setLabelFor(this.informationTextField);
      }
      if (this.tagsLabel != null) {
        this.tagsLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "tagsLabel.mnemonic")).getKeyCode());
        this.tagsLabel.setLabelFor(this.tagsTextField);
      }
      if (this.creatorLabel != null) {
        this.creatorLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "creatorLabel.mnemonic")).getKeyCode());
        this.creatorLabel.setLabelFor(this.creatorTextField);
      }
      if (this.categoryLabel != null) {
        this.categoryLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "categoryLabel.mnemonic")).getKeyCode());
        this.categoryLabel.setLabelFor(this.categoryComboBox);
      }
      if (this.creationDateLabel != null) {
        this.creationDateLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "creationDateLabel.mnemonic")).getKeyCode());
        this.creationDateLabel.setLabelFor(this.creationDateSpinner);
      }
      if (this.priceLabel != null) {
        this.priceLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "priceLabel.mnemonic")).getKeyCode());
        this.priceLabel.setLabelFor(this.priceSpinner);
      }
      if (this.priceLabel != null) {
        this.valueAddedTaxPercentageLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "valueAddedTaxPercentageLabel.mnemonic")).getKeyCode());
        this.valueAddedTaxPercentageLabel.setLabelFor(this.valueAddedTaxPercentageSpinner);
      }
      if (this.widthLabel != null) {
        this.widthLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "widthLabel.mnemonic")).getKeyCode());
        this.widthLabel.setLabelFor(this.widthSpinner);
      }
      if (this.depthLabel != null) {
        this.depthLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "depthLabel.mnemonic")).getKeyCode());
        this.depthLabel.setLabelFor(this.depthSpinner);
      }
      if (this.heightLabel != null) {
        this.heightLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "heightLabel.mnemonic")).getKeyCode());
        this.heightLabel.setLabelFor(this.heightSpinner);
      }
      if (this.keepProportionsCheckBox != null) {
        this.keepProportionsCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "keepProportionsCheckBox.mnemonic")).getKeyCode());
      }
      if (this.elevationLabel != null) {
        this.elevationLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "elevationLabel.mnemonic")).getKeyCode());
        this.elevationLabel.setLabelFor(this.elevationSpinner);
      }
      if (this.movableCheckBox != null) {
        this.movableCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "movableCheckBox.mnemonic")).getKeyCode());
      }
      if (this.doorOrWindowCheckBox != null) {
        this.doorOrWindowCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "doorOrWindowCheckBox.mnemonic")).getKeyCode());
      }
      if (this.doorOrWindowCustomizedCutOutShapeCheckBox != null) {
        this.doorOrWindowCustomizedCutOutShapeCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "doorOrWindowCustomizedCutOutShapeCheckBox.mnemonic")).getKeyCode());
      }
      if (this.staircaseCheckBox != null) {
        this.staircaseCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "staircaseCheckBox.mnemonic")).getKeyCode());
      }
      if (this.staircaseCutOutShapeLabel != null) {
        this.staircaseCutOutShapeLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "staircaseCutOutShapeLabel.mnemonic")).getKeyCode());
        this.staircaseCutOutShapeLabel.setLabelFor(this.staircaseCutOutShapeTextField);
      }
      if (this.backFaceShownCheckBox != null) {
        this.backFaceShownCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "backFaceShownCheckBox.mnemonic")).getKeyCode());
      }
      if (this.resizableCheckBox != null) {
        this.resizableCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "resizableCheckBox.mnemonic")).getKeyCode());
      }
      if (this.deformableCheckBox != null) {
        this.deformableCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "deformableCheckBox.mnemonic")).getKeyCode());
      }
      if (this.texturableCheckBox != null) {
        this.texturableCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            FurniturePanel.class, "texturableCheckBox.mnemonic")).getKeyCode());
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
    if (this.controller.isPropertyEditable(FurnitureController.Property.ICON)) {
      JPanel iconPanel = new JPanel(new GridBagLayout());
      // Add dummy labels with a vertical weight of 1 at top and bottom of iconPanel 
      // to keep iconComponent and rotationButtonsPanel in the middle
      // when grid bag fill constraint is BOTH for iconPanel.
      // If this constraint is set to HORIZONTAL only, iconPanel location may be lower
      // and may not be the first panel treated by the focus traversal algorithm 
      iconPanel.add(new JLabel(), new GridBagConstraints(
          0, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, 
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
      if (this.controller.isPropertyEditable(FurnitureController.Property.MODEL)) {
        iconPanel.add(this.changeModelButton, new GridBagConstraints(
            0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, 
            GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
      }
      iconPanel.add(this.iconComponent, new GridBagConstraints(
          0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, 
          GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
      if (this.controller.isPropertyEditable(FurnitureController.Property.MODEL_ROTATION)) {
        JPanel rotationButtonsPanel = new JPanel(new GridBagLayout()) {
            @Override
            public void applyComponentOrientation(ComponentOrientation o) {
              // Ignore panel orientation to ensure left button is always at left of panel
            }
          };
        rotationButtonsPanel.add(this.turnUpButton, new GridBagConstraints(
            1, 0, 1, 1, 0, 0, GridBagConstraints.SOUTH, 
            GridBagConstraints.NONE, new Insets(0, 0, 2, 0), 0, 0));    
        rotationButtonsPanel.add(this.turnLeftButton, new GridBagConstraints(
            0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, 
            GridBagConstraints.NONE, new Insets(0, 0, 2, 2), 0, 0));
        rotationButtonsPanel.add(this.turnRightButton, new GridBagConstraints(
            2, 1, 1, 1, 1, 0, GridBagConstraints.WEST, 
            GridBagConstraints.NONE, new Insets(0, 2, 2, 0), 0, 0));
        rotationButtonsPanel.add(this.turnDownButton, new GridBagConstraints(
            1, 2, 1, 1, 0, 0, GridBagConstraints.NORTH, 
            GridBagConstraints.NONE, new Insets(0, 0, 2, 0), 0, 0));
        iconPanel.add(rotationButtonsPanel, new GridBagConstraints(
            0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, 
            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      }
      iconPanel.add(new JLabel(), new GridBagConstraints(
          0, 4, 1, 1, 0, 1, GridBagConstraints.CENTER, 
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

      add(iconPanel, new GridBagConstraints(
          0, 0, 1, 16, 0, 0, GridBagConstraints.CENTER, 
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 15), 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.ID)) {
      add(this.idLabel, new GridBagConstraints(
          1, 0, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.idTextField, new GridBagConstraints(
          2, 0, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.NAME)) {
      add(this.nameLabel, new GridBagConstraints(
          1, 1, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.nameTextField, new GridBagConstraints(
          2, 1, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.DESCRIPTION)) {
      add(this.descriptionLabel, new GridBagConstraints(
          1, 2, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.descriptionTextField, new GridBagConstraints(
          2, 2, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.INFORMATION)) {
      add(this.informationLabel, new GridBagConstraints(
          1, 3, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.informationTextField, new GridBagConstraints(
          2, 3, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.TAGS)) {
      add(this.tagsLabel, new GridBagConstraints(
          1, 4, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.tagsTextField, new GridBagConstraints(
          2, 4, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.CREATOR)) {
      add(this.creatorLabel, new GridBagConstraints(
          1, 5, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.creatorTextField, new GridBagConstraints(
          2, 5, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.CATEGORY)) {
      add(this.categoryLabel, new GridBagConstraints(
          3, 5, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.categoryComboBox, new GridBagConstraints(
          4, 5, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.CREATION_DATE)) {
      add(this.creationDateLabel, new GridBagConstraints(
          1, 6, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.creationDateSpinner, new GridBagConstraints(
          2, 6, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.GRADE)) {
      add(this.gradeLabel, new GridBagConstraints(
          3, 6, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.gradeSpinner, new GridBagConstraints(
          4, 6, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.PRICE)) {
      add(this.priceLabel, new GridBagConstraints(
          1, 7, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.priceSpinner, new GridBagConstraints(
          2, 7, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), -10, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.VALUE_ADDED_TAX_PERCENTAGE)) {
      add(this.valueAddedTaxPercentageLabel, new GridBagConstraints(
          3, 7, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.valueAddedTaxPercentageSpinner, new GridBagConstraints(
          4, 7, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 10, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.WIDTH)) {
      add(this.widthLabel, new GridBagConstraints(
          1, 8, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.widthSpinner, new GridBagConstraints(
          2, 8, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), -10, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.DEPTH)) {
      add(this.depthLabel, new GridBagConstraints(
          3, 8, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.depthSpinner, new GridBagConstraints(
          4, 8, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, -10, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.HEIGHT)) {
      add(this.heightLabel, new GridBagConstraints(
          1, 9, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.heightSpinner, new GridBagConstraints(
          2, 9, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), -10, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.ELEVATION)) {
      add(this.elevationLabel, new GridBagConstraints(
          3, 9, 1, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.elevationSpinner, new GridBagConstraints(
          4, 9, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, -10, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.PROPORTIONAL)) {
      JPanel multiplySizePanel = new JPanel();
      multiplySizePanel.add(this.enlargeTenTimesButton);
      multiplySizePanel.add(this.reduceTenTimesButton);
      multiplySizePanel.add(this.enlargeInchTimesButton);
      add(multiplySizePanel, new GridBagConstraints(
          1, 10, 3, 1, 0, 0, GridBagConstraints.CENTER, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
      add(this.keepProportionsCheckBox, new GridBagConstraints(
          4, 10, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.BACK_FACE_SHOWN)) {
      add(this.backFaceShownCheckBox, new GridBagConstraints(
          1, 11, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.DOOR_OR_WINDOW)) {
      add(this.doorOrWindowCheckBox, new GridBagConstraints(
          1, 12, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
      if (this.controller.isPropertyEditable(FurnitureController.Property.DOOR_OR_WINDOW_CUT_OUT_SHAPE)) {
        add(this.doorOrWindowCustomizedCutOutShapeCheckBox, new GridBagConstraints(
            2, 12, 2, 1, 0, 0, labelAlignment, 
            GridBagConstraints.NONE, OperatingSystem.isMacOSX() ? new Insets(0, 0, 5, 0) : labelInsets, 0, 0));
        add(this.doorOrWindowCustomizedCutOutShapeTextField, new GridBagConstraints(
            4, 12, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
            GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
      }
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.STAIRCASE_CUT_OUT_SHAPE)) {
      add(this.staircaseCheckBox, new GridBagConstraints(
          1, 13, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
      add(this.staircaseCutOutShapeLabel, new GridBagConstraints(
          2, 13, 2, 1, 0, 0, labelAlignment, 
          GridBagConstraints.NONE, labelInsets, 0, 0));
      add(this.staircaseCutOutShapeTextField, new GridBagConstraints(
          4, 13, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.MOVABLE)) {
      add(this.movableCheckBox, new GridBagConstraints(
          1, 14, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.RESIZABLE)) {
      add(this.resizableCheckBox, new GridBagConstraints(
          2, 14, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.DEFORMABLE)) {
      add(this.deformableCheckBox, new GridBagConstraints(
          3, 14, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
    }
    if (this.controller.isPropertyEditable(FurnitureController.Property.TEXTURABLE)) {
      add(this.texturableCheckBox, new GridBagConstraints(
          4, 14, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
          GridBagConstraints.NONE, componentInsets, 0, 0));
    }
  }

  /**
   * Displays this panel in a modal dialog box. 
   */
  public void displayView(View parentView) {
    final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (SwingTools.showConfirmDialog((JComponent)parentView, 
            this, this.dialogTitle, this.nameTextField) == JOptionPane.OK_OPTION) {
      if (this.controller.getBackFaceShown() != null
          && this.controller.getBackFaceShown()) {
        JRootPane rootPane = SwingUtilities.getRootPane((JComponent)parentView);
        Cursor defaultCursor = rootPane.getCursor();
        try {
          rootPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          // Generate a modified model with back face (modifying the model is required because this information isn't stored in a SH3F file)
          HomePieceOfFurniture3D piece3D = new HomePieceOfFurniture3D(new HomePieceOfFurniture(
              new CatalogPieceOfFurniture(this.controller.getName(), null, 
                  this.controller.getModel(), this.controller.getWidth(), this.controller.getDepth(), this.controller.getHeight(),
                  0, false, null, null, this.controller.getBackFaceShown(), 0, false)), null, true, true); 
          this.controller.setModel(ImportFurnitureTaskPanel.copyToTemporaryOBJContent(piece3D, this.controller.getModel()));
        } catch (IOException e) {
          JOptionPane.showMessageDialog(rootPane, 
              preferences.getLocalizedString(FurniturePanel.class, "backFaceShownError"),
              preferences.getLocalizedString(FurniturePanel.class, "errorTitle"),
              JOptionPane.ERROR_MESSAGE);
        } finally {
          rootPane.setCursor(defaultCursor);
        }
      }
      this.controller.modifyFurniture();
    }
    if (focusOwner != null) {
      focusOwner.requestFocusInWindow();
    }
  }

  /**
   * Preview component for model icon. 
   */
  private static class IconPreviewComponent extends ModelPreviewComponent {
    private static final int PREFERRED_SIZE = 128;
    
    private float defaultViewYaw;
    private float defaultViewPitch;
    private float defaultViewScale;
    
    public IconPreviewComponent(final FurnitureController controller,
                                final UserPreferences preferences) {
      super(true);
      if (controller != null) {
        addModelListener(controller, preferences);
        addSizeListeners(controller);
        addRotationListener(controller);
        addBackFaceShownListener(controller);
        setBackground(UIManager.getColor("window"));
        setModel(controller.getModel(), preferences);
        if (controller.getModel() != null) {
          setModelRotationAndSize(controller.getModelRotation(), 
              controller.getWidth(), controller.getDepth(), controller.getHeight());
        }
        
        addMouseListener(new MouseAdapter() {
          private boolean mousePressedInIcon;

          @Override
          public void mousePressed(MouseEvent ev) {
            this.mousePressedInIcon = true;
          }
          
          @Override
          public void mouseReleased(MouseEvent ev) {
            if (this.mousePressedInIcon) {
              this.mousePressedInIcon = false;
              // Change icon when mouse is released after a change in the component
              try {
                controller.setIcon(getIcon(400));
              } catch (IOException ex) {
                ex.printStackTrace();
              }
            }
          }
        });
      }

      this.defaultViewYaw = getViewYaw();
      this.defaultViewPitch = getViewPitch();
      this.defaultViewScale = getViewScale();
    }

    /**
     * Adds listeners to <code>controller</code> to update the rotation of the piece model
     * displayed by this component.
     */
    private void addRotationListener(final FurnitureController controller) {
      PropertyChangeListener rotationChangeListener = new PropertyChangeListener () {
          public void propertyChange(PropertyChangeEvent ev) {
            setModelRotation(controller.getModelRotation());
            
            // Update size when a new rotation is provided
            if (ev.getOldValue() != null) {
              float width = controller.getWidth();
              float depth = controller.getDepth();
              float height = controller.getHeight();
              
              // Compute size before old model rotation
              float [][] oldModelRotation = (float [][])ev.getOldValue();
              Matrix3f oldModelRotationMatrix = new Matrix3f(oldModelRotation [0][0], oldModelRotation [0][1], oldModelRotation [0][2],
                  oldModelRotation [1][0], oldModelRotation [1][1], oldModelRotation [1][2],
                  oldModelRotation [2][0], oldModelRotation [2][1], oldModelRotation [2][2]);
              oldModelRotationMatrix.invert();
              float oldWidth = oldModelRotationMatrix.m00 * width 
                  + oldModelRotationMatrix.m01 * height 
                  + oldModelRotationMatrix.m02 * depth;
              float oldHeight = oldModelRotationMatrix.m10 * width 
                  + oldModelRotationMatrix.m11 * height 
                  + oldModelRotationMatrix.m12 * depth;
              float oldDepth = oldModelRotationMatrix.m20 * width 
                  + oldModelRotationMatrix.m21 * height 
                  + oldModelRotationMatrix.m22 * depth;
              
              // Compute size after new model rotation
              float [][] newModelRotation = (float [][])ev.getNewValue();
              controller.setWidth(Math.abs(newModelRotation [0][0] * oldWidth 
                  + newModelRotation [0][1] * oldHeight 
                  + newModelRotation [0][2] * oldDepth));
              controller.setHeight(Math.abs(newModelRotation [1][0] * oldWidth 
                  + newModelRotation [1][1] * oldHeight 
                  + newModelRotation [1][2] * oldDepth));
              controller.setDepth(Math.abs(newModelRotation [2][0] * oldWidth 
                  + newModelRotation [2][1] * oldHeight 
                  + newModelRotation [2][2] * oldDepth));
            }
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.MODEL_ROTATION,
          rotationChangeListener);
    }

    /**
     * Adds listeners to <code>controller</code> to update the rotation and the size of the piece model
     * displayed by this component.
     */
    private void addSizeListeners(final FurnitureController controller) {
      PropertyChangeListener sizeChangeListener = new PropertyChangeListener () {
          public void propertyChange(PropertyChangeEvent ev) {
            setModelRotationAndSize(controller.getModelRotation(),
                controller.getWidth(), controller.getDepth(), controller.getHeight());
          }
        };
      controller.addPropertyChangeListener(FurnitureController.Property.MODEL_ROTATION,
          sizeChangeListener);
      controller.addPropertyChangeListener(FurnitureController.Property.WIDTH,
          sizeChangeListener);
      controller.addPropertyChangeListener(FurnitureController.Property.DEPTH,
          sizeChangeListener);
      controller.addPropertyChangeListener(FurnitureController.Property.HEIGHT,
          sizeChangeListener);
    }
    
    /**
     * Adds listeners to <code>controller</code> to update the face culling of the piece model
     * displayed by this component.
     */
    private void addBackFaceShownListener(final FurnitureController controller) {
      controller.addPropertyChangeListener(FurnitureController.Property.BACK_FACE_SHOWN, 
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              if (controller.getBackFaceShown() != null) {
                setBackFaceShown(controller.getBackFaceShown());
              }
            }
          });
    }
    
    /**
     * Adds a listener to <code>controller</code> to update the model
     * displayed by this component.
     */
    private void addModelListener(final FurnitureController controller,
                                  final UserPreferences preferences) {
      controller.addPropertyChangeListener(FurnitureController.Property.MODEL,
          new PropertyChangeListener () {
            public void propertyChange(PropertyChangeEvent ev) {
              setModel(controller.getModel(), preferences);
            }
          });
    }
    
    /**
     * Sets the 3D model viewed by this model  
     */
    public void setModel(final Content model, final UserPreferences preferences) {
      if (model == null) {
        setModel(null);            
      } else {
        ModelManager.getInstance().loadModel(model, new ModelManager.ModelObserver() {          
            public void modelUpdated(BranchGroup modelRoot) {
              setModel(model);            
            }
            
            public void modelError(Exception ex) {
              JOptionPane.showMessageDialog(SwingUtilities.getRootPane(IconPreviewComponent.this), 
                  preferences.getLocalizedString(FurniturePanel.class, "modelError"),
                  preferences.getLocalizedString(FurniturePanel.class, "errorTitle"),
                  JOptionPane.ERROR_MESSAGE);
            }
          });
      }
    }

    @Override
    public Dimension getPreferredSize() {
      Insets insets = getInsets();
      return new Dimension(PREFERRED_SIZE + insets.left + insets.right, PREFERRED_SIZE  + insets.top + insets.bottom);
    }
    
    /**
     * Resets view angles and scale.
     */
    public void resetView() {
      setViewYaw(this.defaultViewYaw);
      setViewPitch(this.defaultViewPitch);
      setViewScale(this.defaultViewScale);
    }
  }
}
