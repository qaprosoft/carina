package com.qaprosoft.carina.core.foundation.api.adapter;

import com.qaprosoft.carina.core.foundation.api.ApiMethodPreparator;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.PutMapping;
import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

public class PutMappingAdapter extends MappingAdapter<PutMapping> {

    public PutMappingAdapter() {
        super(HttpMethodType.GET, PutMapping.class);
    }

    @Override
    public ApiMethodPreparator convert(PutMapping apiMappingAnnotation) {
        return new ApiMethodPreparator(
                apiMappingAnnotation.path(),
                apiMappingAnnotation.requestTemplatePath(),
                apiMappingAnnotation.responseTemplatePath(),
                apiMappingAnnotation.propertiesPath(),
                apiMappingAnnotation.responseStatus()
        );
    }
}
