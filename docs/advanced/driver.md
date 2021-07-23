#Driver usage

All described methods are implemented inside `IDriverPool` interface and accessible by default from any test class. 
To access this functionality from another places like your services just implement this interface.

> Each thread has their own named driver pool

Supported Browsers: Chrome, Firefox, Internet Explorer, Microsoft Edge, Opera, Safari etc

##Initialization

* **getDriver()** is the core method to start any Selenium/Appium session. It will create a RemoteWebDriver named "default" based on default capabilities from configuration.

    1st call of the method in current thread should start new driver. Next calls will return existing object.

* **getDriver(String name)** start named driver session using default capabilities from configuration. That's allow to start several drivers (up to 3 according to `max_driver_count` property)

* **getDriver(String name, DesiredCapabilities capabilities)** start named driver session using custom capabilities.

* **getDriver(String name, DesiredCapabilities capabilities, String seleniumHost)** start named driver session using custom capabilities vs custom selenium url.

Example:
```
@Test
public void carinaCapsTest() {
    // Chrome default capabilities builder usage:
    WebDriver chromeDriver = getDriver("chrome", new ChromeCapabilities().getCapability("Chrome Browser"));
    HomePage homePageChrome = new HomePage(chromeDriver);
    homePageChrome.open();

    // Firefox default capabilities builder usage:
    getDriver("firefox", new FirefoxCapabilities().getCapability("Firefox Browser"));
    HomePage homePageFirefox = new HomePage(getDriver("firefox")); // return named "firefox" driver from the pool to init HomePage.
    homePageFirefox.open();

    // Safari, Edge, Opera and IE default capabilities builder usage:
    HomePage homePageSafari = new HomePage(getDriver("safari", new SafariCapabilities().getCapability("Safari Browser")));
    homePageSafari.open();
    
    getDriver("edge", new EdgeCapabilities().getCapability("Edge Browser"));
    getDriver("opera", new OperaCapabilities().getCapability("Opera Browser"));
    getDriver("ie", new IECapabilities().getCapability("Internet Explorer Browser"));

}

@Test
public void desiredCapsTest() {
    // Manage DesiredCapabilities on your own to build complicated caps structure:
    DesiredCapabilities capabilities = DesiredCapabilities.safari();
    capabilities.setCapability(CapabilityType.PLATFORM_NAME, SpecialKeywords.MAC);

    HomePage safariHomePage = new HomePage(getDriver("safari", capabilities));
    safariHomePage.open();

    Assert.assertTrue(safariHomePage.isPageOpened());
}
```

###Capabilities 

Simple key value Selenium/Appium pairs can be provided in `_config.properties` using `capabilities.name=value` , for example:
```
capabilities.automationName=uiautomator2
capabilities.deviceName=Samsung_Galaxy_S10
capabilities.platformName=ANDROID
capabilities.app=https://qaprosoft.s3-us-west-2.amazonaws.com/carinademoexample.apk
capabilities.newCommandTimeout=180
```

Visit [selenium](https://github.com/SeleniumHQ/selenium/wiki/DesiredCapabilities) or [appium](https://appium.io/docs/en/writing-running-appium/caps/) to see all capabilities.

###Options 

Options and arguments could be provided through `_config.properties` using comma separated values for multiple options/args, for example:
```
firefox_args=--no-first-run,--disable-notifications
firefox_preferences=
chrome_args=
chrome_experimental_opts=
chrome_mobile_emulation_opts=
```

To provide complicated structures use advanced approach to build capabilities/options/arguments:
```
public void someTest() {
    FirefoxOptions options = new FirefoxOptions();
    options.addArguments("--no-first-run");
    options.addArguments("--disable-notifications");

    DesiredCapabilities capabilities = DesiredCapabilities.firefox();
    capabilities.setCapability(CapabilityType.PLATFORM_NAME, SpecialKeywords.MAC);
    capabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, options);

    HomePage homePage = new HomePage(getDriver("firefox", capabilities));
    homePage.open();

    Assert.assertTrue(homePage.isPageOpened());
}
```

##Quit
Quit driver operation is executed automatically based on driver init phase, i.e. **no need to do it inside your test code**.

* `@BeforeSuite` drivers belong to all tests/classes and will be closed in `@AfterSuite` only
* `@BeforeTest` drivers belong to all `<test>` classes and will be closed in `@AfterTest`.
* `@BeforeClass` drivers belong to all tests inside current class and will be closed in `@AfterClass`
* `@BeforeMethod` or inside `Test Method` drivers belong to current test method and will be closed in `@AfterMethod`
  > Also driver is saved for all dependent test methods inside test class by default.

To quit driver forcibly if needed use **quitDriver()** or **quitDriver(name)**

To disable driver quit strategy completely and cotrol drivers init/quit on your own provide `forcibly_disable_driver_quit=true` or execute from any place of your test code `CarinaListener.disableDriversCleanup();`

##Restart
* **restartDriver()** quit current driver and start new one with the same capabilities
* **restartDriver(boolean isSameDevice)** quit current driver and start new one on the same device using `uuid` capability. It is fully compatible with [MCloud](https://github.com/zebrunner/mcloud) farm.
###Tricks
####Init pages and drivers in places where they are used, 
Correct way:
```
public class TestSample implements IAbstractTest {
  @Test(){
  public void someTest(){
     //Page declared and initialized right before using it
     HomePage homePage = new HomePage(getDriver());
     homePage.open();
     FooterMenu footerMenu = homePage.getFooterMenu();
     CompareModelsPage comparePage = footerMenu.openComparePage();
     List<ModelSpecs> specs = comparePage.compareModels("Samsung Galaxy J3", "Samsung Galaxy J5", "Samsung Galaxy J7 Pro");
  }
}
```
Not like this:
```
public class TestSample implements IAbstractTest {
  HomePage homePage = new HomePage(driver);
  WebDriver driver;
  FooterMenu footerMenu;
  
  @BeforeSuite
  public void driverInit(){
      driver = getDriver();
  }
  
  @Test(){
  public void someTest(){
     homePage.open();
     footerMenu = homePage.getFooterMenu();
     CompareModelsPage comparePage = footerMenu.openComparePage();
     List<ModelSpecs> specs = comparePage.compareModels("Samsung Galaxy J3", "Samsung Galaxy J5", "Samsung Galaxy J7 Pro");
  }
}
```