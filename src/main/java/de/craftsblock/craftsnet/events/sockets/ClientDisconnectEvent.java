package de.craftsblock.craftsnet.events.sockets;

import de.craftsblock.craftscore.event.Event;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.api.annotations.ProcessPriority;
import de.craftsblock.craftsnet.api.websocket.ClosureCode;
import de.craftsblock.craftsnet.api.websocket.SocketExchange;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;

/**
 * The ClientDisconnectEvent class represents an event related to a client disconnection from a websocket connection.
 * It extends the base Event class and provides information about the SocketExchange and the SocketMapping associated with the disconnection event.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.0
 * @see GenericSocketEventBase
 * @since 2.1.1-SNAPSHOT
 */
public class ClientDisconnectEvent extends Event implements GenericSocketEventBase {

    private final SocketExchange exchange;
    private final EnumMap<ProcessPriority.Priority, List<RouteRegistry.EndpointMapping>> mappings;

    private final int rawCloseCode;
    private final ClosureCode closeCode;
    private final String closeReason;
    private final boolean closeByServer;

    /**
     * Constructs a new ClientDisconnectEvent with the specified SocketExchange and SocketMapping.
     *
     * @param exchange      The SocketExchange object representing the socket connection and its associated data.
     * @param closeCode     The close code.
     * @param closeReason   The close reason.
     * @param closeByServer Whether the connection was closed by the server or the client.
     */
    public ClientDisconnectEvent(SocketExchange exchange, int closeCode, String closeReason, boolean closeByServer) {
        this.exchange = exchange;
        this.mappings = exchange.client().getEndpoint();

        this.rawCloseCode = closeCode;
        this.closeCode = ClosureCode.fromInt(closeCode);
        this.closeReason = closeReason;
        this.closeByServer = closeByServer;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public @NotNull SocketExchange getExchange() {
        return exchange;
    }

    /**
     * Gets a {@link EnumMap} of all {@link RouteRegistry.EndpointMapping} associated with the client disconnect event.
     *
     * @return A {@link EnumMap} of {@link RouteRegistry.EndpointMapping} objects representing the mapping for the socket connection.
     */
    public EnumMap<ProcessPriority.Priority, List<RouteRegistry.EndpointMapping>> getMappings() {
        return mappings;
    }

    /**
     * Checks if the client disconnection event has at least one valid SocketMapping associated with it.
     *
     * @return true if the event has at least one valid SocketMapping, false otherwise.
     */
    public boolean hasMappings() {
        return mappings != null && !mappings.isEmpty();
    }

    /**
     * Gets the raw close code why the connection was closed.
     *
     * @return The raw close code if it was present, -1 otherwise
     */
    public int getRawCloseCode() {
        return rawCloseCode;
    }

    /**
     * Gets the close code parsed to a ClosureCode why the connection was closed.
     *
     * @return The parsed close code if it was present, and it is registered in the ClosureCode enum, null otherwise
     */
    public ClosureCode getCloseCode() {
        return closeCode;
    }

    /**
     * Gets the reason why the connection was closed.
     *
     * @return The close reason if it was present, null otherwise
     */
    public String getCloseReason() {
        return closeReason;
    }

    /**
     * Gets whether the server disconnected the client or the client disconnected.
     *
     * @return true if the server closed the connection, false otherwise.
     */
    public boolean wasClosedByServer() {
        return closeByServer;
    }

}
