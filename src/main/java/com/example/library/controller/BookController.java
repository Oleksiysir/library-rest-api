package com.example.library.controller;

import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;
import com.example.library.service.BookService;
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
@RequestMapping("/api/v1/books")
public class BookController {
    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public CollectionModel<EntityModel<BookResponse>> findAll() {
        List<EntityModel<BookResponse>> books = service.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(books,
                linkTo(methodOn(BookController.class).findAll()).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<BookResponse> findById(@PathVariable Long id) {
        return toModel(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<EntityModel<BookResponse>> create(@Valid @RequestBody BookRequest request) {
        BookResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(toModel(created));
    }

    @PutMapping("/{id}")
    public EntityModel<BookResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request
    ) {
        return toModel(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        service.deleteAll();
        return ResponseEntity.noContent().build();
    }

    private EntityModel<BookResponse> toModel(BookResponse book) {
        return EntityModel.of(book,
                linkTo(methodOn(BookController.class).findById(book.getId())).withSelfRel(),
                linkTo(methodOn(BookController.class).findAll()).withRel("books"));
    }
}
