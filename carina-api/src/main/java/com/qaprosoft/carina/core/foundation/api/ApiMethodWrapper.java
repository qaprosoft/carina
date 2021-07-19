package com.qaprosoft.carina.core.foundation.api;

import io.restassured.response.Response;

@FunctionalInterface
public interface ApiMethodWrapper {

    void prepare(ApiOpContext apiOpContext);

    default Response getResult() {
        return null;
    }

}
