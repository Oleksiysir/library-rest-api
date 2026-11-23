package com.example.library.service;

import com.example.library.dto.LoanRequest;
import com.example.library.dto.LoanResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.model.Book;
import com.example.library.model.Client;
import com.example.library.model.Loan;
import com.example.library.repository.BookRepository;
import com.example.library.repository.ClientRepository;
import com.example.library.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.library.config.LibraryProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;

@Service
public class LoanService {
    private final LoanRepository loans;
    private final ClientRepository clients;
    private final BookRepository books;
    private final LibraryProperties properties;

    public LoanService(LoanRepository loans, ClientRepository clients, BookRepository books, LibraryProperties properties) {
        this.loans = loans;
        this.clients = clients;
        this.books = books;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> findAll() {
        return loans.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LoanResponse findById(Long id) {
        return toResponse(getLoan(id));
    }

    @CacheEvict(cacheNames = {"booksAll", "bookById", "clientBooks"}, allEntries = true)
    @Transactional
    public LoanResponse assign(LoanRequest request) {

        if (!properties.getBorrowing().isEnabled()) {
            throw new ConflictException("Book borrowing is currently disabled");
        }

        Client client = clients.findByIdForUpdate(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client with id " + request.getClientId() + " was not found"));

        long activeLoans = loans.countByClient_Id(client.getId());

        if (activeLoans >= properties.getBorrowing().getMaxActivePerClient()) {
            throw new ConflictException(
                    "Client has reached the maximum number of active loans"
            );
        }

        Book book = books.findByIdForUpdate(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book with id " + request.getBookId() + " was not found"));

        if (book.getQuantity() <= 0) {
            throw new ConflictException(
                    "No copies available for book with id " + book.getId()
            );
        }

        book.setQuantity(book.getQuantity() - 1);

        Loan loan = loans.saveAndFlush(new Loan(client, book));

        return toResponse(loan);
    }

    @CacheEvict(cacheNames = {"booksAll", "bookById", "clientBooks"}, allEntries = true)
    @Transactional
    public void returnBook(Long id) {
        Loan loan = getLoan(id);
        Long bookId = loan.getBook().getId();
        Book book = books.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " was not found"));
        book.setQuantity(book.getQuantity() + 1);
        loans.delete(loan);
    }

    @CacheEvict(cacheNames = {"booksAll", "bookById", "clientBooks"}, allEntries = true)
    @Transactional
    public void deleteAllAndReturnBooks() {
        List<Loan> activeLoans = loans.findAllByOrderByIdAsc();
        Map<Long, Long> copiesByBookId = activeLoans.stream()
                .collect(Collectors.groupingBy(loan -> loan.getBook().getId(), Collectors.counting()));

        copiesByBookId.forEach((bookId, count) -> {
            Book book = books.findByIdForUpdate(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " was not found"));
            book.setQuantity(book.getQuantity() + Math.toIntExact(count));
        });
        loans.deleteAllInBatch();
    }

    private Loan getLoan(Long id) {
        return loans.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan with id " + id + " was not found"));
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getClient().getId(), loan.getBook().getId(), loan.getAssignedAt());
    }
}
