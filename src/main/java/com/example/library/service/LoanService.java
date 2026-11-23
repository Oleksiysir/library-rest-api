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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoanService {
    private final LoanRepository loans;
    private final ClientRepository clients;
    private final BookRepository books;

    public LoanService(LoanRepository loans, ClientRepository clients, BookRepository books) {
        this.loans = loans;
        this.clients = clients;
        this.books = books;
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> findAll() {
        return loans.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LoanResponse findById(Long id) {
        return toResponse(getLoan(id));
    }

    @Transactional
    public LoanResponse assign(LoanRequest request) {
        Client client = clients.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client with id " + request.getClientId() + " was not found"));

        // A row-level lock prevents two concurrent requests from issuing the final copy.
        Book book = books.findByIdForUpdate(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book with id " + request.getBookId() + " was not found"));

        if (book.getQuantity() <= 0) {
            throw new ConflictException("No copies available for book with id " + book.getId());
        }

        book.setQuantity(book.getQuantity() - 1);
        Loan loan = loans.saveAndFlush(new Loan(client, book));
        return toResponse(loan);
    }

    @Transactional
    public void returnBook(Long id) {
        Loan loan = getLoan(id);
        Long bookId = loan.getBook().getId();
        Book book = books.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " was not found"));
        book.setQuantity(book.getQuantity() + 1);
        loans.delete(loan);
    }

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
