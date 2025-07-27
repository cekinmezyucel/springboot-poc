package com.cekinmezyucel.springboot.poc.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.model.Account;
import com.cekinmezyucel.springboot.poc.model.User;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsersApiIntegrationTest extends BaseIntegrationTest {
  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Nested
  class GetUsersTests {
    @Test
    @DisplayName("Should return 401 if there is no JWT provided")
    void testGetUsersWithoutJwt() throws Exception {
      mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 if JWT provided but no authorities provided")
    void testGetUsersWithoutAuthorities() throws Exception {
      mockMvc.perform(get("/users").with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 if JWT has wrong authorities")
    void testGetUsersWithWrongAuthorities() throws Exception {
      mockMvc
          .perform(
              get("/users")
                  .with(
                      jwt()
                          .authorities(
                              new SimpleGrantedAuthority(withAuthorityPrefix("poc.users.write")))))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 if JWT has correct authorities")
    void testGetUsersWithCorrectAuthorities() throws Exception {
      mockMvc
          .perform(
              get("/users")
                  .with(
                      jwt()
                          .authorities(
                              new SimpleGrantedAuthority(withAuthorityPrefix("poc.users.read")))))
          .andExpect(status().isOk());
    }
  }

  @Nested
  class CreateUserTests {
    @Test
    @DisplayName("Should return 401 if there is no JWT provided")
    void testCreateUsersWithoutJwt() throws Exception {
      User user = new User();
      user.setEmail("test@example.com");
      user.setName("Test");
      user.setSurname("User");
      String userJson = objectMapper.writeValueAsString(user);
      mockMvc
          .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(userJson))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 201 if JWT provided")
    void testCreateUsersWithJwt() throws Exception {
      User user = new User();
      user.setEmail("test@example.com");
      user.setName("Test");
      user.setSurname("User");
      String userJson = objectMapper.writeValueAsString(user);
      mockMvc
          .perform(
              post("/users").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(userJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.email").value("test@example.com"));
    }
  }

  @Nested
  class LinkUserToAccountTests {
    @Test
    void testLinkAndUnlinkUserToAccount() throws Exception {
      User user = new User();
      user.setEmail("link@example.com");
      user.setName("Link");
      user.setSurname("User");
      String userJson = objectMapper.writeValueAsString(user);
      Account account = new Account();
      account.setName("Account1");
      account.setIndustry("Tech");
      String accountJson = objectMapper.writeValueAsString(account);
      String userResponse =
          mockMvc
              .perform(
                  post("/users")
                      .with(jwt())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(userJson))
              .andReturn()
              .getResponse()
              .getContentAsString();
      JsonNode userNode = objectMapper.readTree(userResponse);
      int userId = userNode.get("id").asInt();
      String accountResponse =
          mockMvc
              .perform(
                  post("/accounts")
                      .with(jwt())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(accountJson))
              .andReturn()
              .getResponse()
              .getContentAsString();
      JsonNode accountNode = objectMapper.readTree(accountResponse);
      int accountId = accountNode.get("id").asInt();
      // Link user to account
      mockMvc
          .perform(post("/users/" + userId + "/accounts/" + accountId).with(jwt()))
          .andExpect(status().isOk());
      // Unlink a user from an account
      mockMvc
          .perform(delete("/users/" + userId + "/accounts/" + accountId).with(jwt()))
          .andExpect(status().isOk());
    }
  }
}
