[![Carina - Web automation](https://raw.githubusercontent.com/qaprosoft/carina/master/docs/img/video.png)](https://youtu.be/Wgyffk7hJQw)

Note: Starting from 7.0.4 consider that instead of `extends AbstractTest` we have to `implements IAbstractTest` interface

Carina framework follows Selenium best practices for web test automation. If you are familiar with Selenium WebDriver and have already implemented a few tests with the Page Object Pattern, the following guide will be much easier for understanding. We have chosen [GSM Arena](https://www.gsmarena.com/) public web site for demonstration purposes, the whole test source code is located in [carina-demo](https://github.com/zebrunner/carina-demo) Github repo.


### Implementation of Page Objects
When you are writing functional tests using Selenium, the major part of your code will consist of interactions with the web interface you are testing through the WebDriver API. After fetching elements, you will verify some state of an element through various assertions and move on to fetching the next element. You may find WebElements directly in your tests:
```
List<WebElement> zipCodes = driver.findElements(By.id("zipCodes"));
for (WebElement zipCode : zipCodes) {
    if (zipCode.getText().equals("12345")){
        zipCode.click();
        break;
    }
}
WebElement city = driver.findElement(By.id("city"));
assertEquals("MyCityName", city.getText());
```
Some of the typical problems for this type of Selenium test are:

* Test cases are difficult to read
* Changes in the UI break multiple tests, often in several places
* Duplication of selectors both inside and across tests - no reuse

So, instead of having each test fetch elements directly and being fragile towards the UI changes, the Page Object Pattern introduces what is basically a decoupling layer. 

You create an object that represents the UI you want to test, which can be a whole page or a significant part of it. The responsibility of this object is to wrap HTML elements and encapsulate interactions with the UI, meaning that this is where all calls to WebDriver will go. This is where most WebElements are. And this is the only place you need to modify when the UI changes.

![Page Object Pattern](../img/page-objects.png)

In general, Page Object contains locators of the elements situated on the page and some business logic that may be reused by different tests:
```
public class ModelInfoPage extends AbstractPage {
    @FindBy(css = ".help-display strong")
    private ExtendedWebElement displayInfoLabel;

    @FindBy(css = ".help-camera strong")
    private ExtendedWebElement cameraInfoLabel;

    @FindBy(css = ".help-expansion strong")
    private ExtendedWebElement displayRamLabel;

    @FindBy(css = ".help-battery strong")
    private ExtendedWebElement batteryInfoLabel;

    public ModelInfoPage(WebDriver driver) {
        super(driver);
    }

    public String readDisplay() {
        assertElementPresent(displayInfoLabel);
        return displayInfoLabel.getText();
    }

    public String readCamera() {
        assertElementPresent(cameraInfoLabel);
        return cameraInfoLabel.getText();
    }

    public String readRam() {
        assertElementPresent(displayRamLabel);
        return displayRamLabel.getText();
    }

    public String readBattery() {
        assertElementPresent(displayInfoLabel);
        return batteryInfoLabel.getText();
    }
}

```
**Important:**

* Page should extend **com.qaprosoft.carina.core.gui.AbstractPage**
* Use **com.qaprosoft.carina.core.foundation.webdriver.decorator.ExtendedWebElement** instead of Selenium WebElement
* Locate Page Object classes in src/main/java


### Implementation of UI Components
In some cases, it is useful to implement UI Objects that may be reused between multiple Page Objects. For instance, a top menu or footer may be shared between multiple pages:
```
public class FooterMenu extends AbstractUIObject {
    @FindBy(linkText = "Home")
    private ExtendedWebElement homeLink;

    @FindBy(linkText = "Compare")
    private ExtendedWebElement compareLink;

    public FooterMenu(WebDriver driver, SearchContext searchContext) {
        super(driver, searchContext);
    }

    public HomePage openHomePage() {
        homeLink.click();
        return new HomePage(driver);
    }

    public CompareModelsPage openComparePage() {
        compareLink.click();
        return new CompareModelsPage(driver);
    }
}
```
And then you can use this in Page Object:
```
public class HomePage extends AbstractPage {
    @FindBy(id = "footmenu")
    private FooterMenu footerMenu;

    @FindBy(xpath = "//div[contains(@class, 'brandmenu-v2')]//a")
    private List<ExtendedWebElement> brandLinks;

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public FooterMenu getFooterMenu() {
        return footerMenu;
    }

    public BrandModelsPage selectBrand(String brand) {
        LOGGER.info("selecting '" + brand + "' brand...");
        for (ExtendedWebElement brandLink : brandLinks) {
            String currentBrand = brandLink.getText();
            LOGGER.info("currentBrand: " + currentBrand);
            if (brand.equalsIgnoreCase(currentBrand)) {
                brandLink.click();
                return new BrandModelsPage(driver);
            }
        }
        throw new RuntimeException("Unable to open brand: " + brand);
    }
}
```
**Important:**

* UI Object should extend **com.qaprosoft.carina.core.gui.AbstractUIObject**
* You should call the super constructor **super(driver, searchContext)** where searchContext is an instance of **org.openqa.selenium.SearchContext**
* Locate UI Object classes in src/main/java source folder

### Implementation of tests
Carina framework uses TestNG for test organization. In general, test represents a manipulation with Page Objects and additional validations of UI events. Here is sample test implementation:
```
public class WebSampleTest implements IAbstractTest {
    @Test()
    @MethodOwner(owner = "qpsdemo")
    public void testCompareModels() {
        // Open GSM Arena home page and verify page is opened
        HomePage homePage = new HomePage(getDriver());
        homePage.open();
        Assert.assertTrue(homePage.isPageOpened(), "Home page is not opened");
        // Open model compare page
        FooterMenu footerMenu = homePage.getFooterMenu();
        Assert.assertTrue(footerMenu.isUIObjectPresent(2), "Footer menu wasn't found!");
        CompareModelsPage comparePage = footerMenu.openComparePage();
        // Compare 3 models
        List<ModelSpecs> specs = comparePage.compareModels("Samsung Galaxy J3", "Samsung Galaxy J5", "Samsung Galaxy J7 Pro");
        // Verify model announced dates
        Assert.assertEquals(specs.get(0).readSpec(SpecType.ANNOUNCED), "2015, November");
        Assert.assertEquals(specs.get(1).readSpec(SpecType.ANNOUNCED), "2016, September");
        Assert.assertEquals(specs.get(2).readSpec(SpecType.ANNOUNCED), "2017, June");
    }
}

```
It is good practice to implement all elements search logic of Page Object/UI Object side and perform assertions and validations in the test, do not mix this logic.

**Important:**

* Test class should implement **com.qaprosoft.carina.core.foundation.IAbstractTest**
* Test method should start with **org.testng.annotations.Test** annotation
* Use **getDriver()** method to get driver instance in the test
* Locate tests in src/test/java source folder

### Test configuration
There are a few critical properties in a _config.properties file which are required for web test execution:

* url=http://www.gsmarena.com
* capabilities.browserName=chrome

The implemented test cases should be placed in a TestNG xml file according to the test group the test belongs to. You can find more details about TestNG configuration in the [official documentation](http://testng.org/doc/documentation-main.html).
```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite verbose="1" name="Carina Demo Tests - Web Sample" parallel="methods">

	<test name="GSM arena web tests">
		<classes>
			<class name="com.qaprosoft.carina.demo.WebSampleTest" />
		</classes>
	</test>
	
</suite>
```

### Page opening strategy

Determines how carina detects whether expected page is opened:

* By Url. (by default)
* By Element presence on the page
* By URL and Element

To check if page was opened is used: 
```
page.isPageOpened();
//or
page.assertPageOpened();  // equals Assert.assertTrue(page.isPageOpened(),"PageName not loaded: reason);
```

Page opening strategy configuration can be set in several places:

1) in [_config.properties](http://zebrunner.github.io/carina/configuration/). This determines whole project page open strategy.

2) In page.class. This overrides global page opening strategy for a specific page.

	```
	public class Page extends AbstractPage {

	    public Page(WebDriver driver){
		super(driver);
		setPageOpeningStrategy(PageOpeningStrategy.BY_URL);
	    }
	}
	```
	
3) In test.class. This also overrides global page opening strategy for a specific page.

	```
	@Test
	public void test(){
		HomePage homePage=new HomePage(getDriver());
		homePage.open();
		homePage.setPageOpeningStrategy(PageOpeningStrategy.BY_URL);
	}
	```

Strategy usage examples:

By URL
	
	```
	//This is a default value. To use it you need to set a real page urls into your page classes.
	
	private final String specificPageUrl = "https://www.gsmarena.com/specific/url";

	public Page(WebDriver driver) {
	    super(driver);
	    setPageOpeningStrategy(PageOpeningStrategy.BY_URL);

	    setPageAbsoluteURL(specificPageUrl);    //set's full url
	    //or
	    setPageURL("/specific/url");            //add's String to url from _config_properties
	}
	```

By Element

	```
	//To use this strategy, you need to specify ui load marker.
	
	@FindBy(id = "id")
	private ExtendedWebElement element;

	public Page(WebDriver driver) {
	    super(driver);

	    setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
	    setUiLoadedMarker(element);
	}
	```

By URL and Element

	```
	private final String specificPageUrl = "https://www.gsmarena.com/specific/url";

	@FindBy(id = "id")
	private ExtendedWebElement element;

	public Page(WebDriver driver) {
	    super(driver);

	    setPageOpeningStrategy(PageOpeningStrategy.BY_URL_AND_ELEMENT);
	    setUiLoadedMarker(element);
	    setPageAbsoluteURL(specificPageUrl);
	}
	```

### Element loading strategy

Determines how carina detects appearing of web elements on page

* By presence. Carina waits for appearance of web elements in page DOM model.
* By visibility. Carina waits until web elements would be visible in page.
* By presence or visibility (default).

> It is recommended to use _element_loading_strategy=BY_VISIBILITY_ because in some cases condition with presence happens faster but elements are still not accessible due to invisibility at this short period of time.

Element loading strategy could be set at the same places as **Page opening strategy**.

To check if element presence:
```
Component component = Page.getComponent();
component.assertUIObjectPresent();      // equals to Assert.assertTrue(component.isUIObjectPresent(),"UI object componentName does not present!");

component.assertUIObjectNotPresent();   // equals to Assert.assertTrue(!component.isUIObjectPresent(),"UI object componentName presents!");
```
>Dynamic elements loading. 
**waitForJSToLoad()** method was introduced in AbstractPage class. It uses JS under the hood and helps to wait till all dynamic web elements on the page are getting loaded.

###Tricks
####Independent tests is the best way for the automation!
Correct example:
```
//Right way to write tests:
public class WebSampleTest implements IAbstractTest {
    @Test()
    public void testCompareModels() {
	// Open GSM Arena home page and verify page is opened
	HomePage homePage = new HomePage(getDriver());
	homePage.open();
	Assert.assertTrue(homePage.isPageOpened(), "Home page is not opened");
	// Open model compare page
	FooterMenu footerMenu = homePage.getFooterMenu();
	Assert.assertTrue(footerMenu.isUIObjectPresent(2), "Footer menu wasn't found!");
	CompareModelsPage comparePage = footerMenu.openComparePage();
	// Compare 3 models
	List<ModelSpecs> specs = comparePage.compareModels("Samsung Galaxy J3", "Samsung Galaxy J5", "Samsung Galaxy J7 Pro");
	// Verify model announced dates
	SoftAssert softAssert = new SoftAssert();
	softAssert.assertEquals(specs.get(0).readSpec(SpecType.ANNOUNCED), "2016, March 31");
	softAssert.assertEquals(specs.get(1).readSpec(SpecType.ANNOUNCED), "2015, June 19");
	softAssert.assertEquals(specs.get(2).readSpec(SpecType.ANNOUNCED), "2017, June");
	softAssert.assertAll();
    }
}
```
If there is no way to do so, make sure you use @Test(dependsOnMethods=XXX) tag:
```
public class WebSampleSingleDriver implements IAbstractTest {
    HomePage homePage = null;
    CompareModelsPage comparePage = null;
    List<ModelSpecs> specs = new ArrayList<>();

    @BeforeSuite
    public void startDriver() {
        // Open GSM Arena home page and verify page is opened
        homePage = new HomePage(getDriver());
    }
    
    @Test
    public void testOpenPage() {
        homePage.open();
        Assert.assertTrue(homePage.isPageOpened(), "Home page is not opened");
    }
    
    @Test(dependsOnMethods="testOpenPage") //for dependent tests Carina keeps driver sessions by default
    public void testOpenCompare() {
        // Open GSM Arena home page and verify page is opened
        // Open model compare page
        FooterMenu footerMenu = homePage.getFooterMenu();
        Assert.assertTrue(footerMenu.isUIObjectPresent(2), "Footer menu wasn't found!");
        comparePage = footerMenu.openComparePage();

    }
    
    @Test(dependsOnMethods="testOpenCompare") //for dependent tests Carina keeps driver sessions by default
    public void testReadSpecs() {
        // Compare 3 models
        specs = comparePage.compareModels("Samsung Galaxy J3", "Samsung Galaxy J5", "Samsung Galaxy J7 Pro");
    }
    
    @Test(dependsOnMethods="testReadSpecs") //for dependent tests Carina keeps driver sessions by default
    public void testCompareModels() {
        // Verify model announced dates
        SoftAssert() softAssert = new SoftAssert();
        softAssert.assertEquals(specs.get(0).readSpec(SpecType.ANNOUNCED), "2016, March 31");
        softAssert.assertEquals(specs.get(1).readSpec(SpecType.ANNOUNCED), "2015, June 19");
        softAssert.assertEquals(specs.get(2).readSpec(SpecType.ANNOUNCED), "2017, June");
        softAssert.assertAll();
    }
}
```
#### Operate with web elements in page classes only!
Correct way:
```
//In page class:
@FindBy(id = "js-lang-list-button")
private ExtendedWebElement langListBtn;
    
@FindBy(xpath = "//div[@id='js-lang-lists']//a")
private List<ExtendedWebElement> langList;

public WikipediaLocalePage goToWikipediaLocalePage(WebDriver driver) {
   openLangList();
   if (!langList.isEmpty()) {
      for (ExtendedWebElement languageBtn : langList) {
         String localeStr = Configuration.get(Configuration.Parameter.LOCALE);
         Locale locale = parseLocale(localeStr);
         if (languageBtn.getAttribute("lang").equals(locale.getLanguage())) {
           languageBtn.click();
           return new WikipediaLocalePage(driver);
         }
      }
   }
   throw new RuntimeException("No language ref was found");
}

public void openLangList() {
   langListBtn.clickIfPresent();
}


//In test class:
public void someTest() {
   WikipediaHomePage wikipediaHomePage = new WikipediaHomePage(getDriver());
   wikipediaHomePage.open();
   WikipediaLocalePage wikipediaLocalePage = wikipediaHomePage.goToWikipediaLocalePage(getDriver());
}
```
Not like this:
```
//In page class:
@FindBy(id = "js-lang-list-button")
public ExtendedWebElement langListBtn;
    
@FindBy(xpath = "//div[@id='js-lang-lists']//a")
private List<ExtendedWebElement> langList;

public List<ExtendedWebElement> getLangList(){
   return langList;
}


//In test class:
public void someTest() {
   WikipediaHomePage wikipediaHomePage = new WikipediaHomePage(getDriver());
   wikipediaHomePage.open();
   wikipediaHomePage.langListBtn.clickIfPresent();
   WikipediaLocalePage wikipediaLocalePage = null;
    for (ExtendedWebElement languageBtn : wikipediaHomePage.getLangList()) {
         String localeStr = Configuration.get(Configuration.Parameter.LOCALE);
         Locale locale = parseLocale(localeStr);
         if (languageBtn.getAttribute("lang").equals(locale.getLanguage())) {
           languageBtn.click();
           wikipediaLocalePage = new WikipediaLocalePage(driver);
         }
    }
}
```
