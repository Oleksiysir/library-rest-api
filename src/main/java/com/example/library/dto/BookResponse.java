package com.example.library.dto;

public class BookResponse {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private Integer publicationYear;
    private Integer quantity;

    public BookResponse() {
    }

    public BookResponse(Long id, String title, String author, String isbn,
                        Integer publicationYear, Integer quantity) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public Integer getPublicationYear() { return publicationYear; }
    public Integer getQuantity() { return quantity; }
}
