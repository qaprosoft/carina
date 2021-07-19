package com.qaprosoft.carina.core.foundation.api;

import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;
import com.qaprosoft.carina.core.foundation.utils.Configuration;

import java.util.Properties;

public class BaseApiMethod extends AbstractApiMethodV2 {

    public BaseApiMethod(String rqPath, String rsPath, String propertiesPath, HttpMethodType methodType, String path) {
        super(rqPath, rsPath, propertiesPath);
        super.methodType = methodType;

        String url = Configuration.getEnvArg("api_url") + path;
        setMethodPath(url);
    }

    public BaseApiMethod(String rqPath, String rsPath, Properties properties, HttpMethodType methodType, String path) {
        super(rqPath, rsPath, properties);
        super.methodType = methodType;

        String url = Configuration.getEnvArg("api_url") + path;
        setMethodPath(url);
    }
}
