package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

public class WebServerBuilder extends ServerBuilder<WebServerBuilder> {

    public WebServerBuilder(@NotNull CraftsNetBuilder root, String namespace, int port) {
        super(root, namespace, port);
        this.acquire(this);
    }

}
