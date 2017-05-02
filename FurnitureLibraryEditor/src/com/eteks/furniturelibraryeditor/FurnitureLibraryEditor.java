/*
 * FurnitureLibraryEditor.java 14 déc. 2009
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
package com.eteks.furniturelibraryeditor;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import com.eteks.furniturelibraryeditor.io.FileFurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.io.FurnitureLibraryFileRecorder;
import com.eteks.furniturelibraryeditor.model.FurnitureLibrary;
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryRecorder;
import com.eteks.furniturelibraryeditor.model.FurnitureLibraryUserPreferences;
import com.eteks.furniturelibraryeditor.swing.SwingEditorViewFactory;
import com.eteks.furniturelibraryeditor.viewcontroller.EditorController;
import com.eteks.furniturelibraryeditor.viewcontroller.EditorView;
import com.eteks.furniturelibraryeditor.viewcontroller.EditorViewFactory;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * An application able to edit the furniture described in a SH3F file.
 * @author Emmanuel Puybaret
 */
public class FurnitureLibraryEditor {
  private FurnitureLibraryFileRecorder furnitureLibraryRecorder;
  private FileFurnitureLibraryUserPreferences    userPreferences;
  private EditorViewFactory            viewFactory;
  private ContentManager               contentManager;

  /**
   * Returns a recorder able to write and read furniture library files.
   */
  public FurnitureLibraryRecorder getFurnitureLibraryRecorder() {
    // Initialize furnitureLibraryRecorder lazily 
    if (this.furnitureLibraryRecorder == null) {
      this.furnitureLibraryRecorder = new FurnitureLibraryFileRecorder();
    }
    return this.furnitureLibraryRecorder;
  }
  
  /**
   * Returns user preferences stored in resources and local file system.
   */
  public FurnitureLibraryUserPreferences getUserPreferences() {
    // Initialize userPreferences lazily
    if (this.userPreferences == null) {
      this.userPreferences = new FileFurnitureLibraryUserPreferences();
    }
    return this.userPreferences;
  }
  
  /**
   * Returns a content manager able to handle files.
   */
  protected ContentManager getContentManager() {
    if (this.contentManager == null) {
      this.contentManager = new FileContentManager(getUserPreferences()) {
          private File modelsDirectory;
  
          @Override
          public String showOpenDialog(View parentView,
                                     String dialogTitle,
                                     ContentType contentType) {
            if (contentType == ContentType.USER_DEFINED) {
              // Let user choose multiple model files
              JFileChooser fileChooser = new JFileChooser();
              // Update current directory
              if (this.modelsDirectory != null) {
                fileChooser.setCurrentDirectory(this.modelsDirectory);
              }
              fileChooser.setDialogTitle(dialogTitle);
              fileChooser.setMultiSelectionEnabled(true);
              if (fileChooser.showOpenDialog((JComponent)parentView) == JFileChooser.APPROVE_OPTION) {
                // Retrieve current directory for future calls
                this.modelsDirectory = fileChooser.getCurrentDirectory();
                // Return selected files separated by path separator character
                String files = "";
                for (File selectedFile : fileChooser.getSelectedFiles()) {
                  if (files.length() > 0) {
                    files += File.pathSeparator;
                  }
                  files += selectedFile;
                }
                return files;
              } else {
                return null;
              }
            } else {
              return super.showOpenDialog(parentView, dialogTitle, contentType);
            }
          }
        };
    }
    return this.contentManager;
  }
  
  /**
   * Returns a Swing view factory. 
   */
  protected EditorViewFactory getViewFactory() {
    if (this.viewFactory == null) {
      this.viewFactory = new SwingEditorViewFactory();
    }
    return this.viewFactory;
  }
  
  /**
   * Returns the name of this application read from resources.
   */
  private String getName() {
    return getUserPreferences().getLocalizedString(FurnitureLibraryEditor.class, "applicationName");
  }

  /**
   * Returns a new instance of an editor controller after <code>furnitureLibrary</code> was created.
   */
  protected EditorController createEditorController(FurnitureLibrary furnitureLibrary) {
    return new EditorController(furnitureLibrary, getFurnitureLibraryRecorder(), getUserPreferences(), 
        getViewFactory(), getContentManager());
  }

  /**
   * Furniture Library Editor entry point.
   * @param args may contain one .sh3f file to open, 
   *     following a <code>-open</code> option.  
   */
  public static void main(String [] args) {
    new FurnitureLibraryEditor().init(args);
  }
  
  /**
   * Initializes application instance.
   */
  protected void init(final String [] args) {
    initSystemProperties();    
    initLookAndFeel();    
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        FurnitureLibraryEditor.this.run(args);
      }
    });
  }

  /**
   * Sets various <code>System</code> properties.
   */
  private void initSystemProperties() {
    if (OperatingSystem.isMacOSX()) {
      // Change Mac OS X application menu name
      String classPackage = FurnitureLibraryEditor.class.getName();
      classPackage = classPackage.substring(0, classPackage.lastIndexOf("."));
      ResourceBundle resource = ResourceBundle.getBundle(classPackage + "." + "package");
      String applicationName = resource.getString("FurnitureLibraryEditor.applicationName");
      System.setProperty("com.apple.mrj.application.apple.menu.about.name", applicationName);
      // Use Mac OS X screen menu bar for frames menu bar
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      // Force the use of Quartz under Mac OS X for better Java 2D rendering performance
      System.setProperty("apple.awt.graphics.UseQuartz", "true");
    }
    // Force 3D antialiasing before toolkit is launched
    if (System.getProperty("j3d.implicitAntialiasing") == null) {
      System.setProperty("j3d.implicitAntialiasing", "true");
    }
  }

  /**
   * Sets application look and feel.
   */
  private void initLookAndFeel() {
    try {
      // Apply current system look and feel
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      // Change default titled borders under Mac OS X 10.5
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        UIManager.put("TitledBorder.border", 
            UIManager.getBorder("TitledBorder.aquaVariant"));
      }
      SwingTools.updateSwingResourceLanguage();
    } catch (Exception ex) {
      // Too bad keep current look and feel
    }
  }

  /**
   * Runs application once initialized.
   */
  private void run(String [] args) {
    final String furnitureLibraryName;
    if (args.length == 2
        && "-open".equals(args [0])) {
      furnitureLibraryName = args [1];
    } else {
      furnitureLibraryName = null;
    }

    final FurnitureLibrary furnitureLibrary = new FurnitureLibrary();
    final EditorController editorController = createEditorController(furnitureLibrary);
    final View editorView = editorController.getView();
    
    final JFrame furnitureFrame = new JFrame() {
        {
          if (editorView instanceof JRootPane) {
            setRootPane((JRootPane)editorView);
          } else {
            add((JComponent)editorView);
          }
        }
      };
    // Update frame image and title 
    Image [] frameImages = {new ImageIcon(FurnitureLibraryEditor.class.getResource("resources/frameIcon.png")).getImage(),
                            new ImageIcon(FurnitureLibraryEditor.class.getResource("resources/frameIcon32x32.png")).getImage()};
    try {
      // Call Java 1.6 setIconImages by reflection
      furnitureFrame.getClass().getMethod("setIconImages", List.class)
          .invoke(furnitureFrame, Arrays.asList(frameImages));
    } catch (Exception ex) {
      // Call setIconImage available in previous versions
      furnitureFrame.setIconImage(frameImages [0]);
    }
    furnitureFrame.setLocationByPlatform(true);
    furnitureFrame.pack();
    furnitureFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    furnitureFrame.setVisible(true);
    furnitureFrame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent ev) {
          editorController.exit();
        }
      });
    if (furnitureFrame.getJMenuBar() == null) {
      installAccelerators(furnitureFrame, editorController);
    }
    
    if (OperatingSystem.isMacOSX()) {
      MacOSXConfiguration.bindToApplicationMenu(editorController);
    }
  
    if (furnitureLibraryName != null) {
      editorController.open(furnitureLibraryName);
    }
    updateFrameTitle(furnitureFrame, furnitureLibrary, getUserPreferences(), getContentManager());
    // Update title when the name or the modified state of library changes
    furnitureLibrary.addPropertyChangeListener(FurnitureLibrary.Property.LOCATION, new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent ev) {
          updateFrameTitle(furnitureFrame, furnitureLibrary, getUserPreferences(), getContentManager());
        }
      });
    furnitureLibrary.addPropertyChangeListener(FurnitureLibrary.Property.MODIFIED, new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent ev) {
          updateFrameTitle(furnitureFrame, furnitureLibrary, getUserPreferences(), getContentManager());
        }
      });
  }

  /**
   * Changes the input map of furniture library view to ensure accelerators work even with no menu.
   */
  private void installAccelerators(JFrame furnitureFrame, 
                                   final EditorController furnitureLibraryController) {
    JComponent furnitureLibraryView = (JComponent)furnitureLibraryController.getView();
    InputMap inputMap = furnitureLibraryView.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = furnitureLibraryView.getActionMap();
    for (Object key : actionMap.allKeys()) {
      if (key instanceof EditorView.ActionType) {
        inputMap.put((KeyStroke)actionMap.get(key).getValue(Action.ACCELERATOR_KEY), key);
      }
    }
  }
  
  /**
   * Updates <code>frame</code> title from <code>furnitureLibrary</code> name.
   */
  private void updateFrameTitle(JFrame frame, 
                                FurnitureLibrary furnitureLibrary,
                                UserPreferences  preferences,
                                ContentManager   contentManager) {
    String furnitureLibraryLocation = furnitureLibrary.getLocation();
    String furnitureLibraryDisplayedName;
    if (furnitureLibraryLocation == null) {
      furnitureLibraryDisplayedName = preferences.getLocalizedString(FurnitureLibraryEditor.class, "untitled"); 
    } else {
      furnitureLibraryDisplayedName = contentManager.getPresentationName(
          furnitureLibraryLocation, ContentManager.ContentType.FURNITURE_LIBRARY);
    }
    
    String title = furnitureLibraryDisplayedName;
    if (OperatingSystem.isMacOSX()) {
      // Use black indicator in close icon for a modified library 
      Boolean furnitureLibraryModified = Boolean.valueOf(furnitureLibrary.isModified());
      // Set Mac OS X 10.4 property for backward compatibility
      frame.getRootPane().putClientProperty("windowModified", furnitureLibraryModified);
      
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        frame.getRootPane().putClientProperty("Window.documentModified", furnitureLibraryModified);
        
        if (furnitureLibraryLocation != null) {        
          File furnitureLibraryFile = new File(furnitureLibraryLocation);
          if (furnitureLibraryFile.exists()) {
            // Update the icon in window title bar for library files
            frame.getRootPane().putClientProperty("Window.documentFile", furnitureLibraryFile);
          }
        }
      }
    } else {
      title += " - " + getName(); 
      if (furnitureLibrary.isModified()) {
        title = "* " + title;
      }
    }
    frame.setTitle(title);
  }
  
  /**
   * Mac OS X configuration. The methods use in this class are invoked in a separated class 
   * because they exist only under Mac OS X. 
   */
  private static class MacOSXConfiguration {
    /**
     * Binds <code>controller</code> to Mac OS X application menu.
     */
    public static void bindToApplicationMenu(final EditorController controller) {
      Application macosxApplication = Application.getApplication();
      // Add a listener to Mac OS X application that will call controller methods
      macosxApplication.addApplicationListener(new ApplicationAdapter() {      
        @Override
        public void handleQuit(ApplicationEvent ev) { 
          controller.exit();
        }
        
        @Override
        public void handleAbout(ApplicationEvent ev) {
          controller.about();
          ev.setHandled(true);
        }

        @Override
        public void handlePreferences(ApplicationEvent ev) {
          controller.editPreferences();
          ev.setHandled(true);
        }
        
        @Override
        public void handleOpenFile(ApplicationEvent ev) {
          controller.open(ev.getFilename());
        }
        
        @Override
        public void handleReOpenApplication(ApplicationEvent ev) {
          SwingUtilities.getWindowAncestor((JComponent)controller.getView()).toFront();
        }
      });
      macosxApplication.setEnabledAboutMenu(true);
      macosxApplication.setEnabledPreferencesMenu(true);
      
      // Set application icon if program wasn't launch from bundle
      if (!"true".equalsIgnoreCase(System.getProperty("furniturelibraryeditor.bundle", "false"))) {
        try {
          Image icon = ImageIO.read(MacOSXConfiguration.class.getResource("swing/resources/aboutIcon.png"));
          macosxApplication.setDockIconImage(icon);
        } catch (NoSuchMethodError ex) {
          // Ignore icon change if setDockIconImage isn't available
        } catch (IOException ex) {
        }
      }
    }
  }
}
