package de.craftsblock.craftsnet.api.routing.filter;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReflectionFilterCollector {

    private ReflectionFilterCollector() {
    }

    public static @NotNull List<Filter<?>> collect(AnnotatedElement element) {
        return collect(element, Collections.emptyList());
    }

    public static @NotNull List<Filter<?>> collect(AnnotatedElement element, List<Filter<?>> base) {
        List<Filter<?>> filters = new ArrayList<>(base);

        // TODO: Implement

        return filters;
    }

}
