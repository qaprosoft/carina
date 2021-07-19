package com.qaprosoft.carina.core.foundation.api.adapter;

import com.qaprosoft.carina.core.foundation.api.ApiMethodPreparator;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.DeleteMapping;
import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

public class DeleteMappingAdapter extends MappingAdapter<DeleteMapping> {

    public DeleteMappingAdapter() {
        super(HttpMethodType.DELETE, DeleteMapping.class);
    }

    @Override
    public ApiMethodPreparator convert(DeleteMapping apiMappingAnnotation) {
        return new ApiMethodPreparator(
                apiMappingAnnotation.path(),
                null,
                apiMappingAnnotation.responseTemplatePath(),
                apiMappingAnnotation.propertiesPath(),
                apiMappingAnnotation.responseStatus()
        );
    }
}
