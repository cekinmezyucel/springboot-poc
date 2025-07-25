package org.cekinmezyucel.springboot.poc.service;

import java.util.List;

import org.cekinmezyucel.springboot.poc.model.User;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private AccountService accountService;

    private UserService userService;


    @BeforeEach
    void setUp() {
        userService = new UserService(accountService);
    }

    @Test
    void testCreateUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setSurname("User");
        User created = userService.createUser(user);
        assertNotNull(created.getId());
        assertEquals("test@example.com", created.getEmail());
        assertEquals("Test", created.getName());
        assertEquals("User", created.getSurname());
        assertNotNull(created.getAccountIds());
    }

    @Test
    void testLinkUserToAccountWithMembership() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setSurname("User");
        User created = userService.createUser(user);
        Integer userId = created.getId();
        assertNotNull(userId);
        int accountId = 42;
        userService.linkUserToAccountWithMembership(userId, accountId);
        assertTrue(created.getAccountIds().contains(accountId));
        verify(accountService, times(1)).linkAccountToUser(accountId, userId);
    }

    @Test
    void testUnlinkUserFromAccountWithMembership() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setSurname("User");
        User created = userService.createUser(user);
        Integer userId = created.getId();
        assertNotNull(userId);
        int accountId = 42;
        userService.linkUserToAccountWithMembership(userId, accountId);
        userService.unlinkUserFromAccountWithMembership(userId, accountId);
        assertFalse(created.getAccountIds().contains(accountId));
        verify(accountService, times(1)).unlinkAccountFromUser(accountId, userId);
    }

    @Test
    void testCreateUserWithMemberships() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setSurname("User");
        user.setAccountIds(List.of(1, 2));
        User created = userService.createUserWithMemberships(user);
        assertTrue(created.getAccountIds().contains(1));
        assertTrue(created.getAccountIds().contains(2));
        Integer userId = created.getId();
        assertNotNull(userId);
        verify(accountService, times(1)).linkAccountToUser(1, userId);
        verify(accountService, times(1)).linkAccountToUser(2, userId);
    }
}
