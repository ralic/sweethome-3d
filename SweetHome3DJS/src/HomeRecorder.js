/*
 * HomeRecorder.js
 *
 * Sweet Home 3D, Copyright (c) 2015 Emmanuel PUYBARET / eTeks <info@eteks.com>
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

// Requires jszip.min.js
//          URLContent.js
//          big.js
//          SweetHome3D.js          

/**
 * Creates a home recorder able to read homes from URLs.
 * @constructor
 * @author Emmanuel Puybaret
 */
function HomeRecorder() {
}

HomeRecorder.READING_HOME = "Reading home";
HomeRecorder.PARSING_HOME = "Parsing home";

/**
 * Reads a home instance from its <code>url</code>.
 * @param url  URL of the read home
 * @param {zipReady, zipError, progression} observer  The callbacks used to follow the reading of the home 
 */
HomeRecorder.prototype.readHome = function(url, observer) {
  observer.progression(HomeRecorder.READING_HOME, url, 0);
  // XML entry where home data is stored is Home.xml, except if url starts with jar: to specify another entry name 
  var homeEntryName = "Home.xml";
  if (url.indexOf("jar:") === 0) {
    var entrySeparatorIndex = url.indexOf("!/");
    homeEntryName = url.substring(entrySeparatorIndex + 2);
    url = url.substring(4, entrySeparatorIndex);
  }
  var recorder = this;
  ZIPTools.getZIP(url,
      {
        zipReady : function(zip) {
          try {
            var homeXmlEntry = zip.file(homeEntryName);
            if (homeXmlEntry !== null) {
              recorder.parseHomeXMLEntry(zip.file(homeEntryName), zip, url, observer);
            } else {
              this.zipError("No " + homeEntryName + " entry in " + url);
            }
          } catch (ex) {
            this.zipError(ex);
          }
        },
        zipError : function(error) {
          if (observer.homeError !== undefined) {
            observer.homeError(error);
          }
        },
        progression : function(part, info, percentage) {
          if (observer.progression !== undefined) {
            observer.progression(HomeRecorder.READING_HOME, url, percentage);
          }
        }
      });
}

/**
 * Parses the content of the given entry to create the home object it contains. 
 * @private
 */
HomeRecorder.prototype.parseHomeXMLEntry = function(homeXmlEntry, zip, zipUrl, observer) {
  var xmlContent = homeXmlEntry.asText();
  observer.progression(HomeRecorder.READING_HOME, homeXmlEntry.name, 1);
  
  observer.progression(HomeRecorder.PARSING_HOME, homeXmlEntry.name, 0);
  
  var handler = new HomeXMLHandler();
  // The handler needs the zip URL for creating the right content URL (see HomeXMLHandler#parseContent)
  handler.homeUrl = zipUrl;
  var saxParser = new SAXParser(handler, handler, handler, handler, handler);
  try {
    // Replace ' by " because SAXParser converts &apos; entities to ' in attributes value too early
    xmlContent = xmlContent.replace(/\'/g, '"');
    saxParser.parseString(xmlContent);
    observer.homeLoaded(handler.getHome());
  } catch (ex) {
    observer.homeError(ex);
  }
  
  observer.progression(HomeRecorder.PARSING_HOME, homeXmlEntry.name, 1);
}

