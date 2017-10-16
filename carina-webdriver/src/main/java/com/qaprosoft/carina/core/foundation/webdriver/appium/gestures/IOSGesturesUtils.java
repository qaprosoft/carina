package com.qaprosoft.carina.core.foundation.webdriver.appium.gestures;

import com.qaprosoft.carina.core.foundation.webdriver.DriverPool;
import com.qaprosoft.carina.core.foundation.webdriver.decorator.ExtendedWebElement;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import org.openqa.selenium.Dimension;

/**
 * Created by yauhenipatotski on 2/9/17.
 */
@Deprecated
public final class IOSGesturesUtils extends IGesturesUtils {

    private IOSGesturesUtils() {
    }

    @Deprecated
    public static void swipeToBottom() {
        Dimension size = DriverPool.getDriver().manage().window().getSize();
        TouchAction swipe = new TouchAction((AppiumDriver) DriverPool.getDriver()).press(size.width / 2, (int) (size.height * 0.7)).waitAction(2000)
                .moveTo(0, (int) (-size.height * 0.5)).release().perform();
        swipe.perform();
    }

    @Deprecated
    public static void swipeToTop() {
        Dimension size = DriverPool.getDriver().manage().window().getSize();
        TouchAction swipe = new TouchAction((AppiumDriver) DriverPool.getDriver()).press(size.width / 2, (int) (size.height * 0.2)).waitAction(2000)
                .moveTo(0, (int) (size.height * 0.7)).release();
        swipe.perform();
    }

    public static void swipeLeft() {
        throw new UnsupportedOperationException("Method not supported for following platform");
    }

    public static void swipeRight() {
        throw new UnsupportedOperationException("Method not supported for following platform");
    }

    @Deprecated
    public static synchronized void scrollToElement(ExtendedWebElement extendedWebElement, int swipeCount) {

        if (!extendedWebElement.getElement().isDisplayed()) {
            int i = 0;
            do {
                swipeToBottom();
                i++;
            } while (!extendedWebElement.getElement().isDisplayed() || swipeCount > i);
        }
    }

}
