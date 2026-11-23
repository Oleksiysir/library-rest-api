package com.example.library.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class LoanRequest {
    @NotNull(message = "clientId is required")
    @Positive(message = "clientId must be positive")
    private Long clientId;

    @NotNull(message = "bookId is required")
    @Positive(message = "bookId must be positive")
    private Long bookId;

    public LoanRequest() {
    }

    public LoanRequest(Long clientId, Long bookId) {
        this.clientId = clientId;
        this.bookId = bookId;
    }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
}
