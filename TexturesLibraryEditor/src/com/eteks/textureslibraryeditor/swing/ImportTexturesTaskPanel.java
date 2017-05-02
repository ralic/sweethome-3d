/*
 * ImportTexturesTaskPanel.java 11 sept. 2012
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.TexturesCatalog;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.swing.ScaledImageComponent;
import com.eteks.sweethome3d.swing.ThreadedTaskPanel;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.tools.TemporaryURLContent;
import com.eteks.sweethome3d.tools.URLContent;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;
import com.eteks.textureslibraryeditor.viewcontroller.ImportTexturesTaskView;

/**
 * A threaded task panel used for textures importation. 
 * @author Emmanuel Puybaret
 */
public class ImportTexturesTaskPanel extends ThreadedTaskPanel implements ImportTexturesTaskView {
  private final TexturesLibraryUserPreferences preferences;
  private ScaledImageComponent                 imageComponent;

  public ImportTexturesTaskPanel(String taskMessage,
                              TexturesLibraryUserPreferences preferences,
                              ThreadedTaskController controller) {
    super(taskMessage, preferences, controller);
    this.preferences = preferences;
    this.imageComponent = new ScaledImageComponent();
    Insets insets = this.imageComponent.getInsets();
    this.imageComponent.setPreferredSize(
        new Dimension(128 + insets.left + insets.right, 128  + insets.top + insets.bottom));
    // Change layout
    GridBagLayout layout = new GridBagLayout();
    setLayout(layout);
    layout.setConstraints(getComponent(0), new GridBagConstraints(1, 0, 1, 1, 0, 1, 
        GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0));
    layout.setConstraints(getComponent(1), new GridBagConstraints(1, 1, 1, 1, 0, 1, 
        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(this.imageComponent, new GridBagConstraints(0, 0, 1, 2, 1, 1, 
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 10), 0, 0));
  }

  /**
   * Returns the catalog texture of textures matching <code>imageContent</code> 
   * or <code>null</code> if the content doesn't contain an image at a supported format.
   */
  public CatalogTexture readTexture(Content imageContent) throws InterruptedException {
    BufferedImage image;
    String textureName = "texture";
    InputStream in = null;
    try {
      in = imageContent.openStream();
      image = ImageIO.read(in);
      this.imageComponent.setImage(image);
      // Copy image to a temporary OBJ content 
      if (imageContent instanceof URLContent) {
        textureName = URLDecoder.decode(((URLContent)imageContent).getURL().getFile().replace("+", "%2B"), "UTF-8");;
        if (textureName.lastIndexOf('/') != -1) {
          textureName = textureName.substring(textureName.lastIndexOf('/') + 1);
        }
      } 
      imageContent = copyToTemporaryContent(image, imageContent);
      int dotIndex = textureName.lastIndexOf('.');
      if (dotIndex != -1) {
        textureName = textureName.substring(0, dotIndex);
      } 
    } catch (IOException ex) {
       return null;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    }
    
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }

    if (image != null) {
      String key;
      if (Arrays.asList(preferences.getEditedProperties()).contains(TexturesLibrary.TEXTURES_ID_PROPERTY)) {
        key = this.preferences.getDefaultCreator();
        if (key == null) {
          key = System.getProperty("user.name");
        }
        key += "#" + textureName;
      } else {
        key = null;
      }
      // Compute a more human readable name with spaces instead of hyphens and without camel case and trailing digit 
      String displayedName = "" + Character.toUpperCase(textureName.charAt(0));
      for (int i = 1; i < textureName.length(); i++) {
        char c = textureName.charAt(i);
        if (c == '-' || c == '_') {
          displayedName += ' ';
        } else if (!Character.isDigit(c) || i < textureName.length() - 1) {
          // Remove camel case
          if ((Character.isUpperCase(c) || Character.isDigit(c)) 
              && Character.isLowerCase(textureName.charAt(i - 1))) {
            displayedName += ' ';
            c = Character.toLowerCase(c);
          }
          displayedName += c;
        }
      }
      CatalogTexture texture = new CatalogTexture(key, 
          displayedName, imageContent, image.getWidth() / 10f, image.getHeight() / 10f, this.preferences.getDefaultCreator());
      TexturesCategory defaultCategory = new TexturesCategory(
          this.preferences.getLocalizedString(ImportTexturesTaskPanel.class, "defaultCategory"));
      new TexturesCatalog().add(defaultCategory , texture);
      return texture;
    } else {
      return null;
    }
  }
  
  /**
   * Returns a copy of a given <code>image</code>.
   */
  static Content copyToTemporaryContent(BufferedImage image, Content imageContent) throws IOException {
    if (imageContent instanceof URLContent) {
      return TemporaryURLContent.copyToTemporaryURLContent(imageContent);
    } else {
      File tempFile = OperatingSystem.createTemporaryFile("texture", "png");
      FileOutputStream out = null;
      try {
        out = new FileOutputStream(tempFile);
        ImageIO.write(image, "PNG", out);
        return new TemporaryURLContent(tempFile.toURI().toURL());
      } finally {
        if (out != null) {
          out.close();
        }
      }
    }
  }
}
