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
package com.qaprosoft.carina.browsermobproxy;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.qaprosoft.carina.core.foundation.utils.R;
import com.qaprosoft.carina.proxy.SystemProxy;

import com.browserup.bup.BrowserUpProxy;

public class BrowserUpPortsRangeTest {
    private static String header = "my_header";
    private static String headerValue = "my_value";

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        R.CONFIG.put("core_log_level", "DEBUG");
        R.CONFIG.put("browserup_proxy", "true");
        R.CONFIG.put("proxy_set_to_system", "false");
        R.CONFIG.put("browserup_port", "NULL");
        R.CONFIG.put("browserup_ports_range", "0:0");
        R.CONFIG.put("browserup_disabled_mitm", "false");
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {
        ProxyPool.stopAllProxies();
    }

    @Test
    public void testPortsRange() {
        initialize();
        Assert.assertTrue(ProxyPool.getProxy().isStarted(), "BrowserUpProxy is not started!");
    }

    private void initialize() {
    	ProxyPool.initProxyPortsRange();
        ProxyPool.setupBrowserUpProxy();
        SystemProxy.setupProxy();

        BrowserUpProxy proxy = ProxyPool.getProxy();
        proxy.addHeader(header, headerValue);
    }

}
