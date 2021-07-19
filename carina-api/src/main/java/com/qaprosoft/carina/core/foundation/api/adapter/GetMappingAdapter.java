package com.qaprosoft.carina.core.foundation.api.adapter;

import com.qaprosoft.carina.core.foundation.api.ApiMethodPreparator;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.GetMapping;
import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

public class GetMappingAdapter extends MappingAdapter<GetMapping> {

    public GetMappingAdapter() {
        super(HttpMethodType.GET, GetMapping.class);
    }

    @Override
    public ApiMethodPreparator convert(GetMapping apiMappingAnnotation) {
        return new ApiMethodPreparator(
                apiMappingAnnotation.path(),
                null,
                apiMappingAnnotation.responseTemplatePath(),
                apiMappingAnnotation.propertiesPath(),
                apiMappingAnnotation.responseStatus()
        );
    }
}
