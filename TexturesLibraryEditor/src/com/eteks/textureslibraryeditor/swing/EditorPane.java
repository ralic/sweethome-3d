/*
 * EditorPane.java 11 sept 2012
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.ControllerAction;
import com.eteks.sweethome3d.swing.ResourceAction;
import com.eteks.sweethome3d.swing.UnfocusableToolBar;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.HomeView.SaveAnswer;
import com.eteks.textureslibraryeditor.model.TexturesLibrary;
import com.eteks.textureslibraryeditor.model.TexturesLibraryUserPreferences;
import com.eteks.textureslibraryeditor.viewcontroller.EditorController;
import com.eteks.textureslibraryeditor.viewcontroller.EditorView;
import com.eteks.textureslibraryeditor.viewcontroller.TexturesLibraryController;

/**
 * The main pane that displays Textures Library Editor. 
 * @author Emmanuel Puybaret
 */
public class EditorPane extends JRootPane implements EditorView {
  private final TexturesLibraryUserPreferences preferences;

  public EditorPane(TexturesLibrary texturesLibrary,
                    TexturesLibraryUserPreferences preferences,
                    EditorController controller) {
    this.preferences = preferences;
    createActions(controller, preferences);
    
    // Layout components
    JComponent texturesLibraryView = (JComponent)controller.getTexturesLibraryController().getView();
    JScrollPane texturesLibraryScrollPane = new JScrollPane(texturesLibraryView);
    JViewport viewport = texturesLibraryScrollPane.getViewport();
    viewport.setTransferHandler(texturesLibraryView.getTransferHandler());    
    JPopupMenu texturesLibraryPopupMenu = createTexturesLibraryPopupMenu(preferences, controller);
    texturesLibraryView.setComponentPopupMenu(texturesLibraryPopupMenu);
    viewport.setComponentPopupMenu(texturesLibraryPopupMenu);
    getContentPane().add(texturesLibraryScrollPane);
    getContentPane().add(createToolBar(preferences, controller), BorderLayout.NORTH);
    
    // Map Enter accelerator of textures library to textures modification action
    texturesLibraryView.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), ActionType.MODIFY_TEXTURES);
    texturesLibraryView.getActionMap().put(ActionType.MODIFY_TEXTURES, getActionMap().get(ActionType.MODIFY_TEXTURES));
    controller.getTexturesLibraryController().addSelectionListener(new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          getActionMap().get(ActionType.DELETE).setEnabled(!ev.getSelectedItems().isEmpty());
          getActionMap().get(ActionType.MODIFY_TEXTURES).setEnabled(!ev.getSelectedItems().isEmpty());
        }
      });
  }

  private void createActions(EditorController controller, 
                             UserPreferences preferences) {
    ActionMap actionMap = getActionMap();
    try {
      actionMap.put(ActionType.NEW_LIBRARY, new ControllerAction(
          preferences, EditorPane.class, ActionType.NEW_LIBRARY.name(), true, 
          controller, "newLibrary"));
      actionMap.put(ActionType.OPEN, new ControllerAction(
          preferences, EditorPane.class, ActionType.OPEN.name(), true, 
          controller, "open"));
      actionMap.put(ActionType.MERGE, new ControllerAction(
          preferences, EditorPane.class, ActionType.MERGE.name(), true, 
          controller, "merge"));
      actionMap.put(ActionType.SAVE, new ControllerAction(
          preferences, EditorPane.class, ActionType.SAVE.name(), true, 
          controller, "save"));
      actionMap.put(ActionType.SAVE_AS, new ControllerAction(
          preferences, EditorPane.class, ActionType.SAVE_AS.name(), true, 
          controller, "saveAs"));
      actionMap.put(ActionType.PREFERENCES, new ControllerAction(
          preferences, EditorPane.class, ActionType.PREFERENCES.name(), true, 
          controller, "editPreferences"));
      actionMap.put(ActionType.EXIT, new ControllerAction(
          preferences, EditorPane.class, ActionType.EXIT.name(), true, 
          controller, "exit"));
      TexturesLibraryController texturesLibraryController = controller.getTexturesLibraryController();
      actionMap.put(ActionType.IMPORT_TEXTURES, new ControllerAction(
          preferences, EditorPane.class, ActionType.IMPORT_TEXTURES.name(), true, 
          texturesLibraryController, "importTextures"));
      boolean selectionEmpty = texturesLibraryController.getSelectedTextures().isEmpty();
      actionMap.put(ActionType.MODIFY_TEXTURES, new ControllerAction(
          preferences, EditorPane.class, ActionType.MODIFY_TEXTURES.name(), !selectionEmpty, 
          texturesLibraryController, "modifySelectedTextures"));
      actionMap.put(ActionType.DELETE, new ControllerAction(
          preferences, EditorPane.class, ActionType.DELETE.name(), !selectionEmpty, 
          texturesLibraryController, "deleteSelectedTextures"));
      actionMap.put(ActionType.SELECT_ALL, new ControllerAction(
          preferences, EditorPane.class, ActionType.SELECT_ALL.name(), true, 
          texturesLibraryController, "selectAll"));
      actionMap.put(ActionType.ABOUT, new ControllerAction(
          preferences, EditorPane.class, ActionType.ABOUT.name(), true, 
          controller, "about"));
    } catch (NoSuchMethodException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  /**
   * Returns the tool bar of this pane.
   */
  private JToolBar createToolBar(UserPreferences preferences,
                                 EditorController controller) {
    JToolBar toolBar = new UnfocusableToolBar();
    toolBar.setFloatable(false);
    addActionToToolBar(EditorView.ActionType.NEW_LIBRARY, toolBar);
    addActionToToolBar(EditorView.ActionType.OPEN, toolBar);
    addActionToToolBar(EditorView.ActionType.MERGE, toolBar);
    addActionToToolBar(EditorView.ActionType.SAVE, toolBar);
    addActionToToolBar(EditorView.ActionType.SAVE_AS, toolBar);
    toolBar.add(Box.createRigidArea(new Dimension(2, 2)));
    addActionToToolBar(EditorView.ActionType.PREFERENCES, toolBar);
    toolBar.addSeparator();
    addActionToToolBar(ActionType.IMPORT_TEXTURES, toolBar);
    addActionToToolBar(ActionType.MODIFY_TEXTURES, toolBar);
    addActionToToolBar(ActionType.DELETE, toolBar);
    toolBar.add(Box.createRigidArea(new Dimension(2, 2)));
    JComponent texturesLanguageView = (JComponent)controller.getTexturesLanguageController().getView();
    texturesLanguageView.setMaximumSize(texturesLanguageView.getPreferredSize());
    toolBar.add(texturesLanguageView);
    toolBar.addSeparator();
    addActionToToolBar(EditorView.ActionType.ABOUT, toolBar);
    return toolBar;
  }

  /**
   * Adds to tool bar the button matching the given <code>actionType</code>. 
   */
  private void addActionToToolBar(ActionType actionType,
                                  JToolBar toolBar) {
    Action action = new ResourceAction.ToolBarAction(getActionMap().get(actionType));
    if (OperatingSystem.isMacOSXLeopardOrSuperior() && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
      // Add a button with higher insets to ensure the top and bottom of segmented buttons are correctly drawn 
      toolBar.add(new JButton(new ResourceAction.ToolBarAction(action)) {
          @Override
          public Insets getInsets() {
            Insets insets = super.getInsets();
            insets.top += 3;
            insets.bottom += 3;
            return insets;
          }
        });
    } else {
      toolBar.add(new JButton(new ResourceAction.ToolBarAction(action)));
    }
  }

  /**
   * Returns the popup menu of the textures library view.
   */
  private JPopupMenu createTexturesLibraryPopupMenu(UserPreferences preferences,
                                                     EditorController controller) {
    JPopupMenu popupMenu = new JPopupMenu();
    ActionMap actionMap = getActionMap();
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(EditorView.ActionType.NEW_LIBRARY)));
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(EditorView.ActionType.OPEN)));
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(EditorView.ActionType.SAVE)));
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(EditorView.ActionType.SAVE_AS)));
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(EditorView.ActionType.PREFERENCES)));
    popupMenu.addSeparator();
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(ActionType.IMPORT_TEXTURES)));
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(ActionType.MODIFY_TEXTURES)));
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(ActionType.DELETE)));
    popupMenu.add(new ResourceAction.MenuItemAction(actionMap.get(ActionType.SELECT_ALL)));
    return popupMenu;
  }

  /**
   * Displays the error message in parameter.
   */
  public void showError(String title, String message) {
    JOptionPane.showMessageDialog(SwingUtilities.getRootPane(this), message, title, JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Displays a dialog that lets user choose whether he wants to save before closing or not.
   * @return {@link com.eteks.sweethome3d.viewcontroller.HomeView.SaveAnswer#SAVE} 
   * if the user chose to save textures library,
   * {@link com.eteks.sweethome3d.viewcontroller.HomeView.SaveAnswer#DO_NOT_SAVE} 
   * if he doesn't want to save textures library,
   * or {@link com.eteks.sweethome3d.viewcontroller.HomeView.SaveAnswer#CANCEL} 
   * if he doesn't want to continue current operation.
   */
  public SaveAnswer confirmSave(String homeName) {
    // Retrieve displayed text in buttons and message
    String message;
    if (homeName != null) {
      message = this.preferences.getLocalizedString(EditorPane.class, "confirmSave.message", 
          "\"" + homeName + "\"");
    } else {
      message = this.preferences.getLocalizedString(EditorPane.class, "confirmSave.message", "");
    }
    String title = this.preferences.getLocalizedString(EditorPane.class, "confirmSave.title");
    String save = this.preferences.getLocalizedString(EditorPane.class, "confirmSave.save");
    String doNotSave = this.preferences.getLocalizedString(EditorPane.class, "confirmSave.doNotSave");
    String cancel = this.preferences.getLocalizedString(EditorPane.class, "confirmSave.cancel");

    switch (JOptionPane.showOptionDialog(SwingUtilities.getRootPane(this), message, title, 
        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, new Object [] {save, doNotSave, cancel}, save)) {
      // Convert showOptionDialog answer to SaveAnswer enum constants
      case JOptionPane.YES_OPTION:
        return SaveAnswer.SAVE;
      case JOptionPane.NO_OPTION:
        return SaveAnswer.DO_NOT_SAVE;
      default : return SaveAnswer.CANCEL;
    }
  }

  /**
   * Displays an about dialog.
   */
  public void showAboutDialog() {
    String messageFormat = this.preferences.getLocalizedString(EditorPane.class, "about.message");
    String version = this.preferences.getLocalizedString(EditorPane.class, "about.version");
    String message = String.format(messageFormat, version, System.getProperty("java.version"));    
    // Use an uneditable editor pane to let user select text in dialog
    JEditorPane messagePane = new JEditorPane("text/html", message);
    messagePane.setOpaque(false);
    messagePane.setEditable(false);
    String title = this.preferences.getLocalizedString(EditorPane.class, "about.title");
    Icon   icon  = new ImageIcon(EditorPane.class.getResource(
        this.preferences.getLocalizedString(EditorPane.class, "about.icon")));
    JOptionPane.showMessageDialog(SwingUtilities.getRootPane(this), 
        messagePane, title, JOptionPane.INFORMATION_MESSAGE, icon);
  }

  /**
   * Runs the given <code>runnable</code> in EDT.
   */
  public void invokeLater(Runnable runnable) {
    EventQueue.invokeLater(runnable);
  }
}
