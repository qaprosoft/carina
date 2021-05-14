package com.qaprosoft.carina.core.foundation.webdriver.useragentparser;

import java.util.Optional;

public class Utils {

    private Utils() {
    }

    public static String[] splitVersion(String version) {
        return version.split("/");
    }

    public static String buildBrowserVersion(Optional<String[]> browserVersionParts) {
        if (browserVersionParts.isPresent()) {
            return browserVersionParts.get()[0] + "." + browserVersionParts.get()[1];
        } else {
            return "";
        }
    }

    public static Boolean isRequiredBrowser(String browser, String auCapabilitie) {
        return auCapabilitie.split("/")[0].contains(browser);
    }

}
