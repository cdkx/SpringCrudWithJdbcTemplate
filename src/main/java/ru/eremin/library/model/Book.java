package ru.eremin.library.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("book")
public class Book {

    @Id
    private Long id;

    private String title;

    private String author;

    @Column("publication_year")
    private Integer publicationYear;
}
