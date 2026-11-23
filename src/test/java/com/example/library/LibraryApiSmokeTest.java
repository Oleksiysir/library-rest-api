package com.example.library;

import com.example.library.repository.BookRepository;
import com.example.library.repository.ClientRepository;
import com.example.library.repository.LoanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ClientRepository clientRepository;

    @BeforeEach
    void cleanDatabase() {
        loanRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        clientRepository.deleteAllInBatch();
    }

    @Test
    void canCreateClientAndGetItWithHateoasLinks() throws Exception {
        String response = mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test Client",
                                  "email": "test-client@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = readId(response);

        mockMvc.perform(get("/api/v1/clients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Client"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.books.href").exists());
    }

    @Test
    void rejectsInvalidClientAndBookDtoValues() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Clean Code",
                                  "author": "Robert C. Martin",
                                  "isbn": "9780132350884",
                                  "publicationYear": 2008,
                                  "quantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());
    }

    @Test
    void assigningAndReturningBookChangesAvailableQuantity() throws Exception {
        long clientId = createClient("reader@example.com");
        long bookId = createBook("9780132350884", 1);

        String loanResponse = mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "bookId": %d
                                }
                                """.formatted(clientId, bookId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.bookId").value(bookId))
                .andReturn().getResponse().getContentAsString();
        long loanId = readId(loanResponse);

        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0));

        mockMvc.perform(delete("/api/v1/loans/{id}", loanId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1));
    }

    @Test
    void cannotIssueMoreCopiesThanAvailable() throws Exception {
        long clientId = createClient("another-reader@example.com");
        long bookId = createBook("9780134685991", 1);

        String request = """
                {
                  "clientId": %d,
                  "bookId": %d
                }
                """.formatted(clientId, bookId);

        mockMvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
    }

    private long createClient(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test Reader",
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readId(response);
    }

    private long createBook(String isbn, int quantity) throws Exception {
        String response = mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test Book",
                                  "author": "Test Author",
                                  "isbn": "%s",
                                  "publicationYear": 2020,
                                  "quantity": %d
                                }
                                """.formatted(isbn, quantity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readId(response);
    }

    private long readId(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        return root.get("id").asLong();
    }
}
