package com.qaprosoft.carina.core.foundation.api;

@FunctionalInterface
public interface ApiMethodWrapper {

    void prepare(ApiOpContext apiOpContext);

    default ApiResponseWrapper getResult() {
        return null;
    }

}
