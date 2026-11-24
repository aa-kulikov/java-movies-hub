package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.http.HttpResponse;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static MoviesServer server;
    private static MoviesStore store;
    private static MoviesApiTestHelper apiHelper;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
        apiHelper = new MoviesApiTestHelper("http://localhost:8080");
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    private void assertJsonResponse(HttpResponse<String> resp, int expectedStatus) {
        //Проверяем код ответа
        assertEquals(expectedStatus, resp.statusCode());

        //Провереям Content-Type
        assertEquals(MoviesApiTestHelper.CT_JSON, resp.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void getMoviesWhenEmptyReturnEmptyArray() throws Exception {
        HttpResponse<String> resp = apiHelper.get("/movies");
        //Оьщие проверки
        assertJsonResponse(resp, 200);

        List<Movie> movies = apiHelper.parseMovieList(resp);

        //Провереям, что вренулся пустой массив
        assertNotNull(movies);
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMoviesWhenNotEmptyReturnsAddedMovies() throws Exception {
        store.add(new Movie("Криминальное чтиво", 1994));
        store.add(new Movie("Омерзительная восьмерка", 2015));

        HttpResponse<String> resp = apiHelper.get("/movies");
        assertJsonResponse(resp, 200);

        //Проверяем, что вернул массив с 2 элементами
        assertEquals(2, apiHelper.parseMovieList(resp).size());
    }

    @Test
    void postMovieWhenValidCreatesAndReturnMovie() throws Exception {
        HttpResponse<String> resp = apiHelper.post("/movies", new Movie("Бешенные псы", 1992));
        assertJsonResponse(resp, 201);

        Movie created = apiHelper.parseMovie(resp);

        //Присвоен ID
        assertNotNull(created.getId());

        //Данные соответствуют переданным
        assertEquals("Бешенные псы", created.getTitle());
        assertEquals(1992, created.getYear());
    }

    @Test
    void postMovieWhenTitleEmptyReturn422() throws Exception {
        //Передаем пустое название
        HttpResponse<String> resp = apiHelper.post("/movies", new Movie("   ", 2020));
        assertJsonResponse(resp, 422);

        ErrorResponse error = apiHelper.parseError(resp);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(error.getError());

        //Проверем, что вернулся "массив строк с деталями проблемы'
        assertNotNull(error.getDetails());
        assertFalse(error.getDetails().isEmpty());
    }

    @Test
    void postMovieWhenTitleTooLongReturn422() throws Exception {
        //Передаем слишком длинное название (длина > 100 символов)
        HttpResponse<String> resp = apiHelper.post("/movies", new Movie("А".repeat(101), 2020));
        assertJsonResponse(resp, 422);

        ErrorResponse error = apiHelper.parseError(resp);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(error.getError());

        //Проверем, что вернулся "массив строк с деталями проблемы"
        assertNotNull(error.getDetails());
        assertFalse(error.getDetails().isEmpty());
    }

    @Test
    void postMovieWhenTitleExactly100CharsReturn201() throws Exception {
        //Передаем название пограничной длины
        String title100 = "А".repeat(100);
        HttpResponse<String> resp = apiHelper.post("/movies", new Movie(title100, 2020));

        //Общая проверка
        assertJsonResponse(resp, 201);

        Movie created = apiHelper.parseMovie(resp);

        //Присвоен ID
        assertNotNull(created.getId());

        //Проверяем, что название в ответе совпадает с отправленным
        assertEquals(title100, created.getTitle());
    }

    @Test
    void postMovieWhenYearOutOfRangeReturn422() throws Exception {
        //Недопустимый год (более чем, текущий год + 1)
        int futureYear = Year.now().getValue() + 2;

        //1887 год - фильмов еще не было
        HttpResponse<String> respOld = apiHelper.post("/movies", new Movie("Старый фильм", 1887));
        HttpResponse<String> respNew = apiHelper.post("/movies", new Movie("Фильм из будущего", futureYear));

        //Проверяем, что вернул корректную ошибку для обоих случаев
        assertJsonResponse(respOld, 422);
        assertJsonResponse(respNew, 422);
    }

    @Test
    void postMovieWhenYearOnBoundariesReturn201() throws Exception {
        //Макс. допустимый год (текущий год + 1)
        int maxYear = Year.now().getValue() + 1;

        HttpResponse<String> respMin = apiHelper.post("/movies", new Movie("Сцена в саду Раундхэй", 1888));
        HttpResponse<String> respMax = apiHelper.post("/movies", new Movie("Фильм следующего года", maxYear));

        //Общая проверка
        assertJsonResponse(respMin, 201);
        assertJsonResponse(respMax, 201);

        //Проверяем нижнюю границу
        assertEquals(1888, apiHelper.parseMovie(respMin).getYear());

        //Проверяем верхнюю границу
        assertEquals(maxYear, apiHelper.parseMovie(respMax).getYear());
    }

    @Test
    void postMovieWhenWrongContentTypeReturn415() throws Exception {
        //Передаем некоррекьный заголовок
        HttpResponse<String> resp = apiHelper.postRaw("/movies", "{\"title\":\"Дюна\",\"year\":2021}",
                "text/plain; charset=UTF-8");

        //Общая проверка
        assertJsonResponse(resp, 415);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void postMovieWhenInvalidJsonReturn400() throws Exception {
        //Передаем некорректный body
        HttpResponse<String> resp = apiHelper.postRaw("/movies", "{title: \"Невалидный JSON\"",
                MoviesApiTestHelper.CT_JSON);

        //Общая проверка
        assertJsonResponse(resp, 400);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void getMovieByIdWhenExistsReturnMovie() throws Exception {
        Movie saved = store.add(new Movie("Побег из Шоушенка", 1994));
        HttpResponse<String> resp = apiHelper.get("/movies/" + saved.getId());

        //Общая проверка
        assertJsonResponse(resp, 200);

        //Проверяем, что по ID вернулся именно тот фильм
        assertEquals(saved, apiHelper.parseMovie(resp));
    }

    @Test
    void getMovieByIdWhenNotFoundReturn404() throws Exception {
        //Получаем несуществуюй фильм
        HttpResponse<String> resp = apiHelper.get("/movies/999");

        //Общая проверка
        assertJsonResponse(resp, 404);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void getMovieByIdWhenIdNotNumberReturn400() throws Exception {
        //Год фильма не число
        HttpResponse<String> resp = apiHelper.get("/movies/abc");

        //Общая проверка
        assertJsonResponse(resp, 400);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void deleteMovieByIdWhenExistsReturn204() throws Exception {
        Movie saved = store.add(new Movie("Дождливый день в Нью-Йорке", 2019));
        HttpResponse<String> resp = apiHelper.delete("/movies/" + saved.getId());

        //Общая проверка
        assertJsonResponse(resp, 204);

        //Проверяем, что фильм удален
        assertTrue(store.findById(saved.getId()).isEmpty());
    }

    @Test
    void deleteMovieByIdWhenNotFoundReturn404() throws Exception {
        //Пробуем удалить несуществующий фильм
        HttpResponse<String> resp = apiHelper.delete("/movies/999");

        //Общая проверка
        assertJsonResponse(resp, 404);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void deleteMovieByIdWhenIdNotNumberReturn400() throws Exception {
        //ID не число
        HttpResponse<String> resp = apiHelper.delete("/movies/invalid");

        //Общая проверка
        assertJsonResponse(resp, 400);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void getMoviesByYearReturnFilteredMovies() throws Exception {
        store.add(new Movie("Полночь в Париже", 2011));
        store.add(new Movie("Кот в сапогах", 2011));
        store.add(new Movie("Начало", 2010));

        //Фильтр по году 2011
        HttpResponse<String> resp = apiHelper.get("/movies?year=2011");

        //Общая проверка
        assertJsonResponse(resp, 200);

        //Проверка, что фильтр отработал верно
        assertEquals(2, apiHelper.parseMovieList(resp).size());
    }

    @Test
    void getMoviesByYearReturnEmptyList() throws Exception {
        store.add(new Movie("Матрица", 1999));

        //Получаем фильмы по фильтру
        HttpResponse<String> resp = apiHelper.get("/movies?year=2025");

        //Общие проверки
        assertJsonResponse(resp, 200);

        //Проверяем, что вернул пустой список
        assertTrue(apiHelper.parseMovieList(resp).isEmpty());
    }

    @Test
    void getMoviesByYearWhenYearNotNumberReturn400() throws Exception {
        //Передаем некорректный year
        HttpResponse<String> resp = apiHelper.get("/movies?year=abcd");

        //Общая проверка
        assertJsonResponse(resp, 400);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void unsupportedMethodReturn405() throws Exception {
        //эндпоинт /movies не поддерживает метод PUT
        HttpResponse<String> resp = apiHelper.putRaw("/movies", "{}");

        //Общая проверка
        assertJsonResponse(resp, 405);

        //Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void unsupportedMethodForMovieByIdReturn405() throws Exception {
        // эндпоинт /movies/{id} не поддерживает метод PUT
        HttpResponse<String> resp = apiHelper.putRaw("/movies/1", "{}");

        // Общая проверка
        assertJsonResponse(resp, 405);

        // Проверяем, что присутствует непустое поле error
        assertNotNull(apiHelper.parseError(resp).getError());
    }

    @Test
    void getMoviesByYearWithExtraParamsReturn200() throws Exception {
        store.add(new Movie("Джентльмены", 2019));
        store.add(new Movie("Терминатор 2", 1991));

        //Передаем year + доп. параметр
        HttpResponse<String> resp = apiHelper.get("/movies?year=2019&sort=title");
        List<Movie> movies = apiHelper.parseMovieList(resp);

        //Общая проверка
        assertJsonResponse(resp, 200);

        //Проверяем, что фильтр по year отработал корректно
        assertEquals(1, movies.size());
        assertEquals("Джентльмены", movies.getFirst().getTitle());
    }

    @Test
    void getMoviesByYearWithEncodedSpacesReturn200() throws Exception {
        store.add(new Movie("Душа", 2020));
        store.add(new Movie("Паразиты", 2019));

        //Передаем year + закодированный пробел (%20)
        HttpResponse<String> resp = apiHelper.get("/movies?year=2020%20");
        List<Movie> movies = apiHelper.parseMovieList(resp);

        //Общая проверка
        assertJsonResponse(resp, 200);

        //Проверяем, что фильтр по year отработал корректно
        assertEquals(1, movies.size());
        assertEquals("Душа", movies.getFirst().getTitle());
    }
}