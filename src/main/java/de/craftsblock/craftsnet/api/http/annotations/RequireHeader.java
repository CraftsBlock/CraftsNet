package de.craftsblock.craftsnet.api.http.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireHeader {

    // Paused due to the fact that the transform annotation could be added only once to a method.

    String value();

}
