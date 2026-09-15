package ru.eremin.library.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.eremin.library.dto.BookCreateRequest;
import ru.eremin.library.dto.BookDto;
import ru.eremin.library.dto.BookUpdateRequest;
import ru.eremin.library.exception.BookNotFoundException;
import ru.eremin.library.mapper.BookMapper;
import ru.eremin.library.model.Book;
import ru.eremin.library.repository.BookRepository;
import ru.eremin.library.service.BookService;

import java.util.List;


@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @Transactional(readOnly = true)
    @Override
    public List<BookDto> findAll() {
        return bookRepository.findAll()
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public BookDto findById(Long id) {
        return bookRepository.findById(id)
                .map(bookMapper::toDto)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    @Override
    public BookDto create(BookCreateRequest request) {
        Book book = bookMapper.toEntity(request);
        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    @Transactional
    @Override
    public BookDto update(Long id, BookUpdateRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        bookMapper.updateEntity(book, request);
        Book updatedBook = bookRepository.save(book);
        return bookMapper.toDto(updatedBook);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        boolean deleted = bookRepository.deleteById(id);

        if (!deleted) {
            throw new BookNotFoundException(id);
        }
    }
}
