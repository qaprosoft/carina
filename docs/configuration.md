[![Carina - Configuration](https://raw.githubusercontent.com/qaprosoft/carina/master/docs/img/video.png)](https://youtu.be/MMviWxCS9x4)

### Configuration files
There are multiple properties files located in src/main/resources:

*  **api.properties** - API test endpoints reference
*  **config.properties** - global test configuration
*  **database.properties** - database connection properties
*  **email.properties** - emailable reports config
*  **testdata.properties** - test user credentials 

All the properties may be retrieved in a test using R class:
```
R.API.get("GetUserMethods")
R.CONFIG.get("browser")
R.DATABASE.get("db.url")
R.EMAIL.get("title")
R.TESTDATA.get("user.email")
```
The default config properties can be obtained by
```
Configuration.get(Parameter.BROWSER)
```

All the project configuration properties are located in a **_config.properties** file. In the table below we are providing a description of most of the parameters:
<table>
	<tr>
		<th>Attribute</th>
		<th>Meaning</th>
		<th>Example</th>
	</tr>
	<tr>
		<td>url</td>
		<td>Base application URL</td>
		<td>http://qaprosoft.com</td>
	</tr>
	<tr>
		<td>browser</td>
		<td>Browser for testing</td>
		<td>chrome / firefox / safari / iexplore</td>
	</tr>
	<tr>
		<td>headless</td>
		<td>Run tests in headless browser mode. Enabled when headless=true. Default: false.</td>
		<td>Boolean</td>
	</tr>
	<tr>
		<td>browser_language</td>
		<td>Browser language or nothing to use the English version by default.</td>
		<td>"es", "fr"</td>
	</tr>
	<tr>
		<td>selenium_url</td>
		<td>Selenium/Appium server url</td>
		<td>http://localhost:4444/wd/hub</td>
	</tr>
	<tr>
		<td>app_version</td>
		<td>Application version/build number for reporting</td>
		<td>1.2.5</td>
	</tr>
	<tr>
		<td>locale</td>
		<td>Locale for using L10N feature</td>
		<td>en_GB,de_DE,fr_FR</td>
	</tr>
	<tr>
		<td>retry_interval</td>
		<td>Timeout interval between calling HTML DOM for the element.<br><b>Note:</b> in ms. For mobile automation specify a number from 500-1000 range</td>
		<td>Integer</td>
	</tr>
	<tr>
		<td>auto_screenshot</td>
		<td>Global switch for taking screenshots. When disabled, screenshots will be captured only after failures</td>
		<td>Boolean</td>
	</tr>
	<tr>
		<td>report_url</td>
		<td>Direct HTTP link to Jenkins workspace report folder. Automatically specified by CI</td>
		<td>http://localhost:8888/job /my_project/1/eTAF_Report</td>
	</tr>
	<tr>
		<td>max_screen_history</td>
		<td>Max number of reports artifacts saving in history. Default: 10</td>
		<td>Integer</td>
	</tr>
	<tr>
		<td>env</td>
		<td>Environment specific configuration. More about this [feature](#environment-specific-configuration)</td>
		<td>STAG, PROD, DEMO</td>
	</tr>
	<tr>
		<td>driver_event_listeners</td>
		<td>Comma-separated list of extra driver listeners listeners. Listeners provide extra custom actions for WebDriver and have to be the instances of WebDriverEventListener</td>
		<td>com.some_company.core.EventListener</td>
	</tr>
	<tr>
		<td>max_driver_count</td>
		<td>Max number of drivers per thread. Default: 3</td>
		<td>Integer</td>
	</tr>
	<tr>
		<td>driver_recorder</td>
		<td>Enable embedded carina recorder for driver session log/video artifacts generation. It is recommended to use for Selenium/Appium hubs which can't record such artifacts automatically. Default: false</td>
		<td>Boolean</td>
	</tr>
	<tr>
		<td>forcibly_disable_driver_quit</td>
		<td>If enabled turns off webdriver shutdown after test finishing by any reason. Default: false</td>
		<td>Boolean</td>
	</tr>
	<tr>
		<td>custom_capabilities</td>
		<td>Name of a properties file with custom capabilities (key-value)</td>
		<td>custom.properties</td>
	</tr>
		<tr>
		<td>proxy_host</td>
		<td>Hostname of the server</td>
		<td>host.example.com</td>
	</tr>
		<tr>
		<td>proxy_port</td>
		<td>Port number</td>
		<td>80</td>
	</tr>
		<tr>
		<td>proxy_protocols</td>
		<td>Comma-separated list of internet protocols used to carry the connection information from the source requesting the connection to the destination for which the connection was requested.</td>
		<td>http, https, ftp, socks</td>
	</tr>
		<tr>
		<td>browserup_proxy</td>
		<td>Boolean parameter which enables or disables the automatic BrowserUp proxy launch</td>
		<td>true, false</td>
	</tr>
		<tr>
		<td>browserup_port</td>
		<td>Port number for BrowserUp proxy (if nothing or 0 specified, then any free port will be reused)</td>
		<td>Integer</td>
	</tr>
		<tr>
		<td>browserup_ports_range</td>
		<td>Range of ports that will be used for starting of browserup proxy. First available port from the range will be used. If all ports are used then test will wait for the first freed port.</td>
		<td>8001:8003</td>
	</tr>
		<tr>
		<td>proxy_set_to_system</td>
		<td>Boolean parameter which enables or disables the setup of a proxy</td>
		<td>true, false</td>
	</tr>
		<tr>
		<td>no_proxy</td>
		<td>Excluded hostname(s) for communication via proxy. Available only when proxy_host and proxy_port are declared!</td>
		<td>localhost.example.com</td>
	</tr>
	<tr>
		<td>explicit_timeout</td>
		<td>Timeout is seconds to wait for a certain condition to occur before proceeding further in the code</td>
		<td>Integer</td>
	</tr>
	<tr>
		<td>auto_download</td>
		<td>The enabled parameter prevents downloading a dialog and downloading a file automatically. The feature is currently available for Chrome and FireFox</td>
		<td>false, true</td>
	</tr>
	<tr>
		<td>auto_download_apps</td>
		<td>MIME types / Internet Media Types. The parameter is needed only to configure auto downloading for FireFox</td>
		<td>application/pdf, list of [values](https://freeformatter.com/mime-types-list.html)</td>
	</tr>
	<tr>
		<td>auto_download_folder</td>
		<td>Path to auto download folder for Chrome and Firefox browsers. If nothing specified custom_artifacts_folder or default artifacts folder is used</td>
		<td>String</td>
	</tr>
	<tr>
		<td>project_report_directory</td>
		<td>Path to a folder where the testing report will be saved</td>
		<td>./reports</td>
	</tr>
	<tr>
		<td>big_screen_width</td>
		<td>Screenshots will be resized according to this width if their own width is bigger. Default: -1 to keep existing size.</td>
		<td>Integer</td>
	</tr>
	<tr>
		<td>big_screen_height</td>
		<td>Screenshots will be resized according to this height if their own height is bigger. Default: -1 to keep existing size.</td>
		<td>Integer</td>
	<tr>
		<td>init_retry_count</td>
		<td>Number of attempts to create a driver. The default value 0 means that there will be only 1 attempt</td>
		<td>Integer</td>
	</tr>
	<tr>
		<td>init_retry_interval</td>
		<td>Interval in seconds between the attempts to create a driver</td>
		<td>Integer</td>
	</tr>
	<tr>
		<td>retry_count</td>
		<td>Number of test-retryings in case of failure. The default value 0 means that a test will be performed only once</td>
		<td>Integer</td>
	</tr>
		<tr>
		<td>thread_count</td>
		<td>Default number of threads to use when running tests in parallel. Set thread-count=custom to disable any updates on carina side.</td>
		<td>Integer</td>
	</tr>
		<tr>
		<td>data_provider_thread_count</td>
		<td>Default number of threads to use for data providers when running tests in parallel.</td>
		<td>Integer</td>
	</tr>
		<tr>
		<td>core_log_level</td>
		<td>Level for Carina logging</td>
		<td>ALL, DEBUG, ERROR, WARN, FATAL, INFO, OFF, TRACE</td>
	</tr>
		<tr>
		<td>core_log_packages</td>
		<td>Comma-separated list of core packages where you want to redefine the log level</td>
		<td>com.qaprosoft.carina.core, com.zebrunner etc</td>
	</tr>
		<tr>
		<td>log_all_json</td>
		<td>API response will be logged in JSON format</td>
		<td>Boolean</td>
	</tr>
		<tr>
		<td>date_format</td>
		<td>Date format for DateUtils.class</td>
		<td>HH:mm:ss dd/MM/yyyy, HH:mm MM/dd/yyyy</td>
	</tr>
		<tr>
		<td>time_format</td>
		<td>Date format for DateUtils.class</td>
		<td>HH:mm:ss.SSS, HH:mm a zzz</td>
	</tr>
		<tr>
		<td>crypto_key_path</td>
		<td>Path to a file with a crypto key</td>
		<td>./src/main/resources/crypto.key</td>
	</tr>
		<tr>
		<td>suite_name</td>
		<td>Suite name for the report and TestRail. If this parameter is NULL, will be taken from TestNG xml (the parameter suite name) or _email.properties (the title)</td>
		<td>Advanced Acceptance</td>
	</tr>
		<tr>
		<td>access_key_id</td>
		<td>Access key id for Amazon S3 build uploader. More info [here](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys)</td>
		<td>gkhcvdgvceUYF67897hbjsbdc</td>
	</tr>
		<tr>
		<td>secret_key</td>
		<td>Secret key for Amazon S3 build uploader. More info [here](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys)</td>
		<td>gkhcvdgvceUYF67897hbjsbdc</td>
	</tr>
		<tr>
		<td>appcenter_token</td>
		<td>Token for authentication in Hockey App</td>
		<td>gkhcvdgvceUYF67897hbjsbdc</td>
	</tr>
		<tr>
		<td>Encoding for a new localization</td>
		<td>UTF-8</td>
	</tr>
		<tr>
		<td>localization_testing</td>
		<td>Enables auto verification for elements that are marked with @Localized</td>
		<td>true, false</td>
	</tr>
		<tr>
		<td>tls_keysecure_location</td>
		<td>Path to a directory with tls secure keys</td>
		<td>./tls/keysecure</td>
	</tr>
		<tr>
		<td>health_check_class</td>
		<td>Class to execute health checks</td>
		<td>Custom class</td>
	</tr>
		<tr>
		<td>health_check_methods</td>
		<td>Comma-separated list of methods of health_check_class to execute preliminarily</td>
		<td>doThis, doThat</td>
	</tr>
	<tr>
		<td>test_run_rules</td>
		<td>Executing rules logic: test_run_rules={RULE_NAME_ENUM}=>{RULE_VALUE1}&&{RULE_VALUE2};;...</td>
		<td>test_run_rules=PRIORITY=>P1&amp;&amp;P2&&P4;;OWNER=>owner;;TAGS=>tag1=temp||!!feature=reg</td>
	</tr>
	<tr>
		<td>element_loading_strategy</td>
		<td>Determines how carina detects appearing of web elements on page: by presence in DOM model or by visibility or by any of these conditions</td>
		<td>BY_PRESENCE, BY_VISIBILITY, BY_PRESENCE_OR_VISIBILITY</td>
	</tr>
	<tr>
		<td>page_opening_strategy</td>
		<td>Determines how carina detects whether expected page is opened: by expected url pattern, by marker element loading state or by both these conditions</td>
		<td>BY_ELEMENT, BY_URL, BY_URL_AND_ELEMENT</td>
	</tr>
</table>
Most of the properties may be read in the following way:

```
Configuration.get(Parameter.URL) // returns string value
Configuration.getBoolean(Parameter.AUTO_SCREENSHOT) // returns boolean value
Configuration.getInt(Parameter.BIG_SCREEN_WIDTH) //return int value
Configuration.getDouble(Parameter.MAX_DRIVER_COUNT) // returns double value
```

### Environment specific configuration
In some cases, it is required to support multiple environments for testing. Let's assume we have STAG and PROD environments which have different application URLs. In this case, we need to specify the following properties in _config.properties:
```
env=PROD
STAG.url=http://stag-app-server.com
PROD.url=http://prod-app-server.com
```

And get an env-specific argument in the test in the following way:
```
Configuration.getEnvArg("url")
```
As a result, you switch between the environments just changing the env argument in the _config.properties file.

### Tests execution filter configuration
The test_run_rules parameter is responsible for filtering tests.
There are 3 filter types:
1) PRIORITY - enum field (from P0 to P6)
2) OWNER - the test owner
3) TAGS - custom label

Example of how to attach labels in code:
```
@Test
@TestPriority(Priority.P3)
@MethodOwner(owner = "Josh")
@MethodOwner(owner = "Jake")
@TestTag(name = "feature", value = "web")
@TestTag(name = "type", value = "regression")
public void t4(){
	...
	some code
	...
}
```

test_run_rules parameter parse logic:

1) A simple filter:
```
test_run_rules=OWNER=>Josh
#Where OWNER is tag, and "=>" split's tag and rule part.
#Because of the "Josh" rule, test will be executed if it has Josh as owner
```
2) With negative logic:
```
test_run_rules=OWNER=>!!Josh
#Test will be executed if it hasn't got Josh as owner
```
3) With boolean logic:
```
#Use || or && to create more difficult rules
#where || == OR; && == AND.

test_run_rules=OWNER=>Josh||Jake
#Test will be executed if it has at least Josh or Jake as owner.

test_run_rules=OWNER=>Josh&&Jake
#Test will be executed if it has at least Jish and Jake as owner

test_run_rules=OWNER=>Josh&&Jake||Peter
#Expression will be processed in sequential priority, like
#test_run_rules=OWNER=>((Josh&&Jake)||Peter)
#So test will be executed if it has at least (Josh and Jake) or (Peter) as owner
```
4) To add more tags to the rule use ";;", example:
```
#;; works as && (AND) but for tags

test_run_rules=PRIORITY=>!!P1;;OWNER=>Josh&&!!Jake;;TAGS=>feature=web&&!!type=smoke||feature=android

#Test will be executed if it has
#1) no @TestPriority(Priority.P1)
#AND
#2) @MethodOwner(owner = "Josh") without @MethodOwner(owner = "Jake")
#AND
#3) (@TestTag(name = "feature", value = "web") without @TestTag(name = "type", value = "smoke"))
	 	or @TestTag(name = "feature", value = "android")	 	

#In other words, will be executed tests with Priority that differs from P1, with Josh as owner if there no Jake 
#and if they are for not smoke web or if they are for android.
```

### [Zebrunner Reporting](https://zebrunner.com/documentation/agents/testng) configuration
[**agent.properties**](https://github.com/zebrunner/carina-demo/blob/master/src/main/resources/agent.properties) file is used for Zebrunner Reporting integration, here you should specify some values for a proper integration:
<table>	
	<tr>
		<th>Attribute</th>
		<th>Meaning</th>
		<th>Example</th>
	</tr>
	<tr>
		<td>reporting.enabled</td>
		<td>Root switch</td>
		<td>true/false</td>
	</tr>
	<tr>
		<td>reporting.server.hostname</td>
		<td>Service URL</td>
		<td>https://mycompany.zebrunner.com</td>
	</tr>
	<tr>
		<td>reporting.server.access-token</td>
		<td>Access Token</td>
		<td>eyJhbGciOiJIUzUxMiJ9...</td>
	</tr>
	<tr>
		<td>reporting.projectKey</td>
		<td>Project Name</td>
		<td>empty or any existing name</td>
	</tr>
</table>
