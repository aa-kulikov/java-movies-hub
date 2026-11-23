package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.Optional;

public class MovieByIdHandler extends BaseHttpHandler {
    private static final String PREFIX = "/movies/";
    private final MoviesStore store;

    public MovieByIdHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange httpEx) throws IOException {
        try {
            String method = httpEx.getRequestMethod().toUpperCase();
            if (!"GET".equals(method) && !"DELETE".equals(method)) {
                sendError(httpEx, 405, "Метод не поддерживается");
                return;
            }

            Integer id = extractId(httpEx);
            if (id == null) {
                sendError(httpEx, 400, "Некорректный ID");
                return;
            }

            if ("GET".equals(method)) {
                Optional<Movie> movie = store.findById(id);
                if (movie.isPresent()) {
                    sendJson(httpEx, 200, gson.toJson(movie.get()));
                } else {
                    sendError(httpEx, 404, "Фильм не найден");
                }
            } else {
                boolean deleted = store.deleteById(id);
                if (deleted) {
                    sendNoContent(httpEx);
                } else {
                    sendError(httpEx, 404, "Фильм не найден");
                }
            }
        } catch (Exception exp) {
            sendError(httpEx, 500, "Внутренняя ошибка сервера");
        }
    }

    private Integer extractId(HttpExchange httpEx) {
        String path = httpEx.getRequestURI().getPath();
        if (path == null || !path.startsWith(PREFIX)) {
            return null;
        }

        String idPart = path.substring(PREFIX.length());
        if (idPart.endsWith("/")) {
            idPart = idPart.substring(0, idPart.length() - 1);
        }

        if (idPart.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(idPart);
        } catch (NumberFormatException exp) {
            return null;
        }
    }
}