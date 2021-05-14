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
package com.qaprosoft.carina.core.foundation.webdriver.core.factory.impl;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;

import com.qaprosoft.carina.core.foundation.webdriver.useragentparser.Browser;
import com.qaprosoft.carina.core.foundation.webdriver.useragentparser.BrowserFactory;
import com.qaprosoft.carina.core.foundation.webdriver.useragentparser.BrowserVersionExtractor;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.R;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.desktop.ChromeCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.desktop.EdgeCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.desktop.FirefoxCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.desktop.IECapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.desktop.OperaCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.desktop.SafariCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.factory.AbstractFactory;

public class DesktopFactory extends AbstractFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static DesiredCapabilities staticCapabilities;

    @Override
    public WebDriver create(String name, DesiredCapabilities capabilities, String seleniumHost) {
        RemoteWebDriver driver = null;
        if (seleniumHost == null) {
            seleniumHost = Configuration.getSeleniumUrl();
        }

        if (isCapabilitiesEmpty(capabilities)) {
            capabilities = getCapabilities(name);
        }

        if (staticCapabilities != null) {
            LOGGER.info("Static DesiredCapabilities will be merged to basic driver capabilities");
            capabilities.merge(staticCapabilities);
        }

        try {
            driver = new RemoteWebDriver(new URL(seleniumHost), capabilities);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed selenium URL!", e);
        }
        resizeBrowserWindow(driver, capabilities);

        R.CONFIG.put(SpecialKeywords.ACTUAL_BROWSER_VERSION, getBrowserVersion(driver));
        return driver;
    }

    @SuppressWarnings("deprecation")
    public DesiredCapabilities getCapabilities(String name) {
        String browser = Configuration.getBrowser();

        if (BrowserType.FIREFOX.equalsIgnoreCase(browser)) {
            return new FirefoxCapabilities().getCapability(name);
        } else if (BrowserType.IEXPLORE.equalsIgnoreCase(browser) || BrowserType.IE.equalsIgnoreCase(browser) || browser.equalsIgnoreCase("ie")) {
            return new IECapabilities().getCapability(name);
        } else if (BrowserType.SAFARI.equalsIgnoreCase(browser)) {
            return new SafariCapabilities().getCapability(name);
        } else if (BrowserType.CHROME.equalsIgnoreCase(browser)) {
            return new ChromeCapabilities().getCapability(name);
        } else if (BrowserType.OPERA_BLINK.equalsIgnoreCase(browser) || BrowserType.OPERA.equalsIgnoreCase(browser)) {
            return new OperaCapabilities().getCapability(name);
        } else if (BrowserType.EDGE.toLowerCase().contains(browser.toLowerCase())) {
            return new EdgeCapabilities().getCapability(name);
        } else {
            throw new RuntimeException("Unsupported browser: " + browser);
        }
    }

    public static void addStaticCapability(String name, Object value) {
        if (staticCapabilities == null) {
            staticCapabilities = new DesiredCapabilities();
        }
        staticCapabilities.setCapability(name, value);
    }

    @SuppressWarnings("deprecation")
    private String getBrowserVersion(WebDriver driver) {
        String browser_version = Configuration.get(Parameter.BROWSER_VERSION);
        try {
            Capabilities cap = ((RemoteWebDriver) driver).getCapabilities();
            browser_version = cap.getVersion().toString();
            if (browser_version != null) {
                if (browser_version.contains(".")) {
                    browser_version = StringUtils.join(StringUtils.split(browser_version, "."), ".", 0, 2);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to get actual browser version!", e);
        }
        
        // hotfix to https://github.com/qaprosoft/carina/issues/882
        String browser = Configuration.get(Parameter.BROWSER);
        if (BrowserType.OPERA.equalsIgnoreCase(browser) || BrowserType.OPERA_BLINK.equalsIgnoreCase(browser)) {
            browser_version = getOperaVersion(driver);
        }
        return browser_version;
    }
    
    //TODO: reformat later using UserAgent for all browser version identification
    private String getOperaVersion(WebDriver driver) {
        String browser_version = Configuration.get(Parameter.BROWSER_VERSION);
        try { 
            String userAgent = (String) ((RemoteWebDriver) driver).executeScript("return navigator.userAgent", "");
            LOGGER.debug("User Agent: " + userAgent);
            Browser browser = BrowserFactory.create("Opera");
            String browserVersion = BrowserVersionExtractor.getBrowserVersion(browser,userAgent);
            LOGGER.info(browserVersion);
        } catch (Exception e){
            // do nothing
            LOGGER.debug("Unable to get browser_version using userAgent call!", e);
        }
        return browser_version;
    }

    /**
     * Sets browser window according to capabilites.resolution value, otherwise
     * maximizes window.
     * 
     * @param driver - instance of desktop @WebDriver
     * @param capabilities - driver capabilities
     */
    private void resizeBrowserWindow(WebDriver driver, DesiredCapabilities capabilities) {
        try {
            Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                    .pollingEvery(Duration.ofMillis(Configuration.getInt(Parameter.RETRY_INTERVAL)))
                    .withTimeout(Duration.ofSeconds(Configuration.getInt(Parameter.EXPLICIT_TIMEOUT)))
                    .ignoring(WebDriverException.class)
                    .ignoring(NoSuchSessionException.class)
                    .ignoring(TimeoutException.class);
            if (capabilities.getCapability("resolution") != null) {
                String resolution = (String) capabilities.getCapability("resolution");
                int expectedWidth = Integer.valueOf(resolution.split("x")[0]);
                int expectedHeight = Integer.valueOf(resolution.split("x")[1]);
                wait.until(new Function<WebDriver, Boolean>(){
                    public Boolean apply(WebDriver driver ) {
                        driver.manage().window().setPosition(new Point(0, 0));
                        driver.manage().window().setSize(new Dimension(expectedWidth, expectedHeight));
                        Dimension actualSize = driver.manage().window().getSize();
                        if (actualSize.getWidth() == expectedWidth && actualSize.getHeight() == expectedHeight) {
                            LOGGER.debug(String.format("Browser window size set to %dx%d", actualSize.getWidth(), actualSize.getHeight()));
                        } else {
                            LOGGER.warn(String.format("Expected browser window %dx%d, but actual %dx%d",
                                    expectedWidth, expectedHeight, actualSize.getWidth(), actualSize.getHeight()));
                        }
                        return true;
                    }
                });
            } else {
                wait.until(new Function<WebDriver, Boolean>(){
                    public Boolean apply(WebDriver driver ) {
                        driver.manage().window().maximize();
                        LOGGER.debug("Browser window size was maximized!");
                        return true;
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.error("Unable to resize browser window", e);
        }
    }
}
