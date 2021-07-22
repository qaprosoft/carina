/*******************************************************************************
 * Copyright 2013-2020 QaProSoft (http://www.qaprosoft.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.qaprosoft.carina.browsermobproxy;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.NetworkUtil;
import com.qaprosoft.carina.core.foundation.utils.R;
import com.qaprosoft.carina.core.foundation.utils.android.recorder.utils.AdbExecutor;
import com.qaprosoft.carina.core.foundation.utils.common.CommonUtils;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;

public final class ProxyPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static ConcurrentHashMap<Long, Integer> proxyPortsByThread = new ConcurrentHashMap<Long, Integer>();
    
    /**
     * map for storing of available ports range and their availability
     */
    private static ConcurrentHashMap<Integer, Boolean> proxyPortsFromRange = new ConcurrentHashMap<Integer, Boolean>();

    
	static {
		initProxyPortsRange();
	}
	
	public static void initProxyPortsRange() {
		if (!Configuration.get(Parameter.BROWSERUP_PORTS_RANGE).isEmpty()) {
			try {
				String[] ports = Configuration.get(Parameter.BROWSERUP_PORTS_RANGE).split(":");
				for (int i = Integer.valueOf(ports[0]); i <= Integer.valueOf(ports[1]); i++) {
					proxyPortsFromRange.put(i, true);
				}
			} catch (Exception e) {
				throw new RuntimeException("Please specify BROWSERUP_PORTS_RANGE in format 'port_from:port_to'");
			}
		}
	}
    
    // ------------------------- BOWSERUP PROXY ---------------------
    // TODO: investigate possibility to return interface to support JettyProxy
    /**
     * create BrowserUpProxy Server object
     * @return BrowserUpProxy
     * 
     */
    public static BrowserUpProxy createProxy() {
        BrowserUpProxyServer proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        //System.setProperty("jsse.enableSNIExtension", "false");
        
        // disable MITM in case we do not need it
        proxy.setMitmDisabled(Configuration.getBoolean(Parameter.BROWSERUP_MITM));
        
        return proxy;
    }
    
    public static void setupBrowserUpProxy()
    {
        if (Configuration.getBoolean(Parameter.BROWSERUP_PROXY)) {
            long threadId = Thread.currentThread().getId();
            BrowserUpProxy proxy = startProxy();

            Integer port = proxy.getPort();
            proxyPortsByThread.put(threadId, port);

            // reuse "proxy_host" to be able to share valid publicly available host. 
            // it is useful when java and web tests are executed absolutely in different containers/networks. 
            if (Configuration.get(Parameter.PROXY_HOST).isEmpty()) {
            	String currentIP = NetworkUtil.getIpAddress();
            	R.CONFIG.put(Parameter.PROXY_HOST.getKey(), currentIP);
            }
            
            LOGGER.warn("Set http/https proxy settings only to use with BrowserUpProxy host: " + Configuration.get(Parameter.PROXY_HOST) + "; port: "
                    + proxyPortsByThread.get(threadId));

            R.CONFIG.put(Parameter.PROXY_PORT.getKey(), port.toString());
            
            R.CONFIG.put("proxy_protocols", "http,https");

            // follow steps to configure https traffic sniffering: https://github.com/lightbody/browsermob-proxy#ssl-support
            // the most important are:
            // download - https://github.com/lightbody/browsermob-proxy/blob/master/browsermob-core/src/main/resources/sslSupport/ca-certificate-rsa.cer
            // import to system (for android we should use certifiate installer in system settings->security...)
        }
    }

    // https://github.com/lightbody/browsermob-proxy/issues/264 'started' flag is not set to false after stopping BrowserUpProxyServer
    // Due to the above issue we can't control BrowserUp isRunning state and shouldn't stop it
    // TODO: investigate possibility to clean HAR files if necessary
    
    /**
     * stop BrowserUpProxy Server
     * 
     */
    /*
    public static void stopProxy() {
        long threadId = Thread.currentThread().getId();

        LOGGER.debug("stopProxy starting...");
        if (proxies.containsKey(threadId)) {
            BrowserUpProxy proxy = proxies.get(threadId);
            if (proxy != null) {
                LOGGER.debug("Found registered proxy by thread: " + threadId);

                if (proxy.isStarted()) {
                    LOGGER.info("Stopping BrowserUp proxy...");
                    proxy.stop();
                } else {
                    LOGGER.info("Stopping BrowserUp proxy skipped as it is not started.");
                }
            }
            proxies.remove(threadId);
        }
        LOGGER.debug("stopProxy finished...");
    }*/
    
    // ------------------------- BOWSERUP PROXY ---------------------
    
    private static final ConcurrentHashMap<Long, BrowserUpProxy> proxies = new ConcurrentHashMap<Long, BrowserUpProxy>();
    
    /**
     * Checking whether BROWSERUP_PORT is declared. then it will be used as port for browserup proxy
     * Otherwise first available port from BROWSERUP_PORTS_RANGE will be used
     * 
     * @return port
     */
	public static int getProxyPortFromConfig() {
		if (!Configuration.get(Parameter.BROWSERUP_PORT).isEmpty())
			return Configuration.getInt(Parameter.BROWSERUP_PORT);
		else if (!Configuration.get(Parameter.BROWSERUP_PORTS_RANGE).isEmpty()) {
			for (Map.Entry<Integer, Boolean> pair : proxyPortsFromRange.entrySet()) {
				if (pair.getValue()) {
					LOGGER.info("Making BrowserUp proxy port busy: " + pair.getKey());
					pair.setValue(false);
					return pair.getKey().intValue();
				}
			}
			throw new RuntimeException(
					"All ports from Parameter.BROWSERUP_PORTS_RANGE are currently busy. Please change execution thread count");
		}
		throw new RuntimeException(
				"Neither Parameter.BROWSERUP_PORT nor Parameter.BROWSERUP_PORTS_RANGE are specified!");
	}

    // TODO: investigate possibility to return interface to support JettyProxy
    /**
     * start BrowserUpProxy Server
     * 
     * @return BrowserUp Proxy
     * 
     */
    public static synchronized BrowserUpProxy startProxy() {
        return startProxy(getProxyPortFromConfig());
    }
    
    public static synchronized BrowserUpProxy startProxy(int proxyPort) {
        if (!Configuration.getBoolean(Parameter.BROWSERUP_PROXY)) {
            LOGGER.debug("Proxy is disabled.");
            return null;
        }
        // integrate browserup proxy if required here
        BrowserUpProxy proxy = null;
        long threadId = Thread.currentThread().getId();
        if (proxies.containsKey(threadId)) {
            proxy = proxies.get(threadId);
        }

        if (proxyPortsByThread.containsKey(threadId)) {
            proxyPort = proxyPortsByThread.get(threadId);
        }

        // case when proxy was already instantiated but port doesn't correspond to current device
        if (null == proxy || proxy.getPort() != proxyPort) {
            proxy = ProxyPool.createProxy();
            proxies.put(Thread.currentThread().getId(), proxy);
        }
        
        if (!proxy.isStarted()) {
            LOGGER.info("Starting BrowserUp proxy...");
        	// TODO: [VD] confirmed with MB that restart was added just in case. Maybe comment/remove?
            killProcessByPort(proxyPort);
            proxy.start(proxyPort);
        } else {
            LOGGER.info("BrowserUp proxy is already started on port " + proxy.getPort());
        }

        return proxy;
    }
    
    private static void setProxyPortToAvailable(long threadId) {
		if (proxyPortsByThread.get(threadId) != null) {
			if (proxyPortsFromRange.get(proxyPortsByThread.get(threadId)) != null) {
				LOGGER.info("Setting BrowserUp proxy port " + proxyPortsByThread.get(threadId) + " to available state");
				proxyPortsFromRange.put(proxyPortsByThread.get(threadId), true);
				proxyPortsByThread.remove(threadId);
			}
		}
    }

    // https://github.com/lightbody/browsermob-proxy/issues/264 'started' flag is not set to false after stopping BrowserUpProxyServer
    // Due to the above issue we can't control BrowserMob isRunning state and shouldn't stop it
    // TODO: investigate possibility to clean HAR files if necessary
    
    /**
     * stop BrowserUpProxy Server
     * 
     */
    public static void stopProxy() {
        stopProxyByThread(Thread.currentThread().getId());
    }
    
    /**
     * Stop all proxies if possible
     */
    public static void stopAllProxies() {
        for (Long threadId : Collections.list(proxies.keys())) {
            stopProxyByThread(threadId);
        }
    }
    
    /**
     * Stop single proxy instance by id
     * @param threadId long
     */
    private static void stopProxyByThread(long threadId) {
        if (proxies.containsKey(threadId)) {
            setProxyPortToAvailable(threadId);
            BrowserUpProxy proxy = proxies.get(threadId);
            if (proxy != null) {
                LOGGER.debug("Found registered proxy by thread: " + threadId);

                // isStarted returns true even if proxy was already stopped
                if (proxy.isStarted()) {
                    try {
                        LOGGER.debug("stopProxy starting...");                        
                        proxy.stop();
                    } catch (IllegalStateException e) {
                        LOGGER.info("Seems like proxy was already stopped.");
                        LOGGER.info(e.getMessage());
                    } finally {
                        LOGGER.debug("stopProxy finished...");
                    }
                }
            }
            proxies.remove(threadId);
        }
    }

    /**
     * get registered BrowserUpProxy Server
     * 
     * @return BrowserUpProxy
     * 
     */
    public static BrowserUpProxy getProxy() {
        BrowserUpProxy proxy = null;
        long threadId = Thread.currentThread().getId();
        if (proxies.containsKey(threadId)) {
            proxy = proxies.get(threadId);
        } else {
            Assert.fail("There is not a registered BrowserUpProxy for thread: " + threadId);
        }
        return proxy;
    }

    public static int getProxyPortFromThread() {
        int port = 0;
        long threadId = Thread.currentThread().getId();
        if (proxyPortsByThread.containsKey(threadId)) {
            port = proxyPortsByThread.get(threadId);
        } else {
            Assert.fail("This is not a register BrowserUpProxy Port for thread: " + threadId);
        }
        return port;
    }
    
    /**
     * return true if proxy is already registered
     * 
     * @return boolean
     * 
     */
    public static boolean isProxyRegistered() {
        long threadId = Thread.currentThread().getId();
        return proxies.containsKey(threadId);
    }

    /**
     * register custom BrowserUpProxy Server
     * 
     * @param proxy
     *            custom BrowserUpProxy
     * 
     */
    public static void registerProxy(BrowserUpProxy proxy) {
        long threadId = Thread.currentThread().getId();
        if (proxies.containsKey(threadId)) {
            LOGGER.warn("Existing proxy is detected and will be overrwitten");
            // No sense to stop as it is not supported
            proxies.remove(threadId);
        }
        if (proxyPortsByThread.containsKey(threadId)) {
            LOGGER.warn("Existing proxyPort is detected and will be overwritten");
            proxyPortsByThread.remove(threadId);
        }
        
        LOGGER.info("Register custom proxy with thread: " + threadId);
        proxies.put(threadId, proxy);
    }
    
    /**
     * Method to kill process by port. It is used before start of new proxy instance
     * 
     * @param port int
     */
    private static void killProcessByPort(int port) {
        if (port == 0) {
            //do nothing as it is default dynamic browserup proxy
            return;
        }
        LOGGER.info(String.format("Process on port %d will be closed.", port));

        //TODO: make OS independent or remove completely
        try {
            List<?> output = new AdbExecutor().execute(String.format("lsof -ti :%d", port).split(" "));
            LOGGER.debug("proxy process before kill: " + StringUtils.join(output, ""));
            
            output = new AdbExecutor().execute(String.format("lsof -ti :%d | xargs kill -9", port).split(" "));
            LOGGER.debug("proxy process kill output: " + StringUtils.join(output, ""));
            
            output = new AdbExecutor().execute(String.format("lsof -ti :%d", port).split(" "));
            LOGGER.debug("proxy process after kill: " + StringUtils.join(output, ""));
            
            CommonUtils.pause(1);
            
            output = new AdbExecutor().execute(String.format("lsof -ti :%d", port).split(" "));
            LOGGER.debug("proxy process after kill and 2 sec pause: " + StringUtils.join(output, ""));
            
        } catch (Exception e) {
            LOGGER.error("Unable to kill process by lsof utility: " + e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
    }
}
