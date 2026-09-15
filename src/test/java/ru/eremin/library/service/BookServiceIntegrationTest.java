package ru.eremin.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.eremin.library.dto.BookCreateRequest;
import ru.eremin.library.dto.BookDto;
import ru.eremin.library.dto.BookUpdateRequest;
import ru.eremin.library.exception.BookNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BookServiceIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("library_test")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private BookService bookService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE book RESTART IDENTITY");
    }

    private BookCreateRequest createRequest(String title, String author, Integer publicationYear) {
        return new BookCreateRequest(title, author, publicationYear);
    }

    private BookDto createSampleBook() {
        BookCreateRequest request = createRequest(
                "Мастер и Маргарита",
                "Михаил Булгаков",
                1967
        );

        return bookService.create(request);
    }

    @Nested
    @DisplayName("CREATE")
    class CreateTests {

        @Test
        @DisplayName("Должен сохранить книгу и вернуть DTO с id")
        void shouldCreateBook() {
            BookCreateRequest request = createRequest(
                    "Война и мир",
                    "Лев Толстой",
                    1869
            );

            BookDto created = bookService.create(request);

            assertThat(created.id()).isNotNull();
            assertThat(created.id()).isEqualTo(1L);
            assertThat(created.title()).isEqualTo("Война и мир");
            assertThat(created.author()).isEqualTo("Лев Толстой");
            assertThat(created.publicationYear()).isEqualTo(1869);

            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book", Integer.class);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Созданная книга должна быть доступна по id")
        void shouldBeAccessibleAfterCreate() {
            BookDto created = createSampleBook();

            BookDto found = bookService.findById(created.id());

            assertThat(found).isEqualTo(created);
        }
    }

    @Nested
    @DisplayName("FIND ALL")
    class FindAllTests {

        @Test
        @DisplayName("Должен вернуть все книги в порядке id")
        void shouldReturnAllBooks() {
            BookDto first = bookService.create(
                    createRequest("Книга 1", "Автор 1", 2001)
            );

            BookDto second = bookService.create(
                    createRequest("Книга 2", "Автор 2", 2002)
            );

            List<BookDto> books = bookService.findAll();

            assertThat(books)
                    .hasSize(2)
                    .containsExactly(first, second);
        }

        @Test
        @DisplayName("Должен вернуть пустой список, если книг нет")
        void shouldReturnEmptyListWhenNoBooks() {
            List<BookDto> books = bookService.findAll();

            assertThat(books).isEmpty();
        }
    }

    @Nested
    @DisplayName("FIND BY ID")
    class FindByIdTests {

        @Test
        @DisplayName("Должен вернуть книгу по id")
        void shouldReturnBookById() {
            BookDto created = createSampleBook();

            BookDto found = bookService.findById(created.id());

            assertThat(found).isEqualTo(created);
        }

        @Test
        @DisplayName("Должен выбросить BookNotFoundException, если книга не найдена")
        void shouldThrowExceptionWhenBookNotFound() {
            assertThatThrownBy(() -> bookService.findById(999L))
                    .isInstanceOf(BookNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("UPDATE")
    class UpdateTests {

        @Test
        @DisplayName("Должен обновить книгу")
        void shouldUpdateBook() {
            BookDto created = createSampleBook();

            BookUpdateRequest updateRequest = new BookUpdateRequest(
                    "Обновленное название",
                    "Обновленный автор",
                    2020
            );

            BookDto updated = bookService.update(created.id(), updateRequest);

            assertThat(updated.id()).isEqualTo(created.id());
            assertThat(updated.title()).isEqualTo("Обновленное название");
            assertThat(updated.author()).isEqualTo("Обновленный автор");
            assertThat(updated.publicationYear()).isEqualTo(2020);

            BookDto found = bookService.findById(created.id());

            assertThat(found).isEqualTo(updated);
        }

        @Test
        @DisplayName("Должен выбросить BookNotFoundException при обновлении несуществующей книги")
        void shouldThrowExceptionWhenUpdatingNonExistentBook() {
            BookUpdateRequest updateRequest = new BookUpdateRequest(
                    "Название",
                    "Автор",
                    2000
            );

            assertThatThrownBy(() -> bookService.update(999L, updateRequest))
                    .isInstanceOf(BookNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("DELETE")
    class DeleteTests {

        @Test
        @DisplayName("Должен удалить книгу")
        void shouldDeleteBook() {
            BookDto created = createSampleBook();

            bookService.delete(created.id());

            assertThatThrownBy(() -> bookService.findById(created.id()))
                    .isInstanceOf(BookNotFoundException.class);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM book",
                    Integer.class
            );

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Должен выбросить BookNotFoundException при удалении несуществующей книги")
        void shouldThrowExceptionWhenDeletingNonExistentBook() {
            assertThatThrownBy(() -> bookService.delete(999L))
                    .isInstanceOf(BookNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }
}
