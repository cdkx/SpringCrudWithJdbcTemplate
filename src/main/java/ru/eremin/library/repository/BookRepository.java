package ru.eremin.library.repository;

import ru.eremin.library.model.Book;

import java.util.List;
import java.util.Optional;


public interface BookRepository {

    List<Book> findAll();

    Optional<Book> findById(long id);

    Book save(Book book);

    boolean deleteById(long id);
}
