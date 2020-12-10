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
package com.qaprosoft.carina.core.foundation.webdriver;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.MDC;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.qaprosoft.carina.browsermobproxy.ProxyPool;
import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.exception.DriverPoolException;
import com.qaprosoft.carina.core.foundation.performance.ACTION_NAME;
import com.qaprosoft.carina.core.foundation.performance.Timer;
import com.qaprosoft.carina.core.foundation.report.ReportContext;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.R;
import com.qaprosoft.carina.core.foundation.utils.common.CommonUtils;
import com.qaprosoft.carina.core.foundation.webdriver.TestPhase.Phase;
import com.qaprosoft.carina.core.foundation.webdriver.core.factory.DriverFactory;
import com.qaprosoft.carina.core.foundation.webdriver.device.Device;
import com.zebrunner.agent.core.registrar.Artifact;

public interface IDriverPool {
    static final Logger POOL_LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String DEFAULT = "default";

    // unified set of Carina WebDrivers
    static final ConcurrentHashMap<CarinaDriver, Integer> driversMap = new ConcurrentHashMap<>();
    @SuppressWarnings("static-access")
    static final Set<CarinaDriver> driversPool = driversMap.newKeySet();
    // static final Set<CarinaDriver> driversPool = new HashSet<CarinaDriver>();
    
    //TODO: [VD] make device related param private after migrating to java 9+
    static final ThreadLocal<Device> currentDevice = new ThreadLocal<Device>();
    static final Device nullDevice = new Device();

    /**
     * Get default driver. If no default driver discovered it will be created.
     * 
     * @return default WebDriver
     */
    default public WebDriver getDriver() {
        return getDriver(DEFAULT);
    }

    /**
     * Get driver by name. If no driver discovered it will be created using
     * default capabilities.
     * 
     * @param name
     *            String driver name
     * @return WebDriver
     */
    default public WebDriver getDriver(String name) {
        return getDriver(name, null, null);
    }

    /**
     * Get driver by name and DesiredCapabilities.
     * 
     * @param name
     *            String driver name
     * @param capabilities
     *            DesiredCapabilities capabilities
     * @return WebDriver
     */
    default public WebDriver getDriver(String name, DesiredCapabilities capabilities) {
        return getDriver(name, capabilities, null);
    }

    /**
     * Get driver by name. If no driver discovered it will be created using
     * custom capabilities and selenium server.
     * 
     * @param name
     *            String driver name
     * @param capabilities
     *            DesiredCapabilities
     * @param seleniumHost
     *            String
     * @return WebDriver
     */
    default public WebDriver getDriver(String name, DesiredCapabilities capabilities, String seleniumHost) {
        WebDriver drv = null;

        ConcurrentHashMap<String, CarinaDriver> currentDrivers = getDrivers();
        if (currentDrivers.containsKey(name)) {
            CarinaDriver cdrv = currentDrivers.get(name);
            drv = cdrv.getDriver();
            if (Phase.BEFORE_SUITE.equals(cdrv.getPhase())) {
                POOL_LOGGER.info("Before suite registered driver will be returned.");
            } else {
                POOL_LOGGER.debug(cdrv.getPhase() + " registered driver will be returned.");
            }
        }

        // Long threadId = Thread.currentThread().getId();
        // ConcurrentHashMap<String, WebDriver> currentDrivers = getDrivers();

        // TODO [VD] do we really need finding by groupThreads?
        /*
         * if (currentDrivers.containsKey(name)) { drv =
         * currentDrivers.get(name); } else if
         * (Configuration.getInt(Parameter.THREAD_COUNT) == 1 &&
         * Configuration.getInt(Parameter.DATA_PROVIDER_THREAD_COUNT) <= 1) {
         * Thread[] threads =
         * getGroupThreads(Thread.currentThread().getThreadGroup());
         * logger.debug(
         * "Try to find driver by ThreadGroup id values! Current ThreadGroup count is: "
         * + threads.length); for (int i = 0; i < threads.length; i++) {
         * currentDrivers = drivers.get(threads[i].getId()); if (currentDrivers
         * != null) { if (currentDrivers.containsKey(name)) { drv =
         * currentDrivers.get(name);
         * logger.debug("##########        GET ThreadGroupId: " + threadId +
         * "; driver: " + drv); break; } } } }
         */

        if (drv == null) {
            POOL_LOGGER.debug("Starting new driver as nothing was found in the pool");
            drv = createDriver(name, capabilities, seleniumHost);
        }

        // [VD] do not wrap EventFiringWebDriver here otherwise DriverListener
        // and all logging will be lost!
        return drv;

    }

    /**
     * Get driver by WebElement.
     * 
     * @param sessionId
     *            - session id to be used for searching a desired driver
     * 
     * @return default WebDriver
     */
    public static WebDriver getDriver(SessionId sessionId) {
        for (CarinaDriver carinaDriver : driversPool) {
            WebDriver drv = carinaDriver.getDriver();
            if (drv instanceof EventFiringWebDriver) {
                EventFiringWebDriver eventFirDriver = (EventFiringWebDriver) drv;
                drv = eventFirDriver.getWrappedDriver();
            }

            SessionId drvSessionId = ((RemoteWebDriver) drv).getSessionId();

            if (drvSessionId != null) {
                if (sessionId.equals(drvSessionId)) {
                    return drv;
                }
            }
        }
        throw new DriverPoolException("Unable to find driver using sessionId artifacts. Returning default one!");
    }

    /**
     * Restart default driver
     * 
     * @return WebDriver
     */
    default public WebDriver restartDriver() {
        return restartDriver(false);
    }

    /**
     * Restart default driver on the same device
     * 
     * @param isSameDevice
     *            boolean restart driver on the same device or not
     * @return WebDriver
     */
    default public WebDriver restartDriver(boolean isSameDevice) {
        WebDriver drv = getDriver(DEFAULT);
        Device device = nullDevice;
        DesiredCapabilities caps = new DesiredCapabilities();
        
        boolean keepProxy = false;

        if (isSameDevice) {
            keepProxy = true;
            device = getDefaultDevice();
            POOL_LOGGER.debug("Added udid: " + device.getUdid() + " to capabilities for restartDriver on the same device.");
            caps.setCapability("udid", device.getUdid());
        }

        POOL_LOGGER.debug("before restartDriver: " + driversPool);
        for (CarinaDriver carinaDriver : driversPool) {
            if (carinaDriver.getDriver().equals(drv)) {
                quitDriver(carinaDriver, keepProxy);
                // [VD] don't remove break or refactor moving removal out of "for" cycle
                driversPool.remove(carinaDriver);
                break;
            }
        }
        POOL_LOGGER.debug("after restartDriver: " + driversPool);

        return createDriver(DEFAULT, caps, null);
    }

    /**
     * Quit default driver
     */
    default public void quitDriver() {
        quitDriver(DEFAULT);
    }

    // TODO: Fix after migrating to java9
    // [VD] quitDriver and quitDrivers has code duplicates as inside interface
    // we can't create private methods!

    /**
     * Quit driver by name
     * 
     * @param name
     *            String driver name
     */
    default public void quitDriver(String name) {

        WebDriver drv = null;
        CarinaDriver carinaDrv = null;
        Long threadId = Thread.currentThread().getId();

        POOL_LOGGER.debug("before quitDriver: " + driversPool);
        for (CarinaDriver carinaDriver : driversPool) {
            if ((Phase.BEFORE_SUITE.equals(carinaDriver.getPhase()) && name.equals(carinaDriver.getName()))
                    || (threadId.equals(carinaDriver.getThreadId()) && name.equals(carinaDriver.getName()))) {
                drv = carinaDriver.getDriver();
                carinaDrv = carinaDriver;
                break;
            }
        }

        if (drv == null || carinaDrv == null) {
            throw new RuntimeException("Unable to find driver '" + name + "'!");
        }
        
        quitDriver(carinaDrv, false);
        driversPool.remove(carinaDrv);

        POOL_LOGGER.debug("after quitDriver: " + driversPool);

    }

    /**
     * Quit current drivers by phase(s). "Current" means assigned to the current test/thread.
     * 
     * @param phase
     *            Comma separated driver phases to quit
     */
    default public void quitDrivers(Phase...phase) {
        List<Phase> phases = Arrays.asList(phase);

        Set<CarinaDriver> drivers4Remove = new HashSet<CarinaDriver>();

        Long threadId = Thread.currentThread().getId();
        for (CarinaDriver carinaDriver : driversPool) {
            if ((phases.contains(carinaDriver.getPhase()) && threadId.equals(carinaDriver.getThreadId()))
                    || phases.contains(Phase.ALL)) {
                quitDriver(carinaDriver, false);
                drivers4Remove.add(carinaDriver);
            }
        }
        driversPool.removeAll(drivers4Remove);

        // don't use modern removeIf as it uses iterator!
        // driversPool.removeIf(carinaDriver -> phase.equals(carinaDriver.getPhase()) && threadId.equals(carinaDriver.getThreadId()));
    }
    
    //TODO: [VD] make it as private after migrating to java 9+
    default void quitDriver(CarinaDriver carinaDriver, boolean keepProxyDuring) {
        try {
            carinaDriver.getDevice().disconnectRemote();
            if (!keepProxyDuring) {
                ProxyPool.stopProxy();
            }
            
            // Collect all possible logs and put them as artifacts
            WebDriver drv = carinaDriver.getDriver();
            if (drv instanceof EventFiringWebDriver) {
                drv = ((EventFiringWebDriver) drv).getWrappedDriver();
            }

            SessionId sessionId = ((RemoteWebDriver) drv).getSessionId();
            
            //TODO: remove in 7.0 after making independent logs/video upload from device to s3 compatible storage
            // https://github.com/qaprosoft/carina/issues/1174
            try {
                for (String logType : getAvailableDriverLogTypes(carinaDriver.getDriver())) {
                    if ("bugreport".equals(logType) || "performance".equals(logType)) {
                        // bugreport -  there is no sense to upload as it is too slow (~1 min) and doesn't return valuable info
                        // performance - no response from Appium in 99% of cases
                        continue;
                    }
                    if ("server".equals(logType) && SpecialKeywords.IOS.equalsIgnoreCase(Configuration.getPlatform())) {
                        // unrecognized exception on this phase for iOS which block below execution
                        continue;
                    }
                    String fileName = ReportContext.getArtifactsFolder().getAbsolutePath() + File.separator + logType + File.separator + sessionId.toString() + ".log";
                    StringBuilder tempStr = new StringBuilder();
                    LogEntries logcatEntries = getDriverLogs(carinaDriver.getDriver(), logType);
                    logcatEntries.getAll().forEach((k) -> tempStr.append(k.toString().concat("\n")));
                    
                    if (tempStr.length() == 0) {
                        //don't write something to file and don't register appropriate artifact
                        continue;
                    }
    
                    File file = null;
                    try {
                        POOL_LOGGER.debug("Saving log artifact: " + fileName);
                        file = new File(fileName);
                        FileUtils.writeStringToFile(file, tempStr.toString(), Charset.defaultCharset());
                        POOL_LOGGER.debug("Saved log artifact: " + fileName);
                    } catch (IOException e) {
                        POOL_LOGGER.warn("Error has been occured during attempt to extract " + logType + " log.", e);
                    }
                    Artifact.upload(file, logType);
                }
            } catch (Exception e) {
                POOL_LOGGER.warn("Unable to extract webdriver server logs!");
                POOL_LOGGER.debug(e.getMessage(), e);
            }
            
            WebDriver driver = carinaDriver.getDriver();
            POOL_LOGGER.debug("start driver quit: " + carinaDriver.getName());
            
            Future<?> future = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
                public Void call() throws Exception {
                    driver.quit();
                    return null;
                }
            });
            
            long wait = 120;
            try {
                future.get(wait, TimeUnit.SECONDS);
            } catch (InterruptedException | java.util.concurrent.TimeoutException e) {
                POOL_LOGGER.error("Unable to quit driver for " + wait + "sec!", e);
            } catch (ExecutionException e) {
                POOL_LOGGER.warn("ExecutionException error on driver quit detected!");
                POOL_LOGGER.debug(e.getMessage(), e);
            } catch (Exception e) {
                POOL_LOGGER.warn("Undefined error on driver quit detected!");
                POOL_LOGGER.debug(e.getMessage(), e);
            }
            
            POOL_LOGGER.debug("finished driver quit: " + carinaDriver.getName());
            // stop timer to be able to track mobile app session time. It should be started on createDriver!
            Timer.stop(carinaDriver.getDevice().getMetricName(), carinaDriver.getName() + carinaDriver.getDevice().getName());
            
        } catch (WebDriverException e) {
            POOL_LOGGER.debug("Error message detected during driver quit: " + e.getMessage(), e);
            // do nothing
        } catch (Exception e) {
            POOL_LOGGER.error("Error discovered during driver quit: " + e.getMessage(), e);
        } finally {
            MDC.remove("device");
        }
    }
    
    default Set<String> getAvailableDriverLogTypes(WebDriver driver) {
        Set<String> logTypes = Collections.<String>emptySet();
        if (driver.manage() != null) {
            try {
                logTypes = driver.manage().logs().getAvailableLogTypes();
            } catch (Exception e) {
                POOL_LOGGER.debug("Unrecognized failure while getAvailableLogTypes()", e);
            }
        }
        // logTypes: logcat, bugreport, server, client
        POOL_LOGGER.debug("logTypes: " + Arrays.toString(logTypes.toArray()));
        return logTypes;
    }
    
    /**
     * Get driver logs by type. 
     * Android: logcat, bugreport, server, client; 
     * iOS: syslog, crashlog, performance, server, safariConsole, safariNetwork, client
     * 
     * @param driver WebDriver
     * @param logType String
     * 
     * @return LogEntries entries
     */
    default LogEntries getDriverLogs(WebDriver driver, String logType) {
        //TODO: make it async in parallel thread
        LogEntries logEntries = new LogEntries(Collections.emptyList());
        POOL_LOGGER.debug("start getting driver logs: " + logType);
        try {
            if (driver.manage() != null) {
                Timer.start(ACTION_NAME.GET_LOGS);
                POOL_LOGGER.debug("Getting log artifact: " + logType);
                logEntries = driver.manage().logs().get(logType);
                POOL_LOGGER.debug("Got log artifact: " + logType);
                Timer.stop(ACTION_NAME.GET_LOGS);
            } else {
                POOL_LOGGER.error("driver.manage() is null!");
            }
        } catch (Exception e) {
            POOL_LOGGER.warn("Unable to get webdriver server logs.");
            POOL_LOGGER.debug("Unable to get webdriver server logs.", e);
        }
        POOL_LOGGER.debug("finish getting driver logs");
        return logEntries;
    }
    /**
     * Create driver with custom capabilities
     * 
     * @param name
     *            String driver name
     * @param capabilities
     *            DesiredCapabilities
     * @param seleniumHost
     *            String
     * @return WebDriver
     */
    default WebDriver createDriver(String name, DesiredCapabilities capabilities, String seleniumHost) {
        // TODO: make current method as private after migrating to java 9+
        int count = 0;
        WebDriver drv = null;
        Device device = nullDevice;

        // 1 - is default run without retry
        int maxCount = Configuration.getInt(Parameter.INIT_RETRY_COUNT) + 1;
        while (drv == null && count++ < maxCount) {
            try {
                POOL_LOGGER.debug("initDriver start...");
                
                Long threadId = Thread.currentThread().getId();
                ConcurrentHashMap<String, CarinaDriver> currentDrivers = getDrivers();

                int maxDriverCount = Configuration.getInt(Parameter.MAX_DRIVER_COUNT);

                if (currentDrivers.size() == maxDriverCount) {
                    Assert.fail("Unable to create new driver as you reached max number of drivers per thread: " + maxDriverCount + "!" +
                            " Override max_driver_count to allow more drivers per test!");
                }

                // [VD] pay attention that similar piece of code is copied into the DriverPoolTest as registerDriver method!
                if (currentDrivers.containsKey(name)) {
                    // [VD] moved containsKey verification before the driver start
                    Assert.fail("Driver '" + name + "' is already registered for thread: " + threadId);
                }
                
                drv = DriverFactory.create(name, capabilities, seleniumHost);

                if (device.isNull()) {
                    // During driver creation we choose device and assign it to
                    // the test thread
                    device = getDefaultDevice();
                }
                // push custom device name for log4j default messages
                if (!device.isNull()) {
                    MDC.put("device", "[" + device.getName() + "] ");
                }
                
                // moved proxy start logic here since device will be initialized
                // here only
                if (Configuration.getBoolean(Parameter.BROWSERMOB_PROXY)) {
                    if (!device.isNull()) {
                    	int proxyPort;
                        try {
                            proxyPort = Integer.parseInt(device.getProxyPort());
                        } catch (NumberFormatException e) {
                            // use default from _config.properties. Use-case for
                            // iOS devices which doesn't have proxy_port as part
                            // of capabilities
                            proxyPort = ProxyPool.getProxyPortFromConfig();
                        }
                        ProxyPool.startProxy(proxyPort);
                    }
                }

                
                // new 6.0 approach to manipulate drivers via regular Set
                CarinaDriver carinaDriver = new CarinaDriver(name, drv, device, TestPhase.getActivePhase(), threadId);
                
                //start timer to be able to track mobile app session time. It should be stopped on quitDriver!
                Timer.start(device.getMetricName(), carinaDriver.getName() + carinaDriver.getDevice().getName());
                driversPool.add(carinaDriver);

                POOL_LOGGER.debug("initDriver finish...");

            } catch (Exception e) {
                device.disconnectRemote();
                //TODO: [VD] think about excluding device from pool for explicit reasons like out of space etc
                // but initially try to implement it on selenium-hub level
                String msg = String.format("Driver initialization '%s' FAILED! Retry %d of %d time - %s", name, count,
                        maxCount, e.getMessage());
                POOL_LOGGER.error(msg, e); //TODO: test how 2 messages are displayed in logs and zafira
                if (count == maxCount) {
                    throw e;
                }
                CommonUtils.pause(Configuration.getInt(Parameter.INIT_RETRY_INTERVAL));
            }
        }
        
        if (drv == null) {
            throw new RuntimeException("Undefined exception detected! Analyze above logs for details.");
        }

        return drv;
    }

    /**
     * Verify if driver is registered in the DriverPool
     * 
     * @param name
     *            String driver name
     *
     * @return boolean
     */
    default boolean isDriverRegistered(String name) {
        return getDrivers().containsKey(name);
    }

    // TODO: think about hiding getDriversCount and removing size when migration to java 9+ happens
    /**
     * Return number of registered driver per thread
     * 
     * @return int
     */
    default public int getDriversCount() {
        Long threadId = Thread.currentThread().getId();
        int size = getDrivers().size();
        POOL_LOGGER.debug("Number of registered drivers for thread '" + threadId + "' is " + size);
        return size;
    }

    /**
     * Return all drivers registered in the DriverPool for this thread including
     * on Before Suite/Class/Method stages
     * 
     * @return ConcurrentHashMap of driver names and Carina WebDrivers
     * 
     */
    default ConcurrentHashMap<String, CarinaDriver> getDrivers() {
        Long threadId = Thread.currentThread().getId();
        ConcurrentHashMap<String, CarinaDriver> currentDrivers = new ConcurrentHashMap<String, CarinaDriver>();
        for (CarinaDriver carinaDriver : driversPool) {
            if (Phase.BEFORE_SUITE.equals(carinaDriver.getPhase())) {
                currentDrivers.put(carinaDriver.getName(), carinaDriver);
            } else if (threadId.equals(carinaDriver.getThreadId())) {
                currentDrivers.put(carinaDriver.getName(), carinaDriver);
            }
        }
        return currentDrivers;
    }

    // ------------------------ DEVICE POOL METHODS -----------------------
    /**
     * Get device registered to default driver. If no default driver discovered nullDevice will be returned.
     * 
     * @return default Device
     */
    default public Device getDevice() {
        return getDevice(DEFAULT);
    }

    /**
     * Get device registered to named driver. If no driver discovered nullDevice will be returned.
     * 
     * @param name
     *            String driver name
     * @return Device
     */
    default public Device getDevice(String name) {
        if (isDriverRegistered(name)) {
            return getDrivers().get(name).getDevice();
        } else {
            return nullDevice;
        }
        
    }

    /**
     * Register device information for current thread by MobileFactory and clear SysLog for Android only
     * 
     * @param device
     *            String Device device
     * 
     * @return Device device
     * 
     */
    public static Device registerDevice(Device device) {

        boolean stfEnabled = R.CONFIG
                .getBoolean(SpecialKeywords.CAPABILITIES + "." + SpecialKeywords.STF_ENABLED);
        if (stfEnabled) {
            device.connectRemote();
        }

        // register current device to be able to transfer it into Zafira at the end of the test
        long threadId = Thread.currentThread().getId();
        POOL_LOGGER.debug("Set current device '" + device.getName() + "' to thread: " + threadId);
        currentDevice.set(device);

        POOL_LOGGER.debug("register device for current thread id: " + threadId + "; device: '" + device.getName() + "'");

        return device;
    }

    /**
     * Return last registered device information for current thread.
     * 
     * @return Device device
     * 
     */
    public static Device getDefaultDevice() {
        long threadId = Thread.currentThread().getId();
        Device device = currentDevice.get();
        if (device == null) {
            device = nullDevice;
        } else if (device.getName().isEmpty()) {
            POOL_LOGGER.debug("Current device name is empty! nullDevice was used for thread: " + threadId);
        } else {
            POOL_LOGGER.debug("Current device name is '" + device.getName() + "' for thread: " + threadId);
        }
        return device;
    }

    /**
     * Return nullDevice object to avoid NullPointerException and tons of verification across carina-core modules.
     * 
     * @return Device device
     * 
     */
    public static Device getNullDevice() {
        return nullDevice;
    }

    /**
     * Verify if device is registered in the Pool
     * 
     * 
     * @return boolean
     */
    default public boolean isDeviceRegistered() {
        Device device = currentDevice.get();
        return device != null && device != nullDevice;
    }
}
