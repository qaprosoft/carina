package com.qaprosoft.carina.core.foundation.api.adapter;

import com.qaprosoft.carina.core.foundation.api.ApiMethodPreparator;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.OptionsMapping;
import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

public class OptionsMappingAdapter extends MappingAdapter<OptionsMapping> {

    public OptionsMappingAdapter() {
        super(HttpMethodType.GET, OptionsMapping.class);
    }

    @Override
    public ApiMethodPreparator convert(OptionsMapping apiMappingAnnotation) {
        return new ApiMethodPreparator(
                apiMappingAnnotation.path(),
                null,
                apiMappingAnnotation.responseTemplatePath(),
                apiMappingAnnotation.propertiesPath(),
                apiMappingAnnotation.responseStatus()
        );
    }
}
