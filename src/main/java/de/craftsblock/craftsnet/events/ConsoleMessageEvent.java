package de.craftsblock.craftsnet.events;

import de.craftsblock.craftscore.event.Event;

public class ConsoleMessageEvent extends Event {

    private boolean cancelled = false;
    private final String message;

    public ConsoleMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
