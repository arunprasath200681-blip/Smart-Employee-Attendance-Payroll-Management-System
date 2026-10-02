package com.smartpayroll;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * StaticFileHandler.java
 * ----------------------
 * Serves HTML, CSS, JavaScript, and static media files from the 'web/' directory
 * directly using standard Java SE.
 * 
 * Beginner Concept:
 * When you open http://localhost:8080/ in your browser, this Java handler reads
 * web/index.html from your disk and transmits the HTML bytes over the HTTP connection.
 * When the HTML references style.css or app.js, this same handler serves those files!
 */
public class StaticFileHandler implements HttpHandler {

    private final String baseDir;

    public StaticFileHandler(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Default root to index.html
        if (path == null || path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Prevent path traversal attack (e.g. /../../)
        if (path.contains("..")) {
            sendNotFound(exchange, "Invalid path");
            return;
        }

        // Locate file on disk
        File file = new File(baseDir, path.replace('/', File.separatorChar));
        if (!file.exists() || file.isDirectory()) {
            sendNotFound(exchange, "File not found: " + path);
            return;
        }

        // Determine content type
        String contentType = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", contentType);

        exchange.sendResponseHeaders(200, file.length());
        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
        }
    }

    private String getMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private void sendNotFound(HttpExchange exchange, String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(404, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
