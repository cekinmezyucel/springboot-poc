package org.cekinmezyucel.springboot.poc.api;

import org.cekinmezyucel.springboot.poc.model.Account;
import org.cekinmezyucel.springboot.poc.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AccountsApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testCreateAccount() throws Exception {
        Account account = new Account();
        account.setName("AccountTest");
        account.setIndustry("Finance");
        String accountJson = objectMapper.writeValueAsString(account);
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("AccountTest"));
    }

    @Test
    void testGetAccounts() throws Exception {
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testLinkAndUnlinkAccountToUser() throws Exception {
        User user = new User();
        user.setEmail("accountuser@example.com");
        user.setName("AccountUser");
        user.setSurname("Test");
        String userJson = objectMapper.writeValueAsString(user);
        Account account = new Account();
        account.setName("Account2");
        account.setIndustry("Tech");
        String accountJson = objectMapper.writeValueAsString(account);
        String userResponse = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andReturn().getResponse().getContentAsString();
        JsonNode userNode = objectMapper.readTree(userResponse);
        int userId = userNode.get("id").asInt();
        String accountResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson))
                .andReturn().getResponse().getContentAsString();
        JsonNode accountNode = objectMapper.readTree(accountResponse);
        int accountId = accountNode.get("id").asInt();
        // Link account to user
        mockMvc.perform(post("/accounts/" + accountId + "/users/" + userId))
                .andExpect(status().isOk());
        // Unlink account from user
        mockMvc.perform(delete("/accounts/" + accountId + "/users/" + userId))
                .andExpect(status().isOk());
    }
}
