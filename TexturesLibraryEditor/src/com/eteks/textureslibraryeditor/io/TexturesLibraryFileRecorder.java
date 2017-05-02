/*
 * TexturesLibraryFileRecorder.java 11 sept. 2012
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
package com.eteks.textureslibraryeditor.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.eteks.sweethome3d.io.DefaultTexturesCatalog;
import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.InterruptedRecorderException;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.TexturesCatalog;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.tools.ResourceURLContent;
import com.eteks.sweethome3d.tools.TemporaryURLContent;
import com.eteks.sweethome3d.tools.URLContent;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryRecorder;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;

/**
 * Manages textures library files.
 * @author Emmanuel Puybaret
 */
public class TexturesLibraryFileRecorder implements TexturesLibraryRecorder {
  private static final Locale DEFAULT_LOCALE = new Locale("");
  
  private static final String ID          = "id"; 
  private static final String NAME        = "name"; 
  private static final String DESCRIPTION = "description"; 
  private static final String VERSION     = "version"; 
  private static final String LICENSE     = "license"; 
  private static final String PROVIDER    = "provider"; 
  
  /**
   * Reads a textures library from the given file, after clearing the given library.
   */
  public void readTexturesLibrary(final TexturesLibrary texturesLibrary, 
                                  String texturesLibraryLocation,
                                  TexturesLibraryUserPreferences preferences) throws RecorderException {
    readTexturesLibrary(texturesLibrary, texturesLibraryLocation, preferences, false);
  }
  
  /**
   * Merges a textures library with one in the given file.
   */
  public void mergeTexturesLibrary(TexturesLibrary texturesLibrary,
                                   String texturesLibraryLocation,
                                   TexturesLibraryUserPreferences preferences) throws RecorderException {
    readTexturesLibrary(texturesLibrary, texturesLibraryLocation, preferences, true);
  }

  /**
   * Reads a Textures library from the given file.
   */
  private void readTexturesLibrary(final TexturesLibrary texturesLibrary, 
                                   String texturesLibraryLocation, 
                                   TexturesLibraryUserPreferences preferences, 
                                   final boolean mergeLibrary) throws RecorderException {
    try {
      // Retrieve textures library with default reader and locale 
      Locale defaultLocale = Locale.getDefault();
      Locale.setDefault(DEFAULT_LOCALE);
      File texturesLibraryFile = File.createTempFile("textures", ".sh3t");
      texturesLibraryFile.deleteOnExit();
      copyFile(new File(texturesLibraryLocation), texturesLibraryFile);      
      URL texturesLibraryUrl = texturesLibraryFile.toURI().toURL();
      String texturesResourcesLocalDirectory = preferences.getTexturesResourcesLocalDirectory();
      URL texturesResourcesUrlBase = texturesResourcesLocalDirectory != null
          ? new File(texturesResourcesLocalDirectory).toURI().toURL()
          : null;
      final List<CatalogTexture> textures = new ArrayList<CatalogTexture>();
      new DefaultTexturesCatalog(new URL [] {texturesLibraryUrl}, texturesResourcesUrlBase) {
          @Override
          protected CatalogTexture readTexture(ResourceBundle resource,
                                               int index,
                                               URL texturesCatalogUrl,
                                               URL texturesResourcesUrlBase) {
            if (index == 1 && !mergeLibrary) {
              texturesLibrary.setId(getOptionalString(resource, ID));
              texturesLibrary.setName(getOptionalString(resource, NAME));
              texturesLibrary.setDescription(getOptionalString(resource, DESCRIPTION));
              texturesLibrary.setVersion(getOptionalString(resource, VERSION));
              texturesLibrary.setLicense(getOptionalString(resource, LICENSE));
              texturesLibrary.setProvider(getOptionalString(resource, PROVIDER));
            }
            CatalogTexture texture = super.readTexture(resource, index, texturesCatalogUrl, texturesResourcesUrlBase);
            if (texture != null) {
              // Set textures category through dummy catalog
              TexturesCategory category = super.readTexturesCategory(resource, index);
              new TexturesCatalog().add(category, texture);
              textures.add(texture);
            }
            return texture;
          }
          
          private String getOptionalString(ResourceBundle resource, String propertyKey) {
            try {
              return resource.getString(propertyKey);
            } catch (MissingResourceException ex) {
              return null;
            }
          }
        };
      
      // Search which locales are supported
      List<ZipEntry> zipEntries = getZipEntries(texturesLibraryUrl);
      Set<Locale>    supportedLocales = new HashSet<Locale>(); 
      for (ZipEntry zipEntry : zipEntries) {
        String entryName = zipEntry.getName();
        if (entryName.startsWith(DefaultTexturesCatalog.PLUGIN_TEXTURES_CATALOG_FAMILY)
            && entryName.endsWith(".properties")) {
          supportedLocales.add(getLocale(entryName));
        }
      }

      // Replace textures by the one read
      if (!mergeLibrary) {
        for (CatalogTexture texture : texturesLibrary.getTextures()) {
          texturesLibrary.deleteTexture(texture);
        }
      }
      for (CatalogTexture texture : textures) {
        texturesLibrary.addTexture(texture);
      }

      // Get textures name and category name in each supported locale
      for (Locale locale : supportedLocales) {
        if (!TexturesLibrary.DEFAULT_LANGUAGE.equals(locale.toString())) {          
          Locale.setDefault(locale);
          final String language = locale.toString();
          new DefaultTexturesCatalog(new URL [] {texturesLibraryUrl}, texturesResourcesUrlBase) {
              @Override
              protected CatalogTexture readTexture(ResourceBundle resource,
                                                                     int index,
                                                                     URL texturesCatalogUrl,
                                                                     URL texturesResourcesUrlBase) {
                CatalogTexture texture = super.readTexture(resource, index, texturesCatalogUrl, texturesResourcesUrlBase);
                if (texture != null) {
                  TexturesCategory category = super.readTexturesCategory(resource, index);
                  CatalogTexture texturesLibraryItem = textures.get(index - 1);
                  texturesLibrary.setTextureLocalizedData(texturesLibraryItem, language, 
                      TexturesLibrary.TEXTURES_NAME_PROPERTY, texture.getName());
                  texturesLibrary.setTextureLocalizedData(texturesLibraryItem, language, 
                      TexturesLibrary.TEXTURES_CATEGORY_PROPERTY, category.getName());
                }
                return texture;
              }
            };
        }
      }
      
      Locale.setDefault(defaultLocale);
    } catch (IOException ex) {
      throw new RecorderException("Invalid textures library file " + texturesLibraryLocation, ex);
    } catch (MissingResourceException ex) {
      throw new RecorderException("Invalid textures library file " + texturesLibraryLocation, ex);
    }
  }

  /**
   * Returns the locale of the given properties file.
   */
  private Locale getLocale(String fileName) {
    String localeString = fileName.substring(DefaultTexturesCatalog.PLUGIN_TEXTURES_CATALOG_FAMILY.length(),
        fileName.lastIndexOf(".properties"));
    if (localeString.matches("_\\w{2}")) {
      return new Locale(localeString.substring(1));
    } else if (localeString.matches("_\\w{2}_\\w{2}")) {
      return new Locale(localeString.substring(1, 3), localeString.substring(4));
    } else {
      return DEFAULT_LOCALE;
    }  
  }
  
  /**
   * Writes textures library in the <code>texturesLibraryName</code> file.  
   */
  public void writeTexturesLibrary(TexturesLibrary texturesLibrary,
                                    String texturesLibraryName,
                                    TexturesLibraryUserPreferences userPreferences) throws RecorderException {
    writeTexturesLibrary(texturesLibrary, texturesLibraryName, 
        userPreferences.isTexturesLibraryOffline(), 
        userPreferences.isContentMatchingTexturesName(),
        userPreferences.getTexturesResourcesLocalDirectory(), 
        userPreferences.getTexturesResourcesRemoteURLBase());
  }

  /**
   * Writes textures library .properties files in the <code>texturesLibraryName</code> file. 
   * @param offlineTexturesLibrary if <code>offlineTexturesLibrary</code> is <code>true</code> content 
   *                       referenced by textures is always embedded in the file
   * @param contentMatchingTexturesName <code>true</code> if the textures content saved with the library 
   *                       should be named from the textures name in the default language                      
   * @param texturesResourcesLocalDirectory  directory where content referenced by textures will be saved
   *                       if it isn't <code>null</code>
   * @param texturesResourcesRemoteUrlBase   URL base used for content referenced by textures in .properties file 
   *                       if <code>texturesResourcesLocalDirectory</code> isn't <code>null</code>              
   */
  private void writeTexturesLibrary(TexturesLibrary texturesLibrary,
                                     String texturesLibraryLocation,
                                     boolean offlineTexturesLibrary,
                                     boolean contentMatchingTexturesName,
                                     String  texturesResourcesLocalDirectory,
                                     String  texturesResourcesRemoteUrlBase) throws RecorderException {
    URL texturesResourcesRemoteAbsoluteUrlBase = null;
    String texturesResourcesRemoteRelativeUrlBase = null;
    if (!offlineTexturesLibrary 
        && texturesResourcesLocalDirectory != null
        && texturesResourcesRemoteUrlBase != null) {
      try {
        texturesResourcesRemoteAbsoluteUrlBase = new URL(texturesResourcesRemoteUrlBase);
      } catch (MalformedURLException ex) {
        // texturesResourcesRemoteUrlBase is a relative URL
        int lastSlashIndex = texturesResourcesRemoteUrlBase.lastIndexOf('/');
        if (lastSlashIndex != 1) {
          texturesResourcesRemoteRelativeUrlBase = texturesResourcesRemoteUrlBase.substring(0, lastSlashIndex + 1);
          texturesResourcesLocalDirectory = new File(texturesResourcesLocalDirectory, texturesResourcesRemoteUrlBase).toString();
        } else {
          texturesResourcesRemoteRelativeUrlBase = "";
        }
      }
    }
    
    ZipOutputStream zipOut = null;
    Map<Content, String> contentEntries = new HashMap<Content, String>();
    File texturesLibraryFile = new File(texturesLibraryLocation);
    File tmpFile = null;
    try {
      tmpFile = File.createTempFile("temp", ".sh3t"); 
      OutputStream out = new FileOutputStream(tmpFile);
      if (out != null) {
        // Create a zip output on file  
        zipOut = new ZipOutputStream(out);
        // Write textures description file in first entry 
        zipOut.putNextEntry(new ZipEntry(DefaultTexturesCatalog.PLUGIN_TEXTURES_CATALOG_FAMILY + ".properties"));
        writeTexturesLibraryProperties(zipOut, texturesLibrary, texturesLibraryFile, 
            offlineTexturesLibrary, contentMatchingTexturesName,
            texturesResourcesRemoteAbsoluteUrlBase, texturesResourcesRemoteRelativeUrlBase, 
            contentEntries);
        zipOut.closeEntry();
        // Write supported languages description files
        for (String language : texturesLibrary.getSupportedLanguages()) {
          if (!TexturesLibrary.DEFAULT_LANGUAGE.equals(language)) {
            zipOut.putNextEntry(new ZipEntry(DefaultTexturesCatalog.PLUGIN_TEXTURES_CATALOG_FAMILY + "_" + language + ".properties"));
            writeTexturesLibraryLocalizedProperties(zipOut, texturesLibrary, language);
            zipOut.closeEntry();
          }
        }        
        // Write Content objects in files
        writeContents(zipOut, offlineTexturesLibrary, texturesResourcesLocalDirectory, contentEntries);
        // Finish zip writing
        zipOut.finish();
        zipOut.close();
        zipOut = null;

        copyFile(tmpFile, texturesLibraryFile);
        tmpFile.delete();
      }
    } catch (IOException ex) {
      throw new RecorderException("Can't save textures library file " + texturesLibraryLocation, ex);
    } finally {
      if (zipOut != null) {
        try {
          zipOut.close();
        } catch (IOException ex) {
          throw new RecorderException("Can't close textures library file " + texturesLibraryLocation, ex);
        }
      }
    }
  }

  /**
   * Writes in <code>output</code> the given textures library
   * with properties as defined as in <code>DefaultTexturesCatalog</code>. 
   */
  private void writeTexturesLibraryProperties(OutputStream output,
                                               TexturesLibrary texturesLibrary,
                                               File texturesLibraryFile,
                                               boolean offlineTexturesLibrary, 
                                               boolean contentMatchingTexturesName,
                                               URL texturesResourcesRemoteAbsoluteUrlBase,
                                               String texturesResourcesRemoteRelativeUrlBase, 
                                               Map<Content, String> contentEntries) throws IOException {
    boolean keepURLContentUnchanged = !offlineTexturesLibrary
        && texturesResourcesRemoteAbsoluteUrlBase == null 
        && texturesResourcesRemoteRelativeUrlBase == null;
    // Store existing entries in lower case to be able to compare their names ignoring case
    Set<String> existingEntryNamesLowerCase = new HashSet<String>();
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "ISO-8859-1"));
    final String CATALOG_FILE_HEADER = "#\n# " 
        + DefaultTexturesCatalog.PLUGIN_TEXTURES_CATALOG_FAMILY + ".properties %tc\n" 
        + "# Generated by Textures Library Editor\n#\n";
    writer.write(String.format(CATALOG_FILE_HEADER, new Date()));
    writer.newLine();
    writeProperty(writer, ID, texturesLibrary.getId());
    writeProperty(writer, NAME, texturesLibrary.getName());
    writeProperty(writer, DESCRIPTION, texturesLibrary.getDescription());
    writeProperty(writer, VERSION, texturesLibrary.getVersion());
    writeProperty(writer, LICENSE, texturesLibrary.getLicense());
    writeProperty(writer, PROVIDER, texturesLibrary.getProvider());
    
    int i = 1;
    for (CatalogTexture texture : texturesLibrary.getTextures()) {
      writer.newLine();
      writeProperty(writer, DefaultTexturesCatalog.PropertyKey.ID, i, texture.getId());
      writeProperty(writer, DefaultTexturesCatalog.PropertyKey.NAME, i, texture.getName());
      writeProperty(writer, DefaultTexturesCatalog.PropertyKey.CATEGORY, i, texture.getCategory().getName());
      Content textureImage = texture.getImage();
      String contentBaseName;
      if (contentMatchingTexturesName
          || !(textureImage instanceof URLContent)) {
        contentBaseName = texture.getName();
      } else {
        String file = ((URLContent)textureImage).getURL().getFile();
        if (file.lastIndexOf('/') != -1) {
          file = file.substring(file.lastIndexOf('/') + 1);
        }
        if (file.lastIndexOf('.') != -1) {
          file = file.substring(0, file.lastIndexOf('.'));
        }
        contentBaseName = file;
      }
      String contentExtension = ".img";
      if (textureImage instanceof URLContent) {
        String file = ((URLContent)textureImage).getURL().getFile();
        if (file.lastIndexOf('.') != -1) {
          contentExtension = file.substring(file.lastIndexOf('.'));
        }
      }
      String imageContentEntryName = contentEntries.get(textureImage);
      // If image content not referenced among saved content yet
      if (imageContentEntryName == null) {
        imageContentEntryName = getContentEntry(textureImage, contentBaseName + contentExtension, 
            keepURLContentUnchanged, existingEntryNamesLowerCase);
        if (imageContentEntryName != null) {
          contentEntries.put(textureImage, imageContentEntryName);
        }
      }
      writeProperty(writer, DefaultTexturesCatalog.PropertyKey.IMAGE, i, 
          getContentProperty(textureImage, imageContentEntryName, offlineTexturesLibrary, 
              texturesResourcesRemoteAbsoluteUrlBase, texturesResourcesRemoteRelativeUrlBase));
      writeProperty(writer, DefaultTexturesCatalog.PropertyKey.WIDTH, i, texture.getWidth());
      writeProperty(writer, DefaultTexturesCatalog.PropertyKey.HEIGHT, i, texture.getHeight());
      writeProperty(writer, DefaultTexturesCatalog.PropertyKey.CREATOR, i, texture.getCreator());
      i++;
    }
    writer.flush();
  }
  
  /**
   * Writes in <code>output</code> the given textures library
   * with properties as defined as in <code>DefaultTexturesCatalog</code>. 
   */
  private void writeTexturesLibraryLocalizedProperties(OutputStream output,
                                                        TexturesLibrary texturesLibrary,
                                                        String language) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "ISO-8859-1"));
    final String CATALOG_FILE_HEADER = "#\n# " 
        + DefaultTexturesCatalog.PLUGIN_TEXTURES_CATALOG_FAMILY + "_" + language + ".properties %tc\n" 
        + "# Generated by Textures Library Editor\n#\n";
    writer.write(String.format(CATALOG_FILE_HEADER, new Date()));
    int i = 1;
    for (CatalogTexture texture : texturesLibrary.getTextures()) {
      writer.newLine();
      Object textureName = texturesLibrary.getTextureLocalizedData(
          texture, language, TexturesLibrary.TEXTURES_NAME_PROPERTY);
      if (textureName != null) {
        writeProperty(writer, DefaultTexturesCatalog.PropertyKey.NAME, i, textureName);
      }
      Object categoryName = texturesLibrary.getTextureLocalizedData(
          texture, language, TexturesLibrary.TEXTURES_CATEGORY_PROPERTY);
      if (categoryName != null) {
        writeProperty(writer, DefaultTexturesCatalog.PropertyKey.CATEGORY, i, categoryName);
      }
      i++;
    }
    writer.flush();
  }
    
  /**
   * Returns the entry name of a <code>content</code>.
   */
  private String getContentEntry(Content content,
                                 String entryName,
                                 boolean keepURLContentUnchanged, 
                                 Set<String> existingEntryNamesLowerCase) throws IOException {
    if (content instanceof TemporaryURLContent
        || content instanceof ResourceURLContent) {
      int slashIndex = entryName.indexOf('/'); 
      if (slashIndex == -1) {
        if (existingEntryNamesLowerCase.contains(entryName.toLowerCase())) {
          // Search an unexisting entry name
          int i = 2;
          String defaultEntryName = entryName;
          do {
            int dotIndex = defaultEntryName.lastIndexOf('.');
            entryName = defaultEntryName.substring(0, dotIndex) 
                + i++ + defaultEntryName.substring(dotIndex);  
          } while (existingEntryNamesLowerCase.contains(entryName.toLowerCase()));
        }
      } else {
        String entryDirectory = entryName.substring(0, slashIndex + 1);
        int i = 2;
        while (true) {
          boolean entryDirectoryExists = false;
          String entryDirectoryLowerCase = entryDirectory.toLowerCase();
          // Search an unexisting entry directory
          for (String existingEntryNameLowerCase : existingEntryNamesLowerCase) {
            // If existing entry name starts with entry directory ignoring case
            if (existingEntryNameLowerCase.startsWith(entryDirectoryLowerCase)) {
              entryDirectoryExists = true;
              break;
            }
          }
          if (entryDirectoryExists) {
            entryDirectory = entryName.substring(0, slashIndex) + i++ + "/";
          } else {            
            entryName = entryDirectory + entryName.substring(slashIndex + 1);
            break;
          }
        }
      }
      existingEntryNamesLowerCase.add(entryName.toLowerCase());
      return entryName;
    } else if (content instanceof URLContent) {
      if (keepURLContentUnchanged) {
        // Won't save content
        return null;
      } else {
        URLContent urlContent = (URLContent)content;
        if (urlContent.isJAREntry()) {
          String file = urlContent.getJAREntryURL().getFile();
          file = file.substring(file.lastIndexOf('/') + 1);
          int zipIndex = file.lastIndexOf(".zip");
          if (zipIndex == -1) {
            return null;
          } else {
            file = file.substring(0, zipIndex);
            entryName = file + "/" + urlContent.getJAREntryName();
          }
        } else {
          String file = urlContent.getURL().getFile();
          entryName = file.substring(file.lastIndexOf('/') + 1);
        }
        existingEntryNamesLowerCase.add(entryName.toLowerCase());
        return entryName;
      }
    } else {
      throw new IOException("Unexpected content class: " + content.getClass().getName());
    }
  }

  /**
   * Returns the property value saved for a resource <code>content</code>.
   */
  private String getContentProperty(Content content,
                                    String  entryName, 
                                    boolean offlineTexturesLibrary, 
                                    URL texturesResourcesRemoteAbsoluteUrlBase,
                                    String texturesResourcesRemoteRelativeUrlBase) throws IOException {
    if (offlineTexturesLibrary
        || (texturesResourcesRemoteAbsoluteUrlBase == null
            && texturesResourcesRemoteRelativeUrlBase == null)) {
      return "/" + entryName;
    } else if (content instanceof TemporaryURLContent
               || content instanceof ResourceURLContent
               || texturesResourcesRemoteAbsoluteUrlBase != null
               || texturesResourcesRemoteRelativeUrlBase != null) {
      int slashIndex = entryName.indexOf('/');
      if (slashIndex == -1) {
        if (texturesResourcesRemoteAbsoluteUrlBase != null) {
          return new URL(texturesResourcesRemoteAbsoluteUrlBase, entryName).toString();
        } else {
          return texturesResourcesRemoteRelativeUrlBase + entryName;
        }
      } else {
        String encodedEntry = URLEncoder.encode(entryName.substring(slashIndex + 1), "UTF-8").replace("+", "%20").replace("%2F", "/");
        if (texturesResourcesRemoteAbsoluteUrlBase != null) {
          return "jar:" + new URL(texturesResourcesRemoteAbsoluteUrlBase, entryName.substring(0, slashIndex) + ".zip") 
              + "!/" + encodedEntry;
        } else {
          return texturesResourcesRemoteRelativeUrlBase + entryName.substring(0, slashIndex) + ".zip" 
              + "!/" + encodedEntry;
        }
      }
    } else {
      return ((URLContent)content).getURL().toString();
    }
  }
  
  /**
   * Writes the (<code>key</code>, <code>value</code>) of a property
   * in <code>writer</code>.
   */
  private void writeProperty(BufferedWriter writer, 
                             DefaultTexturesCatalog.PropertyKey key, 
                             int index, Object value) throws IOException {
    writeProperty(writer, key.getKey(index), value);
  }

  /**
   * Writes the (<code>key</code>, <code>value</code>) of a property
   * in <code>writer</code>, if the <code>value</code> isn't <code>null</code>.
   */
  private void writeProperty(BufferedWriter writer, 
                             String key, 
                             Object value) throws IOException {
    if (value != null) {
      writer.write(key);
      writer.write("=");    
      String s = value.toString();
      CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);      
        switch (c) {
          case '\\':
            writer.write('\\');
            writer.write('\\');
            break;
          case '\t':
            writer.write('\\');
            writer.write('t');
            break;
          default:
            if (encoder.canEncode(c)) {
              writer.write(c);
            } else {
              writer.write('\\');
              writer.write('u');
              writer.write(Integer.toHexString((c >> 12) & 0xF));
              writer.write(Integer.toHexString((c >> 8) & 0xF));
              writer.write(Integer.toHexString((c >> 4) & 0xF));
              writer.write(Integer.toHexString(c & 0xF));
            }
        }
      }
      writer.newLine();
    }
  }

  /**
   * Writes in <code>zipOut</code> stream the given contents.
   */
  private void writeContents(ZipOutputStream zipOut,
                             boolean offlineTexturesLibrary, 
                             String  texturesResourcesLocalDirectory, 
                             Map<Content, String> contentEntries) throws IOException, InterruptedRecorderException {
    if (!offlineTexturesLibrary && texturesResourcesLocalDirectory != null) {
      // Check local directory
      File directory = new File(texturesResourcesLocalDirectory);
      if (!directory.exists()) {
        if (!directory.mkdirs()) {
          throw new IOException("Can't create directory " + directory);
        }
      } else if (!directory.isDirectory()) {
        throw new IOException(directory + " isn't a directory");
      }
    }
    
    Map<String, List<ZipEntry>> zipUrlsEntries = new HashMap<String, List<ZipEntry>>();
    for (Map.Entry<Content, String> contentEntry : contentEntries.entrySet()) {
      Content content = contentEntry.getKey();
      if (content instanceof URLContent) {
        URLContent urlContent = (URLContent)content;
        String entryName = contentEntry.getValue();
        if (entryName.indexOf('/') != -1) { 
          writeZipEntries(zipOut, offlineTexturesLibrary, texturesResourcesLocalDirectory, 
              urlContent, entryName, zipUrlsEntries);
        } else if (offlineTexturesLibrary || texturesResourcesLocalDirectory == null) {
          writeZipEntry(zipOut, urlContent, entryName);
        } else {
          File file = new File(texturesResourcesLocalDirectory, entryName);
          if (!file.exists()) {
            copyContent(urlContent, file);
          }
        }
      }
      if (Thread.interrupted()) {
        throw new InterruptedRecorderException();
      }
    }  
  }
  
  /**
   * Writes in <code>zipOut</code> stream all the sibling files of the zipped 
   * <code>content</code>.
   */
  private void writeZipEntries(ZipOutputStream zipOut,
                               boolean offlineTexturesLibrary,
                               String texturesResourcesLocalDirectory, 
                               URLContent content, 
                               String mainEntryName, 
                               Map<String, List<ZipEntry>> zipUrlsEntries) throws IOException {
    String mainEntryDirectory = mainEntryName.substring(0, mainEntryName.indexOf('/')); 
    if (!offlineTexturesLibrary && texturesResourcesLocalDirectory != null) {
      // Write content entries in a separate zipped file, if the file doesn't exist
      File file = new File(texturesResourcesLocalDirectory, mainEntryDirectory + ".zip");
      if (file.exists()) {
        return;
      }
      zipOut = new ZipOutputStream(new FileOutputStream(file));
      mainEntryDirectory = "";
    } else {
      mainEntryDirectory += "/";
    }
    
    String contentDirectory = "";
    if (content instanceof ResourceURLContent) {
      contentDirectory = URLDecoder.decode(content.getJAREntryName().replace("+", "%2B"), "UTF-8");
      int slashIndex = contentDirectory.lastIndexOf('/'); 
      if (slashIndex != -1) {
        contentDirectory = contentDirectory.substring(0, slashIndex + 1);
      }
    }
    
    URL zipUrl = content.getJAREntryURL();
    // Keep in cache the entries of the read zip files to speed up save process 
    List<ZipEntry> entries = zipUrlsEntries.get(zipUrl.toString());
    if (entries == null) {
      zipUrlsEntries.put(zipUrl.toString(), entries = getZipEntries(zipUrl));
    }
    for (ZipEntry entry : entries) {
      String zipEntryName = entry.getName();
      URLContent siblingContent = new URLContent(new URL("jar:" + zipUrl + "!/" + 
          URLEncoder.encode(zipEntryName, "UTF-8").replace("+", "%20")));
      if (contentDirectory.length() == 0) {
        // Write each zipped stream entry that is stored in content except useless content  
        writeZipEntry(zipOut, siblingContent, mainEntryDirectory + zipEntryName);
      } else if (zipEntryName.startsWith(contentDirectory)) {
        // Write each zipped stream entry that is stored in the same directory as content  
        writeZipEntry(zipOut, siblingContent, mainEntryDirectory + zipEntryName.substring(contentDirectory.length()));
      }
    }

    if (!offlineTexturesLibrary && texturesResourcesLocalDirectory != null) {
      zipOut.close();
    }
  }

  /**
   * Returns the ZIP entries in <code>zipUrl</code>.
   */
  private List<ZipEntry> getZipEntries(URL zipUrl) throws IOException {
    List<ZipEntry> entries;
    // Get zipped stream entries
    ZipInputStream zipIn = null;
    try {
      entries = new ArrayList<ZipEntry>();
      zipIn = new ZipInputStream(zipUrl.openStream());
      for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null; ) {
        entries.add(entry);
      }
      return entries;
    } finally {
      if (zipIn != null) {
        zipIn.close();
      }
    }
  }

  /**
   * Writes in <code>zipOut</code> stream a new entry named <code>entryName</code> that 
   * contains a given <code>content</code>.
   */
  private void writeZipEntry(ZipOutputStream zipOut, 
                             URLContent content, 
                             String entryName) throws IOException {
    byte [] buffer = new byte [8096];
    InputStream contentIn = null;
    try {
      zipOut.putNextEntry(new ZipEntry(entryName));
      contentIn = content.openStream();          
      int size; 
      while ((size = contentIn.read(buffer)) != -1) {
        zipOut.write(buffer, 0, size);
      }
      zipOut.closeEntry();  
    } finally {
      if (contentIn != null) {          
        contentIn.close();
      }
    }
  }

  /**
   * Copy the given <code>content</code> to <code>file</code>.
   */
  private void copyContent(Content content, File file) throws IOException {
    InputStream in = null;
    try {
      in = content.openStream();
      copyContentToFile(in, file);
    } catch (IOException ex) { 
      throw new IOException("Can't copy content " + content + " to " + file);
    } finally {
      try {
        if (in != null) {          
          in.close();
        }
      } catch (IOException ex) {
        // Forget exception
      }
    }
  }

  /**
   * Copy <code>file1</code> to <code>file2</code>.
   */
  private void copyFile(File file1, File file2) throws IOException {
    InputStream in = null;
    try {
      in = new FileInputStream(file1);
      copyContentToFile(in, file2);
    } catch (IOException ex) { 
      throw new IOException("Can't copy file " + file1 + " to " + file2);
    } finally {
      try {
        if (in != null) {          
          in.close();
        }
      } catch (IOException ex) {
        // Forget exception
      }
    }
  }
  
  /**
   * Copy the content of <code>in</code> stream to <code>file</code>.
   */
  private void copyContentToFile(InputStream in, File file) throws IOException {
    byte [] buffer = new byte [8192];
    OutputStream out = null;
    try {
      out = new FileOutputStream(file);
      int size; 
      while ((size = in.read(buffer)) != -1) {
        out.write(buffer, 0, size);
      }
    } finally {
      try {
        if (out != null) {          
          out.close();
        }
      } catch (IOException ex) {
        throw new IOException("Can't close file " + file);
      }
    }
  }
}
