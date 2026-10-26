package com.smartpayroll;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ServletBridge.java
 * ------------------
 * A lightweight bridge that allows standard HttpServlets (EmployeeServlet, AttendanceServlet,
 * PayrollServlet, DashboardServlet) to run directly inside a standalone Java application.
 * 
 * Beginner Concept:
 * When you run this standalone application without an external Tomcat server,
 * this bridge translates incoming HTTP requests into standard HttpServletRequest
 * and HttpServletResponse objects and forwards them into servlet.service(req, resp).
 * This means your servlets are 100% genuine HttpServlets that work both standalone
 * AND when deployed directly into Apache Tomcat!
 */
public class ServletBridge implements HttpHandler {

    private final HttpServlet servlet;

    public ServletBridge(HttpServlet servlet) {
        this.servlet = servlet;
        try {
            servlet.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        // Handle CORS pre-flight
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            ByteArrayOutputStream respBody = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(respBody, StandardCharsets.UTF_8));
            int[] statusCode = new int[]{200};
            Map<String, String> respHeaders = new HashMap<>();

            // Parse URL query parameters
            Map<String, String> queryParams = new HashMap<>();
            String query = exchange.getRequestURI().getQuery();
            if (query != null && !query.isEmpty()) {
                for (String pair : query.split("&")) {
                    String[] kv = pair.split("=", 2);
                    queryParams.put(kv[0], kv.length > 1 ? kv[1] : "");
                }
            }

            // Read request body
            byte[] requestBodyBytes = exchange.getRequestBody().readAllBytes();

            // Dynamic Proxy for HttpServletRequest
            HttpServletRequest reqProxy = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, m, args) -> {
                    String name = m.getName();
                    if (name.equals("getMethod")) return exchange.getRequestMethod();
                    if (name.equals("getRequestURI")) return exchange.getRequestURI().getPath();
                    if (name.equals("getQueryString")) return exchange.getRequestURI().getQuery();
                    if (name.equals("getPathInfo")) return exchange.getRequestURI().getPath();
                    if (name.equals("getParameter")) {
                        return queryParams.get(args[0]);
                    }
                    if (name.equals("getReader")) {
                        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(requestBodyBytes), StandardCharsets.UTF_8));
                    }
                    if (name.equals("getInputStream")) {
                        return new ByteArrayInputStream(requestBodyBytes);
                    }
                    if (name.equals("getHeader")) {
                        return exchange.getRequestHeaders().getFirst((String) args[0]);
                    }
                    return null;
                }
            );

            // Dynamic Proxy for HttpServletResponse
            HttpServletResponse respProxy = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, m, args) -> {
                    String name = m.getName();
                    if (name.equals("setContentType")) respHeaders.put("Content-Type", (String) args[0]);
                    if (name.equals("setCharacterEncoding")) {}
                    if (name.equals("setStatus")) statusCode[0] = (Integer) args[0];
                    if (name.equals("setHeader")) respHeaders.put((String) args[0], (String) args[1]);
                    if (name.equals("getWriter")) return writer;
                    if (name.equals("getOutputStream")) return respBody;
                    return null;
                }
            );

            // Forward to standard HttpServlet.service(req, resp)
            servlet.service(reqProxy, respProxy);
            writer.flush();

            // Set response headers
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            for (Map.Entry<String, String> e : respHeaders.entrySet()) {
                exchange.getResponseHeaders().set(e.getKey(), e.getValue());
            }

            byte[] outputBytes = respBody.toByteArray();
            exchange.sendResponseHeaders(statusCode[0], outputBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(outputBytes);
            }

        } catch (Exception e) {
            e.printStackTrace();
            byte[] err = ("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, err.length);
            exchange.getResponseBody().write(err);
            exchange.getResponseBody().close();
        }
    }
}
