package org.cekinmezyucel.springboot.poc.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void usersEndpointShouldReturn200AndList() {
        ResponseEntity<String> response = restTemplate.getForEntity("/users", String.class);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        Assertions.assertNotNull(body);
    }
}
