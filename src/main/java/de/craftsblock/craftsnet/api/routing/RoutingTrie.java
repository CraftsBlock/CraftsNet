package de.craftsblock.craftsnet.api.routing;

import java.util.*;
import java.util.function.Predicate;

public final class RoutingTrie {

    private final Node root = new Node();

    public void insert(RouteInfo<?> routeInfo) {
        Node current = root;
        Set<String> parameterNames = new HashSet<>();

        for (String segment : split(routeInfo.path())) {
            if (isParameter(segment)) {
                String name = parameterName(segment);

                if (!parameterNames.add(name)) {
                    throw new IllegalArgumentException(
                            "Duplicate named parameter '" + name + "' in route '" + routeInfo.path() + "'."
                    );
                }

                current = current.parameter(name);
            } else {
                current = current.children.computeIfAbsent(segment, k -> new Node());
            }
        }

        current.routeInfos.add(routeInfo);
    }

    public void remove(RouteInfo<?> routeInfo) {
        remove(root, split(routeInfo.path()), 0, routeInfo);
    }

    private boolean remove(Node node, String[] segments, int index, RouteInfo<?> routeInfo) {
        if (index == segments.length) {
            node.routeInfos.remove(routeInfo);
            return node.isEmpty();
        }

        String segment = segments[index];

        if (isParameter(segment)) {
            String name = parameterName(segment);

            Node parameterNode = node.parameters.get(name);

            if (parameterNode != null && remove(parameterNode, segments, index + 1, routeInfo)) {
                node.parameters.remove(name);
            }
        } else {
            Node child = node.children.get(segment);

            if (child != null && remove(child, segments, index + 1, routeInfo)) {
                node.children.remove(segment);
            }
        }

        return node.isEmpty();
    }

    public List<RouteSearchResult<?>> lookup(String path) {
        List<RouteSearchResult<?>> results = new ArrayList<>();
        search(root, split(path), 0, new LinkedHashMap<>(), results);
        return List.copyOf(results);
    }

    private void search(Node node, String[] segments, int index,
                        Map<String, String> params, List<RouteSearchResult<?>> results) {

        if (index == segments.length) {
            for (RouteInfo<?> routeInfo : node.routeInfos) {
                results.add(createResult(routeInfo, params));
            }
            return;
        }

        String segment = segments[index];

        Node literal = node.children.get(segment);
        if (literal != null) {
            search(literal, segments, index + 1, params, results);
        }

        for (Map.Entry<String, Node> entry : node.parameters.entrySet()) {
            String parameterName = entry.getKey();
            Node parameterNode = entry.getValue();

            params.put(parameterName, segment);

            search(
                    parameterNode,
                    segments,
                    index + 1,
                    params,
                    results
            );

            params.remove(parameterName);
        }
    }

    private RouteSearchResult<?> createResult(RouteInfo<?> routeInfo, Map<String, String> params) {
        return RouteSearchResult.of(
                routeInfo,
                params.isEmpty()
                        ? RouteSearchResult.EMPTY_PARAMS
                        : Collections.unmodifiableMap(new LinkedHashMap<>(params))
        );
    }

    private static boolean isParameter(String segment) {
        return segment.startsWith("{") && segment.endsWith("}");
    }

    private static String parameterName(String segment) {
        return segment.substring(1, segment.length() - 1);
    }

    private static String[] split(String path) {
        return Arrays.stream(path.split("/"))
                .filter(Predicate.not(String::isEmpty))
                .toArray(String[]::new);
    }

    private static final class Node {

        private final Map<String, Node> children = new HashMap<>();
        private final Map<String, Node> parameters = new HashMap<>();

        private final List<RouteInfo<?>> routeInfos = new ArrayList<>();

        private Node parameter(String name) {
            return parameters.computeIfAbsent(name, k -> new Node());
        }

        private boolean isEmpty() {
            return children.isEmpty()
                    && parameters.isEmpty()
                    && routeInfos.isEmpty();
        }
    }

}