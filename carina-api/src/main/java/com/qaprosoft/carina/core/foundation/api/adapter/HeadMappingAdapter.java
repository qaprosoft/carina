package com.qaprosoft.carina.core.foundation.api.adapter;

import com.qaprosoft.carina.core.foundation.api.ApiMethodPreparator;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.HeadMapping;
import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

public class HeadMappingAdapter extends MappingAdapter<HeadMapping> {

    public HeadMappingAdapter() {
        super(HttpMethodType.GET, HeadMapping.class);
    }

    @Override
    public ApiMethodPreparator convert(HeadMapping apiMappingAnnotation) {
        return new ApiMethodPreparator(
                apiMappingAnnotation.path(),
                null,
                apiMappingAnnotation.responseTemplatePath(),
                apiMappingAnnotation.propertiesPath(),
                apiMappingAnnotation.responseStatus()
        );
    }
}
