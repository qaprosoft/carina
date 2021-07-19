package com.qaprosoft.carina.core.foundation.api;

import com.qaprosoft.carina.core.foundation.api.adapter.*;
import com.qaprosoft.carina.core.foundation.api.annotation.v2.PathVariable;
import io.restassured.response.Response;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ApiMappingPreparator {

    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{(.*?)\\}");

    private static final List<MappingAdapter<?>> MAPPING_ADAPTERS = List.of(
            new DeleteMappingAdapter(),
            new GetMappingAdapter(),
            new HeadMappingAdapter(),
            new OptionsMappingAdapter(),
            new PachMappingAdapter(),
            new PostMappingAdapter(),
            new PutMappingAdapter()
    );

    private ApiMappingPreparator() {
    }

    public static <M> M getMapping(Class<M> clazz) {
        return getMapping(clazz, new Object[0]);
    }

    @SuppressWarnings("unchecked")
    public static <M> M getMapping(Class<M> clazz, Object... args) {
        M result;
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(clazz);
        MethodHandler handler = prepareHandler();

        Class<?>[] types = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class[]::new);

        try {
            result = (M) factory.create(types, args, handler);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return result;
    }

    static MethodHandler prepareHandler() {
        return (self, thisMethod, proceed, args) -> {
            Object invocationResult = proceed.invoke(self, args);

            return fetchMappingAdapter(thisMethod).map(mappingAdapter -> {
                if (!(invocationResult instanceof ApiMethodWrapper)) {
                    throw new RuntimeException("Method " + thisMethod.getName() + " should return instance of " + ApiMethodWrapper.class.getName() + " class");
                }

                return mappingAdapter.convert(thisMethod).map(abstractApiMethodV2 -> {
                    ApiOpContext apiOpContext = new ApiOpContext(abstractApiMethodV2);

                    String resolvedPath = resolvePathPatterns(apiOpContext.getApiMethod().getMethodPath(), thisMethod, args);
                    apiOpContext.getApiMethod().setMethodPath(resolvedPath);

                    ApiMethodWrapper apiMethodWrapper = (ApiMethodWrapper) invocationResult;
                    apiMethodWrapper.prepare(apiOpContext);
                    Response response = abstractApiMethodV2.callAPI();
                    apiOpContext.setResponse(response);

                    ApiMethodWrapper caller = prepareResultApiMethodCallerInstance(response);
                    return (Object) caller;
                }).orElse(invocationResult);
            }).orElse(invocationResult);
        };
    }

    private static ApiMethodWrapper prepareResultApiMethodCallerInstance(Response response) {
        return new ApiMethodWrapper() {
            @Override
            public void prepare(ApiOpContext apiOpContext) {
                System.out.println("already called");
            }

            @Override
            public Response getResult() {
                return response;
            }
        };
    }

    private static Optional<MappingAdapter<?>> fetchMappingAdapter(Method method) {
        return MAPPING_ADAPTERS.stream()
                .filter(mappingAdapter -> mappingAdapter.isSupported(method))
                .findFirst();
    }

    private static String resolvePathPatterns(String path, Method method, Object... values) {
        Parameter[] parameters = method.getParameters();

        Map<String, Object> parameterValues = IntStream.range(0, parameters.length)
                .boxed()
                .filter(index -> parameters[index].isAnnotationPresent(PathVariable.class))
                .collect(Collectors.toMap(index -> parameters[index].getAnnotation(PathVariable.class).value(), index -> values[index]));

        Matcher matcher = PATH_VARIABLE_PATTERN.matcher(path);

        while (matcher.find()) {
            String foundPattern = matcher.group(1);
            Object maybeValue = parameterValues.get(foundPattern);
            if (maybeValue == null) {
                throw new RuntimeException(String.format("No values found for pattern with name %s", foundPattern));
            }

            path = matcher.replaceFirst(maybeValue.toString());
        }

        return path;
    }

}
