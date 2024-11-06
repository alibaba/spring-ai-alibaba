package org.bsc.langgraph4j.studio.jetty;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.MemorySaver;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.studio.LangGraphStreamingServer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


/**
 * LangGraphStreamingServer is an interface that represents a server that supports streaming
 * of LangGraph.
 * Implementations of this interface can be used to create a web server
 * that exposes an API for interacting with compiled language graphs.
 */
public class LangGraphStreamingServerJetty implements LangGraphStreamingServer {

    final Server server;

    private LangGraphStreamingServerJetty(Server server) {
        Objects.requireNonNull(server, "server cannot be null");
        this.server = server;
    }

    public CompletableFuture<Void> start() throws Exception {
        return CompletableFuture.runAsync(() -> {
            try {
                server.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Runnable::run);
    }


    static public Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int port = 8080;
        private final Map<String, ArgumentMetadata> inputArgs = new HashMap<>();
        private String title = null;
        private ObjectMapper objectMapper;
        private BaseCheckpointSaver saver;
        private StateGraph<? extends AgentState> stateGraph;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder addInputStringArg(String name, boolean required) {
            inputArgs.put(name, new ArgumentMetadata("string", required));
            return this;
        }

        public Builder addInputStringArg(String name) {
            inputArgs.put(name, new ArgumentMetadata("string", true));
            return this;
        }

        public Builder checkpointSaver(BaseCheckpointSaver saver) {
            this.saver = saver;
            return this;
        }

        public <State extends AgentState> Builder stateGraph(StateGraph<State> stateGraph) {
            this.stateGraph = stateGraph;
            return this;
        }

        public LangGraphStreamingServerJetty build() throws Exception {
            Objects.requireNonNull( stateGraph, "stateGraph cannot be null");

            if (saver == null) {
                saver = new MemorySaver();
            }

            Server server = new Server();

            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            server.addConnector(connector);

            var resourceHandler = new ResourceHandler();

            var baseResource = ResourceFactory.of(resourceHandler).newClassLoaderResource("webapp");
            resourceHandler.setBaseResource(baseResource);

            resourceHandler.setDirAllowed(true);

            var context = new ServletContextHandler(ServletContextHandler.SESSIONS);

            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
            }

            context.setSessionHandler(new org.eclipse.jetty.ee10.servlet.SessionHandler());

            context.addServlet(new ServletHolder(new GraphInitServlet(stateGraph, title, inputArgs)), "/init");

            context.addServlet(new ServletHolder(new GraphStreamServlet(stateGraph, objectMapper, saver)), "/stream");

            var handlerList = new Handler.Sequence( resourceHandler, context);

            server.setHandler(handlerList);

            return new LangGraphStreamingServerJetty(server);

        }
    }
}
