package com.qaprosoft.carina.core.foundation.cucumber;

import com.qaprosoft.carina.core.foundation.utils.image.ImageProcessing;
import com.qaprosoft.carina.core.foundation.webdriver.DriverPool;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

public class CucumberBaseTest extends CucumberRunner {


    /**
     * Check is it Cucumber Test or not.
     *
     * @throws Throwable java.lang.Throwable
     */
    @Before
    public void beforeScenario() throws Throwable {
        if (!isCucumberTest()) {
            throw new Exception("Not Cucumber Test. Please check your configuration and config.properties file.");
        }
    }

    /**
     * take Screenshot Of Failure - this step should be added manually in common step definition
     * files if it will not be executed automatically
     *
     * @param scenario Scenario
     */
    @After
    public void takeScreenshotOfFailure(Scenario scenario) {
        LOGGER.info("In  @After takeScreenshotOfFailure");
        if (scenario.isFailed()) {
            LOGGER.error("Cucumber Scenario FAILED! Creating screenshot.");
            //TODO: remove reference onto the DriverPool reusing functionality from Screenshot object!
            byte[] screenshot = ((TakesScreenshot) DriverPool.getDriver()).getScreenshotAs(OutputType.BYTES);
            screenshot = ImageProcessing.imageResize(screenshot);
            scenario.embed(screenshot, "image/png"); //stick it in the report
        }

    }
}
