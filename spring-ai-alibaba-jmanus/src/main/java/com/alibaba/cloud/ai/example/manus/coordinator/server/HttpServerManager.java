/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus.coordinator.server;

import com.alibaba.cloud.ai.example.manus.coordinator.tool.EndPointUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * HTTP Server manager
 */
@Component
public class HttpServerManager {
    
    private static final Logger log = LoggerFactory.getLogger(HttpServerManager.class);
    
    private DisposableServer httpServer;
    
    /**
     * Start HTTP server
     * @param router Router function
     */
    public void startHttpServer(RouterFunction<?> router) {
        try {
            log.info("Starting HTTP server on port: {}", EndPointUtils.SERVICE_PORT);
            
            // Create HTTP handler
            HttpHandler httpHandler = RouterFunctions.toHttpHandler(router);
            ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

            // Start HTTP server
            this.httpServer = HttpServer.create().port(EndPointUtils.SERVICE_PORT).handle(adapter).bindNow();

            log.info("HTTP server started, listening on port: {}", EndPointUtils.SERVICE_PORT);
            log.info("Server address: http://{}:{}", EndPointUtils.SERVICE_HOST, EndPointUtils.SERVICE_PORT);
            
        } catch (Exception e) {
            log.error("Error starting HTTP server: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Recreate HTTP server
     * @param router Router function
     */
    public void recreateHttpServer(RouterFunction<?> router) {
        try {
            log.info("Starting to recreate HTTP server to update routes");

            // Stop current HTTP server
            if (this.httpServer != null) {
                this.httpServer.disposeNow();
                log.info("Current HTTP server stopped");
            }

            // Start new HTTP server
            startHttpServer(router);

            log.info("Successfully recreated HTTP server, listening on port: {}", EndPointUtils.SERVICE_PORT);

        } catch (Exception e) {
            log.error("Exception occurred while recreating HTTP server: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Stop HTTP server
     */
    public void stopHttpServer() {
        if (this.httpServer != null) {
            this.httpServer.disposeNow();
            log.info("HTTP server stopped");
        }
    }
    
    /**
     * Check if HTTP server is running
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return this.httpServer != null && !this.httpServer.isDisposed();
    }
}
