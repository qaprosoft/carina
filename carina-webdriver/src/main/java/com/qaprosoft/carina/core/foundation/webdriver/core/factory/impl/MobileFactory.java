package com.qaprosoft.carina.core.foundation.webdriver.core.factory.impl;


import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.CapabilitiesLoder;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.mobile.MobileGridCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.mobile.MobileNativeCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.mobile.MobilePoolCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.capability.impl.mobile.MobileWebCapabilities;
import com.qaprosoft.carina.core.foundation.webdriver.core.factory.AbstractFactory;
import com.qaprosoft.carina.core.foundation.webdriver.device.Device;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.IOSElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;

import java.net.MalformedURLException;
import java.net.URL;

public class MobileFactory extends AbstractFactory {

    @Override
    public WebDriver create(String name, Device device) {

        String selenium = Configuration.get(Configuration.Parameter.SELENIUM_HOST);
        String driverType = Configuration.get(Configuration.Parameter.DRIVER_TYPE);
        String mobile_platform_name = Configuration.get(Configuration.Parameter.MOBILE_PLATFORM_NAME);

        if (device != null && !device.isNull()) {
        	selenium = device.getSeleniumServer();
        	LOGGER.debug("selenium_host: " + selenium);
        }
        
        LOGGER.debug("selenium: " + selenium);
        
        RemoteWebDriver driver = null;
        DesiredCapabilities capabilities = getCapabilities(name, device);
        try {
            if (driverType.equalsIgnoreCase(SpecialKeywords.MOBILE_GRID)) {
                driver = new RemoteWebDriver(new URL(selenium), capabilities);
            } else if (driverType.equalsIgnoreCase(SpecialKeywords.MOBILE) || driverType.equalsIgnoreCase(SpecialKeywords.MOBILE_POOL)) {
                if (mobile_platform_name.toLowerCase().equalsIgnoreCase(SpecialKeywords.ANDROID)) {
                    // use uiautomator2 for Android 7
                    if (Configuration.getBoolean(Configuration.Parameter.ENABLE_AUTOMATOR2) && 
                    		Configuration.get(Configuration.Parameter.MOBILE_PLATFORM_VERSION).trim().startsWith("7.")) {
                        LOGGER.debug("uiautomator2 will be enabled for Android 7");
                        capabilities.setCapability("automationName", "uiautomator2");
                    }
                    // handler in case app was installed via adb and there is no need to sign app using appium
                    if (Configuration.getBoolean(Configuration.Parameter.MOBILE_APP_INSTALL) && !Configuration.getBoolean(Configuration.Parameter.MOBILE_APP_UNINSTALL)) {
                    	capabilities.setCapability("app", "");
                    }
                    driver = new AndroidDriver<AndroidElement>(new URL(selenium), capabilities);
                } else if (mobile_platform_name.toLowerCase().equalsIgnoreCase(SpecialKeywords.IOS)) {
                    driver = new IOSDriver<IOSElement>(new URL(selenium), capabilities);
                }
            } else if (driverType.equalsIgnoreCase(SpecialKeywords.CUSTOM)) {
                driver = new RemoteWebDriver(new URL(selenium), capabilities);
            } else {
                throw new RuntimeException("Unsupported browser");
            }
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed selenium URL! " + e.getMessage(), e);
        }

        
    	if (driver == null ) {
    		Assert.fail("Unable to initialize driver: " + name + "!");
    	}
    	
        return driver;
    }

    public DesiredCapabilities getCapabilities(String name, Device device) {
        String customCapabilities = Configuration.get(Parameter.CUSTOM_CAPABILITIES);
        if (!customCapabilities.isEmpty()) {
            return new CapabilitiesLoder().loadCapabilities(customCapabilities);
        } else {
            String driverType = Configuration.get(Configuration.Parameter.DRIVER_TYPE);

            if (driverType.equalsIgnoreCase(SpecialKeywords.MOBILE_GRID)) {
                return new MobileGridCapabilities().getCapability(name);
            } else if (driverType.equalsIgnoreCase(SpecialKeywords.MOBILE)
                    && !Configuration.get(Configuration.Parameter.BROWSER).isEmpty()) {
                return new MobileWebCapabilities().getCapability(name);
            } else if (driverType.equalsIgnoreCase(SpecialKeywords.MOBILE)
                    && Configuration.get(Configuration.Parameter.BROWSER).isEmpty()) {
                return new MobileNativeCapabilities().getCapability(name);
            } else if (driverType.equalsIgnoreCase(SpecialKeywords.MOBILE_POOL)) {
            	//TODO: ensure that mobile_pool works for web testing too!
            	return new MobilePoolCapabilities().getCapability(name, device);            	
            }else {
                throw new RuntimeException("Unsupported driver type:" + driverType);
            }
        }

    }


}
