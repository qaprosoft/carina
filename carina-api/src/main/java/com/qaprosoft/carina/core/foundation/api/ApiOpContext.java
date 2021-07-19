package com.qaprosoft.carina.core.foundation.api;

import io.restassured.response.Response;

public class ApiOpContext {

    private final AbstractApiMethodV2 apiMethod;
    private Response response;

    ApiOpContext(AbstractApiMethodV2 apiMethod) {
        this.apiMethod = apiMethod;
    }

    public AbstractApiMethodV2 getApiMethod() {
        return apiMethod;
    }

    public Response getResponse() {
        return response;
    }

    void setResponse(Response response) {
        this.response = response;
    }
}
