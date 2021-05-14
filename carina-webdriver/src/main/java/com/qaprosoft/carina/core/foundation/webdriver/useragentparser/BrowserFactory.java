package com.qaprosoft.carina.core.foundation.webdriver.useragentparser;

import com.qaprosoft.carina.core.foundation.exception.NotSupportedOperationException;
import com.qaprosoft.carina.core.foundation.webdriver.useragentparser.browsers.*;

public class BrowserFactory {

    private BrowserFactory() {
    }
    
    public static Browser create(String browserName) {
        switch (browserName) {
        case "Chrome":
        case "Firefox":
        case "Safari":
        case "Edge":
            return new GenericBrowser(browserName);
        case "IE":
            return new IExplorer(browserName);
        case "Opera":
            return new Opera(browserName);
        default:
            throw new NotSupportedOperationException();
        }
    }

}
