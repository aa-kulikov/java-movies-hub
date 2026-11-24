package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class MoviesApiTestHelper {
    public static final String CT_JSON = "application/json; charset=UTF-8";
    private final String baseUrl;
    private final HttpClient client;
    private final Gson gson;

    public MoviesApiTestHelper(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.gson = new Gson();
    }

    public HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> delete(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .DELETE()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> post(String path, Object body) throws Exception {
        return postRaw(path, gson.toJson(body), CT_JSON);
    }

    public HttpResponse<String> postRaw(String path, String rawBody, String contentType) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(rawBody, StandardCharsets.UTF_8));

        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> putRaw(String path, String rawBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .PUT(HttpRequest.BodyPublishers.ofString(rawBody, StandardCharsets.UTF_8))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public Movie parseMovie(HttpResponse<String> resp) {
        return gson.fromJson(resp.body(), Movie.class);
    }

    public List<Movie> parseMovieList(HttpResponse<String> resp) {
        return gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
    }

    public ErrorResponse parseError(HttpResponse<String> resp) {
        return gson.fromJson(resp.body(), ErrorResponse.class);
    }
}