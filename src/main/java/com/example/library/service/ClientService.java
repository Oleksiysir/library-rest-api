package com.example.library.service;

import com.example.library.dto.BookResponse;
import com.example.library.dto.ClientRequest;
import com.example.library.dto.ClientResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.model.Book;
import com.example.library.model.Client;
import com.example.library.model.Loan;
import com.example.library.repository.BookRepository;
import com.example.library.repository.ClientRepository;
import com.example.library.repository.LoanRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClientService {
    private final ClientRepository clients;
    private final BookRepository books;
    private final LoanRepository loans;

    public ClientService(ClientRepository clients, BookRepository books, LoanRepository loans) {
        this.clients = clients;
        this.books = books;
        this.loans = loans;
    }

    @Cacheable(cacheNames = "clientsAll")
    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clients.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Cacheable(cacheNames = "clientById", key = "#id")
    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        return toResponse(getClient(id));
    }

    @CacheEvict(cacheNames = {"clientsAll", "clientById"}, allEntries = true)
    @Transactional
    public ClientResponse create(ClientRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (clients.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A client with email '" + email + "' already exists");
        }

        Client client = new Client(request.getName().trim(), email);
        return toResponse(clients.saveAndFlush(client));
    }

    @CacheEvict(cacheNames = {"clientsAll", "clientById"}, allEntries = true)
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = getClient(id);
        String email = normalizeEmail(request.getEmail());
        if (clients.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("A client with email '" + email + "' already exists");
        }

        client.setName(request.getName().trim());
        client.setEmail(email);
        return toResponse(clients.saveAndFlush(client));
    }

    @CacheEvict(
            cacheNames = {
                    "clientsAll",
                    "clientById",
                    "clientBooks",
                    "booksAll",
                    "bookById"
            },
            allEntries = true
    )
    @Transactional
    public void delete(Long id) {
        Client client = getClient(id);
        List<Loan> activeLoans = loans.findAllByClient_IdOrderByIdAsc(id);
        returnCopies(activeLoans);
        loans.deleteAllByClient_Id(id);
        clients.delete(client);
    }

    @CacheEvict(
            cacheNames = {
                    "clientsAll",
                    "clientById",
                    "clientBooks",
                    "booksAll",
                    "bookById"
            },
            allEntries = true
    )
    @Transactional
    public void deleteAll() {
        returnCopies(loans.findAllByOrderByIdAsc());
        loans.deleteAllInBatch();
        clients.deleteAllInBatch();
    }

    @Cacheable(cacheNames = "clientBooks", key = "#clientId")
    @Transactional(readOnly = true)
    public List<BookResponse> findBooks(Long clientId) {
        getClient(clientId);
        return loans.findAllByClient_IdOrderByIdAsc(clientId).stream()
                .map(loan -> toBookResponse(loan.getBook()))
                .toList();
    }

    private void returnCopies(List<Loan> activeLoans) {
        Map<Long, Long> copiesByBookId = activeLoans.stream()
                .collect(Collectors.groupingBy(loan -> loan.getBook().getId(), Collectors.counting()));

        copiesByBookId.forEach((bookId, count) -> {
            Book book = books.findByIdForUpdate(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " was not found"));
            book.setQuantity(book.getQuantity() + Math.toIntExact(count));
        });
    }

    private Client getClient(Long id) {
        return clients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client with id " + id + " was not found"));
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(client.getId(), client.getName(), client.getEmail());
    }

    private BookResponse toBookResponse(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getPublicationYear(), book.getQuantity());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
