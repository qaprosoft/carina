package com.qaprosoft.carina.core.foundation.webdriver.useragentparser.browsers;

import com.qaprosoft.carina.core.foundation.webdriver.useragentparser.Browser;

public class GenericBrowser extends Browser {

    public GenericBrowser(String browserName) {
        super(browserName);
    }

    @Override public String getUserAgentKey() {
        return getBrowserName();
    }

}
