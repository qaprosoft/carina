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
package com.qaprosoft.carina.core.foundation.utils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;

/**
 * Configuration utility.
 * 
 * @author Aliaksei_Khursevich
 *         hursevich@gmail.com
 */
public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static IEnvArgResolver envArgResolver;

    static {
        String envArgResolverClass = Configuration.get(Parameter.ENV_ARG_RESOLVER);
        if (envArgResolverClass.isEmpty()) {
            // redefine using default class
            envArgResolverClass = "com.qaprosoft.carina.core.foundation.utils.DefaultEnvArgResolver";
        }

        try {
            Class<?> cl = Class.forName(envArgResolverClass);
            Constructor<?> ct = cl.getConstructor();
            Configuration.setEnvArgResolver((IEnvArgResolver) ct.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Configuration failure: can not initiate EnvArgResolver - '" + envArgResolverClass + "'", e);
        }

    }

    /**
     * All available configuration parameter keys along with default values.
     */
    public enum Parameter {
        URL("url"),

        ENV("env"),

        ENV_ARG_RESOLVER("env_arg_resolver"),

        BROWSER("browser"),

        BROWSER_VERSION("browser_version"),

        BROWSER_LANGUAGE("browser_language"),

        SELENIUM_HOST("selenium_host"),

        DRIVER_EVENT_LISTENERS("driver_event_listeners"),

        MAX_DRIVER_COUNT("max_driver_count"),

        CUSTOM_CAPABILITIES("custom_capabilities"),
        
        CHROME_ARGS("chrome_args"),
        
        CHROME_EXPERIMENTAL_OPTS("chrome_experimental_opts"),
        
        CHROME_MOBILE_EMULATION_OPTS("chrome_mobile_emulation_opts"),
        
        FIREFOX_ARGS("firefox_args"),
        
        FIREFOX_PREFERENCES("firefox_preferences"),

        HEADLESS("headless"),

        APP_VERSION("app_version"),

        PROXY_HOST("proxy_host"),

        PROXY_PORT("proxy_port"),

        PROXY_PROTOCOLS("proxy_protocols"),
        
        NO_PROXY("no_proxy"),

        BROWSERMOB_PROXY("browsermob_proxy"),

        BROWSERMOB_HOST("browsermob_host"),

        BROWSERMOB_PORT("browsermob_port"),

        BROWSERMOB_PORTS_RANGE("browsermob_ports_range"),

        BROWSERMOB_MITM("browsermob_disabled_mitm"),

        PROXY_SET_TO_SYSTEM("proxy_set_to_system"),

        REPORT_URL("report_url"),

        TRACK_KNOWN_ISSUES("track_known_issues"),

        AUTO_SCREENSHOT("auto_screenshot"),

        EXPLICIT_TIMEOUT("explicit_timeout"),

        AUTO_DOWNLOAD("auto_download"),

        AUTO_DOWNLOAD_APPS("auto_download_apps"),

        AUTO_DOWNLOAD_FOLDER("auto_download_folder"),

        CUSTOM_ARTIFACTS_FOLDER("custom_artifacts_folder"),

        RETRY_INTERVAL("retry_interval"),

        PROJECT_REPORT_DIRECTORY("project_report_directory"),

        MAX_SCREENSHOOT_HISTORY("max_screen_history"),

        MAX_LOG_FILE_SIZE("max_log_file_size"),

        RESULT_SORTING("result_sorting"),

        BIG_SCREEN_WIDTH("big_screen_width"),

        BIG_SCREEN_HEIGHT("big_screen_height"),

        INIT_RETRY_COUNT("init_retry_count"),

        INIT_RETRY_INTERVAL("init_retry_interval"),

        RETRY_COUNT("retry_count"),

        ENABLE_L10N("enable_l10n"),

        L10N_ENCODING("l10n_encoding"),

        LOCALE("locale"),

        THREAD_COUNT("thread_count"),

        DATA_PROVIDER_THREAD_COUNT("data_provider_thread_count"),

        CORE_LOG_LEVEL("core_log_level"),

        CORE_LOG_PACKAGES("core_log_packages"),

        ARTIFACTS_EXPIRATION_SECONDS("artifacts_expiration_seconds"),

        LOG_ALL_JSON("log_all_json"),

        DATE_FORMAT("date_format"),

        TIME_FORMAT("time_format"),

        CRYPTO_KEY_PATH("crypto_key_path"),

        SUITE_NAME("suite_name"),

        JIRA_URL("jira_url"),

        TEST_NAMING_PATTERN("test_naming_pattern"),
        
        ELEMENT_LOADING_STRATEGY("element_loading_strategy"),
        
        PAGE_OPENING_STRATEGY("page_opening_strategy"),
        
        FORCIBLY_DISABLE_DRIVER_QUIT("forcibly_disable_driver_quit"),

        // Amazon
        S3_BUCKET_NAME("s3_bucket_name"),

        ACCESS_KEY_ID("access_key_id"),

        SECRET_KEY("secret_key"),

        S3_USE_PRESIGN_URL("s3_use_presign_url"),
        
        S3_LOCAL_STORAGE("s3_local_storage"),

        // AppCenter token
        APPCENTER_TOKEN("appcenter_token"),

        APPCENTER_LOCAL_STORAGE("appcenter_local_storage"),

        // For localization parser
        ADD_NEW_LOCALIZATION("add_new_localization"),

        ADD_NEW_LOCALIZATION_ENCODING("add_new_localization_encoding"),

        ADD_NEW_LOCALIZATION_PATH("add_new_localization_path"),

        ADD_NEW_LOCALIZATION_PROPERTY_NAME("add_new_localization_property_name"),

        // TLS
        TLS_KEYSECURE_LOCATION("tls_keysecure_location"),

        // HealthCheck
        HEALTH_CHECK_CLASS("health_check_class"),

        HEALTH_CHECK_METHODS("health_check_methods"),

        UNINSTALL_RELATED_APPS("uninstall_related_apps"),

        // For Device default timezone and language
        DEFAULT_DEVICE_TIMEZONE("default_device_timezone"),

        DEFAULT_DEVICE_TIME_FORMAT("default_device_time_format"),

        DEFAULT_DEVICE_LANGUAGE("default_device_language"),

        // For screen recording
        ANDROID_SCREEN_RECORDING_SIZE("android_screen_record_size"),

        ANDROID_SCREEN_RECORDING_BITRATE("android_screen_record_bitrate"),

        ANDROID_ENABLE_BUG_REPORT("android_enable_bug_report"),

        IOS_SCREEN_RECORDING_QUALITY("ios_screen_record_quality"),

        IOS_SCREEN_RECORDING_CODEC("ios_screen_record_codec"),

        IOS_SCREEN_RECORDING_FPS("ios_screen_record_fps"),

        SCREEN_RECORD_DURATION("screen_record_duration"),

        VIDEO_SCALE("video_scale"),

        // Ignore SSL
        IGNORE_SSL("ignore_ssl"),

        // Test Execution Filter rules
        TEST_RUN_RULES("test_run_rules");

        private final String key;

        Parameter(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * Returns configuration value from startup properties or from configuration
     * file if not found in startup args.
     * 
     * @param param
     *            - parameter key.
     * @return parameter value if it is found by key or default value if not.
     */
    public static String get(Parameter param) {
        String value = R.CONFIG.get(param.getKey());
        return !(value == null || value.equalsIgnoreCase(SpecialKeywords.NULL)) ? value : StringUtils.EMPTY;
    }

    public static int getInt(Parameter param) {
        return Integer.valueOf(get(param).trim());
    }

    public static long getLong(Parameter param) {
        return Long.valueOf(get(param).trim());
    }

    public static double getDouble(Parameter param) {
        return Double.valueOf(get(param).trim());
    }

    public static boolean getBoolean(Parameter param) {
        return Boolean.valueOf(get(param).trim());
    }

    public static String asString() {
        StringBuilder asString = new StringBuilder();
        asString.append("\n============= Test configuration =============\n");
        for (Parameter param : Parameter.values()) {
            if (!Parameter.CRYPTO_KEY_PATH.equals(param)) {
                asString.append(String.format("%s=%s%n", param.getKey(), Configuration.get(param)));
            }
        }

        // write into the log extra information about selenium_host together with capabilities
        asString.append(String.format("%s=%s%n", "selenium_host", R.CONFIG.get("selenium_host")));
        asString.append("\n------------- Driver capabilities -----------\n");
        // read all properties from config.properties and use "capabilities.*"
        final String prefix = SpecialKeywords.CAPABILITIES + ".";
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, String> capabilitiesMap = new HashMap(R.CONFIG.getProperties());
        for (Map.Entry<String, String> entry : capabilitiesMap.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(prefix)) {
                asString.append(String.format("%s=%s%n", entry.getKey(), R.CONFIG.get(entry.getKey())));
            }
        }

        asString.append("================================================\n");
        return asString.toString();
    }

    public static void validateConfiguration() {
        for (Parameter param : Parameter.values()) {
            if (StringUtils.isEmpty(Configuration.get(param)) || SpecialKeywords.MUST_OVERRIDE.equals(Configuration.get(param))) {
                throw new RuntimeException("Configuration failure: parameter '" + param.getKey() + "' not specified!");
            }
        }
    }

    public static String getEnvArg(String key) {
        return envArgResolver.get(get(Parameter.ENV), key);
    }

    public static IEnvArgResolver getEnvArgResolver() {
        return envArgResolver;
    }

    public static void setEnvArgResolver(IEnvArgResolver envArgResolver) {
        Configuration.envArgResolver = envArgResolver;
    }

    public static boolean isNull(Parameter param) {
        // null is never returned from configuration now so compare with empty string
        return get(param).isEmpty();
    }

    /**
     * Get platform name from configuration properties.
     * @return String platform name
     */
    public static String getPlatform() {
        return getPlatform(new DesiredCapabilities());
    }

    /**
     * Get platform name from configuration properties or DesiredCapabilities.
     * @param caps
     *            DesiredCapabilities
     * @return String platform name
     */
    public static String getPlatform(DesiredCapabilities caps) {
        // any platform by default
        String platform = "*";

        // redefine platform if mobile.platformName is available
        if (!R.CONFIG.get(SpecialKeywords.PLATFORM).isEmpty()) {
            platform = R.CONFIG.get(SpecialKeywords.PLATFORM);
        }
        
        // redefine platform if mobile.platformName is available
        if (!R.CONFIG.get(SpecialKeywords.PLATFORM_NAME).isEmpty()) {
            platform = R.CONFIG.get(SpecialKeywords.PLATFORM_NAME);
        }
        
        if (caps != null && caps.getCapability("platform") != null) {
            platform = caps.getCapability("platform").toString();
        }

        if (caps != null && caps.getCapability("platformName") != null) {
            platform = caps.getCapability("platformName").toString();
        }        
        
        //TODO: try to get actual platform name
        return platform;
    }

    public static int getAdbExecTimeout() {
        // default "capabilities.adbExecTimeout=value" should be used to determine current platform
        int adbExecTimeout = SpecialKeywords.DEFAULT_ADB_EXEC_TIMEOUT;

        // redefine adb exec timeout if capabilities.AdbExecTimeout is available
        if (!R.CONFIG.get(SpecialKeywords.ADB_EXEC_TIMEOUT).isEmpty()) {
            adbExecTimeout = R.CONFIG.getInt(SpecialKeywords.ADB_EXEC_TIMEOUT);
        }

        return adbExecTimeout;
    }
    
    public static String getPlatformVersion() {
        // default "os_version=value" should be used to determine current platform
        String platformVersion = "";

        // redefine platform if mobile.platformVersion is available
        if (!R.CONFIG.get(SpecialKeywords.PLATFORM_VERSION).isEmpty()) {
            platformVersion = R.CONFIG.get(SpecialKeywords.PLATFORM_VERSION);
        }
        
        //TODO: try to get actual platform version
        return platformVersion;
    }

    public static String getBrowser() {
        String browser = "";
        if (!Configuration.get(Parameter.BROWSER).isEmpty()) {
            // default "browser=value" should be used to determine current browser
            browser = Configuration.get(Parameter.BROWSER);
        }

        // redefine browser if capabilities.browserName is available
        if (!R.CONFIG.get("capabilities.browserName").isEmpty() && !"null".equalsIgnoreCase(R.CONFIG.get("capabilities.browserName"))) {
            browser = R.CONFIG.get("capabilities.browserName");
        }
        return browser;
    }
    
    public static String getBrowserVersion() {
        String browserVersion = "";
        if (!Configuration.get(Parameter.BROWSER_VERSION).isEmpty()) {
            // default "browser_version=value" should be used to determine current browser
            browserVersion = Configuration.get(Parameter.BROWSER_VERSION);
        }

        // redefine browserVersion if capabilities.browserVersion is available
        if (!R.CONFIG.get("capabilities.browserVersion").isEmpty()  && !"null".equalsIgnoreCase(R.CONFIG.get("capabilities.browserVersion"))) {
            browserVersion = R.CONFIG.get("capabilities.browserVersion");
        }
        
        // read from actual_browser_version if specified
        if (R.CONFIG.containsKey(SpecialKeywords.ACTUAL_BROWSER_VERSION)) {
            browserVersion = R.CONFIG.get(SpecialKeywords.ACTUAL_BROWSER_VERSION);
        }
        
        return browserVersion;
    }

    public static String getDriverType() {

        String platform = getPlatform();
        if (platform.equalsIgnoreCase(SpecialKeywords.ANDROID) || platform.equalsIgnoreCase(SpecialKeywords.IOS) || platform.equalsIgnoreCase(SpecialKeywords.TVOS)) {
            return SpecialKeywords.MOBILE;
        }
        
        if (SpecialKeywords.WINDOWS.equalsIgnoreCase(platform)) {
            return SpecialKeywords.WINDOWS;
        }

        return SpecialKeywords.DESKTOP;
    }

    public static String getDriverType(DesiredCapabilities capabilities) {
        if (capabilities == null) {
            // calculate driver type based on config.properties arguments
            return getDriverType();
        }

        String platform = "";
        if (capabilities.getCapability("platform") != null) {
            platform = capabilities.getCapability("platform").toString();
        }

        if (capabilities.getCapability("platformName") != null) {
            platform = capabilities.getCapability("platformName").toString();
        }

        if (SpecialKeywords.ANDROID.equalsIgnoreCase(platform) || SpecialKeywords.IOS.equalsIgnoreCase(platform) || SpecialKeywords.TVOS.equalsIgnoreCase(platform)) {
            return SpecialKeywords.MOBILE;
        }
        
        if (SpecialKeywords.WINDOWS.equalsIgnoreCase(platform)) {
            return SpecialKeywords.WINDOWS;
        }

        // handle use-case when we provide only uuid object among desired capabilities
        if (capabilities.getCapability("udid") != null) {
            LOGGER.debug("Detected MOBILE driver_type by uuid inside capabilities");
            return SpecialKeywords.MOBILE;
        }

        return SpecialKeywords.DESKTOP;
    }

    public static String getMobileApp() {
        // redefine platform if capabilities.app is available
        String mobileApp = "";
        String prefix = SpecialKeywords.CAPABILITIES + ".";
        if (!R.CONFIG.get(prefix + "app").isEmpty()) {
            mobileApp = R.CONFIG.get(prefix + "app");
        }
        return mobileApp;
    }

    public static void setMobileApp(String mobileApp) {
        R.CONFIG.put(SpecialKeywords.CAPABILITIES + ".app", mobileApp);
        LOGGER.info("Updated mobile app: " + mobileApp);
    }

    public static Object getCapability(String name) {
        return R.CONFIG.get("capabilities." + name);
    }
}