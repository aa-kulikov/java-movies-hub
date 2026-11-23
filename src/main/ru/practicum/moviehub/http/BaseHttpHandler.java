package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected final Gson gson = new Gson();

    protected void sendJson(HttpExchange httpEx, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        httpEx.getResponseHeaders().set("Content-Type", CT_JSON);
        httpEx.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = httpEx.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange httpEx) throws IOException {
        httpEx.getResponseHeaders().set("Content-Type", CT_JSON);
        httpEx.sendResponseHeaders(204, -1);
    }

    protected void sendError(HttpExchange httpEx, int status, String message) throws IOException {
        sendJson(httpEx, status, gson.toJson(new ErrorResponse(message)));
    }

    protected void sendValidationError(HttpExchange httpEx, String message, List<String> details) throws IOException {
        sendJson(httpEx, 422, gson.toJson(new ErrorResponse(message, details)));
    }
}