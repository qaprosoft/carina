package com.qaprosoft.carina.core.foundation.dataprovider.core.impl;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;

import com.qaprosoft.carina.core.foundation.dataprovider.parser.DSBean;
import com.qaprosoft.carina.core.foundation.utils.ParameterGenerator;

/**
 * Created by Patotsky on 19.12.2014.
 */

public abstract class BaseDataProvider {

    protected Map<String, String> testNameArgsMap = Collections.synchronizedMap(new HashMap<String, String>());
    protected Map<String, String> testMethodNameArgsMap = Collections.synchronizedMap(new HashMap<String, String>());
	protected Map<String, String> canonicalTestNameArgsMap = Collections.synchronizedMap(new HashMap<String, String>());
    
    protected Map<String, String> testMethodOwnerArgsMap = Collections.synchronizedMap(new HashMap<String, String>());

    protected Map<String, String> jiraArgsMap = Collections.synchronizedMap(new HashMap<String, String>());
    protected Map<String, String> testRailsArgsMap = Collections.synchronizedMap(new HashMap<String, String>());
	protected Map<String, String> bugArgsMap = Collections.synchronizedMap(new HashMap<String, String>());

	protected List<String> argsList;
    protected List<String> staticArgsList;

	protected List<String> doNotRunTestNames;

	public abstract Object[][] getDataProvider(Annotation annotation, ITestContext context, ITestNGMethod testMethod);

    protected static Object getStaticParam(String name, ITestContext context, DSBean dsBean) {
        return ParameterGenerator.process(dsBean
                        .getTestParams().get(name));
    }

    public Map<String, String> getTestNameArgsMap() {
        return testNameArgsMap;
    }
    
    public Map<String, String> getTestMethodNameArgsMap() {
        return testMethodNameArgsMap;
    }
    
    public Map<String, String> getTestMethodOwnerArgsMap() {
        return testMethodOwnerArgsMap;
    }


    public Map<String, String> getJiraArgsMap() {
        return jiraArgsMap;
    }

    public Map<String, String> getTestRailsArgsMap() {
		return testRailsArgsMap;
	}

	public Map<String, String> getBugArgsMap()
	{
		return bugArgsMap;
	}

	public List<String> getDoNotRunRowsIDs()
	{
		return doNotRunTestNames;
	}

	public Map<String, String> getCanonicalTestNameArgsMap()
	{
		return canonicalTestNameArgsMap;
	}

}
