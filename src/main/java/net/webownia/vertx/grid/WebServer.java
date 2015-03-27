package net.webownia.vertx.grid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by abarczewski on 2015-03-26.
 */
public class WebServer extends Verticle {

    private int count1;
    private int count2;
    private int count3;
    private int count4;

    private final String HOST = "localhost";
//    private final String HOST = "10.0.0.10";

    @Override
    public void start() {
        final Pattern gridUrlPattern = Pattern.compile("/(\\w+)");
        final EventBus eventBus = vertx.eventBus();
        final Logger logger = container.logger();

        // HTTP server
        RouteMatcher httpRouteMatcher = new RouteMatcher().get("/", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest request) {
                request.response().sendFile("web/grid.html");
            }
        }).get(".*\\.(css|js)$", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest request) {
                request.response().sendFile("web/" + new File(request.path()));
            }
        });

        vertx.createHttpServer().requestHandler(httpRouteMatcher).listen(8080, HOST);

        // WebSocket client
        vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>() {
            @Override
            public void handle(final ServerWebSocket ws) {
                final Matcher matcher = gridUrlPattern.matcher(ws.path());
                if (!matcher.matches()) {
                    ws.reject();
                    return;
                }

                final String grid = matcher.group(1);
                final String id = ws.textHandlerID();
                logger.info("registering new connection with id: " + id + " for grid-form: " + grid);
                vertx.sharedData().getSet("grid." + grid).add(id);
                ObjectMapper m = new ObjectMapper();
                try {
                    JsonNode rootNode = m.createObjectNode();
                    ((ObjectNode) rootNode).put("count1", count1);
                    ((ObjectNode) rootNode).put("count2", count2);
                    ((ObjectNode) rootNode).put("count3", count3);
                    ((ObjectNode) rootNode).put("count4", count4);
                    String jsonOutput = m.writeValueAsString(rootNode);
                    logger.info("json generated: " + jsonOutput);
                    for (Object chatter : vertx.sharedData().getSet("grid." + grid)) {
                        eventBus.send((String) chatter, jsonOutput);
                    }
                } catch (IOException e) {
                    ws.reject();
                }

                ws.closeHandler(new Handler<Void>() {
                    @Override
                    public void handle(final Void event) {
                        logger.info("un-registering connection with id: " + id + " from grid-form: " + grid);
                        vertx.sharedData().getSet("grid." + grid).remove(id);
                    }
                });

                ws.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer data) {

                        ObjectMapper m = new ObjectMapper();
                        try {
                            JsonNode rootNode = m.readTree(data.toString());
                            int countClick = rootNode.get("countClick").intValue();
                            ((ObjectNode) rootNode).remove("countClick");
                            countClick += 1;

                            int blockNumber = rootNode.get("blockNumber").intValue();
                            switch (blockNumber) {
                                case 1:
                                    count1 += 1;
                                    countClick = count1;
                                    break;
                                case 2:
                                    count2 += 1;
                                    countClick = count2;
                                    break;
                                case 3:
                                    count3 += 1;
                                    countClick = count3;
                                    break;
                                case 4:
                                    count4 += 1;
                                    countClick = count4;
                                    break;
                                default:
                                    break;
                            }

                            ((ObjectNode) rootNode).put("countClick", countClick);
                            String jsonOutput = m.writeValueAsString(rootNode);
                            logger.info("json generated: " + jsonOutput);
                            for (Object chatter : vertx.sharedData().getSet("grid." + grid)) {
                                eventBus.send((String) chatter, jsonOutput);
                            }
                        } catch (IOException e) {
                            ws.reject();
                        }
                    }
                });
            }
        }).listen(8090, HOST);
    }
}
