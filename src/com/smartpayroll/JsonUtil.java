package com.smartpayroll;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JsonUtil.java
 * -------------
 * A lightweight utility class for handling JSON formatting and HTTP responses
 * using ONLY standard Java (no external libraries like Jackson or Gson needed!).
 * 
 * Beginner Concept:
 * When your web browser talks to your Java server, they exchange data using JSON
 * (JavaScript Object Notation). This class turns Java objects and SQL results
 * into JSON text and vice versa.
 */
public class JsonUtil {

    /**
     * Converts a JDBC ResultSet directly into a JSON Array string.
     * Example output: [ {"id": 1, "name": "Arun"}, {"id": 2, "name": "Priya"} ]
     */
    public static String resultSetToJson(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        boolean firstRow = true;
        while (rs.next()) {
            if (!firstRow) {
                sb.append(",");
            }
            firstRow = false;
            sb.append("{");

            for (int i = 1; i <= colCount; i++) {
                if (i > 1) {
                    sb.append(",");
                }
                String colName = meta.getColumnLabel(i);
                Object val = rs.getObject(i);

                sb.append("\"").append(escape(colName)).append("\":");
                if (val == null) {
                    sb.append("null");
                } else if (val instanceof Number || val instanceof Boolean) {
                    sb.append(val);
                } else {
                    sb.append("\"").append(escape(val.toString())).append("\"");
                }
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parses a simple flat JSON string into a Java Map<String, String>.
     * e.g., {"name":"John", "salary":45000} -> map.get("name") = "John"
     */
    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }

        // Regex matches key-value pairs: "key"\s*:\s*("value"|value)
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\]\\s]+))");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (val != null && val.equals("null")) {
                val = "";
            }
            map.put(key.trim(), val != null ? val.trim() : "");
        }
        return map;
    }

    /**
     * Reads the entire body from an incoming HTTP request.
     */
    public static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Sends an HTTP response with JSON content type and CORS support.
     */
    public static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /**
     * Parses query string parameters from URL (e.g. ?month=8&year=2026)
     */
    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return params;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            } else if (kv.length == 1) {
                params.put(kv[0], "");
            }
        }
        return params;
    }

    /**
     * Escapes special characters for JSON strings.
     */
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
