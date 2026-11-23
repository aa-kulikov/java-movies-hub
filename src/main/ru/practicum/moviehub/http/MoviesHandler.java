package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange httpEx) throws IOException {
        try {
            String path = httpEx.getRequestURI().getPath();
            if (!"/movies".equals(path)) {
                sendError(httpEx, 404, "Ресурс не найден");
                return;
            }

            String method = httpEx.getRequestMethod().toUpperCase();
            switch (method) {
                case "GET":
                    handleGetMovies(httpEx, httpEx.getRequestURI().getQuery());
                    break;
                case "POST":
                    handlePostMovie(httpEx);
                    break;
                default:
                    sendError(httpEx, 405, "Метод не поддерживается");
                    break;
            }
        } catch (Exception exp) {
            sendError(httpEx, 500, "Внутренняя ошибка сервера");
        }
    }

    private void handleGetMovies(HttpExchange httpEx, String query) throws IOException {
        if (query == null || query.isBlank()) {
            sendJson(httpEx, 200, gson.toJson(store.findAll()));
            return;
        }

        Integer year = parseYearQueryParam(query);
        if (year == null) {
            sendError(httpEx, 400, "Некорректный параметр запроса — 'year'");
            return;
        }

        sendJson(httpEx, 200, gson.toJson(store.findByYear(year)));
    }

    private Integer parseYearQueryParam(String query) {
        String[] pairs = query.split("&");
        if (pairs.length != 1) {
            return null;
        }

        String[] kv = pairs[0].split("=", 2);
        if (kv.length != 2 || !"year".equals(kv[0])) {
            return null;
        }

        try {
            return Integer.parseInt(kv[1]);
        } catch (NumberFormatException exp) {
            return null;
        }
    }

    private void handlePostMovie(HttpExchange httpEx) throws IOException {
        String contentType = httpEx.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            sendError(httpEx, 415, "Неподдерживаемый тип контента: ожидается application/json");
            return;
        }

        String body;
        try (InputStream is = httpEx.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        if (body.isBlank()) {
            sendError(httpEx, 400, "Тело запроса не должно быть пустым");
            return;
        }

        Movie incoming;
        try {
            incoming = gson.fromJson(body, Movie.class);
        } catch (JsonSyntaxException exp) {
            sendError(httpEx, 400, "Некорректный формат JSON");
            return;
        }

        if (incoming == null) {
            sendError(httpEx, 400, "Некорректный формат JSON");
            return;
        }

        List<String> validationErrors = validateMovie(incoming);
        if (!validationErrors.isEmpty()) {
            sendValidationError(httpEx, "Ошибка валидации", validationErrors);
            return;
        }

        Movie saved = store.add(incoming);
        sendJson(httpEx, 201, gson.toJson(saved));
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        int maxYear = Year.now().getValue() + 1;

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            errors.add("длина названия не должна превышать " + MAX_TITLE_LENGTH + " символов");
        }

        if (movie.getYear() == null || movie.getYear() < MIN_YEAR || movie.getYear() > maxYear) {
            errors.add("год должен быть между " + MIN_YEAR + " и " + maxYear);
        }

        return errors;
    }
}