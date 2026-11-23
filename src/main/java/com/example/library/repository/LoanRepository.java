package com.example.library.repository;

import com.example.library.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findAllByOrderByIdAsc();

    List<Loan> findAllByClient_IdOrderByIdAsc(Long clientId);

    void deleteAllByClient_Id(Long clientId);

    void deleteAllByBook_Id(Long bookId);

    long countByClient_Id(Long clientId);
}
