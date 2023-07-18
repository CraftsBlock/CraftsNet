package de.craftsblock.craftsnet.api.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Route {

    String path();

    RequestMethod[] methods() default {RequestMethod.POST, RequestMethod.GET};

}
