SWEET HOME 3D JS v 2.0
----------------------

This archive contains source code of Sweet Home 3D JS, an HTML5 3D viewer 
for Sweet Home 3D files exported with Export to HTML5 plug-in.

You may also download this source code with the following CVS command:

cvs -z3 -d:pserver:anonymous@sweethome3d.cvs.sourceforge.net:/cvsroot/sweethome3d co -P -r V_2_0 SweetHome3DJS


As this project depends on Sweet Home 3D, download and install also Sweet Home 3D source code from 
http://prdownloads.sourceforge.net/sweethome3d/SweetHome3D-5.4-src.zip 
or with the following CVS command: 

cvs -z3 -d:pserver:anonymous@sweethome3d.cvs.sourceforge.net:/cvsroot/sweethome3d co -P -r V_5_4 SweetHome3D


HOW TO USE THIS SOURCE CODE
---------------------------

Javascript code of this project is compatible with HTML5 / WebGL, and the directory where
you find this README.TXT file contains all the information required to run the code.

You may execute tests found in the test folder of this archive with any compatible browser. 
Other examples can be found at http://www.sweethome3d.com/gallery.jsp 

If you want to build Sweet Home 3D JS with Ant, please install last JDK 8 or greater and 
Ant available at http://ant.apache.org/, then run ant command in the directory where
you uncompressed this archive.
The default target provided by build.xml Ant file concatenates .js files by license type 
then minifies them to dist folder. Read build.xml Ant file for more information about 
other available targets. 


Read LICENSE.TXT file for more details about licenses applicable to this software
and included materials developed by third parties.
    
    
Sweet Home 3D, Copyright (c) 2016-2017 Emmanuel PUYBARET / eTeks. 
Distributed under GNU General Public License    