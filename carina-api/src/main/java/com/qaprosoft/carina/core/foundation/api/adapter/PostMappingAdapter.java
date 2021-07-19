package com.qaprosoft.carina.core.foundation.api.adapter;

import com.qaprosoft.carina.core.foundation.api.ApiMethodPreparator;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.PostMapping;
import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

public class PostMappingAdapter extends MappingAdapter<PostMapping> {

    public PostMappingAdapter() {
        super(HttpMethodType.GET, PostMapping.class);
    }

    @Override
    public ApiMethodPreparator convert(PostMapping apiMappingAnnotation) {
        return new ApiMethodPreparator(
                apiMappingAnnotation.path(),
                apiMappingAnnotation.requestTemplatePath(),
                apiMappingAnnotation.responseTemplatePath(),
                apiMappingAnnotation.propertiesPath(),
                apiMappingAnnotation.responseStatus()
        );
    }
}
