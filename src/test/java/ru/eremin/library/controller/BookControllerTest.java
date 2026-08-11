package ru.eremin.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.eremin.library.dto.BookCreateRequest;
import ru.eremin.library.dto.BookDto;
import ru.eremin.library.dto.BookUpdateRequest;
import ru.eremin.library.exception.BookNotFoundException;
import ru.eremin.library.service.BookService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/books";

    private BookDto sampleBookDto() {
        return new BookDto(1L, "Война и мир", "Лев Толстой", 1869);
    }


    @Nested
    @DisplayName("POST /api/v1/books")
    class CreateTests {

        @Test
        @DisplayName("Должен создать книгу и вернуть 201")
        void shouldCreateBookAndReturn201() throws Exception {
            BookCreateRequest request = new BookCreateRequest(
                    "Мастер и Маргарита", "Михаил Булгаков", 1967
            );
            BookDto response = new BookDto(3L, "Мастер и Маргарита", "Михаил Булгаков", 1967);

            when(bookService.create(any(BookCreateRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.title").value("Мастер и Маргарита"))
                    .andExpect(jsonPath("$.author").value("Михаил Булгаков"))
                    .andExpect(jsonPath("$.publicationYear").value(1967));

            verify(bookService).create(any(BookCreateRequest.class));
        }

        @Test
        @DisplayName("Должен вернуть 400 при невалидном теле запроса")
        void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
            BookCreateRequest invalidRequest = new BookCreateRequest("", "", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Должен вернуть 400 при пустом title")
        void shouldReturn400WhenTitleIsBlank() throws Exception {
            BookCreateRequest request = new BookCreateRequest("   ", "Автор", 2000);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }


    @Nested
    @DisplayName("GET /api/v1/books")
    class FindAllTests {

        @Test
        @DisplayName("Должен вернуть список книг и 200")
        void shouldReturnAllBooksAnd200() throws Exception {
            List<BookDto> books = List.of(
                    sampleBookDto(),
                    new BookDto(2L, "Преступление и наказание", "Фёдор Достоевский", 1866)
            );

            when(bookService.findAll()).thenReturn(books);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("Война и мир"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].author").value("Фёдор Достоевский"));

            verify(bookService).findAll();
        }

        @Test
        @DisplayName("Должен вернуть пустой список и 200")
        void shouldReturnEmptyListAnd200() throws Exception {
            when(bookService.findAll()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }


    @Nested
    @DisplayName("GET /api/v1/books/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("Должен вернуть книгу по id и 200")
        void shouldReturnBookByIdAnd200() throws Exception {
            when(bookService.findById(1L)).thenReturn(sampleBookDto());

            mockMvc.perform(get(BASE_URL + "/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Война и мир"))
                    .andExpect(jsonPath("$.author").value("Лев Толстой"))
                    .andExpect(jsonPath("$.publicationYear").value(1869));

            verify(bookService).findById(1L);
        }

        @Test
        @DisplayName("Должен вернуть 404 если книга не найдена")
        void shouldReturn404WhenBookNotFound() throws Exception {
            when(bookService.findById(999L))
                    .thenThrow(new BookNotFoundException(999L));

            mockMvc.perform(get(BASE_URL + "/{id}", 999L))
                    .andExpect(status().isNotFound());

            verify(bookService).findById(999L);
        }
    }


    @Nested
    @DisplayName("PUT /api/v1/books/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Должен обновить книгу и вернуть 200")
        void shouldUpdateBookAndReturn200() throws Exception {
            BookUpdateRequest request = new BookUpdateRequest(
                    "Война и мир (изд. 2)", "Лев Николаевич Толстой", 1869
            );
            BookDto updated = new BookDto(1L, "Война и мир (изд. 2)", "Лев Николаевич Толстой", 1869);

            when(bookService.update(eq(1L), any(BookUpdateRequest.class))).thenReturn(updated);

            mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Война и мир (изд. 2)"))
                    .andExpect(jsonPath("$.author").value("Лев Николаевич Толстой"));

            verify(bookService).update(eq(1L), any(BookUpdateRequest.class));
        }

        @Test
        @DisplayName("Должен вернуть 404 при обновлении несуществующей книги")
        void shouldReturn404WhenUpdatingNonExistentBook() throws Exception {
            BookUpdateRequest request = new BookUpdateRequest("Title", "Author", 2000);

            when(bookService.update(eq(999L), any(BookUpdateRequest.class)))
                    .thenThrow(new BookNotFoundException(999L));

            mockMvc.perform(put(BASE_URL + "/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Должен вернуть 400 при невалидном теле обновления")
        void shouldReturn400WhenUpdateBodyIsInvalid() throws Exception {
            BookUpdateRequest invalidRequest = new BookUpdateRequest("", "", null);

            mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }


    @Nested
    @DisplayName("DELETE /api/v1/books/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Должен удалить книгу и вернуть 204")
        void shouldDeleteBookAndReturn204() throws Exception {
            doNothing().when(bookService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(bookService).delete(1L);
        }

        @Test
        @DisplayName("Должен вернуть 404 при удалении несуществующей книги")
        void shouldReturn404WhenDeletingNonExistentBook() throws Exception {
            doThrow(new BookNotFoundException(999L)).when(bookService).delete(999L);

            mockMvc.perform(delete(BASE_URL + "/{id}", 999L))
                    .andExpect(status().isNotFound());

            verify(bookService).delete(999L);
        }
    }
}
