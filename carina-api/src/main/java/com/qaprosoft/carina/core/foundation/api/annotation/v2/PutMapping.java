package com.qaprosoft.carina.core.foundation.api.annotation.v2;

import com.qaprosoft.carina.core.foundation.api.http.HttpResponseStatusType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.qaprosoft.carina.core.foundation.api.http.HttpResponseStatusType.OK_200;

@Target(value = { ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface PutMapping {

    String path() default "";

    HttpResponseStatusType responseStatus() default OK_200;

    String requestTemplatePath() default "";

    String responseTemplatePath() default "";

    String propertiesPath() default "";

}
