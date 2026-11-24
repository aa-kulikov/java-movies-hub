package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    public Movie add(Movie movie) {
        int id = nextId;
        nextId += 1;
        Movie saved = new Movie(id, movie.getTitle(), movie.getYear());
        movies.put(id, saved);
        return saved;
    }

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public List<Movie> findByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() != null && movie.getYear() == year)
                .collect(Collectors.toList());
    }

    public Optional<Movie> findById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean deleteById(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}