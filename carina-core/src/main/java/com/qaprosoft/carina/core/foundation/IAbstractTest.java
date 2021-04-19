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
package com.qaprosoft.carina.core.foundation;

import java.lang.annotation.Annotation;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;

import com.nordstrom.automation.testng.LinkedListeners;
import com.qaprosoft.carina.core.foundation.dataprovider.core.DataProviderFactory;
import com.qaprosoft.carina.core.foundation.listeners.CarinaListener;
import com.qaprosoft.carina.core.foundation.report.testrail.ITestCases;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.common.CommonUtils;
import com.qaprosoft.carina.core.foundation.utils.factory.ICustomTypePageFactory;
import com.zebrunner.agent.core.registrar.CurrentTest;
import com.zebrunner.agent.testng.listener.DataProviderInterceptor;
import com.zebrunner.agent.testng.listener.TestRunListener;

/*
 * IAbstractTest - base test for UI and API tests.
 */

// https://github.com/qaprosoft/carina/issues/951
// reused com.nordstrom.tools.testng-foundation to register ordered listeners

// on start order is TestRunListener and CarinaListener
// on finish reverse order, i.e. CarinaListener, TestRunListener
@LinkedListeners({ CarinaListener.class, TestRunListener.class, DataProviderInterceptor.class })
@Listeners({DataProviderInterceptor.class})
public interface IAbstractTest extends ICustomTypePageFactory, ITestCases {

    long EXPLICIT_TIMEOUT = Configuration.getLong(Parameter.EXPLICIT_TIMEOUT);

    @BeforeSuite(alwaysRun = true)
    private void onCarinaBeforeSuite() {
        // do nothing
    }

    @BeforeClass(alwaysRun = true)
    private void onCarinaBeforeClass() {
        // do nothing
    }

    @BeforeMethod(alwaysRun = true)
    private void onCarinaBeforeMethod() {
        // do nothing
    }

    @DataProvider(name = "DataProvider", parallel = true)
    default Object[][] createData(final ITestNGMethod testMethod, ITestContext context) {
        Annotation[] annotations = testMethod.getConstructorOrMethod().getMethod().getDeclaredAnnotations();
        Object[][] objects = DataProviderFactory.getDataProvider(annotations, context, testMethod);
        return objects;
    }

    @DataProvider(name = "SingleDataProvider")
    default Object[][] createDataSingleThread(final ITestNGMethod testMethod,
            ITestContext context) {
        Annotation[] annotations = testMethod.getConstructorOrMethod().getMethod().getDeclaredAnnotations();
        Object[][] objects = DataProviderFactory.getDataProvider(annotations, context, testMethod);
        return objects;
    }

    /**
     * Pause for specified timeout.
     *
     * @param timeout in seconds.
     */
    default void pause(long timeout) {
        CommonUtils.pause(timeout);
    }

    default void pause(Double timeout) {
        CommonUtils.pause(timeout);
    }

    default void skipExecution(String message) {
        CurrentTest.revertRegistration();
        throw new SkipException(message);
    }
}
