package com.example.library.service;

import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.model.Book;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.Locale;

@Service
public class BookService {
    private final BookRepository books;
    private final LoanRepository loans;

    public BookService(BookRepository books, LoanRepository loans) {
        this.books = books;
        this.loans = loans;
    }

    @Cacheable(cacheNames = "booksAll")
    @Transactional(readOnly = true)
    public List<BookResponse> findAll() {
        return books.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Cacheable(cacheNames = "bookById", key = "#id")
    @Transactional(readOnly = true)
    public BookResponse findById(Long id) {
        return toResponse(getBook(id));
    }

    @CacheEvict(cacheNames = {"booksAll", "bookById", "clientBooks"}, allEntries = true)
    @Transactional
    public BookResponse create(BookRequest request) {
        String isbn = normalizeIsbn(request.getIsbn());
        if (books.existsByIsbnIgnoreCase(isbn)) {
            throw new ConflictException("A book with ISBN '" + isbn + "' already exists");
        }

        Book book = new Book(
                request.getTitle().trim(),
                request.getAuthor().trim(),
                isbn,
                request.getPublicationYear(),
                request.getQuantity()
        );
        return toResponse(books.saveAndFlush(book));
    }

    @CacheEvict(cacheNames = {"booksAll", "bookById", "clientBooks"}, allEntries = true)
    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = getBook(id);
        String isbn = normalizeIsbn(request.getIsbn());
        if (books.existsByIsbnIgnoreCaseAndIdNot(isbn, id)) {
            throw new ConflictException("A book with ISBN '" + isbn + "' already exists");
        }

        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(isbn);
        book.setPublicationYear(request.getPublicationYear());
        book.setQuantity(request.getQuantity());
        return toResponse(books.saveAndFlush(book));
    }

    @CacheEvict(cacheNames = {"booksAll", "bookById", "clientBooks"}, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Book book = getBook(id);
        loans.deleteAllByBook_Id(id);
        books.delete(book);
    }

    @CacheEvict(cacheNames = {"booksAll", "bookById", "clientBooks"}, allEntries = true)
    @Transactional
    public void deleteAll() {
        loans.deleteAllInBatch();
        books.deleteAllInBatch();
    }

    private Book getBook(Long id) {
        return books.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + id + " was not found"));
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getPublicationYear(), book.getQuantity());
    }

    private String normalizeIsbn(String isbn) {
        return isbn.trim().toUpperCase(Locale.ROOT);
    }
}
