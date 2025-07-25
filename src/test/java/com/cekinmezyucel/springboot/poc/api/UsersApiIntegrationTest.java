package com.cekinmezyucel.springboot.poc.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.model.Account;
import com.cekinmezyucel.springboot.poc.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class UsersApiIntegrationTest extends BaseIntegrationTest {
  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testCreateUser() throws Exception {
    User user = new User();
    user.setEmail("test@example.com");
    user.setName("Test");
    user.setSurname("User");
    String userJson = objectMapper.writeValueAsString(user);
    mockMvc
        .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(userJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("test@example.com"));
  }

  @Test
  void testGetUsers() throws Exception {
    mockMvc
        .perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

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
            .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(userJson))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode userNode = objectMapper.readTree(userResponse);
    int userId = userNode.get("id").asInt();
    String accountResponse =
        mockMvc
            .perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(accountJson))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode accountNode = objectMapper.readTree(accountResponse);
    int accountId = accountNode.get("id").asInt();
    // Link user to account
    mockMvc.perform(post("/users/" + userId + "/accounts/" + accountId)).andExpect(status().isOk());
    // Unlink user from account
    mockMvc
        .perform(delete("/users/" + userId + "/accounts/" + accountId))
        .andExpect(status().isOk());
  }
}
