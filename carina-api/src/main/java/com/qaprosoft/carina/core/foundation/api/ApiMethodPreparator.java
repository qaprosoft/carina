package com.qaprosoft.carina.core.foundation.api;

import com.qaprosoft.carina.core.foundation.api.http.HttpResponseStatusType;
import org.apache.commons.lang3.StringUtils;

public class ApiMethodPreparator {

    private final String path;
    private final String requestTemplatePath;
    private final String responseTemplatePath;
    private final String propertiesPath;
    private final HttpResponseStatusType responseStatus;

    public ApiMethodPreparator(String path, String requestTemplatePath, String responseTemplatePath, String propertiesPath, HttpResponseStatusType responseStatus) {
        this.path = path;
        this.requestTemplatePath = prepareAnnotationStringValue(requestTemplatePath);
        this.responseTemplatePath = prepareAnnotationStringValue(responseTemplatePath);
        this.propertiesPath = prepareAnnotationStringValue(propertiesPath);
        this.responseStatus = responseStatus;
    }

    public String getPath() {
        return path;
    }

    public String getRequestTemplatePath() {
        return requestTemplatePath;
    }

    public String getResponseTemplatePath() {
        return responseTemplatePath;
    }

    public String getPropertiesPath() {
        return propertiesPath;
    }

    public HttpResponseStatusType getResponseStatus() {
        return responseStatus;
    }

    private String prepareAnnotationStringValue(String value) {
        return StringUtils.isEmpty(value) ? null : value;
    }
}
