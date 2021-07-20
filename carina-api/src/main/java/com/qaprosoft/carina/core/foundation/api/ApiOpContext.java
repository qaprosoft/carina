package com.qaprosoft.carina.core.foundation.api;

public class ApiOpContext {

    private final AbstractApiMethodV2 apiMethod;

    ApiOpContext(AbstractApiMethodV2 apiMethod) {
        this.apiMethod = apiMethod;
    }

    public AbstractApiMethodV2 getApiMethod() {
        return apiMethod;
    }

}
