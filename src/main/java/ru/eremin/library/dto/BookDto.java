package ru.eremin.library.dto;

public record BookDto(
        Long id,
        String title,
        String author,
        Integer publicationYear
) {
}
