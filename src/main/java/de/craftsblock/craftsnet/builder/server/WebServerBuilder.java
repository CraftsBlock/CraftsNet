package de.craftsblock.craftsnet.builder.server;

import de.craftsblock.craftsnet.builder.CraftsNetBuilder;
import org.jetbrains.annotations.NotNull;

public class WebServerBuilder extends ServerBuilder<WebServerBuilder> {

    public WebServerBuilder(@NotNull CraftsNetBuilder root) {
        super(root, "web", 5000);
        this.acquire(this);
    }

}
