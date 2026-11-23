package com.example.library.dto;

import java.time.Instant;

public class LoanResponse {
    private Long id;
    private Long clientId;
    private Long bookId;
    private Instant assignedAt;

    public LoanResponse() {
    }

    public LoanResponse(Long id, Long clientId, Long bookId, Instant assignedAt) {
        this.id = id;
        this.clientId = clientId;
        this.bookId = bookId;
        this.assignedAt = assignedAt;
    }

    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public Long getBookId() { return bookId; }
    public Instant getAssignedAt() { return assignedAt; }
}
