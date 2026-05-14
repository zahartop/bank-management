package com.bank.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.management.dto.request.CardCreateRequest;
import com.bank.management.dto.request.LoginRequest;
import com.bank.management.dto.response.CardResponse;
import com.bank.management.dto.response.TokenResponse;
import com.bank.management.entity.Role;
import com.bank.management.entity.User;
import com.bank.management.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class CardLifecycleIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bank_management")
            .withUsername("bank")
            .withPassword("bank");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
    }

    @Test
    void createCard_encryptsPanInDb_andApiReturnsMaskedPan() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user.setRole(Role.USER);
        userRepository.save(user);

        ResponseEntity<TokenResponse> login = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest("alice", "secret123"),
                TokenResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        String jwt = login.getBody().accessToken();

        LocalDate expiry = LocalDate.now(ZoneOffset.UTC).plusYears(1);
        CardCreateRequest createRequest = new CardCreateRequest("4111111111111111", expiry, BigDecimal.ZERO, null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwt);
        HttpEntity<CardCreateRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<CardResponse> created = restTemplate.postForEntity(
                "/api/v1/cards",
                createEntity,
                CardResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().maskedPan()).isEqualTo("4111 **** **** 1111");

        Long cardId = created.getBody().id();

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<CardResponse> fetched = restTemplate.exchange(
                "/api/v1/cards/" + cardId,
                HttpMethod.GET,
                getEntity,
                CardResponse.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().maskedPan()).isEqualTo("4111 **** **** 1111");

        String panCipher = jdbcTemplate.queryForObject(
                "select pan_cipher from cards where id = ?",
                String.class,
                cardId);
        assertThat(panCipher).doesNotContain("4111111111111111");
        assertThat(panCipher).isNotBlank();
    }
}
