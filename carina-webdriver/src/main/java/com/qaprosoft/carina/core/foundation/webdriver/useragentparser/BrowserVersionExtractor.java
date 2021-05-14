package com.qaprosoft.carina.core.foundation.webdriver.useragentparser;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class BrowserVersionExtractor {

    private BrowserVersionExtractor() {
    }

    public static String getBrowserVersion(Browser browser, String userAgent) {
        if (browser.getBrowserName().equals("IE")) {
            String[] version = userAgent.split(" ");
            for (int i = 1; i < version.length - 1; i++) {
                if (version[i].equalsIgnoreCase(browser.getUserAgentKey())) {
                    return version[i + 1].replaceAll("[^\\d.]", "");
                }
            }
        } else {
            Stream<String> streamVersion = Arrays.stream(userAgent.split(" "));
            Optional<String[]> version = streamVersion
                    .filter(str -> Utils.isRequiredBrowser(browser.getUserAgentKey(), str))
                    .findFirst().map(str -> Utils.splitVersion(str)[1].split("\\."));
            streamVersion.close();
            return Utils.buildBrowserVersion(version).replaceAll("[^\\d.]", "");
        }
        return null;
    }

}
