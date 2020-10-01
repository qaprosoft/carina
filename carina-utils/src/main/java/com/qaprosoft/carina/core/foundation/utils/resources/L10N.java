/*******************************************************************************
 * Copyright 2013-2020 QaProSoft (http://www.qaprosoft.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.qaprosoft.carina.core.foundation.utils.resources;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;

/*
 * http://maven.apache.org/surefire/maven-surefire-plugin/examples/class-loading.html
 * Need to set useSystemClassLoader=false for maven surefire plugin to receive access to classloader L10N files on CI
 * <plugin>
 * <groupId>org.apache.maven.plugins</groupId>
 * <artifactId>maven-surefire-plugin</artifactId>
 * <version>3.0.0-M4</version>
 * <configuration>
 * <useSystemClassLoader>false</useSystemClassLoader>
 * </configuration>
 */

public class L10N {
    private static final Logger LOGGER = Logger.getLogger(L10N.class);

    private static ArrayList<ResourceBundle> resBoundles = new ArrayList<ResourceBundle>();

    public static void init() {
        if (!Configuration.getBoolean(Parameter.ENABLE_L10N)) {
            return;
        }

        List<Locale> locales = LocaleReader.init(Configuration
                .get(Parameter.LOCALE));

        List<String> loadedResources = new ArrayList<String>();
        
        try {

            for (URL u : Resources.getResourceURLs(new ResourceURLFilter() {
                public @Override boolean accept(URL u) {
                    String s = u.getPath();
                    boolean contains = s.contains(SpecialKeywords.L10N.toLowerCase());
                    if (contains) {
                        LOGGER.debug("L10N: file URL: " + u);
                    }
                    return contains;
                }
            })) {
                LOGGER.debug(String.format(
                        "Analyzing '%s' L10N resource for loading...", u));
                /*
                 * 2. Exclude localization resources like such L10N.messages_de, L10N.messages_ptBR etc...
                 * Note: we ignore valid resources if 3rd or 5th char from the end is "_". As designed :(
                 */
                String fileName = FilenameUtils.getBaseName(u.getPath());

                if (u.getPath().endsWith("L10N.class")
                        || u.getPath().endsWith("L10N$1.class")) {
                    // separate conditions to support core JUnit tests
                    continue;
                }

                if (isLocalizedResource(fileName)) {
                    LOGGER.debug(String
                            .format("'%s' resource IGNORED as it looks like localized resource!",
                                    fileName));
                    continue;
                }
                /*
                 * convert "file: <REPO>\target\classes\L10N\messages.properties" to "L10N.messages"
                 */
                String filePath = FilenameUtils.getPath(u.getPath());
                int index = filePath.indexOf(SpecialKeywords.L10N.toLowerCase());

                if (index == -1) {
                    LOGGER.warn("Unable to find L10N pattern for " + u.getPath() + " resource!");
                    continue;
                }

                String resource = filePath.substring(
                        filePath.indexOf(SpecialKeywords.L10N.toLowerCase()))
                        .replaceAll("/", ".")
                        + fileName;

                if (!loadedResources.contains(resource)) {
                    loadedResources.add(resource);
                    try {
                        LOGGER.debug(String.format("Adding '%s' resource...",
                                resource));
                        for (Locale locale : locales) {
                            resBoundles.add(ResourceBundle.getBundle(resource, locale));
                        }
                        LOGGER.debug(String
                                .format("Resource '%s' added.", resource));
                    } catch (MissingResourceException e) {
                        LOGGER.debug(e.getMessage(), e);
                    }
                } else {
                    LOGGER.debug(String
                            .format("Requested resource '%s' is already loaded into the ResourceBundle!",
                                    resource));
                }
            }
            LOGGER.debug("init: L10N bundle size: " + resBoundles.size());
        } catch (ClassCastException e) {
            // #910 starting from java 9 "Base ClassLoader No Longer from URLClassLoader"
            LOGGER.error("Unable to use L10N resources yet using Java 9+!");
        }
    }

    private static boolean isLocalizedResource(String fileName) {
        if (fileName.lastIndexOf("_") == fileName.length() - 3) {
            String countryCode = fileName.substring(fileName.length() - 2);
            if (countryCode.equals(countryCode.toUpperCase())) {
                boolean isCountryCode = false;
                for (String code : Locale.getISOCountries()) {
                    if (countryCode.equals(code)) {
                        // Filename contains country code
                        isCountryCode = true;
                        break;
                    }
                }
                if (isCountryCode) {
                    String fileNameWithoutCountryCode = fileName.substring(0, fileName.length() - 3);
                    if (fileNameWithoutCountryCode.lastIndexOf("_") == fileNameWithoutCountryCode.length() - 3) {
                        String languageCode = fileNameWithoutCountryCode.substring(fileNameWithoutCountryCode.length() - 2);
                        for (String code : Locale.getISOLanguages()) {
                            if (languageCode.equals(code)) {
                                LOGGER.debug("File " + fileName + " contains language and country codes!");
                                return true;
                            }
                        }
                    }
                }
            } else {
                for (String code : Locale.getISOLanguages()) {
                    if (countryCode.equals(code)) {
                        LOGGER.debug("File " + fileName + " contains language code!");
                        return true;
                    }
                }
            }
        }
        LOGGER.debug("File " + fileName + " is not localized resource!");
        return false;
    }

    /**
     * get Default Locale
     * 
     * @return Locale
     */
    public static Locale getDefaultLocale() {
        List<Locale> locales = LocaleReader.init(Configuration
                .get(Parameter.LOCALE));

        if (locales.size() == 0) {
            throw new RuntimeException("Undefined default locale specified! Review 'locale' setting in _config.properties.");
        }

        return locales.get(0);
    }

    /**
     * getText by key for default locale.
     * 
     * @param key
     *            - String
     * 
     * @return String
     */
    public static String getText(String key) {
        return getText(key, getDefaultLocale());
    }

    /**
     * getText for specified locale and key.
     * 
     * @param key
     *            - String
     * @param locale
     *            - Locale
     * @return String
     */
    public static String getText(String key, Locale locale) {
        LOGGER.debug("getText: L10N bundle size: " + resBoundles.size());
        Iterator<ResourceBundle> iter = resBoundles.iterator();
        while (iter.hasNext()) {
            ResourceBundle bundle = iter.next();
            try {
                String value = bundle.getString(key);
				if (isUTF8()) {
					try {
						LOGGER.debug("converting key '" + value + "' to UTF-8");
						value = new String(value.getBytes("ISO-8859-1"), "UTF-8");
					} catch (UnsupportedEncodingException er) {
						LOGGER.debug("Error: ", er);
					}
				}
                LOGGER.debug("Looking for value for locale:'"
                        + locale.toString()
                        + "' current iteration locale is: '"
                        + bundle.getLocale().toString() + "'.");
                if (bundle.getLocale().toString().equals(locale.toString())) {
                    LOGGER.debug("Found locale:'" + locale.toString()
                            + "' and value is '" + value + "'.");
                    return value;
                }
            } catch (MissingResourceException e) {
                // do nothing
            }
        }
        return key;
    }

    /*
     * This method helps when translating strings that have single quote or other special characters that get omitted.
     */
    public static String formatString(String resource, String... parameters) {
        for (int i = 0; i < parameters.length; i++) {
            resource = resource.replace("{" + i + "}", parameters[i]);
            LOGGER.debug("Localized string value is: " + resource);
        }
        return resource;
    }

    /*
     * Make sure you remove the single quotes around %s in xpath as string
     * returned will either have it added for you or single quote won't be
     * added as concat() doesn't need them.
     */
    public static String generateConcatForXPath(String xpathString) {
        String returnString = "";
        String searchString = xpathString;
        char[] quoteChars = new char[] { '\'', '"' };

        int quotePos = StringUtils.indexOfAny(searchString, quoteChars);
        if (quotePos == -1) {
            returnString = "'" + searchString + "'";
        } else {
            returnString = "concat(";
            LOGGER.debug("Current concatenation: " + returnString);
            while (quotePos != -1) {
                String subString = searchString.substring(0, quotePos);
                returnString += "'" + subString + "', ";
                LOGGER.debug("Current concatenation: " + returnString);
                if (searchString.substring(quotePos, quotePos + 1).equals("'")) {
                    returnString += "\"'\", ";
                    LOGGER.debug("Current concatenation: " + returnString);
                } else {
                    returnString += "'\"', ";
                    LOGGER.debug("Current concatenation: " + returnString);
                }
                searchString = searchString.substring(quotePos + 1,
                        searchString.length());
                quotePos = StringUtils.indexOfAny(searchString, quoteChars);
            }
            returnString += "'" + searchString + "')";
            LOGGER.debug("Concatenation result: " + returnString);
        }
        return returnString;
    }

    /**
     * get Localization Encoding from properties.
     *
     * @return boolean
     */
    public static boolean isUTF8() {
        String encoding = "ISO-8859-1";
        try {
            encoding = Configuration.get(Parameter.L10N_ENCODING);
        } catch (Exception e) {
            LOGGER.debug("There is no l10n_encoding parameter in config properties.");
        }
        LOGGER.debug("Will use L10N encoding: " + encoding);
        return encoding.toUpperCase().contains("UTF-8");
    }
}
