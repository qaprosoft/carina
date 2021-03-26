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
package com.qaprosoft.carina.core.foundation.webdriver.core.capability;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qaprosoft.carina.browsermobproxy.ProxyPool;
import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.report.ReportContext;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.R;
import com.qaprosoft.carina.proxy.SystemProxy;

public abstract class AbstractCapabilities {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static ArrayList<Integer> firefoxPorts = new ArrayList<Integer>();

    public abstract DesiredCapabilities getCapability(String testName);

    protected DesiredCapabilities initBaseCapabilities(DesiredCapabilities capabilities, String browser, String testName) {

        // TODO: should use provide capabilities as an argument for getPlatform call below?
        String platform = Configuration.getPlatform();
        if (!platform.equals("*")) {
            capabilities.setPlatform(Platform.extractFromSysProperty(platform));
        }

        capabilities.setBrowserName(browser);

        // Selenium 3.4 doesn't support '*'. Only explicit or empty browser version should be provided
        String browserVersion = Configuration.get(Parameter.BROWSER_VERSION);
        if ("*".equalsIgnoreCase(browserVersion)) {
            browserVersion = "";
        }
//        capabilities.setVersion(browserVersion);
        capabilities.setCapability("name", testName);

        Proxy proxy = setupProxy();
        if (proxy != null) {
            capabilities.setCapability(CapabilityType.PROXY, proxy);
        }

        // add capabilities based on dynamic _config.properties variables
        capabilities = initCapabilities(capabilities);
        
        return capabilities;
    }

    protected DesiredCapabilities initCapabilities(DesiredCapabilities capabilities) {
        // read all properties which starts from "capabilities.*" prefix and add them into desired capabilities.
        final String prefix = SpecialKeywords.CAPABILITIES + ".";
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, String> capabilitiesMap = new HashMap(R.CONFIG.getProperties());
        for (Map.Entry<String, String> entry : capabilitiesMap.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(prefix)) {
                String value = R.CONFIG.get(entry.getKey());
                if (!value.isEmpty()) {
                    String cap = entry.getKey().replaceAll(prefix, "");
                    if ("false".equalsIgnoreCase(value)) {
                        capabilities.setCapability(cap, false);
                    } else if ("true".equalsIgnoreCase(value)) {
                        capabilities.setCapability(cap, true);
                    } else {
                        capabilities.setCapability(cap, value);
                    }
                }
            }
        }
        capabilities.setCapability("carinaTestRunId", SpecialKeywords.TEST_RUN_ID);
        
        //TODO: [VD] reorganize in the same way Firefox profiles args/options if any and review other browsers
        // support customization for Chrome args and options

        // for pc we may set browserName through Desired capabilities in our Test with a help of a method initBaseCapabilities,
        // so we don't want to override with value from config
        String browser;
        if (capabilities.getBrowserName() != null && capabilities.getBrowserName().length() > 0) {
            browser = capabilities.getBrowserName();
        } else {
            browser = Configuration.getBrowser();
        }

        if (BrowserType.FIREFOX.equalsIgnoreCase(browser)) {
            capabilities = addFirefoxOptions(capabilities);
        } else if (BrowserType.CHROME.equalsIgnoreCase(browser)) {
            capabilities = addChromeOptions(capabilities);
        }

        if (Configuration.getBoolean(Parameter.HEADLESS)) {
            if (BrowserType.FIREFOX.equalsIgnoreCase(browser)
                    || BrowserType.CHROME.equalsIgnoreCase(browser)
                    && Configuration.getDriverType().equalsIgnoreCase(SpecialKeywords.DESKTOP)) {
                LOGGER.info("Browser will be started in headless mode. VNC and Video will be disabled.");
                capabilities.setCapability("enableVNC", false);
                capabilities.setCapability("enableVideo", false);
            } else {
                LOGGER.error(String.format("Headless mode isn't supported by %s browser / platform.", browser));
            }
        }

        return capabilities;
    }

    protected Proxy setupProxy() {
        ProxyPool.setupBrowserMobProxy();
        SystemProxy.setupProxy();

        String proxyHost = Configuration.get(Parameter.PROXY_HOST);
        String proxyPort = Configuration.get(Parameter.PROXY_PORT);
        String noProxy = Configuration.get(Parameter.NO_PROXY);
        
        if (Configuration.get(Parameter.BROWSERMOB_PROXY).equals("true")) {
            proxyPort = Integer.toString(ProxyPool.getProxyPortFromThread());
        }
        List<String> protocols = Arrays.asList(Configuration.get(Parameter.PROXY_PROTOCOLS).split("[\\s,]+"));

        //TODO: test removal comparing with null
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {

            org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
            String proxyAddress = String.format("%s:%s", proxyHost, proxyPort);

            if (protocols.contains("http")) {
                LOGGER.info(String.format("Http proxy will be set: %s:%s", proxyHost, proxyPort));
                proxy.setHttpProxy(proxyAddress);
            }

            if (protocols.contains("https")) {
                LOGGER.info(String.format("Https proxy will be set: %s:%s", proxyHost, proxyPort));
                proxy.setSslProxy(proxyAddress);
            }

            if (protocols.contains("ftp")) {
                LOGGER.info(String.format("FTP proxy will be set: %s:%s", proxyHost, proxyPort));
                proxy.setFtpProxy(proxyAddress);
            }

            if (protocols.contains("socks")) {
                LOGGER.info(String.format("Socks proxy will be set: %s:%s", proxyHost, proxyPort));
                proxy.setSocksProxy(proxyAddress);
            }
            
            if (!noProxy.isEmpty()) {
                proxy.setNoProxy(noProxy);
            }

            return proxy;
        }

        return null;
    }
    

    private DesiredCapabilities addChromeOptions(DesiredCapabilities caps) {
        // add default carina options and arguments
        ChromeOptions options = new ChromeOptions();
        options.addArguments("test-type");
        
        //prefs 
        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        boolean needsPrefs = false;
        
        //update browser language
        String browserLang = Configuration.get(Parameter.BROWSER_LANGUAGE); 
        if (!browserLang.isEmpty()) {
            LOGGER.info("Set Chrome language to: " + browserLang);
            options.addArguments("--lang=" + browserLang);
            chromePrefs.put("intl.accept_languages", browserLang);
            needsPrefs = true;
        }

        if (Configuration.getBoolean(Configuration.Parameter.AUTO_DOWNLOAD)) {
            chromePrefs.put("download.prompt_for_download", false);
            chromePrefs.put("download.default_directory", getAutoDownloadFolderPath());
            chromePrefs.put("plugins.always_open_pdf_externally", true);
            needsPrefs = true;
        }

        if (needsPrefs) {
        	options.setExperimentalOption("prefs", chromePrefs);
        }
        
        // [VD] no need to set proxy via options anymore!
        // moreover if below code is uncommented then we have double proxy start and mess in host:port values
        
        // setup default mobile chrome args and preferences
        String driverType = Configuration.getDriverType();
        if (SpecialKeywords.MOBILE.equals(driverType)) {
            options.addArguments("--no-first-run");
            options.addArguments("--disable-notifications");
            options.setExperimentalOption("w3c", false);
        }
        
        // add all custom chrome args
        for (String arg: Configuration.get(Parameter.CHROME_ARGS).split(",")) {
            if (arg.isEmpty()) {
                continue;
            }
            options.addArguments(arg.trim());
        }
    
        // add all custom chrome experimental options, w3c=false
        for (String option: Configuration.get(Parameter.CHROME_EXPERIMENTAL_OPTS).split(",")) {
            if (option.isEmpty()) {
                continue;
            }

            //TODO: think about equal sign inside name or value later
            option = option.trim();
            String name = option.split("=")[0].trim();
            String value = option.split("=")[1].trim();
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                options.setExperimentalOption(name, Boolean.valueOf(value));
            } else {
                options.setExperimentalOption(name, value);
            }
        }
        
        // add all custom chrome mobileEmulation options, deviceName=Nexus 5
        Map<String, String> mobileEmulation = new HashMap<>();
        for (String option: Configuration.get(Parameter.CHROME_MOBILE_EMULATION_OPTS).split(",")) {
            if (option.isEmpty()) {
                continue;
            }

            option = option.trim();
            String name = option.split("=")[0].trim();
            String value = option.split("=")[1].trim();
            mobileEmulation.put(name, value);
        }
        
        if (!mobileEmulation.isEmpty()) {
            options.setExperimentalOption("mobileEmulation", mobileEmulation);
        }

        if (Configuration.getBoolean(Parameter.HEADLESS)
                && driverType.equals(SpecialKeywords.DESKTOP)) {
            options.setHeadless(Configuration.getBoolean(Parameter.HEADLESS));
        }

        caps.setCapability(ChromeOptions.CAPABILITY, options);
        return caps;
    }


    private DesiredCapabilities addFirefoxOptions(DesiredCapabilities caps) {
        FirefoxProfile profile = getDefaultFirefoxProfile();
        FirefoxOptions options = new FirefoxOptions().setProfile(profile);
        caps.setCapability(FirefoxOptions.FIREFOX_OPTIONS, options);

        // add all custom firefox args
        for (String arg : Configuration.get(Parameter.FIREFOX_ARGS).split(",")) {
            if (arg.isEmpty()) {
                continue;
            }
            options.addArguments(arg.trim());
        }
        // add all custom firefox preferences
        for (String preference : Configuration.get(Parameter.FIREFOX_PREFERENCES).split(",")) {
            if (preference.isEmpty()) {
                continue;
            }
            // TODO: think about equal sign inside name or value later
            preference = preference.trim();
            String name = preference.split("=")[0].trim();
            String value = preference.split("=")[1].trim();
            // TODO: test approach with numbers
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                options.addPreference(name, Boolean.valueOf(value));
            } else {
                options.addPreference(name, value);
            }
        }

        String driverType = Configuration.getDriverType();
        if (Configuration.getBoolean(Parameter.HEADLESS)
                && driverType.equals(SpecialKeywords.DESKTOP)) {
            options.setHeadless(Configuration.getBoolean(Parameter.HEADLESS));
        }

        return caps;
    }

    /**
     * Generate default default Carina FirefoxProfile.
     *
     * @return Firefox profile.
     */
    // keep it public to be bale to get default and override on client layerI
    public FirefoxProfile getDefaultFirefoxProfile() {
        FirefoxProfile profile = new FirefoxProfile();

        // update browser language
        String browserLang = Configuration.get(Parameter.BROWSER_LANGUAGE);
        if (!browserLang.isEmpty()) {
            LOGGER.info("Set Firefox lanaguage to: " + browserLang);
            profile.setPreference("intl.accept_languages", browserLang);
        }

        boolean generated = false;
        int newPort = 7055;
        int i = 100;
        while (!generated && (--i > 0)) {
            newPort = PortProber.findFreePort();
            generated = firefoxPorts.add(newPort);
        }
        if (!generated) {
            newPort = 7055;
        }
        if (firefoxPorts.size() > 20) {
            firefoxPorts.remove(0);
        }

        profile.setPreference(FirefoxProfile.PORT_PREFERENCE, newPort);
        LOGGER.debug("FireFox profile will use '" + newPort + "' port number.");

        profile.setPreference("dom.max_chrome_script_run_time", 0);
        profile.setPreference("dom.max_script_run_time", 0);

        if (Configuration.getBoolean(Configuration.Parameter.AUTO_DOWNLOAD) && !(Configuration.isNull(Configuration.Parameter.AUTO_DOWNLOAD_APPS)
                || "".equals(Configuration.get(Configuration.Parameter.AUTO_DOWNLOAD_APPS)))) {
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.dir", getAutoDownloadFolderPath());
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk", Configuration.get(Configuration.Parameter.AUTO_DOWNLOAD_APPS));
            profile.setPreference("browser.download.manager.showWhenStarting", false);
            profile.setPreference("browser.download.saveLinkAsFilenameTimeout", 1);
            profile.setPreference("pdfjs.disabled", true);
            profile.setPreference("plugin.scan.plid.all", false);
            profile.setPreference("plugin.scan.Acrobat", "99.0");
        } else if (Configuration.getBoolean(Configuration.Parameter.AUTO_DOWNLOAD) && Configuration.isNull(Configuration.Parameter.AUTO_DOWNLOAD_APPS)
                || "".equals(Configuration.get(Configuration.Parameter.AUTO_DOWNLOAD_APPS))) {
            LOGGER.warn(
                    "If you want to enable auto-download for FF please specify '" + Configuration.Parameter.AUTO_DOWNLOAD_APPS.getKey() + "' param");
        }

        profile.setAcceptUntrustedCertificates(true);
        profile.setAssumeUntrustedCertificateIssuer(true);

        // TODO: implement support of custom args if any
        return profile;
    }
    
    
    private String getAutoDownloadFolderPath() {
        // use custom folder for auto download
        String autoDownloadFolder = Configuration.get(Parameter.AUTO_DOWNLOAD_FOLDER);
        File autoDownloadPath;

        if (!autoDownloadFolder.isEmpty()) {
            autoDownloadPath = new File(autoDownloadFolder);
            boolean isCreated = autoDownloadPath.exists() && autoDownloadPath.isDirectory();
            if (!isCreated) {
                isCreated = autoDownloadPath.mkdir();
            } else {
                LOGGER.info("Folder for auto download already exists: " + autoDownloadPath.getAbsolutePath());
            }
        } else {
            // if no AUTO_DOWNLOAD_FOLDER defined use artifacts folder
            autoDownloadPath = ReportContext.getArtifactsFolder();
        }

        return autoDownloadPath.getAbsolutePath();
    }
}
