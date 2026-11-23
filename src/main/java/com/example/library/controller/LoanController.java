package com.example.library.controller;

import com.example.library.dto.LoanRequest;
import com.example.library.dto.LoanResponse;
import com.example.library.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {
    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @GetMapping
    public CollectionModel<EntityModel<LoanResponse>> findAll() {
        List<EntityModel<LoanResponse>> loans = service.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(loans,
                linkTo(methodOn(LoanController.class).findAll()).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<LoanResponse> findById(@PathVariable Long id) {
        return toModel(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<EntityModel<LoanResponse>> assign(@Valid @RequestBody LoanRequest request) {
        LoanResponse created = service.assign(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(toModel(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> returnBook(@PathVariable Long id) {
        service.returnBook(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> returnAllAndDeleteLoans() {
        service.deleteAllAndReturnBooks();
        return ResponseEntity.noContent().build();
    }

    private EntityModel<LoanResponse> toModel(LoanResponse loan) {
        return EntityModel.of(loan,
                linkTo(methodOn(LoanController.class).findById(loan.getId())).withSelfRel(),
                linkTo(methodOn(LoanController.class).findAll()).withRel("loans"),
                linkTo(methodOn(ClientController.class).findById(loan.getClientId())).withRel("client"),
                linkTo(methodOn(BookController.class).findById(loan.getBookId())).withRel("book"));
    }
}
