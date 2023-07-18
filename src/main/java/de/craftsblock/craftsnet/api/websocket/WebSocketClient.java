package de.craftsblock.craftsnet.api.websocket;

import de.craftsblock.craftscore.json.JsonParser;
import de.craftsblock.craftsnet.Main;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.events.sockets.ClientConnectEvent;
import de.craftsblock.craftsnet.events.sockets.ClientDisconnectEvent;
import de.craftsblock.craftsnet.events.sockets.IncomingSocketMessageEvent;
import de.craftsblock.craftsnet.events.sockets.OutgoingSocketMessageEvent;
import de.craftsblock.craftsnet.utils.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

public class WebSocketClient extends Thread {

    private final WebSocketServer server;
    private final Socket socket;
    private List<String> headers;
    private String ip;
    private RouteRegistry.SocketMapping mapping;

    private BufferedReader reader;
    private PrintWriter writer;

    private boolean active = false;

    public WebSocketClient(Socket socket, WebSocketServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        if (active)
            throw new IllegalStateException("This websocket client is already running!");
        SocketExchange exchange = new SocketExchange(server, this);
        active = true;
        Logger logger = Main.logger;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            headers = Collections.unmodifiableList(readHeaders());

            ip = socket.getInetAddress().getHostAddress();
            if (getHeader("X-forwarded-for") != null)
                ip = Objects.requireNonNull(getHeader("X-forwarded-for")).split(", ")[0];
            if (getHeader("Cf-connecting-ip") != null)
                ip = getHeader("Cf-connecting-ip");

            sendHandshake();
            if (getEndpoint(headers.get(0)) != null) {
                mapping = getEndpoint(headers.get(0));
                ClientConnectEvent event = new ClientConnectEvent(exchange, mapping);
                Main.listenerRegistry.call(event);
                if (event.isCancelled()) {
                    if (event.getReason() != null)
                        sendMessage(event.getReason());
                    disconnect();
                    logger.debug(ip + " connected to " + headers.get(0).split(" ")[1] + " \u001b[38;5;9m[ABORTED]");
                    return;
                }
                assert mapping != null;
                logger.info(ip + " connected to " + headers.get(0).split(" ")[1]);
                server.add(mapping, this);
                String message;
                while ((message = readMessage()) != null) {
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    if (IntStream.range(0, data.length).map(i -> data[i]).anyMatch(tmp -> tmp < 0)) break;
                    if (message.isEmpty() || message.isBlank()) break;

                    Matcher matcher = mapping.validator().matcher(headers.get(0).split(" ")[1]);
                    if (!matcher.matches()) {
                        sendMessage(JsonParser.parse("{}")
                                .set("error", "There was an unexpected error while matching!")
                                .asString());
                        break;
                    }

                    Object[] args = new Object[matcher.groupCount() + 1];
                    args[0] = exchange;
                    args[1] = message;
                    for (int i = 2; i <= matcher.groupCount(); i++) args[i] = matcher.group(i);
                    IncomingSocketMessageEvent event2 = new IncomingSocketMessageEvent(exchange, message);
                    Main.listenerRegistry.call(event2);
                    if (event2.isCancelled())
                        continue;
                    for (Method method : mapping.receiver()) method.invoke(mapping.handler(), args);
                }
            } else {
                logger.debug(ip + " connected to " + headers.get(0).split(" ")[1] + " \u001b[38;5;9m[NOT FOUND]");
                sendMessage(JsonParser.parse("{}").set("error", "Path do not match any API endpoint!").asString());
            }
        } catch (SocketException ignored) {
        } catch (IOException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private List<String> readHeaders() throws IOException {
        List<String> headers = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) headers.add(line);
        return headers;
    }

    @Nullable
    private RouteRegistry.SocketMapping getEndpoint(String header) {
        return Main.routeRegistry.getSocket(header.split(" ")[1]);
    }

    private void sendHandshake() {
        try {
            String concatenated = getHeader("Sec-WebSocket-Key") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(concatenated.getBytes(StandardCharsets.UTF_8));
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + Base64.getEncoder().encodeToString(hash) + "\r\n"
                    + "\r\n";
            writer.write(response);
            writer.flush();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private String readMessage() throws IOException {
        InputStream inputStream = socket.getInputStream();
        byte[] frame = new byte[10];
        inputStream.read(frame, 0, 2);
        byte payloadLength = (byte) (frame[1] & 0x7F);
        if (payloadLength == 126) inputStream.read(frame, 2, 2);
        else if (payloadLength == 127) inputStream.read(frame, 2, 8);
        long payloadLengthValue = getPayloadLengthValue(frame, payloadLength);
        byte[] masks = new byte[4];
        inputStream.read(masks);
        ByteArrayOutputStream payloadBuilder = new ByteArrayOutputStream();
        long bytesRead = 0;
        long bytesToRead = payloadLengthValue;
        byte[] chunk = new byte[4096];
        int chunkSize;
        while (bytesToRead > 0 && (chunkSize = inputStream.read(chunk, 0, (int) Math.min(chunk.length, bytesToRead))) != -1) {
            for (int i = 0; i < chunkSize; i++) chunk[i] ^= masks[(int) ((bytesRead + i) % 4)];
            payloadBuilder.write(chunk, 0, chunkSize);
            bytesRead += chunkSize;
            bytesToRead -= chunkSize;
        }
        return payloadBuilder.toString(StandardCharsets.UTF_8);
    }


    private long getPayloadLengthValue(byte[] frame, byte payloadLength) {
        if (payloadLength == 126) return ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
        else if (payloadLength == 127) return ((frame[2] & 0xFFL) << 56)
                | ((frame[3] & 0xFFL) << 48)
                | ((frame[4] & 0xFFL) << 40)
                | ((frame[5] & 0xFFL) << 32)
                | ((frame[6] & 0xFFL) << 24)
                | ((frame[7] & 0xFFL) << 16)
                | ((frame[8] & 0xFFL) << 8)
                | (frame[9] & 0xFFL);
        else return payloadLength;
    }

    public String getHeader(String key) {
        for (String header : headers)
            if (header.toLowerCase().startsWith(key.toLowerCase() + ":"))
                return header.substring(header.indexOf(":") + 2).trim();
        return null;
    }

    public String getIp() {
        return ip;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void sendMessage(String data) {
        try {
            OutgoingSocketMessageEvent event = new OutgoingSocketMessageEvent(new SocketExchange(server, this), data);
            Main.listenerRegistry.call(event);
            if (event.isCancelled())
                return;
            data = event.getData();
            byte[] rawData = data.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write((byte) 0x81);
            outputStream.write((byte) rawData.length);
            outputStream.write(rawData);
            OutputStream socketOutputStream = socket.getOutputStream();
            socketOutputStream.write(outputStream.toByteArray());
            socketOutputStream.flush();
        } catch (SocketException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void disconnect() {
        try {
            Main.listenerRegistry.call(new ClientDisconnectEvent(new SocketExchange(server, this), mapping));
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
            server.remove(this);
            Main.logger.debug(ip + " disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}

