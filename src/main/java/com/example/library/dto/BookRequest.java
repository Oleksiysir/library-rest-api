package com.example.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.example.library.validation.NotFutureYear;

public class BookRequest {
    @NotBlank(message = "Title must not be blank")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @NotBlank(message = "Author must not be blank")
    @Size(max = 150, message = "Author must be at most 150 characters")
    private String author;

    @NotBlank(message = "ISBN must not be blank")
    @Size(max = 20, message = "ISBN must be at most 20 characters")
    @Pattern(regexp = "^[0-9Xx-]{10,20}$", message = "ISBN must contain only digits, X or hyphens")
    private String isbn;

    @NotNull(message = "Publication year is required")
    @Min(value = 1, message = "Publication year must be greater than 0")
    @NotFutureYear
    private Integer publicationYear;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    public BookRequest() {
    }

    public BookRequest(String title, String author, String isbn, Integer publicationYear, Integer quantity) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.quantity = quantity;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
