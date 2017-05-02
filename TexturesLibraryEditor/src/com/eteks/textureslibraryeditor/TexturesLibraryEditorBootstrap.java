/*
 * TexturesLibraryEditorBootstrap.java 11 sept. 2012
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
package com.eteks.textureslibraryeditor;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.eteks.sweethome3d.tools.ExtensionsClassLoader;

/**
 * This bootstrap class loads Textures library editor classes from jars in classpath 
 * or from extension jars stored as resources.
 * @author Emmanuel Puybaret
 */
public class TexturesLibraryEditorBootstrap {
  public static void main(String [] args) throws MalformedURLException, IllegalAccessException, 
      InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
    Class<?> texturesLibraryEditorBootstrapClass = TexturesLibraryEditorBootstrap.class;
    List<String> extensionJarsAndDlls = new ArrayList<String>(Arrays.asList(new String [] {
        "jnlp.jar"}));
    
    String [] applicationPackages = {
        "com.eteks.sweethome3d",
        "com.eteks.textureslibraryeditor"};
    ClassLoader java3DClassLoader = new ExtensionsClassLoader(
        texturesLibraryEditorBootstrapClass.getClassLoader(), 
        texturesLibraryEditorBootstrapClass.getProtectionDomain(),
        extensionJarsAndDlls.toArray(new String [extensionJarsAndDlls.size()]), applicationPackages);  
    
    String applicationClassName = "com.eteks.textureslibraryeditor.TexturesLibraryEditor";
    Class<?> applicationClass = java3DClassLoader.loadClass(applicationClassName);
    Method applicationClassMain = 
        applicationClass.getMethod("main", Array.newInstance(String.class, 0).getClass());
    // Call application class main method with reflection
    applicationClassMain.invoke(null, new Object [] {args});
  }
}
