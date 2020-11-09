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
package com.qaprosoft.carina.core.foundation.webdriver.listener;

import java.io.File;
import java.util.Arrays;

import com.qaprosoft.zafira.util.UploadUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.openqa.selenium.support.events.WebDriverEventListener;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.qaprosoft.carina.core.foundation.report.ReportContext;
import com.qaprosoft.carina.core.foundation.utils.FileManager;
import com.qaprosoft.carina.core.foundation.webdriver.IDriverPool;
import com.qaprosoft.carina.core.foundation.webdriver.Screenshot;
import com.qaprosoft.zafira.client.ZafiraSingleton;
import com.qaprosoft.zafira.models.dto.TestArtifactType;

/**
 * ScreenshotEventListener - captures screenshot after essential webdriver event.
 * IMPORTANT! Please avoid any driver calls with extra listeners (recursive exception generation)
 * 
 * @author Alex Khursevich (alex@qaprosoft.com)
 */
public class DriverListener implements WebDriverEventListener {
    private static final Logger LOGGER = Logger.getLogger(DriverListener.class);
    
	// 1. register live vnc url in DriverFactory (method streamVNC should return valid TestArtifactType
	// 2. DriverFactory->getEventListeners(TestArtifactType vncArtifact)
	// 3. declare vncArtifact using constructor in DriverListener
	// 4. onBefore any action try to register vncArtifact in Zafira. Detailed use-cases find in onBeforeAction method 
	protected TestArtifactType vncArtifact;
	
	public DriverListener(TestArtifactType vncArtifact) {
		this.vncArtifact = vncArtifact;
	}
	
    private final static ThreadLocal<String> currentPositiveMessage = new ThreadLocal<String>();
    private final static ThreadLocal<String> currentNegativeMessage = new ThreadLocal<String>();

    @Override
    public void afterAlertAccept(WebDriver driver) {
        onAfterAction("Alert accepted", driver);
    }

    @Override
    public void afterAlertDismiss(WebDriver driver) {
        onAfterAction("Alert dismissed", driver);
    }

    @Override
    public void afterChangeValueOf(WebElement element, WebDriver driver, CharSequence[] value) {
        String comment = String.format("Text '%s' typed", charArrayToString(value));
        captureScreenshot(comment, driver, element, false);
    }

    @Override
    public void afterClickOn(WebElement element, WebDriver driver) {
        String comment = "Element clicked";
        captureScreenshot(comment, driver, element, false);
    }

    @Override
    public void afterFindBy(By by, WebElement element, WebDriver driver) {
        // Do nothing
    }

    @Override
    public void afterNavigateBack(WebDriver driver) {
        onAfterAction("Navigated back", driver);
    }

    @Override
    public void afterNavigateForward(WebDriver driver) {
        onAfterAction("Navigated forward", driver);
    }

    @Override
    public void afterNavigateRefresh(WebDriver driver) {
        onAfterAction("Page refreshed", driver);
    }

    @Override
    public void afterNavigateTo(String url, WebDriver driver) {
        String comment = String.format("URL '%s' opened", url);
        onAfterAction(comment, driver);
    }

    @Override
    public void afterScript(String script, WebDriver driver) {
        // Do nothing
    }

    @Override
    public void beforeAlertAccept(WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void beforeAlertDismiss(WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void beforeChangeValueOf(WebElement element, WebDriver driver, CharSequence[] value) {
    	onBeforeAction();
    }

    @Override
    public void beforeClickOn(WebElement element, WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void beforeFindBy(By by, WebElement element, WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void beforeNavigateBack(WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void beforeNavigateForward(WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void beforeNavigateRefresh(WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void beforeNavigateTo(String script, WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void beforeScript(String script, WebDriver driver) {
    	onBeforeAction();
    }

    @Override
    public void onException(Throwable thr, WebDriver driver) {
        // [VD] make below code as much safety as possible otherwise potential recursive failure could occur with driver related issue.
        // most suspicious are capture screenshots, generating dumps etc
        if (thr == null 
                || thr.getMessage() == null
                || thr.getMessage().contains("Method has not yet been implemented")
                || thr.getMessage().contains("Method is not implemented")
                || thr.getMessage().contains("An element could not be located on the page using the given search parameters")
                || thr.getMessage().contains("no such element: Unable to locate element")
                // carina has a lot of extra verifications to solve all stale reference issue and finally perform an action so ignore such exception in listener!
                || thr.getMessage().contains("StaleElementReferenceException")
                || thr.getMessage().contains("stale_element_reference.html")) {
            // do nothing
            return;
        }
        
        // handle use-case when application crashed on iOS but tests continue to execute something because doesn't raise valid exception
        // Example:

        // 10:25:20 2018-09-14 10:29:39 DriverListener [TestNG-31] [ERROR]
        // [iPhone_6s] An unknown server-side error occurred while
        // processing the command. Original error: The application under
        // test with bundle id 'Q5AWL8WCY6' is not running,
        // possibly crashed (WARNING: The server did not provide any
        // stacktrace information)

        // TODO: investigate if we run @AfterMethod etc system events after this crash
        if (thr.getMessage().contains("is not running, possibly crashed")) {
            throw new RuntimeException(thr);
        }
        
        // hopefully castDriver below resolve root cause of the recursive onException calls but keep below if to ensure
        if (thr.getStackTrace() != null
                && (Arrays.toString(thr.getStackTrace()).contains("com.qaprosoft.carina.core.foundation.webdriver.listener.DriverListener.onException")
                || Arrays.toString(thr.getStackTrace()).contains("Unable to capture screenshot due to the WebDriverException"))) {
            LOGGER.error("Do not generate screenshot for invalid driver!");
            // prevent recursive crash for onException
            return;
        }
        
        LOGGER.debug("DriverListener->onException starting..." + thr.getMessage());
        driver = castDriver(driver);

        try {
            // 1. if you see mess with afterTest carina actions and Timer startup failure you should follow steps #2+ to determine root cause.
            //      Driver initialization 'default' FAILED! Retry 1 of 1 time - Operation already started: mobile_driverdefault
            // 2. carefully track all preliminary exception for the same thread to detect 1st problematic exception
            // 3. 99% those root exception means that we should prohibit screenshot generation for such use-case
            // 4. if 3rd one is true just update Screenshot.isCaptured() adding part of the exception to the list
            // handle cases which should't be captured
            if (Screenshot.isCaptured(thr.getMessage())) {
                captureScreenshot(thr.getMessage(), driver, null, true);
            }
        } catch (Exception e) {
            if (!e.getMessage().isEmpty()
                    && (e.getMessage().contains("Method has not yet been implemented") || (e.getMessage().contains("Method is not implemented")))) {
                LOGGER.debug("Unrecognized exception detected in DriverListener->onException! " + e.getMessage(), e);
            } else {
                LOGGER.error("Unrecognized exception detected in DriverListener->onException! " + e.getMessage(), e);
            }
        } catch (Throwable e) {
            LOGGER.error("Take a look to the logs above for current thread and add exception into the exclusion for Screenshot.isCaptured(). " + e.getMessage(), e);
        }
        
        LOGGER.debug("DriverListener->onException finished.");
    }

    /**
     * Converts char sequence to string.
     * 
     * @param csa - char sequence array
     * @return string representation
     */
    private String charArrayToString(CharSequence[] csa) {
        String s = StringUtils.EMPTY;
        if (csa != null) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence cs : csa) {
                sb.append(String.valueOf(cs));
            }
            s = sb.toString();
        }
        return s;
    }

    @Override
    public void afterSwitchToWindow(String arg0, WebDriver driver) {
        // do nothing

    }

    @Override
    public void beforeSwitchToWindow(String arg0, WebDriver driver) {
    	onBeforeAction();
    }
    
    @Override
    public <X> void afterGetScreenshotAs(OutputType<X> arg0, X arg1) {
        // do nothing
        
    }

    @Override
    public <X> void beforeGetScreenshotAs(OutputType<X> arg0) {
        onBeforeAction();
    }

    @Override
    public void afterGetText(WebElement element, WebDriver driver, String arg2) {
        // do nothing       
    }

    @Override
    public void beforeGetText(WebElement element, WebDriver driver) {
        // do nothing       
    }    

    private void captureScreenshot(String comment, WebDriver driver, WebElement element, boolean errorMessage) {
        driver = castDriver(driver);
        if (getMessage(errorMessage) != null) {
            comment = getMessage(errorMessage);
        }

        try {
            if (errorMessage) {
                LOGGER.error(comment);
                if (Screenshot.isEnabled()) {
                    String screenName = Screenshot.capture(driver, comment, true); // in case of failure
                    //do not generate UI dump if no screenshot
                    if (!screenName.isEmpty()) {
                        generateDump(screenName);
                    }
                }
            } else {
                LOGGER.info(comment);
                Screenshot.captureByRule(driver, comment);
            }
        } catch (Exception e) {
            LOGGER.debug("Unrecognized failure detected in DriverListener->captureScreenshot: " + e.getMessage(), e);
        } finally {
            resetMessages();
        }
    }

    private void generateDump(String screenName) {
        // XML layout extraction
        File uiDumpFile = IDriverPool.getDefaultDevice().generateUiDump(screenName);
        if (uiDumpFile != null) {
            // use the same naming but with zip extension. Put into the test artifacts folder
            String dumpArtifact = ReportContext.getArtifactsFolder().getAbsolutePath() + "/" + screenName.replace(".png", ".zip");
            LOGGER.debug("UI Dump artifact: " + dumpArtifact);
            
            // build path to screenshot using name 
            File screenFile = new File(ReportContext.getTestDir().getAbsolutePath() + "/" + screenName);
            
            // archive page source dump and screenshot both together
            FileManager.zipFiles(dumpArtifact, uiDumpFile, screenFile);

            UploadUtil.uploadArtifact(new File(dumpArtifact), "UI Dump artifact");
        } else {
            LOGGER.debug("Dump file is empty.");
        }
    }
    
    private void onAfterAction(String comment, WebDriver driver) {
        captureScreenshot(comment, driver, null, false);
    }
    
	private void onBeforeAction() {
		// 4a. if "tzid" not exist inside vncArtifact and exists in Reporter -> register new vncArtifact in Zafira.
		// 4b. if "tzid" already exists in current artifact but in Reporter there is another value. Then this is use case for class/suite mode when we share the same
		// driver across different tests

		ITestResult res = Reporter.getCurrentTestResult();
		if (res != null && res.getAttribute("ztid") != null) {
			Long ztid = (Long) res.getAttribute("ztid");
			if (!ztid.equals(vncArtifact.getTestId()) && vncArtifact != null && ! StringUtils.isBlank(vncArtifact.getName())) {
				vncArtifact.setTestId(ztid);
				LOGGER.debug("Registered live video artifact " + vncArtifact.getName() + " into zafira");
				ZafiraSingleton.INSTANCE.getClient().addTestArtifact(vncArtifact);
			}

		}
	}

    public static String getMessage(boolean errorMessage) {
    	if (errorMessage) {
    		return currentNegativeMessage.get();
    	} else {
    		return currentPositiveMessage.get();
    	}
    }

    public static void setMessages(String positiveMessage, String negativeMessage) {
        currentPositiveMessage.set(positiveMessage);
        currentNegativeMessage.set(negativeMessage);
    }

    private void resetMessages() {
        currentPositiveMessage.remove();
        currentNegativeMessage.remove();
    }
    
    /**
     * Cast Carina driver to WebDriver removing all extra listeners (try to avoid direct operations via WebDriver as it doesn't support logging etc)
     *
     * @return WebDriver
     */
    private WebDriver castDriver(WebDriver drv) {
        if (drv instanceof EventFiringWebDriver) {
            drv = ((EventFiringWebDriver) drv).getWrappedDriver();
        }
        return drv;
    }



}