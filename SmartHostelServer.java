import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Smart Hostel Room Allocation System
 * - No external dependencies (no Maven/Gradle needed)
 * - Backend: Java built-in HttpServer + JSON (minimal custom parsing)
 * - Frontend: served from ./web (index.html, styles.css, app.js)
 *
 * Run:
 *   javac SmartHostelServer.java
 *   java SmartHostelServer
 *
 * Then open:
 *   http://localhost:8080
 */
public class SmartHostelServer {

    // -----------------------------
    // Data model
    // -----------------------------
    static class Room {
        final String roomNo;
        final int capacity;
        final boolean hasAC;
        final boolean hasAttachedWashroom;

        Room(String roomNo, int capacity, boolean hasAC, boolean hasAttachedWashroom) {
            this.roomNo = roomNo;
            this.capacity = capacity;
            this.hasAC = hasAC;
            this.hasAttachedWashroom = hasAttachedWashroom;
        }
    }

    // -----------------------------
    // Business logic (ported from your Swing version)
    // -----------------------------
    static class HostelManager {
        private final List<Room> rooms = new ArrayList<>();
        private final Path dbPath;

        HostelManager(Path dbPath) {
            this.dbPath = dbPath;
            if (!loadFromDisk()) {
                // preload sample data
                rooms.add(new Room("101", 1, true, true));
                rooms.add(new Room("102", 2, false, true));
                rooms.add(new Room("103", 4, true, false));
                rooms.add(new Room("104", 2, true, true));
                rooms.add(new Room("201", 6, false, false));
                saveToDisk();
            }
        }

        synchronized boolean addRoom(String roomNo, int capacity, boolean ac, boolean washroom) {
            String rNo = roomNo == null ? "" : roomNo.trim();
            if (rNo.isEmpty() || capacity <= 0) return false;
            for (Room r : rooms) {
                if (r.roomNo.equalsIgnoreCase(rNo)) return false;
            }
            rooms.add(new Room(rNo, capacity, ac, washroom));
            saveToDisk();
            return true;
        }

        synchronized List<Room> getAllRooms() {
            return new ArrayList<>(rooms);
        }

        synchronized List<Room> searchRooms(int minCapacity, boolean requireAC, boolean requireWashroom) {
            return rooms.stream()
                    .filter(r -> r.capacity >= minCapacity)
                    .filter(r -> !requireAC || r.hasAC)
                    .filter(r -> !requireWashroom || r.hasAttachedWashroom)
                    .sorted(Comparator.comparingInt((Room r) -> r.capacity).thenComparing(r -> r.roomNo))
                    .collect(Collectors.toList());
        }

        synchronized Room allocateRoom(int students, boolean needsAC, boolean needsWashroom) {
            List<Room> candidates = searchRooms(students, needsAC, needsWashroom);
            if (candidates.isEmpty()) return null;
            candidates.sort(Comparator.comparingInt(r -> r.capacity));
            return candidates.get(0);
        }

        private boolean loadFromDisk() {
            try {
                if (dbPath == null) return false;
                if (!Files.exists(dbPath)) return false;
                String raw = Files.readString(dbPath, StandardCharsets.UTF_8).trim();
                if (raw.isEmpty()) return false;
                List<Room> loaded = parseRoomsJsonArray(raw);
                if (loaded.isEmpty()) return false;
                rooms.clear();
                rooms.addAll(loaded);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private void saveToDisk() {
            try {
                if (dbPath == null) return;
                Files.createDirectories(dbPath.getParent());
                String json = toJsonArray(rooms);
                Path tmp = dbPath.resolveSibling(dbPath.getFileName().toString() + ".tmp");
                Files.writeString(tmp, json, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, dbPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception e) {
                    Files.move(tmp, dbPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }
        }
    }

    // -----------------------------
    // Server
    // -----------------------------
    private final Path webRoot = Path.of("web");
    private final Path dbPath = Path.of("data", "rooms.json");
    private final HostelManager manager = new HostelManager(dbPath);

    public static void main(String[] args) throws Exception {
        new SmartHostelServer().start(8080);
    }

    void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        server.createContext("/api/rooms", this::handleRooms);
        server.createContext("/api/rooms/search", this::handleSearch);
        server.createContext("/api/rooms/allocate", this::handleAllocate);

        server.createContext("/", new StaticHandler(webRoot));

        System.out.println("Smart Hostel Server running on http://localhost:" + port);
        System.out.println("Web root: " + webRoot.toAbsolutePath());
        server.start();
    }

    // -----------------------------
    // API Handlers
    // -----------------------------
    private void handleRooms(HttpExchange ex) throws IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                sendEmpty(ex, 204);
                return;
            }
            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                List<Room> rooms = manager.getAllRooms();
                sendJson(ex, 200, toJsonArray(rooms));
                return;
            }
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                String body = readBody(ex);
                Map<String, String> json = parseFlatJsonObject(body);

                String roomNo = json.getOrDefault("roomNo", "").trim();
                int capacity = parseIntSafe(json.get("capacity"), -1);
                boolean hasAC = parseBoolSafe(json.get("hasAC"), false);
                boolean hasWashroom = parseBoolSafe(json.get("hasAttachedWashroom"), false);

                boolean ok = manager.addRoom(roomNo, capacity, hasAC, hasWashroom);
                if (!ok) {
                    sendJson(ex, 409, "{\"message\":\"Room number already exists (or invalid).\"}");
                    return;
                }
                sendJson(ex, 201, "{\"message\":\"Room added.\"}");
                return;
            }
            sendJson(ex, 405, "{\"message\":\"Method not allowed\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"message\":\"Internal server error\"}");
        }
    }

    private void handleSearch(HttpExchange ex) throws IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                sendEmpty(ex, 204);
                return;
            }
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"message\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
            int minCapacity = parseIntSafe(q.get("minCapacity"), 1);
            boolean needsAC = parseBoolSafe(q.get("needsAC"), false);
            boolean needsWashroom = parseBoolSafe(q.get("needsWashroom"), false);
            if (minCapacity < 1) minCapacity = 1;

            List<Room> rooms = manager.searchRooms(minCapacity, needsAC, needsWashroom);
            sendJson(ex, 200, toJsonArray(rooms));
        } catch (Exception e) {
            sendJson(ex, 500, "{\"message\":\"Internal server error\"}");
        }
    }

    private void handleAllocate(HttpExchange ex) throws IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                sendEmpty(ex, 204);
                return;
            }
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"message\":\"Method not allowed\"}");
                return;
            }
            String body = readBody(ex);
            Map<String, String> json = parseFlatJsonObject(body);

            int students = parseIntSafe(json.get("students"), -1);
            boolean needsAC = parseBoolSafe(json.get("needsAC"), false);
            boolean needsWashroom = parseBoolSafe(json.get("needsWashroom"), false);
            if (students < 1) {
                sendJson(ex, 400, "{\"message\":\"students must be >= 1\"}");
                return;
            }

            Room allocated = manager.allocateRoom(students, needsAC, needsWashroom);
            if (allocated == null) {
                sendJson(ex, 404, "{\"message\":\"No room available\"}");
                return;
            }
            sendJson(ex, 200, toJson(allocated));
        } catch (Exception e) {
            sendJson(ex, 500, "{\"message\":\"Internal server error\"}");
        }
    }

    // -----------------------------
    // Static files
    // -----------------------------
    static class StaticHandler implements HttpHandler {
        private final Path webRoot;

        StaticHandler(Path webRoot) {
            this.webRoot = webRoot;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                path = "/index.html";
            }
            // Basic path traversal protection
            Path resolved = webRoot.resolve(path.substring(1)).normalize();
            if (!resolved.startsWith(webRoot.normalize())) {
                sendPlain(ex, 403, "Forbidden");
                return;
            }
            if (!Files.exists(resolved) || Files.isDirectory(resolved)) {
                sendPlain(ex, 404, "Not Found");
                return;
            }
            byte[] bytes = Files.readAllBytes(resolved);
            Headers h = ex.getResponseHeaders();
            h.set("Content-Type", contentType(resolved));
            h.set("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }

        private static String contentType(Path p) {
            String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".html")) return "text/html; charset=utf-8";
            if (name.endsWith(".css")) return "text/css; charset=utf-8";
            if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (name.endsWith(".json")) return "application/json; charset=utf-8";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            if (name.endsWith(".svg")) return "image/svg+xml";
            return "application/octet-stream";
        }
    }

    // -----------------------------
    // Helpers: IO + JSON
    // -----------------------------
    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        addCorsHeaders(h);
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Cache-Control", "no-store");
        h.set("X-Server-Time", Instant.now().toString());
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendPlain(HttpExchange ex, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        addCorsHeaders(h);
        h.set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendEmpty(HttpExchange ex, int status) throws IOException {
        Headers h = ex.getResponseHeaders();
        addCorsHeaders(h);
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }

    private static void addCorsHeaders(Headers h) {
        // Allow browser requests from file:// and other origins (local dev)
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
        h.set("Access-Control-Max-Age", "86400");
    }

    private static String toJson(Room r) {
        return "{"
                + "\"roomNo\":\"" + jsonEscape(r.roomNo) + "\","
                + "\"capacity\":" + r.capacity + ","
                + "\"hasAC\":" + r.hasAC + ","
                + "\"hasAttachedWashroom\":" + r.hasAttachedWashroom
                + "}";
    }

    private static String toJsonArray(List<Room> rooms) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rooms.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(rooms.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static List<Room> parseRoomsJsonArray(String body) {
        String s = body == null ? "" : body.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        if (s.trim().isEmpty()) return List.of();

        List<String> objs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        int braceDepth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inQuotes = !inQuotes;
            if (!inQuotes) {
                if (c == '{') braceDepth++;
                if (c == '}') braceDepth--;
            }
            cur.append(c);
            if (!inQuotes && braceDepth == 0 && c == '}') {
                objs.add(cur.toString().trim());
                cur.setLength(0);
            }
        }

        List<Room> rooms = new ArrayList<>();
        for (String obj : objs) {
            Map<String, String> m = parseFlatJsonObject(obj);
            String roomNo = m.getOrDefault("roomNo", "").trim();
            int capacity = parseIntSafe(m.get("capacity"), -1);
            boolean hasAC = parseBoolSafe(m.get("hasAC"), false);
            boolean hasWashroom = parseBoolSafe(m.get("hasAttachedWashroom"), false);
            if (!roomNo.isEmpty() && capacity > 0) {
                rooms.add(new Room(roomNo, capacity, hasAC, hasWashroom));
            }
        }
        return rooms;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Minimal JSON object parser for flat payloads like:
     * {"roomNo":"101","capacity":2,"hasAC":true,"hasAttachedWashroom":false}
     *
     * Assumptions:
     * - No nested objects/arrays
     * - Values are strings/numbers/booleans
     */
    private static Map<String, String> parseFlatJsonObject(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null) return map;
        String s = body.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
        if (s.trim().isEmpty()) return map;

        // split by commas not inside quotes (simple state machine)
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inQuotes = !inQuotes;
            if (c == ',' && !inQuotes) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());

        for (String p : parts) {
            int idx = p.indexOf(':');
            if (idx <= 0) continue;
            String key = stripQuotes(p.substring(0, idx).trim());
            String valRaw = p.substring(idx + 1).trim();
            String val = stripQuotes(valRaw);
            map.put(key, val);
        }
        return map;
    }

    private static String stripQuotes(String s) {
        String t = s.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            t = t.substring(1, t.length() - 1);
        }
        // unescape minimal
        t = t.replace("\\\"", "\"").replace("\\\\", "\\");
        return t;
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            if (s == null) return fallback;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean parseBoolSafe(String s, boolean fallback) {
        if (s == null) return fallback;
        String t = s.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(t)) return true;
        if ("false".equals(t)) return false;
        return fallback;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        String[] parts = rawQuery.split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            String k = idx >= 0 ? part.substring(0, idx) : part;
            String v = idx >= 0 ? part.substring(idx + 1) : "";
            map.put(urlDecode(k), urlDecode(v));
        }
        return map;
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}

