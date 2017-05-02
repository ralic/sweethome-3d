/*
 * FurnitureLibraryTable.java 18 déc. 2009
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JToolTip;
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

import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureLanguageController;
import com.eteks.furniturelibraryeditor.viewcontroller.FurnitureLibraryController;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.CatalogItemToolTip;
import com.eteks.sweethome3d.swing.IconManager;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * A table used to edit furniture library.
 * @author Emmanuel Puybaret
 */
public class FurnitureLibraryTable extends JTable implements View {
  private ListSelectionListener tableSelectionListener;
  private CatalogItemToolTip      toolTip;

  public FurnitureLibraryTable(FurnitureLibrary furnitureLibrary,
                               FurnitureLibraryUserPreferences preferences,
                               FurnitureLibraryController furnitureLibraryController,
                               FurnitureLanguageController furnitureLanguageController) {
    super(new FurnitureLibraryTableModel(furnitureLibrary, furnitureLanguageController),
        new FurnitureLibraryTableColumnModel(furnitureLibrary, preferences, furnitureLanguageController));
    this.toolTip = new CatalogItemToolTip(CatalogItemToolTip.DisplayedInformation.ICON, preferences);
    addTableHeaderListener();
    setAutoResizeMode(AUTO_RESIZE_OFF);
    updateTableColumnsWidth();
    if (furnitureLibraryController != null) {
      addSelectionListeners(furnitureLibraryController);
      addMouseListener(furnitureLibraryController);
      addFurnitureLanguageListener(furnitureLibrary, furnitureLanguageController);
      setTransferHandler(new TableTransferHandler(furnitureLibraryController));
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
    private WeakReference<FurnitureLibraryTable>  furnitureLibraryTable;

    public UserPreferencesChangeListener(FurnitureLibraryTable furnitureTable) {
      this.furnitureLibraryTable = new WeakReference<FurnitureLibraryTable>(furnitureTable);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      // If furniture table was garbage collected, remove this listener from preferences
      FurnitureLibraryTable furnitureLibraryTable = this.furnitureLibraryTable.get();
      if (furnitureLibraryTable == null) {
        ((UserPreferences)ev.getSource()).removePropertyChangeListener(
            UserPreferences.Property.valueOf(ev.getPropertyName()), this);
      } else {
        furnitureLibraryTable.repaint();
        furnitureLibraryTable.getTableHeader().repaint();
      }
    }
  }

  /**
   * Adds selection listeners to this table.
   */
  private void addSelectionListeners(final FurnitureLibraryController controller) {   
    final SelectionListener controllerSelectionListener = new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          setSelectedFurniture(controller.getSelectedFurniture());        
        }
      };
    this.tableSelectionListener = new ListSelectionListener () {
        public void valueChanged(ListSelectionEvent ev) {
          if (!ev.getValueIsAdjusting()) {
            controller.removeSelectionListener(controllerSelectionListener);
            int [] selectedRows = getSelectedRows();
            // Build the list of selected furniture
            List<CatalogPieceOfFurniture> selectedFurniture =
                new ArrayList<CatalogPieceOfFurniture>(selectedRows.length);
            TableModel tableModel = getModel();
            for (int index : selectedRows) {
              // Add to selectedFurniture table model value that stores piece
              selectedFurniture.add((CatalogPieceOfFurniture)tableModel.getValueAt(index, 0));
            }
            // Set the new selection in controller
            controller.setSelectedFurniture(selectedFurniture);
            controller.addSelectionListener(controllerSelectionListener);
          }
        }
      };
    getSelectionModel().addListSelectionListener(this.tableSelectionListener);
    controller.addSelectionListener(controllerSelectionListener);
  }

  /**
   * Adds a double click mouse listener to modify selected furniture.
   */
  private void addMouseListener(final FurnitureLibraryController controller) {
    addMouseListener(new MouseAdapter () {
        @Override
        public void mouseClicked(MouseEvent ev) {
          if (ev.getClickCount() == 2) {
            controller.modifySelectedFurniture();
          }
        }
      });
  }

  /**
   * Adds a mouse listener on table header that will sort furniture.
   */
  private void addTableHeaderListener() {
    // Sort on click in column header 
    getTableHeader().addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          FurnitureLibraryTableModel tableModel = (FurnitureLibraryTableModel)getModel();
          List<CatalogPieceOfFurniture> selectedFurniture = new ArrayList<CatalogPieceOfFurniture>();
          for (int index : getSelectedRows()) {
            selectedFurniture.add((CatalogPieceOfFurniture)tableModel.getValueAt(index, 0));
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
            } else if (tableModel.getFurnitureComparator(propertyKey) != null) {
              tableModel.setSortProperty(propertyKey);
              tableModel.setDescendingOrder(false);
            }
          }
          getTableHeader().repaint();
          setSelectedFurniture(selectedFurniture);
        }
      });
  }

  /**
   * Selects furniture in table. 
   */
  private void setSelectedFurniture(List<CatalogPieceOfFurniture> selectedFurniture) {
    getSelectionModel().removeListSelectionListener(this.tableSelectionListener);
    clearSelection();
    FurnitureLibraryTableModel tableModel = (FurnitureLibraryTableModel)getModel();
    int minIndex = Integer.MAX_VALUE;
    int maxIndex = Integer.MIN_VALUE;
    for (CatalogPieceOfFurniture piece : selectedFurniture) {
      if (piece instanceof CatalogPieceOfFurniture) {
        // Search index of piece in sorting table model
        int index = tableModel.getPieceOfFurnitureIndex((CatalogPieceOfFurniture)piece);
        // If the piece was found (during the addition of a piece to library, the model may not be updated yet) 
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
   * Adds listeners on furniture language change to resort furniture.
   */
  private void addFurnitureLanguageListener(FurnitureLibrary furnitureLibrary,
                                            final FurnitureLanguageController controller) {
    PropertyChangeListener listener = new PropertyChangeListener() {
        private boolean sorting = false;
        
        public void propertyChange(PropertyChangeEvent ev) {
          if (!sorting) {
            // Postpone update in case of multiple localized data is set
            sorting = true;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  FurnitureLibraryTableModel tableModel = (FurnitureLibraryTableModel)getModel();
                  List<CatalogPieceOfFurniture> selectedFurniture = new ArrayList<CatalogPieceOfFurniture>();
                  for (int index : getSelectedRows()) {
                    selectedFurniture.add((CatalogPieceOfFurniture)tableModel.getValueAt(index, 0));
                  }
                  tableModel.sortFurniture();
                  setSelectedFurniture(selectedFurniture);
                  sorting = false;
                }
              });
          }
        }
      };
    controller.addPropertyChangeListener(FurnitureLanguageController.Property.FURNITURE_LANGUAGE, listener);
    furnitureLibrary.addPropertyChangeListener(FurnitureLibrary.Property.LOCALIZED_DATA, listener);
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
   * Returns the tool tip displayed by this tree.
   */
  @Override
  public JToolTip createToolTip() {    
    if (this.toolTip.isTipTextComplete()) {
      // Use toolTip object only for its text returned in getToolTipText
      return super.createToolTip();
    } else {
      this.toolTip.setComponent(this);
      return this.toolTip;
    }
  }

  /**
   * Returns a tooltip for furniture pieces described in this tree.
   */
  @Override
  public String getToolTipText(MouseEvent ev) {
    int column = columnAtPoint(ev.getPoint());
    if (column != -1 
        && FurnitureLibrary.FURNITURE_ICON_PROPERTY.equals(getColumnModel().getColumn(column).getIdentifier())) {
      int row = rowAtPoint(ev.getPoint());
      if (row != -1) {
        this.toolTip.setCatalogItem((CatalogPieceOfFurniture)getModel().getValueAt(row, 0));
        return this.toolTip.getTipText();
      }
    }
    return null;
  }
  
  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return new Dimension(getPreferredSize().width, 400);
  }
  
  
  /**
   * Model used by furniture table.
   */
  private static class FurnitureLibraryTableModel extends AbstractTableModel {
    private final FurnitureLibrary              furnitureLibrary;
    private final FurnitureLanguageController   controller; 
    private List<CatalogPieceOfFurniture>       sortedFurniture;
    private String                              sortProperty;
    private boolean                             descendingOrder;
    
    public FurnitureLibraryTableModel(FurnitureLibrary furnitureLibrary,
                                      FurnitureLanguageController controller) {
      this.furnitureLibrary = furnitureLibrary;
      this.controller = controller;
      addFurnitureLibraryListener(furnitureLibrary);
      sortFurniture();
    }

    private void addFurnitureLibraryListener(final FurnitureLibrary furnitureLibrary) {
      furnitureLibrary.addListener(new CollectionListener<CatalogPieceOfFurniture>() {
        public void collectionChanged(CollectionEvent<CatalogPieceOfFurniture> ev) {
            CatalogPieceOfFurniture piece = ev.getItem();
            int pieceIndex = ev.getIndex();
            switch (ev.getType()) {
              case ADD :
                int insertionIndex = getPieceOfFurnitureInsertionIndex(piece, furnitureLibrary, pieceIndex);
                if (insertionIndex != -1) {
                  sortedFurniture.add(insertionIndex, piece);
                  fireTableRowsInserted(insertionIndex, insertionIndex);
                }
                break;
              case DELETE :
                int deletionIndex = getPieceOfFurnitureDeletionIndex(piece, furnitureLibrary, pieceIndex);
                if (deletionIndex != -1) {
                  sortedFurniture.remove(deletionIndex);
                  fireTableRowsDeleted(deletionIndex, deletionIndex);
                }
                break;
            }
          }
  
          /**
           * Returns the index of an added <code>piece</code> in furniture table, with a default index
           * of <code>pieceIndex</code> if furniture library isn't sorting.
           * If <code>piece</code> isn't added to furniture table, the returned value is
           * equals to the insertion index where piece should be added.
           */
          private int getPieceOfFurnitureInsertionIndex(CatalogPieceOfFurniture piece, 
                                                        FurnitureLibrary furnitureLibrary, 
                                                        int pieceIndex) {
            if (sortProperty == null) {
              return pieceIndex;
            } 
            // Default case when piece is included and furniture is  sorting 
            int sortedIndex = Collections.binarySearch(sortedFurniture, piece, getFurnitureComparator(sortProperty));
            if (sortedIndex >= 0) {
              return sortedIndex;
            } else {
              return -(sortedIndex + 1);
            }              
          }
  
          /**
           * Returns the index of an existing <code>piece</code> in furniture table, with a default index
           * of <code>pieceIndex</code> if furniture isn't sorting.
           */
          private int getPieceOfFurnitureDeletionIndex(CatalogPieceOfFurniture piece, 
                                                       FurnitureLibrary furnitureLibrary, 
                                                       int pieceIndex) {
            if (sortProperty == null) {
              return pieceIndex;
            } 
            return getPieceOfFurnitureIndex(piece);              
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
      return this.sortedFurniture.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      // Always return piece itself, the real property displayed at screen is chosen by renderer
      return this.sortedFurniture.get(rowIndex);
    }

    /**
     * Returns the index of <code>piece</code> in furniture table, or -1. 
     */
    public int getPieceOfFurnitureIndex(CatalogPieceOfFurniture searchedPiece) {
      for (int i = 0, n = this.sortedFurniture.size(); i < n; i++) {
        CatalogPieceOfFurniture piece = this.sortedFurniture.get(i);
        if (searchedPiece == piece) {
          return i;
        }
      }
      return -1;
    }

    /**
     * Sorts furniture.
     */
    public void sortFurniture() {
      int previousRowCount = this.sortedFurniture != null 
          ? this.sortedFurniture.size()
          : 0;
      List<CatalogPieceOfFurniture> libraryFurniture = this.furnitureLibrary.getFurniture();
      this.sortedFurniture = new ArrayList<CatalogPieceOfFurniture>(libraryFurniture);
      // Sort it if necessary
      if (this.sortProperty != null) {
        Comparator<CatalogPieceOfFurniture> furnitureComparator = getFurnitureComparator(this.sortProperty);
        Collections.sort(this.sortedFurniture, furnitureComparator);         
      }
      
      if (previousRowCount != this.sortedFurniture.size()) {
        fireTableDataChanged();
      } else {
        fireTableRowsUpdated(0, getRowCount() - 1);
      }
    }

    public Comparator<CatalogPieceOfFurniture> getFurnitureComparator(final String propertyKey) {
      final Collator collator = Collator.getInstance();
      Comparator<CatalogPieceOfFurniture> furnitureComparator = null;
      if (FurnitureLibrary.FURNITURE_ID_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              if (piece1.getId() == null) {
                return -1;
              } else if (piece2.getId() == null) {
                return 1; 
              } else {
                return collator.compare(piece1.getId(), piece2.getId());
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_NAME_PROPERTY.equals(propertyKey)) {
         furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
             public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
               String piece1Name = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                   piece1, controller.getFurnitureLangauge(), propertyKey, piece1.getName());
               String piece2Name = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                   piece2, controller.getFurnitureLangauge(), propertyKey, piece2.getName());
               return collator.compare(piece1Name, piece2Name);
             }
           };
      } else if (FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              String piece1Description = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                  piece1, controller.getFurnitureLangauge(), propertyKey, piece1.getDescription());
              if (piece1Description == null) {
                return -1;
              } else {
                String piece2Description = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                    piece2, controller.getFurnitureLangauge(), propertyKey, piece2.getDescription());
                if (piece2Description == null) {
                  return 1; 
                } else {
                  return collator.compare(piece1Description, piece2Description);
                }
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_CREATOR_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return collator.compare(piece1.getCreator(), piece2.getCreator());
            }
          };
      } else if (FurnitureLibrary.FURNITURE_TAGS_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              String [] piece1Tags = (String [])furnitureLibrary.getPieceOfFurnitureLocalizedData(
                  piece1, controller.getFurnitureLangauge(), propertyKey, piece1.getTags());
              if (piece1Tags == null) {
                return -1;
              } else {
                String [] piece2Tags = (String [])furnitureLibrary.getPieceOfFurnitureLocalizedData(
                    piece2, controller.getFurnitureLangauge(), propertyKey, piece2.getTags());
                if (piece2Tags == null) {
                  return 1; 
                } else {
                  return collator.compare(Arrays.toString(piece1Tags), Arrays.toString(piece2Tags));
                }
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              String piece1Information = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                  piece1, controller.getFurnitureLangauge(), propertyKey, piece1.getInformation());
              if (piece1Information == null) {
                return -1;
              } else {
                String piece2Information = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                    piece2, controller.getFurnitureLangauge(), propertyKey, piece2.getInformation());
                if (piece2Information == null) {
                  return 1; 
                } else {
                  return collator.compare(piece1Information, piece2Information);
                }
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_CREATION_DATE_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              if (piece1.getCreationDate() == null) {
                return -1;
              } else if (piece2.getCreationDate() == null) {
                return 1; 
              } else {
                return piece1.getCreationDate().compareTo(piece2.getCreationDate());
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_GRADE_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              if (piece1.getGrade() == null) {
                return -1;
              } else if (piece2.getGrade() == null) {
                return 1; 
              } else {
                return piece1.getGrade().compareTo(piece2.getGrade());
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              String piece1Category = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                  piece1, controller.getFurnitureLangauge(), propertyKey, piece1.getCategory().getName());
              String piece2Category = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                  piece2, controller.getFurnitureLangauge(), propertyKey, piece2.getCategory().getName());
              return collator.compare(piece1Category, piece2Category);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_PRICE_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              if (piece1.getPrice() == null) {
                return -1;
              } else if (piece2.getPrice() == null) {
                return 1; 
              } else {
                return piece1.getPrice().compareTo(piece2.getPrice());
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_VALUE_ADDED_TAX_PERCENTAGE_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              if (piece1.getValueAddedTaxPercentage() == null) {
                return -1;
              } else if (piece2.getValueAddedTaxPercentage() == null) {
                return 1; 
              } else {
                return piece1.getValueAddedTaxPercentage().compareTo(piece2.getValueAddedTaxPercentage());
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_WIDTH_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.getWidth() < piece2.getWidth()  
                  ? -1
                  : (piece1.getWidth() == piece2.getWidth()
                      ? 0 : 1);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_DEPTH_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.getDepth() < piece2.getDepth()  
                  ? -1
                  : (piece1.getDepth() == piece2.getDepth()
                      ? 0 : 1);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_HEIGHT_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.getHeight() < piece2.getHeight()  
                  ? -1
                  : (piece1.getHeight() == piece2.getHeight()
                      ? 0 : 1);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_MOVABLE_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.isMovable() == piece2.isMovable()  
                  ? 0
                  : (piece1.isMovable()
                      ? -1 : 1);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.isDoorOrWindow() == piece2.isDoorOrWindow()  
                  ? 0
                  : (piece1.isDoorOrWindow()
                      ? -1 : 1);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_STAIRCASE_CUT_OUT_SHAPE_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              if (piece1.getStaircaseCutOutShape() == null) {
                return -1;
              } else if (piece2.getStaircaseCutOutShape() == null) {
                return 1; 
              } else {
                return piece1.getStaircaseCutOutShape().compareTo(piece2.getStaircaseCutOutShape());
              }
            }
          };
      } else if (FurnitureLibrary.FURNITURE_ELEVATION_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.getElevation() < piece2.getElevation()  
                  ? -1
                  : (piece1.getElevation() == piece2.getElevation()
                      ? 0 : 1);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_RESIZABLE_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.isResizable() == piece2.isResizable()  
                  ? 0
                  : (piece1.isResizable()
                      ? -1 : 1);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_DEFORMABLE_PROPERTY.equals(propertyKey)) {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.isDeformable() == piece2.isDeformable()  
                  ? 0
                  : (piece1.isDeformable()
                      ? -1 : 1);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_TEXTURABLE_PROPERTY.equals(propertyKey)) {
          furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
            public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
              return piece1.isTexturable() == piece2.isTexturable()  
                  ? 0
                  : (piece1.isTexturable()
                      ? -1 : 1);
            }
          };
      } else {
        furnitureComparator = new Comparator<CatalogPieceOfFurniture>() {
          @SuppressWarnings("unchecked")
          public int compare(CatalogPieceOfFurniture piece1, CatalogPieceOfFurniture piece2) {
            Object piece1Property = furnitureLibrary.getPieceOfFurnitureLocalizedData(
                piece1, controller.getFurnitureLangauge(), propertyKey);
            if (piece1Property == null
                || !(piece1Property instanceof Comparable)) {
              return -1;
            } else {
              Object piece2Property = furnitureLibrary.getPieceOfFurnitureLocalizedData(
                  piece2, controller.getFurnitureLangauge(), propertyKey, piece2.getDescription());
              return ((Comparable)piece1Property).compareTo(piece2Property);
            }
          }
        };
      }
      if (this.descendingOrder) {
        furnitureComparator = Collections.reverseOrder(furnitureComparator);
      }
      return furnitureComparator;
    }

    public String getSortProperty() {
      return this.sortProperty;
    }

    public void setSortProperty(String sortProperty) {
      this.sortProperty = sortProperty;
      sortFurniture();
    }
    
    public boolean isDescendingOrder() {
      return this.descendingOrder;
    }
    
    public void setDescendingOrder(boolean descendingOrder) {
      this.descendingOrder = descendingOrder;
      sortFurniture();
    }
  }

  
  /**
   * Column table model used by furniture library table.
   */
  private static class FurnitureLibraryTableColumnModel extends DefaultTableColumnModel {
    public FurnitureLibraryTableColumnModel(FurnitureLibrary furnitureLibrary, 
                                            FurnitureLibraryUserPreferences preferences, 
                                            FurnitureLanguageController controller) {
      createColumns(furnitureLibrary, preferences, controller);
      addLanguageListener(preferences);
    }

    /**
     * Creates the list of available columns from furniture sortable properties.
     */
    private void createColumns(FurnitureLibrary furnitureLibrary, 
                               FurnitureLibraryUserPreferences preferences, 
                               FurnitureLanguageController controller) {
      // Create the list of custom columns
      TableCellRenderer headerRenderer = getHeaderRenderer();
      for (String columnProperty : preferences.getEditedProperties()) {
        if (!FurnitureLibrary.FURNITURE_MODEL_ROTATION_PROPERTY.equals(columnProperty) 
            && !FurnitureLibrary.FURNITURE_MODEL_PROPERTY.equals(columnProperty) 
            && !FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_CUT_OUT_SHAPE_PROPERTY.equals(columnProperty)) {
          TableColumn tableColumn = new TableColumn();
          tableColumn.setIdentifier(columnProperty);
          tableColumn.setHeaderValue(getColumnName(columnProperty, preferences));
          tableColumn.setPreferredWidth(getColumnPreferredWidth(columnProperty));
          tableColumn.setCellRenderer(getColumnRenderer(columnProperty, furnitureLibrary, preferences, controller));
          tableColumn.setHeaderRenderer(headerRenderer);
          addColumn(tableColumn);
        }
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
      private WeakReference<FurnitureLibraryTableColumnModel> furnitureTableColumnModel;

      public LanguageChangeListener(FurnitureLibraryTableColumnModel furnitureTable) {
        this.furnitureTableColumnModel = new WeakReference<FurnitureLibraryTableColumnModel>(furnitureTable);
      }
      
      public void propertyChange(PropertyChangeEvent ev) {
        // If furniture table column model was garbage collected, remove this listener from preferences
        FurnitureLibraryTableColumnModel furnitureTableColumnModel = this.furnitureTableColumnModel.get();
        UserPreferences preferences = (UserPreferences)ev.getSource();
        if (furnitureTableColumnModel == null) {
          preferences.removePropertyChangeListener(UserPreferences.Property.LANGUAGE, this);
        } else {          
          // Change column name and renderer from current locale
          for (int i = 0; i < furnitureTableColumnModel.getColumnCount(); i++) {
            TableColumn tableColumn = furnitureTableColumnModel.getColumn(i);
            Object columnIdentifier = tableColumn.getIdentifier();
            if (columnIdentifier instanceof String) {
              try {
                tableColumn.setHeaderValue(furnitureTableColumnModel.getColumnName(
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
      if (FurnitureLibrary.FURNITURE_ID_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "idColumn");
      } else if (FurnitureLibrary.FURNITURE_NAME_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "nameColumn");
      } else if (FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "descriptionColumn");
      } else if (FurnitureLibrary.FURNITURE_CREATOR_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "creatorColumn");
      } else if (FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "informationColumn");
      } else if (FurnitureLibrary.FURNITURE_TAGS_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "tagsColumn");
      } else if (FurnitureLibrary.FURNITURE_CREATION_DATE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "creationDateColumn");
      } else if (FurnitureLibrary.FURNITURE_GRADE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "gradeColumn");
      } else if (FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "categoryColumn");
      } else if (FurnitureLibrary.FURNITURE_PRICE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "priceColumn");
      } else if (FurnitureLibrary.FURNITURE_VALUE_ADDED_TAX_PERCENTAGE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "valueAddedTaxPercentageColumn");
      } else if (FurnitureLibrary.FURNITURE_ICON_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "iconColumn");
      } else if (FurnitureLibrary.FURNITURE_PLAN_ICON_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "planIconColumn");
      } else if (FurnitureLibrary.FURNITURE_MODEL_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "modelColumn");
      } else if (FurnitureLibrary.FURNITURE_WIDTH_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "widthColumn");
      } else if (FurnitureLibrary.FURNITURE_DEPTH_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "depthColumn");
      } else if (FurnitureLibrary.FURNITURE_HEIGHT_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "heightColumn");
      } else if (FurnitureLibrary.FURNITURE_MOVABLE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "movableColumn");
      } else if (FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "doorOrWindowColumn");
      } else if (FurnitureLibrary.FURNITURE_STAIRCASE_CUT_OUT_SHAPE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "staircaseColumn");
      } else if (FurnitureLibrary.FURNITURE_ELEVATION_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "elevationColumn");
      } else if (FurnitureLibrary.FURNITURE_MODEL_ROTATION_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "modelRotationColumn");
      } else if (FurnitureLibrary.FURNITURE_RESIZABLE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "resizableColumn");
      } else if (FurnitureLibrary.FURNITURE_DEFORMABLE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "deformableColumn");
      } else if (FurnitureLibrary.FURNITURE_TEXTURABLE_PROPERTY.equals(propertyKey)) {
        return preferences.getLocalizedString(FurnitureLibraryTable.class, "texturableColumn");
      } else {
        throw new IllegalArgumentException("Unknown key " + propertyKey);
      }
    }
    
    /**
     * Returns the preferred width of a column.
     */
    private int getColumnPreferredWidth(String propertyKey) {
      if (FurnitureLibrary.FURNITURE_ID_PROPERTY.equals(propertyKey)) {
        return 120;
      } else if (FurnitureLibrary.FURNITURE_NAME_PROPERTY.equals(propertyKey)) {
        return 100;
      } else if (FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_TAGS_PROPERTY.equals(propertyKey)) {
        return 150;
      } else if (FurnitureLibrary.FURNITURE_CREATION_DATE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_GRADE_PROPERTY.equals(propertyKey)) {
        return 50;
      } else if (FurnitureLibrary.FURNITURE_CREATOR_PROPERTY.equals(propertyKey)) {
        return 100;
      } else if (FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY.equals(propertyKey)) {
        return 70;
      } else if (FurnitureLibrary.FURNITURE_PRICE_PROPERTY.equals(propertyKey)) {
        return 70;
      } else if (FurnitureLibrary.FURNITURE_VALUE_ADDED_TAX_PERCENTAGE_PROPERTY.equals(propertyKey)) {
        return 50;
      } else if (FurnitureLibrary.FURNITURE_ICON_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_PLAN_ICON_PROPERTY.equals(propertyKey)) {
        return 50;
      } else if (FurnitureLibrary.FURNITURE_MODEL_PROPERTY.equals(propertyKey)) {
        return 70;
      } else if (FurnitureLibrary.FURNITURE_WIDTH_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_DEPTH_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_HEIGHT_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_ELEVATION_PROPERTY.equals(propertyKey)) {
        return 45;
      } else if (FurnitureLibrary.FURNITURE_MOVABLE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_STAIRCASE_CUT_OUT_SHAPE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_RESIZABLE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_DEFORMABLE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_TEXTURABLE_PROPERTY.equals(propertyKey)) {
        return 20;
      } else if (FurnitureLibrary.FURNITURE_MODEL_ROTATION_PROPERTY.equals(propertyKey)) {
        return 70;
      } else {
        throw new IllegalArgumentException("Unknown key " + propertyKey);
      }
    }
    
    /**
     * Returns column renderers.
     */
    private TableCellRenderer getColumnRenderer(String propertyKey, 
                                                FurnitureLibrary furnitureLibrary, 
                                                UserPreferences preferences, 
                                                FurnitureLanguageController controller) {
      if (FurnitureLibrary.FURNITURE_ID_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_NAME_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_TAGS_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_CREATOR_PROPERTY.equals(propertyKey)) {
        return getStringRenderer(propertyKey, furnitureLibrary, controller); 
      } else if (FurnitureLibrary.FURNITURE_ICON_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_PLAN_ICON_PROPERTY.equals(propertyKey)) {
        return getIconRenderer(propertyKey); 
      } else if (FurnitureLibrary.FURNITURE_CREATION_DATE_PROPERTY.equals(propertyKey)) {
        return getCreationDateRenderer();
      } else if (FurnitureLibrary.FURNITURE_GRADE_PROPERTY.equals(propertyKey)) {
        return getGradeRenderer();
      } else if (FurnitureLibrary.FURNITURE_MODEL_PROPERTY.equals(propertyKey)) {
        return getButtonRenderer(propertyKey, preferences);
      } else if (FurnitureLibrary.FURNITURE_PRICE_PROPERTY.equals(propertyKey)) {
        return getPriceRenderer(preferences);          
      } else if (FurnitureLibrary.FURNITURE_VALUE_ADDED_TAX_PERCENTAGE_PROPERTY.equals(propertyKey)) {
        return getValueAddedTaxPercentageRenderer();          
      } else if (FurnitureLibrary.FURNITURE_WIDTH_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_DEPTH_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_HEIGHT_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_ELEVATION_PROPERTY.equals(propertyKey)) {
        return getSizeRenderer(propertyKey, preferences);
      } else if (FurnitureLibrary.FURNITURE_MOVABLE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_STAIRCASE_CUT_OUT_SHAPE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_RESIZABLE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_DEFORMABLE_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_TEXTURABLE_PROPERTY.equals(propertyKey)) {
        return getBooleanRenderer(propertyKey);
      } else if (FurnitureLibrary.FURNITURE_MODEL_ROTATION_PROPERTY.equals(propertyKey)) {
        return getButtonRenderer(propertyKey, preferences);
      } else {
        throw new IllegalArgumentException("Unknown key " + propertyKey);
      }
    }

    /**
     * Returns a renderer that displays a string property of a piece of furniture. 
     */
    private TableCellRenderer getStringRenderer(final String propertyKey, 
                                                final FurnitureLibrary furnitureLibrary, 
                                                final FurnitureLanguageController controller) {
      if (FurnitureLibrary.FURNITURE_ID_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(
                  table, ((CatalogPieceOfFurniture)value).getId(), isSelected, hasFocus, row, column); 
            }
          };
      } else if (FurnitureLibrary.FURNITURE_NAME_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              CatalogPieceOfFurniture piece = (CatalogPieceOfFurniture)value;
              String pieceName = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                    piece, controller.getFurnitureLangauge(), propertyKey, piece.getName());
              return super.getTableCellRendererComponent(
                  table, pieceName, isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_DESCRIPTION_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              CatalogPieceOfFurniture piece = (CatalogPieceOfFurniture)value;
              String pieceDescription = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                    piece, controller.getFurnitureLangauge(), propertyKey, piece.getDescription());
              return super.getTableCellRendererComponent(
                  table, pieceDescription, isSelected, hasFocus, row, column); 
            }
          };
      } else if (FurnitureLibrary.FURNITURE_INFORMATION_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              CatalogPieceOfFurniture piece = (CatalogPieceOfFurniture)value;
              String pieceInformation = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                    piece, controller.getFurnitureLangauge(), propertyKey, piece.getInformation());
              return super.getTableCellRendererComponent(
                  table, pieceInformation, isSelected, hasFocus, row, column); 
            }
          };
      } else if (FurnitureLibrary.FURNITURE_TAGS_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              CatalogPieceOfFurniture piece = (CatalogPieceOfFurniture)value;
              String [] pieceTags = (String [])furnitureLibrary.getPieceOfFurnitureLocalizedData(
                    piece, controller.getFurnitureLangauge(), propertyKey, piece.getTags());
              String tagsText = Arrays.toString(pieceTags);
              return super.getTableCellRendererComponent(
                  table, tagsText.substring(1, tagsText.length() - 1), isSelected, hasFocus, row, column); 
            }
          };
      } else if (FurnitureLibrary.FURNITURE_CATEGORY_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              CatalogPieceOfFurniture piece = (CatalogPieceOfFurniture)value;
              String pieceCategory = (String)furnitureLibrary.getPieceOfFurnitureLocalizedData(
                    piece, controller.getFurnitureLangauge(), propertyKey, piece.getCategory().getName());
              return super.getTableCellRendererComponent(
                  table, pieceCategory, isSelected, hasFocus, row, column); 
            }
          };
      } else if (FurnitureLibrary.FURNITURE_CREATOR_PROPERTY.equals(propertyKey)) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(
                  table, ((CatalogPieceOfFurniture)value).getCreator(), isSelected, hasFocus, row, column); 
            }
          };
      } else {
          throw new IllegalArgumentException(propertyKey + " column not a string column"); 
      }
    }

    /**
     * Returns a renderer that displays a button.
     */
    private TableCellRenderer getButtonRenderer(String propertyKey,
                                                final UserPreferences preferences) {
      if (FurnitureLibrary.FURNITURE_MODEL_PROPERTY.equals(propertyKey)
          || FurnitureLibrary.FURNITURE_MODEL_ROTATION_PROPERTY.equals(propertyKey)) {
        return new TableCellRenderer() {
          private JButton component = new JButton();
          public Component getTableCellRendererComponent(JTable table, Object value,
              boolean isSelected, boolean hasFocus, int row, int column) {
            component.setText(preferences.getLocalizedString(FurnitureLibraryTable.class, "modifyButton"));
            return component;
          }
        };
      } else {
        throw new IllegalArgumentException(propertyKey + " column not a button column");
      }
    }

    /**
     * Returns a renderer that displays the icons of a piece of furniture. 
     */
    private TableCellRenderer getIconRenderer(final String propertyKey) {
      return new DefaultTableCellRenderer() { 
        @Override
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          CatalogPieceOfFurniture piece = (CatalogPieceOfFurniture)value; 
          JLabel label = (JLabel)super.getTableCellRendererComponent(
            table, "", isSelected, hasFocus, row, column); 
          Content iconContent;
          if (FurnitureLibrary.FURNITURE_ICON_PROPERTY.equals(propertyKey)) {
            iconContent = piece.getIcon(); 
          } else {
            iconContent = piece.getPlanIcon(); 
          }
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
     * Returns a renderer that displays the creation date of a piece of furniture. 
     */
    private TableCellRenderer getCreationDateRenderer() {
      return new DefaultTableCellRenderer() {
          public Component getTableCellRendererComponent(JTable table, 
               Object value, boolean isSelected, boolean hasFocus, 
               int row, int column) {
            value = ((CatalogPieceOfFurniture)value).getCreationDate();
            if (value != null) {
              value = DateFormat.getDateInstance(DateFormat.SHORT).format(value);
            } else {
              value = "";
            }
            setHorizontalAlignment(JLabel.RIGHT);
            return super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
          }
        };
    }

    /**
     * Returns a renderer that displays the grade of a piece of furniture. 
     */
    private TableCellRenderer getGradeRenderer() {
      return new DefaultTableCellRenderer() {
          public Component getTableCellRendererComponent(JTable table, 
               Object value, boolean isSelected, boolean hasFocus, 
               int row, int column) {
            value = ((CatalogPieceOfFurniture)value).getGrade();
            if (value != null) {
              value = DecimalFormat.getPercentInstance().format(value);
            } else {
              value = "";
            }
            setHorizontalAlignment(JLabel.RIGHT);
            return super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
          }
        };
    }

    /**
     * Returns a renderer that converts the displayed <code>property</code> of a piece of furniture 
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
      
      if (FurnitureLibrary.FURNITURE_WIDTH_PROPERTY.equals(propertyKey)) {
        return new SizeRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).getWidth(), isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_DEPTH_PROPERTY.equals(propertyKey)) {
        return new SizeRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).getDepth(), isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_HEIGHT_PROPERTY.equals(propertyKey)) {
        return new SizeRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).getHeight(), isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_ELEVATION_PROPERTY.equals(propertyKey)) {
        return new SizeRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).getElevation(), isSelected, hasFocus, row, column);
            }
          };
      } else {
        throw new IllegalArgumentException(propertyKey + " column not a size column");
      }
    }

    /**
     * Returns a renderer that displays the price of a piece of furniture. 
     */
    private TableCellRenderer getPriceRenderer(final UserPreferences preferences) {
      return new DefaultTableCellRenderer() {
          public Component getTableCellRendererComponent(JTable table, 
               Object value, boolean isSelected, boolean hasFocus, 
               int row, int column) {
            value = ((CatalogPieceOfFurniture)value).getPrice();
            if (value != null) {
              String currency = preferences.getCurrency();
              NumberFormat currencyFormat;
              if (currency != null) {
                currencyFormat = DecimalFormat.getCurrencyInstance();
                currencyFormat.setCurrency(Currency.getInstance(currency));
              } else {
                currencyFormat = new DecimalFormat("##0.00");
              }
              value = currencyFormat.format(value);
            } else {
              value = "";
            }
            setHorizontalAlignment(JLabel.RIGHT);
            return super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
          }
        };
    }

    /**
     * Returns a renderer that displays the value added tax percentage property of a piece of furniture. 
     */
    private TableCellRenderer getValueAddedTaxPercentageRenderer() {
      return new DefaultTableCellRenderer() { 
          @Override
          public Component getTableCellRendererComponent(JTable table, 
               Object value, boolean isSelected, boolean hasFocus, 
               int row, int column) {
            BigDecimal valueAddedTaxPercentage = ((CatalogPieceOfFurniture)value).getValueAddedTaxPercentage();
            if (valueAddedTaxPercentage != null) {
              NumberFormat percentInstance = DecimalFormat.getPercentInstance();
              percentInstance.setMinimumFractionDigits(valueAddedTaxPercentage.scale() - 2);
              value = percentInstance.format(valueAddedTaxPercentage);
            } else {
              value = "";
            }
            setHorizontalAlignment(JLabel.RIGHT);
            return super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
          }
        };
    }

    /**
     * Returns a renderer that displays a property of a piece of furniture 
     * with <code>JTable</code> default boolean renderer. 
     */
    private TableCellRenderer getBooleanRenderer(String propertyKey) {
      // Renderer super class used to display booleans
      class BooleanRenderer implements TableCellRenderer {
        private TableCellRenderer booleanRenderer;

        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, int row, int column) {
          if (this.booleanRenderer == null) {
            this.booleanRenderer = table.getDefaultRenderer(Boolean.class);
          }
          return this.booleanRenderer.getTableCellRendererComponent(
              table, value, isSelected, hasFocus, row, column);
        }
      };
      
      if (FurnitureLibrary.FURNITURE_MOVABLE_PROPERTY.equals(propertyKey)) {
        return new BooleanRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).isMovable(), isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_DOOR_OR_WINDOW_PROPERTY.equals(propertyKey)) {
        return new BooleanRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).isDoorOrWindow(), isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_STAIRCASE_CUT_OUT_SHAPE_PROPERTY.equals(propertyKey)) {
        return new BooleanRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).getStaircaseCutOutShape() != null, isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_RESIZABLE_PROPERTY.equals(propertyKey)) {
        return new BooleanRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).isResizable(), isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_DEFORMABLE_PROPERTY.equals(propertyKey)) {
        return new BooleanRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).isDeformable(), isSelected, hasFocus, row, column);
            }
          };
      } else if (FurnitureLibrary.FURNITURE_TEXTURABLE_PROPERTY.equals(propertyKey)) {
        return new BooleanRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, int row, int column) {
              return super.getTableCellRendererComponent(table, 
                  ((CatalogPieceOfFurniture)value).isTexturable(), isSelected, hasFocus, row, column);
            }
          };
      } else {
        throw new IllegalArgumentException(propertyKey + " column not a boolean column");
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
            FurnitureLibraryTableModel model = (FurnitureLibraryTableModel)table.getModel();
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
    private final FurnitureLibraryController furnitureController;
    
    /**
     * Creates a handler able to receive furniture files.
     */
    public TableTransferHandler(FurnitureLibraryController furnitureController) {
      this.furnitureController = furnitureController;
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
      return this.furnitureController != null
          && Arrays.asList(flavors).contains(DataFlavor.javaFileListFlavor);
    }

    /**
     * Add to library the furniture contained in <code>transferable</code>.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(JComponent destination, Transferable transferable) {
      if (canImport(destination, transferable.getTransferDataFlavors())) {
        try {
          List<File> files = (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
          final List<String> importableModels = new ArrayList<String>();        
          for (File file : files) {
            if (!file.isDirectory()) {
              String absolutePath = file.getAbsolutePath();
              importableModels.add(absolutePath);
            }        
          }
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                furnitureController.importFurniture(importableModels.toArray(new String [importableModels.size()]));
              }
            });
          return !importableModels.isEmpty();
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


