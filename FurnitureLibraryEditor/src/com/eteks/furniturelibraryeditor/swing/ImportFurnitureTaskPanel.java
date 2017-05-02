/*
 * ImportFurnitureTaskPanel.java 13 janv. 2010
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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.media.j3d.BranchGroup;
import javax.vecmath.Vector3f;

import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.viewcontroller.ImportFurnitureTaskView;
import com.eteks.sweethome3d.j3d.ModelManager;
import com.eteks.sweethome3d.j3d.OBJWriter;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.FurnitureCatalog;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.swing.ModelPreviewComponent;
import com.eteks.sweethome3d.swing.ThreadedTaskPanel;
import com.eteks.sweethome3d.tools.TemporaryURLContent;
import com.eteks.sweethome3d.tools.URLContent;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;

/**
 * A threaded task panel used for furniture importation. 
 * @author Emmanuel Puybaret
 */
public class ImportFurnitureTaskPanel extends ThreadedTaskPanel implements ImportFurnitureTaskView {
  private static final int PREVIEW_PREFERRED_SIZE = 128;
  
  private final FurnitureLibraryUserPreferences preferences;
  private ModelPreviewComponent       iconPreviewComponent;
  private boolean                     firstRendering = true;

  public ImportFurnitureTaskPanel(String taskMessage,
                                  FurnitureLibraryUserPreferences preferences,
                                  ThreadedTaskController controller) {
    super(taskMessage, preferences, controller);
    this.preferences = preferences;
    this.iconPreviewComponent = new ModelPreviewComponent();
    Insets insets = this.iconPreviewComponent.getInsets();
    this.iconPreviewComponent.setPreferredSize(
        new Dimension(PREVIEW_PREFERRED_SIZE + insets.left + insets.right, PREVIEW_PREFERRED_SIZE  + insets.top + insets.bottom));
    // Change layout
    GridBagLayout layout = new GridBagLayout();
    setLayout(layout);
    layout.setConstraints(getComponent(0), new GridBagConstraints(1, 0, 1, 1, 0, 1, 
        GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0));
    layout.setConstraints(getComponent(1), new GridBagConstraints(1, 1, 1, 1, 0, 1, 
        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(this.iconPreviewComponent, new GridBagConstraints(0, 0, 1, 2, 1, 1, 
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 10), 0, 0));
  }

  /**
   * Returns the catalog piece of furniture matching <code>model</code> 3D model 
   * or <code>null</code> if the content doesn't contain a 3D model at a supported format.
   */
  public CatalogPieceOfFurniture readPieceOfFurniture(final Content model) throws InterruptedException {
    try {
      final AtomicReference<BranchGroup> modelNode = new AtomicReference<BranchGroup>();
      String modelName = "model";
      final CountDownLatch latch = new CountDownLatch(1);
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            // Load content using cache to make it accessible by preview components
            ModelManager.getInstance().loadModel(model, new ModelManager.ModelObserver() {
                public void modelUpdated(BranchGroup modelRoot) {
                  latch.countDown();
                  modelNode.set(modelRoot);
                }
                
                public void modelError(Exception ex) {
                  latch.countDown();
                }
              });
          }
        });
      
      latch.await();
      
      Content pieceModel = null;
      Content previewModel = null;
      if (modelNode.get() != null) {
        // Copy model to a temporary OBJ content with materials and textures
        if (model instanceof URLContent) {
          modelName = URLDecoder.decode(((URLContent)model).getURL().getFile().replace("+", "%2B"), "UTF-8");;
          if (modelName.lastIndexOf('/') != -1) {
            modelName = modelName.substring(modelName.lastIndexOf('/') + 1);
          }
        } 

        previewModel = model;
        pieceModel = copyToTemporaryOBJContent(modelNode.get(), model);
        int dotIndex = modelName.lastIndexOf('.');
        if (dotIndex != -1) {
          modelName = modelName.substring(0, dotIndex);
        } 
      } else {
        ZipInputStream zipIn = null;
        try {
          // If content couldn't be loaded, copy model content to a temporary content 
          // and try to load model as a zipped file 
          URLContent urlContent = TemporaryURLContent.copyToTemporaryURLContent(model);
          // Open zipped stream
          zipIn = new ZipInputStream(urlContent.openStream());
          // Parse entries to see if a obj file is readable
          for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null; ) {
            String entryName = entry.getName();
            // Ignore directory entries and entries starting by a dot
            if (!entryName.endsWith("/")) {
              int slashIndex = entryName.lastIndexOf('/');
              String entryFileName = entryName.substring(slashIndex + 1);
              if (!entryFileName.startsWith(".")) {
                int dotIndex = entryFileName.lastIndexOf(".");
                if (dotIndex != -1) {
                  modelName = entryFileName.substring(0, dotIndex);
                } else {
                  modelName = entryFileName;
                }
                URL entryUrl = new URL("jar:" + urlContent.getURL() + "!/" 
                    + URLEncoder.encode(entryName, "UTF-8").replace("+", "%20").replace("%2F", "/"));
                final Content entryContent = new TemporaryURLContent(entryUrl);
                final CountDownLatch entryLatch = new CountDownLatch(1);
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                      // Load content using cache to make it accessible by preview components
                      ModelManager.getInstance().loadModel(entryContent, new ModelManager.ModelObserver() {
                          public void modelUpdated(BranchGroup modelRoot) {
                            modelNode.set(modelRoot);
                            entryLatch.countDown();
                          }
                          
                          public void modelError(Exception ex) {
                            entryLatch.countDown();
                          }
                        });
                    }
                  });
                
                entryLatch.await();
                if (modelNode.get() != null) {
                  previewModel =
                  pieceModel = new TemporaryURLContent(entryUrl);
                  if (!entryFileName.toLowerCase().endsWith(".obj")
                      && (this.preferences.isModelContentAlwaysConvertedToOBJFormat()
                          || slashIndex > 0)) {
                    // Convert models in subdirectories at format different from OBJ
                    pieceModel = copyToTemporaryOBJContent(modelNode.get(), model);
                  }
                  break;
                }
              }
            }
          }
        } catch (IOException ex) {
          return null;
        } finally {
          try {
            if (zipIn != null) {
              zipIn.close();
            }
          } catch (IOException ex2) {
            // Ignore close exception
          }
        }
      }

      if (Thread.interrupted()) {
        throw new InterruptedException();
      }

      if (modelNode.get() == null) {
        return null;
      }
      
      Vector3f size = ModelManager.getInstance().getSize(modelNode.get());
      // Generate icon image        
      final Content finalPreviewModel = previewModel;
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            iconPreviewComponent.setModel(finalPreviewModel);
          }
        });
      Thread.sleep(this.firstRendering ? 1000 : 100);
      this.firstRendering = false;
      final AtomicReference<Content> iconContent = new AtomicReference<Content>();
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            try {
              iconContent.set(iconPreviewComponent.getIcon(100));
            } catch (IOException ex) {
              throw new RuntimeException("Couldn't retrieve icon", ex);
            }    
          }
        });
      
      String key;
      if (Arrays.asList(preferences.getEditedProperties()).contains(FurnitureLibrary.FURNITURE_ID_PROPERTY)) {
        key = this.preferences.getDefaultCreator();
        if (key == null) {
          key = System.getProperty("user.name");
        }
        key += "#" + modelName;
      } else {
        key = null;
      }
      // Compute a more human readable name with spaces instead of hyphens and without camel case and trailing digit 
      String pieceName = "" + Character.toUpperCase(modelName.charAt(0));
      for (int i = 1; i < modelName.length(); i++) {
        char c = modelName.charAt(i);
        if (c == '-' || c == '_') {
          pieceName += ' ';
        } else if (!Character.isDigit(c) || i < modelName.length() - 1) {
          // Remove camel case
          if ((Character.isUpperCase(c) || Character.isDigit(c)) 
              && Character.isLowerCase(modelName.charAt(i - 1))) {
            pieceName += ' ';
            c = Character.toLowerCase(c);
          }
          pieceName += c;
        }
      }
      CatalogPieceOfFurniture piece = new CatalogPieceOfFurniture(key, 
          pieceName, null, iconContent.get(), null, pieceModel, 
          size.x, size.z, size.y, 0f, true, null, this.preferences.getDefaultCreator(), true, null, null);
      FurnitureCategory defaultCategory = new FurnitureCategory(
          this.preferences.getLocalizedString(ImportFurnitureTaskPanel.class, "defaultCategory"));
      new FurnitureCatalog().add(defaultCategory , piece);
      return piece;
    } catch (IllegalArgumentException ex) {
      // Thrown by getSize if model node is empty
      return null;
    } catch (IOException ex) {
      // If copying failed
      return null;
    } catch (InvocationTargetException ex) {
      ex.printStackTrace();
      return null;
    }
  }
  
  /**
   * Returns a copy of a given <code>model</code> as a zip content at OBJ format.
   */
  static Content copyToTemporaryOBJContent(BranchGroup model, Content modelContent) throws IOException {
    String objFile;
    if (modelContent instanceof URLContent) {
      objFile = ((URLContent)modelContent).getURL().getFile();
      if (objFile.lastIndexOf('/') != -1) {
        objFile = objFile.substring(objFile.lastIndexOf('/') + 1);
      }
      objFile = new File(objFile).getName();
      if (!objFile.toLowerCase().endsWith(".obj")) {
        if (objFile.lastIndexOf('.') != -1) {
          objFile = objFile.substring(0, objFile.lastIndexOf('.')); 
        }
        objFile += ".obj";
      }
      // Decode file name (replace %.. values)
      objFile = URLDecoder.decode(objFile.replace("+", "%2B"), "UTF-8");
      // Ensure the file contains only letters, figures, underscores, dots, hyphens or spaces
      if (objFile.matches(".*[^a-zA-Z0-9_\\.\\-\\ ].*")) {
        objFile = "model.obj";
      }
    } else {
      objFile = "model.obj";
    }

    File tempZipFile = File.createTempFile("urlContent", "tmp");
    tempZipFile.deleteOnExit();
    OBJWriter.writeNodeInZIPFile(model, tempZipFile, 0, objFile, "3D model " + objFile);
    return new TemporaryURLContent(new URL("jar:" + tempZipFile.toURI().toURL() + "!/" 
        + URLEncoder.encode(objFile, "UTF-8").replace("+", "%20")));
  }
}
