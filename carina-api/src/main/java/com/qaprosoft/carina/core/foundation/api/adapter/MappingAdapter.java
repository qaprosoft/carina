package com.qaprosoft.carina.core.foundation.api.adapter;

import com.qaprosoft.carina.core.foundation.api.AbstractApiMethodV2;
import com.qaprosoft.carina.core.foundation.api.ApiMethodPreparator;
import com.qaprosoft.carina.core.foundation.api.BaseApiMethod;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.ApiMapping;
import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

public abstract class MappingAdapter<A extends Annotation> {

    private final HttpMethodType methodType;
    private final Class<A> supportedAnnotationClass;

    protected MappingAdapter(HttpMethodType methodType, Class<A> supportedAnnotationClass) {
        this.methodType = methodType;
        this.supportedAnnotationClass = supportedAnnotationClass;
    }

    protected abstract ApiMethodPreparator convert(A apiMappingAnnotation);

    public Optional<AbstractApiMethodV2> convert(Method method) {
        String globalPath = resolveGlobalPath(method);
        return resolveApiMethod(method, globalPath);
    }

    private String resolveGlobalPath(Method method) {
        String globalPath = null;

        boolean apiMappingPresents = method.getDeclaringClass().isAnnotationPresent(ApiMapping.class);
        if (apiMappingPresents) {
            ApiMapping apiMapping = method.getDeclaringClass().getAnnotation(ApiMapping.class);
            globalPath = apiMapping.path();
        }

        return globalPath;
    }

    private Optional<AbstractApiMethodV2> resolveApiMethod(Method method, String globalPath) {
        AbstractApiMethodV2 apiMethod = null;

        if (isMethodValid(method)) {
            A apiMappingAnnotation = method.getAnnotation(supportedAnnotationClass);
            ApiMethodPreparator apiMethodPreparator = convert(apiMappingAnnotation);

            String path = apiMethodPreparator.getPath();
            if (globalPath != null) {
                path = buildPath(globalPath, apiMethodPreparator.getPath());
            }

            boolean nullPropertiesPath = apiMethodPreparator.getPropertiesPath() == null;
            apiMethod = nullPropertiesPath
                    ? new BaseApiMethod(apiMethodPreparator.getRequestTemplatePath(), apiMethodPreparator.getResponseTemplatePath(), new Properties(), methodType, path)
                    : new BaseApiMethod(apiMethodPreparator.getRequestTemplatePath(), apiMethodPreparator.getResponseTemplatePath(), apiMethodPreparator.getPropertiesPath(), methodType, path);
            apiMethod.expectResponseStatus(apiMethodPreparator.getResponseStatus());
        }
        return Optional.ofNullable(apiMethod);
    }

    private boolean isMethodValid(Method method) {
        return !Modifier.isStatic(method.getModifiers())
                && Modifier.isPublic(method.getModifiers())
                && method.isAnnotationPresent(supportedAnnotationClass);
    }

    private String buildPath(String... pathSlices) {
        return Arrays.stream(pathSlices).reduce((slice1, slice2) -> {
            slice1 = preparePathSlice(slice1);
            slice2 = preparePathSlice(slice2);
            return slice1 + slice2;
        }).orElse(null);
    }

    private String preparePathSlice(String pathSlice) {
        if (!pathSlice.startsWith("/")) {
            pathSlice = "/" + pathSlice;
        }
        if (pathSlice.endsWith("/")) {
            pathSlice = pathSlice.substring(0, pathSlice.lastIndexOf("/"));
        }
        return pathSlice;
    }

    public boolean isSupported(Method method) {
        return method.isAnnotationPresent(supportedAnnotationClass);
    }
}
