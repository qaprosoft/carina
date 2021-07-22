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
package com.qaprosoft.carina.proxy;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.R;

public class SystemProxyTest {
    private static String host = "localhost";
    private static String port = "80";

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        // do nothing

        R.CONFIG.put("browserup_proxy", "false");
        R.CONFIG.put("proxy_set_to_system", "true");
        R.CONFIG.put("proxy_host", host);
        R.CONFIG.put(Parameter.PROXY_PORT.getKey(), port);
    }

    @BeforeMethod
    public void resetSystemProperties() {
        System.setProperty("http.proxyHost", "");
        System.setProperty("https.proxyHost", "");
        System.setProperty("ftp.proxyHost", "");
        System.setProperty("socksProxyHost", "");

        System.setProperty("http.proxyPort", "");
        System.setProperty("https.proxyPort", "");
        System.setProperty("ftp.proxyPort", "");
        System.setProperty("socksProxyPort", "");
    }

    @Test
    public void testHttpSystemProxy() {
        R.CONFIG.put("proxy_protocols", "http");
        SystemProxy.setupProxy();
        Assert.assertEquals(System.getProperty("http.proxyHost"), host);
        Assert.assertEquals(System.getProperty("http.proxyPort"), port);

        Assert.assertEquals(System.getProperty("https.proxyHost"), "");
        Assert.assertEquals(System.getProperty("ftp.proxyHost"), "");
        Assert.assertEquals(System.getProperty("socksProxyHost"), "");

        Assert.assertEquals(System.getProperty("https.proxyPort"), "");
        Assert.assertEquals(System.getProperty("ftp.proxyPort"), "");
        Assert.assertEquals(System.getProperty("socksProxyPort"), "");
    }

    @Test
    public void testHttpsSystemProxy() {
        R.CONFIG.put("proxy_protocols", "https");
        SystemProxy.setupProxy();
        Assert.assertEquals(System.getProperty("https.proxyHost"), host);
        Assert.assertEquals(System.getProperty("https.proxyPort"), port);

        Assert.assertEquals(System.getProperty("http.proxyHost"), "");
        Assert.assertEquals(System.getProperty("ftp.proxyHost"), "");
        Assert.assertEquals(System.getProperty("socksProxyHost"), "");

        Assert.assertEquals(System.getProperty("http.proxyPort"), "");
        Assert.assertEquals(System.getProperty("ftp.proxyPort"), "");
        Assert.assertEquals(System.getProperty("socksProxyPort"), "");
    }

    @Test
    public void testFtpSystemProxy() {
        R.CONFIG.put("proxy_protocols", "ftp");
        SystemProxy.setupProxy();
        Assert.assertEquals(System.getProperty("ftp.proxyHost"), host);
        Assert.assertEquals(System.getProperty("ftp.proxyPort"), port);

        Assert.assertEquals(System.getProperty("http.proxyHost"), "");
        Assert.assertEquals(System.getProperty("https.proxyHost"), "");
        Assert.assertEquals(System.getProperty("socksProxyHost"), "");

        Assert.assertEquals(System.getProperty("http.proxyPort"), "");
        Assert.assertEquals(System.getProperty("https.proxyPort"), "");
        Assert.assertEquals(System.getProperty("socksProxyPort"), "");
    }

    @Test
    public void testSocksSystemProxy() {
        R.CONFIG.put("proxy_protocols", "socks");
        SystemProxy.setupProxy();
        Assert.assertEquals(System.getProperty("socksProxyHost"), host);
        Assert.assertEquals(System.getProperty("socksProxyPort"), port);

        Assert.assertEquals(System.getProperty("http.proxyHost"), "");
        Assert.assertEquals(System.getProperty("https.proxyHost"), "");
        Assert.assertEquals(System.getProperty("ftp.proxyHost"), "");

        Assert.assertEquals(System.getProperty("http.proxyPort"), "");
        Assert.assertEquals(System.getProperty("https.proxyPort"), "");
        Assert.assertEquals(System.getProperty("ftp.proxyPort"), "");
    }
}