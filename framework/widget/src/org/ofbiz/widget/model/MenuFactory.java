/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.widget.model;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Widget Library - Menu factory class
 * <p>
 * SCIPIO: now also as instance
 */
@SuppressWarnings("serial")
public class MenuFactory extends WidgetFactory {

    //private static final Debug.OfbizLogger module = Debug.getOfbizLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());

    public static final UtilCache<String, Map<String, ModelMenu>> menuWebappCache = UtilCache.createUtilCache("widget.menu.webappResource", 0, 0, false);
    public static final UtilCache<String, Map<String, ModelMenu>> menuLocationCache = UtilCache.createUtilCache("widget.menu.locationResource", 0, 0, false);

    public static MenuFactory getMenuFactory() { // SCIPIO: new
        return menuFactory;
    }
    
    public static ModelMenu getMenuFromWebappContext(String resourceName, String menuName, HttpServletRequest request)
            throws IOException, SAXException, ParserConfigurationException {
        String webappName = UtilHttp.getApplicationName(request);
        String cacheKey = webappName + "::" + resourceName;

        Map<String, ModelMenu> modelMenuMap = menuWebappCache.get(cacheKey);
        if (modelMenuMap == null) {
            synchronized (MenuFactory.class) {
                modelMenuMap = menuWebappCache.get(cacheKey);
                if (modelMenuMap == null) {
                    ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");

                    URL menuFileUrl = servletContext.getResource(resourceName);
                    Document menuFileDoc = UtilXml.readXmlDocument(menuFileUrl, true, true);
                    // SCIPIO: New: Save original location as user data in Document
                    if (menuFileDoc != null) {
                        WidgetDocumentInfo.retrieveAlways(menuFileDoc).setResourceLocation(resourceName);
                    }
                    modelMenuMap = readMenuDocument(menuFileDoc, cacheKey);
                    menuWebappCache.put(cacheKey, modelMenuMap);
                }
            }
        }

        if (UtilValidate.isEmpty(modelMenuMap)) {
            throw new IllegalArgumentException("Could not find menu file in webapp resource [" + resourceName + "] in the webapp [" + webappName + "]");
        }

        ModelMenu modelMenu = modelMenuMap.get(menuName);
        if (modelMenu == null) {
            throw new IllegalArgumentException("Could not find menu with name [" + menuName + "] in webapp resource [" + resourceName + "] in the webapp [" + webappName + "]");
        }
        return modelMenu;
    }

    public static Map<String, ModelMenu> readMenuDocument(Document menuFileDoc, String menuLocation) {
        Map<String, ModelMenu> modelMenuMap = new HashMap<String, ModelMenu>();
        if (menuFileDoc != null) {
            // read document and construct ModelMenu for each menu element
            Element rootElement = menuFileDoc.getDocumentElement();
            for (Element menuElement: UtilXml.childElementList(rootElement, "menu")){
                ModelMenu modelMenu = new ModelMenu(menuElement, menuLocation);
                modelMenuMap.put(modelMenu.getName(), modelMenu);
            }
         }
        return modelMenuMap;
    }

    /**
     * Gets widget from location or exception. 
     * <p>
     * SCIPIO: now delegating.
     */
    public static ModelMenu getMenuFromLocation(String resourceName, String menuName) throws IOException, SAXException, ParserConfigurationException {
        ModelMenu modelMenu = getMenuFromLocationOrNull(resourceName, menuName);
        if (modelMenu == null) {
            throw new IllegalArgumentException("Could not find menu with name [" + menuName + "] in location [" + resourceName + "]");
        }
        return modelMenu;
    }
    
    /**
     * SCIPIO: Gets widget from location or null if name not within the location.
     */
    public static ModelMenu getMenuFromLocationOrNull(String resourceName, String menuName) throws IOException, SAXException, ParserConfigurationException {
        Map<String, ModelMenu> modelMenuMap = menuLocationCache.get(resourceName);
        if (modelMenuMap == null) {
            synchronized (MenuFactory.class) {
                modelMenuMap = menuLocationCache.get(resourceName);
                if (modelMenuMap == null) {
                    URL menuFileUrl = FlexibleLocation.resolveLocation(resourceName);
                    Document menuFileDoc = UtilXml.readXmlDocument(menuFileUrl, true, true);
                    // SCIPIO: New: Save original location as user data in Document
                    if (menuFileDoc != null) {
                        WidgetDocumentInfo.retrieveAlways(menuFileDoc).setResourceLocation(resourceName);
                    }
                    modelMenuMap = readMenuDocument(menuFileDoc, resourceName);
                    menuLocationCache.put(resourceName, modelMenuMap);
                }
            }
        }

        if (UtilValidate.isEmpty(modelMenuMap)) {
            throw new IllegalArgumentException("Could not find menu file in location [" + resourceName + "]");
        }

        ModelMenu modelMenu = modelMenuMap.get(menuName);
        return modelMenu;
    }

    @Override
    public ModelMenu getWidgetFromLocation(ModelLocation modelLoc) throws IOException, IllegalArgumentException {
        try {
            return getMenuFromLocation(modelLoc.getResource(), modelLoc.getName());
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public ModelMenu getWidgetFromLocationOrNull(ModelLocation modelLoc) throws IOException {
        try {
            return getMenuFromLocationOrNull(modelLoc.getResource(), modelLoc.getName());
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }
}
