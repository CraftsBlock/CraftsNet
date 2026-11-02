package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

public class WebServerBuilder extends ServerBuilder<WebServerBuilder> {

    public WebServerBuilder(@NotNull CraftsNetBuilder root) {
        super(root, "web");
        this.acquire(this);

        this.backlog(0);
        this.maxUriLength(4096);
        this.port(5000);
        this.responseEncoding(false);
        this.skipDefaultRoute(false);
        this.ssl(false);
        this.state(ServerState.DYNAMIC);
    }

}
