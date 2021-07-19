package com.qaprosoft.carina.core.foundation.api.adapter;

import com.qaprosoft.carina.core.foundation.api.ApiMethodPreparator;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.PachMapping;
import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

public class PachMappingAdapter extends MappingAdapter<PachMapping> {

    public PachMappingAdapter() {
        super(HttpMethodType.GET, PachMapping.class);
    }

    @Override
    public ApiMethodPreparator convert(PachMapping apiMappingAnnotation) {
        return new ApiMethodPreparator(
                apiMappingAnnotation.path(),
                apiMappingAnnotation.requestTemplatePath(),
                apiMappingAnnotation.responseTemplatePath(),
                apiMappingAnnotation.propertiesPath(),
                apiMappingAnnotation.responseStatus()
        );
    }
}
