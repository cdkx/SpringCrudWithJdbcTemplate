package ru.eremin.library.service;

import ru.eremin.library.dto.BookCreateRequest;
import ru.eremin.library.dto.BookDto;
import ru.eremin.library.dto.BookUpdateRequest;

import java.util.List;


public interface BookService {

    List<BookDto> findAll();

    BookDto findById(Long id);

    BookDto create(BookCreateRequest request);

    BookDto update(Long id, BookUpdateRequest request);

    void delete(Long id);
}
