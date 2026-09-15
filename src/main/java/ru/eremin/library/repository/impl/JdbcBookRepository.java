package ru.eremin.library.repository.impl;

import lombok.AllArgsConstructor;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.eremin.library.exception.BookNotFoundException;
import ru.eremin.library.model.Book;
import ru.eremin.library.repository.BookRepository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;


@Repository
@AllArgsConstructor
public class JdbcBookRepository implements BookRepository {

    private static final String FIND_ALL_SQL = "SELECT id, title, author, publication_year FROM book ORDER BY id";

    private static final String FIND_BY_ID_SQL = "SELECT id, title, author, publication_year FROM book WHERE id = ?";

    private static final String INSERT_SQL = "INSERT INTO book (title, author, publication_year) VALUES (?, ?, ?)";

    private static final String UPDATE_SQL = "UPDATE book SET title = ?, author = ?, publication_year = ? WHERE id = ?";

    private static final String DELETE_SQL = "DELETE FROM book WHERE id = ?";

    private static final RowMapper<Book> BOOK_ROW_MAPPER = (rs, rowNum) -> {
        Book book = new Book();

        book.setId(rs.getLong("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));

        int year = rs.getInt("publication_year");
        book.setPublicationYear(rs.wasNull() ? null : year);

        return book;
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Book> findAll() {
        return jdbcTemplate.query(FIND_ALL_SQL, BOOK_ROW_MAPPER);
    }

    @Override
    public Optional<Book> findById(long id) {
        List<Book> result = jdbcTemplate.query(FIND_BY_ID_SQL, BOOK_ROW_MAPPER, id);
        return DataAccessUtils.optionalResult(result);
    }

    @Override
    public Book save(Book book) {
        if (book.getId() == null) {
            return insert(book);
        }
        return update(book);
    }

    private Book insert(Book book) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        PreparedStatementCreator psc = connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    INSERT_SQL,
                    new String[]{"id"}
            );

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());

            if (book.getPublicationYear() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, book.getPublicationYear());
            }

            return ps;
        };

        jdbcTemplate.update(psc, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Unable to retrieve generated book id");
        }

        book.setId(generatedId.longValue());
        return book;
    }

    private Book update(Book book) {
        int updatedRows = jdbcTemplate.update(UPDATE_SQL, ps -> {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());

            if (book.getPublicationYear() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, book.getPublicationYear());
            }

            ps.setLong(4, book.getId());
        });

        if (updatedRows == 0) {
            throw new BookNotFoundException(book.getId());
        }

        return book;
    }

    @Override
    public boolean deleteById(long id) {
        int deletedRows = jdbcTemplate.update(DELETE_SQL, id);
        return deletedRows > 0;
    }
}
