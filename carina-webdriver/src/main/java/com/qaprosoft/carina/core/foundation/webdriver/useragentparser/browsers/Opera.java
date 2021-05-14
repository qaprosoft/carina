package com.qaprosoft.carina.core.foundation.webdriver.useragentparser.browsers;

import com.qaprosoft.carina.core.foundation.webdriver.useragentparser.Browser;

public class Opera extends Browser {

    private static final String USER_AGENT_KEY = "OPR";

    public Opera(String browser) {
        super(browser);
    }

    @Override public String getUserAgentKey() {
        return USER_AGENT_KEY;
    }

}
