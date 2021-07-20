package com.qaprosoft.carina.core.foundation.api;

import com.qaprosoft.apitools.validation.XmlCompareMode;
import io.restassured.response.Response;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class ApiResponseWrapper {

    private final Response response;
    private final AbstractApiMethodV2 apiMethodV2;

    ApiResponseWrapper(Response response, AbstractApiMethodV2 apiMethodV2) {
        this.response = response;
        this.apiMethodV2 = apiMethodV2;
    }

    public void validateResponse(JSONCompareMode mode, String... validationFlags) {
        apiMethodV2.validateResponse(mode, validationFlags);
    }

    public void validateXmlResponse(XmlCompareMode mode) {
        apiMethodV2.validateXmlResponse(mode);
    }

    public void validateResponse(String... validationFlags) {
        apiMethodV2.validateResponse(validationFlags);
    }

    public void validateResponseAgainstSchema(String schemaPath) {
        apiMethodV2.validateResponseAgainstSchema(schemaPath);
    }

    public Response getResponse() {
        return response;
    }
}
