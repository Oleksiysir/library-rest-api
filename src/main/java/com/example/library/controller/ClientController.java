package com.example.library.controller;

import com.example.library.dto.BookResponse;
import com.example.library.dto.ClientRequest;
import com.example.library.dto.ClientResponse;
import com.example.library.service.ClientService;
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
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public CollectionModel<EntityModel<ClientResponse>> findAll() {
        List<EntityModel<ClientResponse>> clients = service.findAll().stream()
                .map(this::toModel)
                .toList();
        return CollectionModel.of(clients,
                linkTo(methodOn(ClientController.class).findAll()).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<ClientResponse> findById(@PathVariable Long id) {
        return toModel(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<EntityModel<ClientResponse>> create(@Valid @RequestBody ClientRequest request) {
        ClientResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(toModel(created));
    }

    @PutMapping("/{id}")
    public EntityModel<ClientResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request
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

    @GetMapping("/{id}/books")
    public CollectionModel<EntityModel<BookResponse>> findBooks(@PathVariable Long id) {
        List<EntityModel<BookResponse>> books = service.findBooks(id).stream()
                .map(book -> EntityModel.of(book,
                        linkTo(methodOn(BookController.class).findById(book.getId())).withSelfRel()))
                .toList();
        return CollectionModel.of(books,
                linkTo(methodOn(ClientController.class).findBooks(id)).withSelfRel(),
                linkTo(methodOn(ClientController.class).findById(id)).withRel("client"));
    }

    private EntityModel<ClientResponse> toModel(ClientResponse client) {
        return EntityModel.of(client,
                linkTo(methodOn(ClientController.class).findById(client.getId())).withSelfRel(),
                linkTo(methodOn(ClientController.class).findBooks(client.getId())).withRel("books"),
                linkTo(methodOn(ClientController.class).findAll()).withRel("clients"));
    }
}
