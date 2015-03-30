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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by abarczewski on 2015-03-26.
 */
public class WebServer extends Verticle {

    private Map<Integer, GridData> gridDataMap = new HashMap<>(0);
    private Map<String, UserData> userDataMap = new HashMap<>(0);
    private UserData userData;
    private final int GRID_SIZE = 25;

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
                    for (int i = 1; i <= GRID_SIZE; i++) {
                        if (gridDataMap.size() < GRID_SIZE) {
                            gridDataMap.put(i, new GridData(i));
                        } else {
                            ((ObjectNode) rootNode).put("count" + i, gridDataMap.get(i).countConquer);
                        }
                    }
                    String jsonOutput = m.writeValueAsString(rootNode);
                    logger.info("json generated: " + jsonOutput);
                    for (Object chatter : vertx.sharedData().getSet("grid." + grid)) {
                        eventBus.send((String) chatter, jsonOutput);
                    }
                    userDataMap.put(id, new UserData(id, GRID_SIZE));
                } catch (IOException e) {
                    ws.reject();
                }

                ws.closeHandler(new Handler<Void>() {
                    @Override
                    public void handle(final Void event) {
                        logger.info("un-registering connection with id: " + id + " from grid-form: " + grid);
                        vertx.sharedData().getSet("grid." + grid).remove(id);
                        userDataMap.remove(id);
                    }
                });

                ws.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer data) {

                        userData = userDataMap.get(id);
                        userData.increment();

                        ObjectMapper m = new ObjectMapper();
                        try {
                            JsonNode rootNode = m.readTree(data.toString());

                            int blockNumber = rootNode.get("blockNumber").intValue();

                            userData.gridDataMap.get(blockNumber).increment(gridDataMap.get(blockNumber).countConquer);
                            gridDataMap.get(blockNumber).increment();

                            ((ObjectNode) rootNode).put("countAllClick", userData.countAllClick);
                            ((ObjectNode) rootNode).put("userId", id);

                            final GridData gridUserData = userData.gridDataMap.get(blockNumber);
                            final GridData gridData = gridDataMap.get(blockNumber);
                            gridDataMap.replace(blockNumber, gridData);
                            ((ObjectNode) rootNode).put("countEven", gridUserData.countEven);
                            ((ObjectNode) rootNode).put("countConquer", gridData.countConquer);

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
