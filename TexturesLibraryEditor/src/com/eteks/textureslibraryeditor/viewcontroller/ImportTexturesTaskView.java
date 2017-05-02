/*
 * ImportTexturesTaskView.java 12 sept. 2012
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
package com.eteks.textureslibraryeditor.viewcontroller;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskView;

/**
 * A threaded task view able to import textures and show importation progress.
 * @author Emmanuel Puybaret
 */
public interface ImportTexturesTaskView extends ThreadedTaskView {
  /**
   * Sets the status of the progress shown by this panel as indeterminate.
   * This method may be called from an other thread than the toolkit event thread.  
   */
  public void setIndeterminateProgress();
  
  /**
   * Sets the current value of the progress that may display this panel.  
   * This method may be called from an other thread than the toolkit event thread.  
   */
  public void setProgress(final int value, final int minimum,  final int maximum);

  /**
   * Returns the catalog of textures matching <code>textureContent</code> texture 
   * or <code>null</code> if the content doesn't contain a texture at a supported format.
   */
  public CatalogTexture readTexture(Content textureContent) throws InterruptedException;
}
