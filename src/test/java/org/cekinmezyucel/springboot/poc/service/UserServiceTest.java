package org.cekinmezyucel.springboot.poc.service;

import java.util.HashSet;
import java.util.Optional;

import org.cekinmezyucel.springboot.poc.BaseUnitTest;
import org.cekinmezyucel.springboot.poc.entity.AccountEntity;
import org.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.cekinmezyucel.springboot.poc.repository.AccountRepository;
import org.cekinmezyucel.springboot.poc.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
