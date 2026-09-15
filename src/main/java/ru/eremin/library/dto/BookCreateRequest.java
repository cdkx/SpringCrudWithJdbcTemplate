package ru.eremin.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookCreateRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author,

        @NotNull(message = "Publication year is required")
        @Positive(message = "Publication year must be positive")
        Integer publicationYear
) {
}
