package de.craftsblock.craftsnet.api.routing;

import de.craftsblock.craftsnet.api.Exchange;
import de.craftsblock.craftsnet.api.Server;
import de.craftsblock.craftsnet.api.http.HttpExchange;
import de.craftsblock.craftsnet.api.http.WebServer;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.api.websocket.WebSocketExchange;
import de.craftsblock.craftsnet.api.websocket.WebSocketServer;
import de.craftsblock.craftsnet.api.websocket.annotations.WebSocket;
import de.craftsblock.craftsnet.utils.reflection.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.function.Function;

public enum ServerType {

    HTTP(WebServer.class, HttpExchange.class, exchange -> exchange.request().getUrl(), Route.class, Route::value),
    WS(WebSocketServer.class, WebSocketExchange.class, exchange -> exchange.client().getPath(), WebSocket.class, WebSocket::value),
    ;

    private final Class<? extends Server> serverClass;

    private final Class<? extends Exchange> exchangeClass;
    private final Function<Exchange, String> exchangePathExtractor;

    private final Class<? extends Annotation> pathAnnotation;
    private final Function<Annotation, String> annotationPathExtractor;

    <E extends Exchange, A extends Annotation> ServerType(
            Class<? extends Server> serverClass,
            Class<E> exchangeClass,
            Function<E, String> exchangePathExtractor,
            Class<A> pathAnnotation,
            Function<A, String> annotationPathExtractor
    ) {
        this.serverClass = serverClass;

        this.exchangeClass = exchangeClass;
        this.exchangePathExtractor = exchange -> exchangePathExtractor.apply(exchangeClass.cast(exchange));

        this.pathAnnotation = pathAnnotation;
        this.annotationPathExtractor = annotation -> annotationPathExtractor.apply(pathAnnotation.cast(annotation));
    }

    public Class<? extends Server> getServerClass() {
        return serverClass;
    }

    public Class<? extends Exchange> getExchangeClass() {
        return exchangeClass;
    }

    public Class<? extends Annotation> getPathAnnotation() {
        return pathAnnotation;
    }

    public String extractPath(AnnotatedElement element) {
        Annotation annotation = element.getAnnotation(this.pathAnnotation);
        return annotation != null ? annotationPathExtractor.apply(annotation) : null;
    }

    public String extractPath(Exchange exchange) {
        if (!TypeUtils.isInstance(exchangeClass, exchange)) {
            throw new IllegalArgumentException(
                    "Expected " + exchangeClass.getName()
                            + " but got " + exchange.getClass().getName());
        }

        return exchangePathExtractor.apply(exchange);
    }
}