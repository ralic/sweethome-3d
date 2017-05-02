/*
 * TexturesLibraryTable.java 11 sept 2012
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
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.IconManager;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.tools.URLContent;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;
import com.eteks.textureslibraryeditor.viewcontroller.TexturesLanguageController;
import com.eteks.textureslibraryeditor.viewcontroller.TexturesLibraryController;

/**
 * A table used to edit textures library.
 * @author Emmanuel Puybaret
 */
public class TexturesLibraryTable extends JTable implements View {
  private ListSelectionListener tableSelectionListener;

  public TexturesLibraryTable(TexturesLibrary texturesLibrary,
                              TexturesLibraryUserPreferences preferences,
                              TexturesLibraryController texturesLibraryController,
                              TexturesLanguageController texturesLanguageController) {
    super(new TexturesLibraryTableModel(texturesLibrary, texturesLanguageController),
        new TexturesLibraryTableColumnModel(texturesLibrary, preferences, texturesLanguageController));
    addTableHeaderListener();
    setAutoResizeMode(AUTO_RESIZE_OFF);
    updateTableColumnsWidth();
    if (texturesLibraryController != null) {
      addSelectionListeners(texturesLibraryController);
      addMouseListener(texturesLibraryController);
      addTexturesLanguageListener(texturesLibrary, texturesLanguageController);
      setTransferHandler(new TableTransferHandler(texturesLibraryController));
    }
    addUserPreferencesListener(preferences);
  }
  
  /**
   * Adds a listener to <code>preferences</code> to repaint this table
   * and its header when unit changes.  
   */
  private void addUserPreferencesListener(UserPreferences preferences) {
    preferences.addPropertyChangeListener(
        UserPreferences.Property.UNIT, new UserPreferencesChangeListener(this));
  }

  /**
   * Preferences property listener bound to this table with a weak reference to avoid
   * strong link between user preferences and this table.  
   */
  private static class UserPreferencesChangeListener implements PropertyChangeListener {
    private WeakReference<TexturesLibraryTable>  texturesLibraryTable;

    public UserPreferencesChangeListener(TexturesLibraryTable texturesTable) {
      this.texturesLibraryTable = new WeakReference<TexturesLibraryTable>(texturesTable);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      // If textures table was garbage collected, remove this listener from preferences
      TexturesLibraryTable texturesLibraryTable = this.texturesLibraryTable.get();
      if (texturesLibraryTable == null) {
        ((UserPreferences)ev.getSource()).removePropertyChangeListener(
            UserPreferences.Property.valueOf(ev.getPropertyName()), this);
      } else {
        texturesLibraryTable.repaint();
        texturesLibraryTable.getTableHeader().repaint();
      }
    }
  }

  /**
   * Adds selection listeners to this table.
   */
  private void addSelectionListeners(final TexturesLibraryController controller) {   
    final SelectionListener controllerSelectionListener = new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          setSelectedTextures(controller.getSelectedTextures());        
        }
      };
    this.tableSelectionListener = new ListSelectionListener () {
        public void valueChanged(ListSelectionEvent ev) {
          if (!ev.getValueIsAdjusting()) {
            controller.removeSelectionListener(controllerSelectionListener);
            int [] selectedRows = getSelectedRows();
            // Build the list of selected textures
            List<CatalogTexture> selectedTextures =
                new ArrayList<CatalogTexture>(selectedRows.length);
            TableModel tableModel = getModel();
            for (int index : selectedRows) {
              // Add to selectedTextures table model value that stores texture
              selectedTextures.add((CatalogTexture)tableModel.getValueAt(index, 0));
            }
            // Set the new selection in controller
            controller.setSelectedTextures(selectedTextures);
            controller.addSelectionListener(controllerSelectionListener);
          }
        }
      };
    getSelectionModel().addListSelectionListener(this.tableSelectionListener);
    controller.addSelectionListener(controllerSelectionListener);
  }

  /**
   * Adds a double click mouse listener to modify selected textures.
   */
  private void addMouseListener(final TexturesLibraryController controller) {
    addMouseListener(new MouseAdapter () {
        @Override
        public void mouseClicked(MouseEvent ev) {
          if (ev.getClickCount() == 2) {
            controller.modifySelectedTextures();
          }
        }
      });
  }

  /**
   * Adds a mouse listener on table header that will sort textures.
   */
  private void addTableHeaderListener() {
    // Sort on click in column header 
    getTableHeader().addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          TexturesLibraryTableModel tableModel = (TexturesLibraryTableModel)getModel();
          List<CatalogTexture> selectedTextures = new ArrayList<CatalogTexture>();
          for (int index : getSelectedRows()) {
            selectedTextures.add((CatalogTexture)tableModel.getValueAt(index, 0));
          }
          int columnIndex = getTableHeader().columnAtPoint(ev.getPoint());
          Object columnIdentifier = getColumnModel().getColumn(columnIndex).getIdentifier();
          if (columnIdentifier instanceof String) {
            String propertyKey = (String)columnIdentifier; 
            if (columnIdentifier.equals(tableModel.getSortProperty())) {
              if (tableModel.isDescendingOrder()) {
                tableModel.setSortProperty(null);
              } else {
                tableModel.setDescendingOrder(true);
              }
            } else if (tableModel.getTexturesComparator(propertyKey) != null) {
              tableModel.setSortProperty(propertyKey);
              tableModel.setDescendingOrder(false);
            }
          }
          getTableHeader().repaint();
          setSelectedTextures(selectedTextures);
        }
      });
  }

  /**
   * Selects textures in table. 
   */
  private void setSelectedTextures(List<CatalogTexture> selectedTextures) {
    getSelectionModel().removeListSelectionListener(this.tableSelectionListener);
    clearSelection();
    TexturesLibraryTableModel tableModel = (TexturesLibraryTableModel)getModel();
    int minIndex = Integer.MAX_VALUE;
    int maxIndex = Integer.MIN_VALUE;
    for (CatalogTexture texture : selectedTextures) {
      if (texture instanceof CatalogTexture) {
        // Search index of texture in sorted table model
        int index = tableModel.getTextureIndex((CatalogTexture)texture);
        // If the texture was found (during the addition of a texture to library, the model may not be updated yet) 
        if (index != -1) {
          addRowSelectionInterval(index, index);
          minIndex = Math.min(minIndex, index);
          maxIndex = Math.max(maxIndex, index);
        }
      }
    }
    if (minIndex != Integer.MIN_VALUE) {
      makeRowsVisible(minIndex, maxIndex);
    }
    getSelectionModel().addListSelectionListener(this.tableSelectionListener);
  }
  
  /**
   * Ensures the rectangle which displays rows from <code>minIndex</code> to <code>maxIndex</code> is visible.
   */
  private void makeRowsVisible(int minRow, int maxRow) {    
    // Compute the rectangle that includes a row 
    Rectangle includingRectangle = getCellRect(minRow, 0, true);
    if (minRow != maxRow) {
      includingRectangle = includingRectangle.
          union(getCellRect(maxRow, 0, true));      
    }
    if (getAutoResizeMode() == AUTO_RESIZE_OFF) {
      int lastColumn = getColumnCount() - 1;
      includingRectangle = includingRectangle.
          union(getCellRect(minRow, lastColumn, true));
      if (minRow != maxRow) {
        includingRectangle = includingRectangle.
            union(getCellRect(maxRow, lastColumn, true));      
      }
    }
    scrollRectToVisible(includingRectangle);
  }

  /**
   * Adds a listener on textures language change to resort textures.
   */
  private void addTexturesLanguageListener(TexturesLibrary texturesLibrary, 
                                           TexturesLanguageController controller) {
    PropertyChangeListener listener = new PropertyChangeListener() {
        private boolean sorting = false;
        
        public void propertyChange(PropertyChangeEvent ev) {
          if (!sorting) {
            // Postpone update in case of multiple localized data is set
            sorting = true;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  TexturesLibraryTableModel tableModel = (TexturesLibraryTableModel)getModel();
                  List<CatalogTexture> selectedTextures = new ArrayList<CatalogTexture>();
                  for (int index : getSelectedRows()) {
                    selectedTextures.add((CatalogTexture)tableModel.getValueAt(index, 0));
                  }
                  tableModel.sortTextures();
                  setSelectedTextures(selectedTextures);
                  sorting = false;
                }
              });
          }
        }
      };
    controller.addPropertyChangeListener(TexturesLanguageController.Property.TEXTURES_LANGUAGE, listener);
    texturesLibrary.addPropertyChangeListener(TexturesLibrary.Property.LOCALIZED_DATA, listener);
  }

  /**
   * Updates table columns width from the content of its cells.
   */
  private void updateTableColumnsWidth() {
    int intercellWidth = getIntercellSpacing().width;
    TableColumnModel columnModel = getColumnModel();
    TableModel tableModel = getModel();
    for (int columnIndex = 0, n = columnModel.getColumnCount(); columnIndex < n; columnIndex++) {
      TableColumn column = columnModel.getColumn(columnIndex);
      int modelColumnIndex = convertColumnIndexToModel(columnIndex);
      int preferredWidth = column.getPreferredWidth();
      preferredWidth = Math.max(preferredWidth, column.getHeaderRenderer().getTableCellRendererComponent(
          this, column.getHeaderValue(), false, false, -1, columnIndex).getPreferredSize().width);
      for (int rowIndex = 0, m = tableModel.getRowCount(); rowIndex < m; rowIndex++) {
        preferredWidth = Math.max(preferredWidth, 
            column.getCellRenderer().getTableCellRendererComponent(
                this, tableModel.getValueAt(rowIndex, modelColumnIndex), false, false, -1, columnIndex).
                    getPreferredSize().width);
      }
      column.setPreferredWidth(preferredWidth + intercellWidth);
      column.setWidth(preferredWidth + intercellWidth);
    }
  }

  /**
   * Returns a tooltip for textures textures described in this table.
   */
  @Override
  public String getToolTipText(MouseEvent ev) {
    int column = columnAtPoint(ev.getPoint());
    if (column != -1 
        && TexturesLibrary.TEXTURES_IMAGE_PROPERTY.equals(getColumnModel().getColumn(column).getIdentifier())) {
      int row = rowAtPoint(ev.getPoint());
      if (row != -1) {
        CatalogTexture texture = (CatalogTexture)getModel().getValueAt(row, 0);
        if (texture.getImage() instanceof URLContent) {
          try {
            // Ensure image will always be viewed in a 128x128 pixels cell
            BufferedImage image = ImageIO.read(((URLContent)texture.getImage()).getURL());
            if (image == null) {
              return null;
            }
            int width;
            int height;
            if (image.getHeight() > image.getWidth()) {
              width = Math.round(128f * image.getWidth() / image.getHeight());
              height = Math.round((float)width * image.getHeight() / image.getWidth());
            } else {
              height = Math.round(128f * image.getHeight() / image.getWidth());
              width = Math.round((float)height * image.getWidth() / image.getHeight());
            }              
            return "<html><table><tr><td width='128' height='128' align='center' valign='middle'>" 
                + "<img width='" + width + "' height='" + height + "' src='" 
                + ((URLContent)texture.getImage()).getURL() + "'></td></tr></table>";
          } catch (IOException ex) {
            return null;
          }
        }
      }
    }
    return null;
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return new Dimension(getPreferredSize().width, 400);
  }
  
  
  /**
   * Model used by textures table.
   */
  private static class TexturesLibraryTableModel extends AbstractTableModel {
    private final TexturesLibrary              texturesLibrary;
    private final TexturesLanguageController   controller; 
    private List<CatalogTexture>               sortedTextures;
    private String                             sortProperty;
    private boolean                            descendingOrder;
    
    public TexturesLibraryTableModel(TexturesLibrary texturesLibrary,
                                      TexturesLanguageController controller) {
      this.texturesLibrary = texturesLibrary;
      this.controller = controller;
      addTexturesLibraryListener(texturesLibrary);
      sortTextures();
    }

    private void addTexturesLibraryListener(final TexturesLibrary texturesLibrary) {
      texturesLibrary.addListener(new CollectionListener<CatalogTexture>() {
        public void collectionChanged(CollectionEvent<CatalogTexture> ev) {
          CatalogTexture texture = ev.getItem();
          int textureIndex = ev.getIndex();
          switch (ev.getType()) {
            case ADD :
              int insertionIndex = getTextureInsertionIndex(texture, texturesLibrary, textureIndex);
              if (insertionIndex != -1) {
                sortedTextures.add(insertionIndex, texture);
                fireTableRowsInserted(insertionIndex, insertionIndex);
              }
              break;
            case DELETE :
              int deletionIndex = getTextureDeletionIndex(texture, texturesLibrary, textureIndex);
              if (deletionIndex != -1) {
                sortedTextures.remove(deletionIndex);
                fireTableRowsDeleted(deletionIndex, deletionIndex);
              }
              break;
          }
        }

        /**
         * Returns the index of an added <code>texture</code> in textures table, with a default index
         * of <code>textureIndex</code> if textures library isn't sorted.
         * If <code>texture</code> isn't added to textures table, the returned value is
         * equals to the insertion index where texture should be added.
         */
        private int getTextureInsertionIndex(CatalogTexture texture, 
                                                      TexturesLibrary texturesLibrary, 
                                                      int textureIndex) {
          if (sortProperty == null) {
            return textureIndex;
          } 
          // Default case when texture is included and textures is  sorted 
          int sortedIndex = Collections.binarySearch(sortedTextures, texture, getTexturesComparator(sortProperty));
          if (sortedIndex >= 0) {
            return sortedIndex;
          } else {
            return -(sortedIndex + 1);
          }              
        }

        /**
         * Returns the index of an existing <code>texture</code> in textures table, with a default index
         * of <code>textureIndex</code> if textures isn't sorted.
         */
        private int getTextureDeletionIndex(CatalogTexture texture, 
                                                     TexturesLibrary texturesLibrary, 
                                                     int textureIndex) {
          if (sortProperty == null) {
            return textureIndex;
          } 
          return getTextureIndex(texture);              
        }
      });
    }

    @Override
    public String getColumnName(int columnIndex) {
      // Column name is set by TableColumn instances themselves 
      return null;
    }

    public int getColumnCount() {
      // Column count is set by TableColumnModel itself 
      return 0;
    }

    public int getRowCount() {
      return this.sortedTextures.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      // Always return texture itself, the real property displayed at screen is chosen by renderer
      return this.sortedTextures.get(rowIndex);
    }

    /**
     * Returns the index of <code>texture</code> in textures table, or -1. 
     */
    public int getTextureIndex(CatalogTexture searchedTexture) {
      for (int i = 0, n = this.sortedTextures.size(); i < n; i++) {
        CatalogTexture texture = this.sortedTextures.get(i);
        if (searchedTexture == texture) {
          return i;
        }
      }
      return -1;
    }

    /**
     * Sorts textures.
     */
    public void sortTextures() {
      int previousRowCount = this.sortedTextures != null 
          ? this.sortedTextures.size()
          : 0;
      List<CatalogTexture> libraryTextures = this.texturesLibrary.getTextures();
      this.sortedTextures = new ArrayList<CatalogTexture>(libraryTextures);
      // Sort it if necessary
      if (this.sortProperty != null) {
        Comparator<CatalogTexture> texturesComparator = getTexturesComparator(this.sortProperty);
        Collections.sort(this.sortedTextures, texturesComparator);         
      }
      
      if (previousRowCount != this.sortedTextures.size()) {
        fireTableDataChanged();
      } else {
        fireTableRowsUpdated(0, getRowCount() - 1);
      }
    }

    public Comparator<CatalogTexture> getTexturesComparator(final String propertyKey) {
      final Collator collator = Collator.getInstance();
      Comparator<CatalogTexture> texturesComparator = null;
      if (TexturesLibrary.TEXTURES_ID_PROPERTY.equals(propertyKey)) {
        texturesComparator = new Comparator<CatalogTexture>() {
            public int compare(CatalogTexture texture1, CatalogTexture texture2) {
              if (texture1.getId() == null) {
                return -1;
              } else if (texture2.getId() == null) {
                return 1; 
              } else {
                return collator.compare(texture1.getId(), texture2.getId());
              }
            }
          };
      } else if (TexturesLibrary.TEXTURES_NAME_PROPERTY.equals(propertyKey)) {
         texturesComparator = new Comparator<CatalogTexture>() {
             public int compare(CatalogTexture texture1, CatalogTexture texture2) {
               String texture1Name = (String)texturesLibrary.getTextureLocalizedData(
                   texture1, controller.getTexturesLangauge(), propertyKey, texture1.getName());
               String texture2Name = (String)texturesLibrary.getTextureLocalizedData(
                   texture2, controller.getTexturesLangauge(), propertyKey, texture2.getName());
               return collator.compare(texture1Name, texture2Name);
             }
           };
      } else if (TexturesLibrary.TEXTURES_CREATOR_PROPERTY.equals(propertyKey)) {
        texturesComparator = new Comparator<CatalogTexture>() {
            public int compare(CatalogTexture texture1, CatalogTexture texture2) {
              return collator.compare(texture1.getCreator(), texture2.getCreator());
            }
          };
      } else if (TexturesLibrary.TEXTURES_CATEGORY_PROPERTY.equals(propertyKey)) {
        texturesComparator = new Comparator<CatalogTexture>() {
            public int compare(CatalogTexture texture1, CatalogTexture texture2) {
              String texture1Category = (String)texturesLibrary.getTextureLocalizedData(
                  texture1, controller.getTexturesLangauge(), propertyKey, texture1.getCategory().getName());
              String texture2Category = (String)texturesLibrary.getTextureLocalizedData(
                  texture2, controller.getTexturesLangauge(), propertyKey, texture2.getCategory().getName());
              return collator.compare(texture1Category, texture2Category);
            }
          };
      } else if (TexturesLibrary.TEXTURES_WIDTH_PROPERTY.equals(propertyKey)) {
        texturesComparator = new Comparator<CatalogTexture>() {
            public int compare(CatalogTexture texture1, CatalogTexture texture2) {
              return texture1.getWidth() < texture2.getWidth()  
                  ? -1
                  : (texture1.getWidth() == texture2.getWidth()
                      ? 0 : 1);
            }
          };
      } else if (TexturesLibrary.TEXTURES_HEIGHT_PROPERTY.equals(propertyKey)) {
        texturesComparator = new Comparator<CatalogTexture>() {
            public int compare(CatalogTexture texture1, CatalogTexture texture2) {
              return texture1.getHeight() < texture2.getHeight()  
                  ? -1
                  : (texture1.getHeight() == texture2.getHeight()
                      ? 0 : 1);
            }
          };
      } else {
        texturesComparator = new Comparator<CatalogTexture>() {
          @SuppressWarnings("unchecked")
          public int compare(CatalogTexture texture1, CatalogTexture texture2) {
            Object texture1Property = texturesLibrary.getTextureLocalizedData(
                texture1, controller.getTexturesLangauge(), propertyKey);
            if (texture1Property == null
                || !(texture1Property instanceof Comparable)) {
              return -1;
            } else {
              Object texture2Property = texturesLibrary.getTextureLocalizedData(
                  texture2, controller.getTexturesLangauge(), propertyKey, texture2.getName());
              return ((Comparable)texture1Property).compareTo(texture2Property);
            }
          }
        };
      }
      if (this.descendingOrder) {
        texturesComparator = Collections.reverseOrder(texturesComparator);
      }
      return texturesComparator;
    }

    public String getSortProperty() {
      return this.sortProperty;
    }

    public void setSortProperty(String sortProperty) {
      this.sortProperty = sortProperty;
      sortTextures();
    }
    
    public boolean isDescendingOrder() {
      return this.descendingOrder;
    }
    
    public void setDescendingOrder(boolean descendingOrder) {
      this.descendingOrder = descendingOrder;
      sortTextures();
    }
  }

  
  /**
   * Column table model used by textures library table.
   */
  private static class TexturesLibraryTableColumnModel extends DefaultTableColumnModel {
    public TexturesLibraryTableColumnModel(TexturesLibrary texturesLibrary, 
                                            TexturesLibraryUserPreferences preferences, 
                                            TexturesLanguageController controller) {
      createColumns(texturesLibrary, preferences, controller);
      addLanguageListener(preferences);
    }

    /**
     * Creates the list of available columns from textures sortable properties.
     */
    private void createColumns(TexturesLibrary texturesLibrary, 
                               TexturesLibraryUserPreferences preferences, 
                               TexturesLanguageController controller) {
        // Create the list of custom columns
      TableCellRenderer headerRenderer = getHeaderRenderer();
      for (String columnProperty : preferences.getEditedProperties()) {
        TableColumn tableColumn = new TableColumn();
        tableColumn.setIdentifier(columnProperty);
        tableColumn.setHeaderValue(getColumnName(columnProperty, preferences));
        tableColumn.setPreferredWidth(getColumnPreferredWidth(columnProperty));
        tableColumn.setCellRenderer(getColumnRenderer(columnProperty, texturesLibrary, preferences, controller));
        tableColumn.setHeaderRenderer(headerRenderer);
        addColumn(tableColumn);
      }
    }

    /**
     * Adds a property change listener to <code>preferences</code> to update
     * column names when preferred language changes.
     */
    private void addLanguageListener(UserPreferences preferences) {
      preferences.addPropertyChangeListener(UserPreferences.Property.LANGUAGE, 
          new LanguageChangeListener(this));
    }

    /**
     * Preferences property listener bound to this component with a weak reference to avoid
     * strong link between preferences and this component.  
     */
    private static class LanguageChangeListener implements PropertyChangeListener {
      private WeakReference<TexturesLibraryTableColumnModel> texturesTableColumnModel;

      public LanguageChangeListener(TexturesLibraryTableColumnModel texturesTable) {
        this.texturesTableColumnModel = new WeakReference<TexturesLibraryTableColumnModel>(texturesTable);
      }
      
      public void propertyChange(PropertyChangeEvent ev) {
        // If textures table column model was garbage collected, remove this listener from preferences
        TexturesLibraryTableColumnModel texturesTableColumnModel = this.texturesTableColumnModel.get();
        UserPreferences preferences = (UserPreferences)ev.getSource();
        if (texturesTableColumnModel == null) {
          preferences.removePropertyChangeListener(UserPreferences.Property.LANGUAGE, this);
        } else {          
          // Change column name and renderer from current locale
          for (int i = 0; i < texturesTableColumnModel.getColumnCount(); i++) {
            TableColumn tableColumn = texturesTableColumnModel.getColumn(i);
            Object columnIdentifier = tableColumn.getIdentifier();
            if (columnIdentifier instanceof String) {
              try {
                tableColumn.setHeaderValue(texturesTableColumnModel.getColumnName(
                    (String)columnIdentifier, preferences));
              } catch (IllegalArgumentException ex) {
                // Don't change unknown columns
              }
            }
          }
        }
      }
    }
    
    /**
     * Returns localized column names.
     */
    private String getColumnName(String propertyKey, 
                                 UserPreferences preferences) {
      if (TexturesLibrary.TEXTURES_ID_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(TexturesLibraryTable.class, "idColumn");
      } else if (TexturesLibrary.TEXTURES_NAME_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(TexturesLibraryTable.class, "nameColumn");
      } else if (TexturesLibrary.TEXTURES_CREATOR_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(TexturesLibraryTable.class, "creatorColumn");
      } else if (TexturesLibrary.TEXTURES_CATEGORY_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(TexturesLibraryTable.class, "categoryColumn");
      } else if (TexturesLibrary.TEXTURES_IMAGE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(TexturesLibraryTable.class, "imageColumn");
      } else if (TexturesLibrary.TEXTURES_WIDTH_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(TexturesLibraryTable.class, "widthColumn");
      } else if (TexturesLibrary.TEXTURES_HEIGHT_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(TexturesLibraryTable.class, "heightColumn");
      } else {
        throw new IllegalArgumentException("Unknown key " + propertyKey);
      }
    }
    
    /**
     * Returns the preferred width of a column.
     */
    private int getColumnPreferredWidth(String propertyKey) {
      if (TexturesLibrary.TEXTURES_ID_PROPERTY.equals(propertyKey)) {
        return 120;
      } else if (TexturesLibrary.TEXTURES_NAME_PROPERTY.equals(propertyKey)) {
        return OperatingSystem.isMacOSX() ? 250 : 100;
      } else if (TexturesLibrary.TEXTURES_CREATOR_PROPERTY.equals(propertyKey)) {
        return 100;
      } else if (TexturesLibrary.TEXTURES_CATEGORY_PROPERTY.equals(propertyKey)) {
        return OperatingSystem.isMacOSX() ? 150 : 70;
      } else if (TexturesLibrary.TEXTURES_IMAGE_PROPERTY.equals(propertyKey)) {
        return 50;
      } else if (TexturesLibrary.TEXTURES_WIDTH_PROPERTY.equals(propertyKey)
          || TexturesLibrary.TEXTURES_HEIGHT_PROPERTY.equals(propertyKey)) {
        return 45;
      } else {
        throw new IllegalArgumentException("Unknown key " + propertyKey);
      }
    }
    
    /**
     * Returns column renderers.
     */
    private TableCellRenderer getColumnRenderer(String propertyKey, 
                                                TexturesLibrary texturesLibrary, 
                                                UserPreferences preferences, 
                                                TexturesLanguageController controller) {
      if (TexturesLibrary.TEXTURES_ID_PROPERTY.equals(propertyKey)
          || TexturesLibrary.TEXTURES_NAME_PROPERTY.equals(propertyKey)
          || TexturesLibrary.TEXTURES_CATEGORY_PROPERTY.equals(propertyKey)
          || TexturesLibrary.TEXTURES_CREATOR_PROPERTY.equals(propertyKey)) {
        return getStringRenderer(propertyKey, texturesLibrary, controller); 
      } else if (TexturesLibrary.TEXTURES_IMAGE_PROPERTY.equals(propertyKey)) {
        return getImageRenderer(propertyKey); 
      } else if (TexturesLibrary.TEXTURES_WIDTH_PROPERTY.equals(propertyKey)
          || TexturesLibrary.TEXTURES_HEIGHT_PROPERTY.equals(propertyKey)) {
        return getSizeRenderer(propertyKey, preferences);
      } else {
        throw new IllegalArgumentException("Unknown key " + propertyKey);
      }
    }

    /**
     * Returns a renderer that displays a string property of a texture of textures. 
     */
    private TableCellRenderer getStringRenderer(final String propertyKey, 
                                                final TexturesLibrary texturesLibrary, 
                                                final TexturesLanguageController controller) {
      if (TexturesLibrary.TEXTURES_ID_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(
                  table, ((CatalogTexture)value).getId(), isSelected, hasFocus, row, column); 
            }
          };
      } else if (TexturesLibrary.TEXTURES_NAME_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              CatalogTexture texture = (CatalogTexture)value;
              String textureName = (String)texturesLibrary.getTextureLocalizedData(
                    texture, controller.getTexturesLangauge(), propertyKey, texture.getName());
              return super.getTableCellRendererComponent(
                  table, textureName, isSelected, hasFocus, row, column);
            }
          };
      } else if (TexturesLibrary.TEXTURES_CATEGORY_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              CatalogTexture texture = (CatalogTexture)value;
              String textureCategory = (String)texturesLibrary.getTextureLocalizedData(
                    texture, controller.getTexturesLangauge(), propertyKey, texture.getCategory().getName());
              return super.getTableCellRendererComponent(
                  table, textureCategory, isSelected, hasFocus, row, column); 
            }
          };
      } else if (TexturesLibrary.TEXTURES_CREATOR_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(
                  table, ((CatalogTexture)value).getCreator(), isSelected, hasFocus, row, column); 
            }
          };
      } else {
          throw new IllegalArgumentException(propertyKey + " column not a string column"); 
      }
    }

    /**
     * Returns a renderer that displays the icons of a texture of textures. 
     */
    private TableCellRenderer getImageRenderer(final String propertyKey) {
      return new DefaultTableCellRenderer() { 
        @Override
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          CatalogTexture texture = (CatalogTexture)value; 
          JLabel label = (JLabel)super.getTableCellRendererComponent(
            table, "", isSelected, hasFocus, row, column); 
          Content iconContent = texture.getImage(); 
          if (iconContent != null) {
            label.setIcon(IconManager.getInstance().getIcon(
                iconContent, table.getRowHeight(), table));
            label.setHorizontalAlignment(JLabel.CENTER);
          } else {
            label.setIcon(null);
          }
          return label;
        }
      };
    }

    /**
     * Returns a renderer that converts the displayed <code>property</code> of a texture of textures 
     * to inch in case preferences unit us equal to INCH. 
     */
    private TableCellRenderer getSizeRenderer(String propertyKey,
                                              final UserPreferences preferences) {
      // Renderer super class used to display sizes
      class SizeRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          value = preferences.getLengthUnit().getFormat().format((Float)value);
          setHorizontalAlignment(JLabel.RIGHT);
          return super.getTableCellRendererComponent(
              table, value, isSelected, hasFocus, row, column);
        }
      };
      
      if (TexturesLibrary.TEXTURES_WIDTH_PROPERTY.equals(propertyKey)) {
        return new SizeRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogTexture)value).getWidth(), isSelected, hasFocus, row, column);
            }
          };
      } else if (TexturesLibrary.TEXTURES_HEIGHT_PROPERTY.equals(propertyKey)) {
        return new SizeRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogTexture)value).getHeight(), isSelected, hasFocus, row, column);
            }
          };
      } else {
        throw new IllegalArgumentException(propertyKey + " column not a size column");
      }
    }

    /**
     * Returns column header renderer that displays an ascending or a descending icon 
     * when column is sorted, beside column name.
     */
    private TableCellRenderer getHeaderRenderer() {
      // Return a table renderer that displays the icon matching current sort
      return new TableCellRenderer() {
          private TableCellRenderer headerRenderer;        
          private ImageIcon ascendingSortIcon = new ImageIcon(getClass().getResource("resources/ascending.png"));
          private ImageIcon descendingSortIcon = new ImageIcon(getClass().getResource("resources/descending.png"));
          
          public Component getTableCellRendererComponent(JTable table, 
               Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (this.headerRenderer == null) {
              this.headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            // Get default label
            JLabel label = (JLabel)this.headerRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            // Add to column an icon matching sort
            TexturesLibraryTableModel model = (TexturesLibraryTableModel)table.getModel();
            if (getColumn(column).getIdentifier().equals(model.getSortProperty())) {
              label.setHorizontalTextPosition(JLabel.LEADING);
              if (model.isDescendingOrder()) {
                label.setIcon(descendingSortIcon);
              } else {
                label.setIcon(ascendingSortIcon);
              }
            } else {
              label.setIcon(null);
            }
            return label;
          }
        };
    }
  }

  
  /**
   * Table transfer handler.
   */
  private class TableTransferHandler extends TransferHandler {
    private final TexturesLibraryController texturesController;
    
    /**
     * Creates a handler able to receive textures files.
     */
    public TableTransferHandler(TexturesLibraryController texturesController) {
      this.texturesController = texturesController;
    }

    @Override
    public int getSourceActions(JComponent source) {
      return NONE;
    }

    /**
     * Returns <code>true</code> if flavors contains 
     * <code>DataFlavor.javaFileListFlavor</code> flavor.
     */
    @Override
    public boolean canImport(JComponent destination, DataFlavor [] flavors) {
      return this.texturesController != null
          && Arrays.asList(flavors).contains(DataFlavor.javaFileListFlavor);
    }

    /**
     * Add to library the textures contained in <code>transferable</code>.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(JComponent destination, Transferable transferable) {
      if (canImport(destination, transferable.getTransferDataFlavors())) {
        try {
          List<File> files = (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
          final List<String> importableTextures = new ArrayList<String>();        
          for (File file : files) {
            if (!file.isDirectory()) {
              String absolutePath = file.getAbsolutePath();
              importableTextures.add(absolutePath);
            }        
          }
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                texturesController.importTextures(importableTextures.toArray(new String [importableTextures.size()]));
              }
            });
          return !importableTextures.isEmpty();
        } catch (UnsupportedFlavorException ex) {
          throw new RuntimeException("Can't import", ex);
        } catch (IOException ex) {
          throw new RuntimeException("Can't access to data", ex);
        }
      } else {
        return false;
      }
    }
  }
}


