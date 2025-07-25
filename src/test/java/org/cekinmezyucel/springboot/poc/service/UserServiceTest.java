package org.cekinmezyucel.springboot.poc.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.cekinmezyucel.springboot.poc.BaseUnitTest;
import org.cekinmezyucel.springboot.poc.entity.AccountEntity;
import org.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.cekinmezyucel.springboot.poc.repository.AccountRepository;
import org.cekinmezyucel.springboot.poc.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;

class UserServiceTest extends BaseUnitTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private UserService userService;

    @Test
    void testCreateUser() {
        UserEntity user = new UserEntity();
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setSurname("User");
        UserEntity savedUser = new UserEntity();
        savedUser.setId(1);
        savedUser.setEmail("test@example.com");
        savedUser.setName("Test");
        savedUser.setSurname("User");
        savedUser.setAccounts(new HashSet<>());
        when(userRepository.save(user)).thenReturn(savedUser);
        UserEntity created = userService.createUser(user);
        assertNotNull(created.getId());
        assertEquals("test@example.com", created.getEmail());
        assertEquals("Test", created.getName());
        assertEquals("User", created.getSurname());
        assertNotNull(created.getAccounts());
    }

    @Test
    void testLinkUserToAccountWithMembership() {
        UserEntity user = new UserEntity();
        user.setId(1);
        user.setAccounts(new HashSet<>());
        AccountEntity account = new AccountEntity();
        account.setId(42);
        account.setUsers(new HashSet<>());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(42)).thenReturn(Optional.of(account));
        when(userRepository.save(user)).thenReturn(user);
        when(accountRepository.save(account)).thenReturn(account);
        userService.linkUserToAccountWithMembership(1, 42);
        assertTrue(user.getAccounts().contains(account));
        assertTrue(account.getUsers().contains(user));
    }

    @Test
    void testUnlinkUserFromAccountWithMembership() {
        UserEntity user = new UserEntity();
        user.setId(1);
        AccountEntity account = new AccountEntity();
        account.setId(42);
        user.setAccounts(new HashSet<>());
        user.getAccounts().add(account);
        account.setUsers(new HashSet<>());
        account.getUsers().add(user);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(42)).thenReturn(Optional.of(account));
        when(userRepository.save(user)).thenReturn(user);
        when(accountRepository.save(account)).thenReturn(account);
        userService.unlinkUserFromAccountWithMembership(1, 42);
        assertFalse(user.getAccounts().contains(account));
        assertFalse(account.getUsers().contains(user));
    }

    @Test
    void testCreateUserWithMemberships() {
        UserEntity user = new UserEntity();
        user.setId(1);
        user.setAccounts(new HashSet<>());
        AccountEntity account1 = new AccountEntity();
        account1.setId(2);
        account1.setUsers(new HashSet<>());
        AccountEntity account2 = new AccountEntity();
        account2.setId(3);
        account2.setUsers(new HashSet<>());
        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(2)).thenReturn(Optional.of(account1));
        when(accountRepository.findById(3)).thenReturn(Optional.of(account2));
        when(accountRepository.save(account1)).thenReturn(account1);
        when(accountRepository.save(account2)).thenReturn(account2);
        List<Integer> accountIds = List.of(2, 3);
        UserEntity created = userService.createUserWithMemberships(user, accountIds);
        assertTrue(created.getAccounts().contains(account1));
        assertTrue(created.getAccounts().contains(account2));
    }
}
