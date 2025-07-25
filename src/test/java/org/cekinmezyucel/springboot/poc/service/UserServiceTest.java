package org.cekinmezyucel.springboot.poc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import org.cekinmezyucel.springboot.poc.BaseUnitTest;
import org.cekinmezyucel.springboot.poc.entity.AccountEntity;
import org.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.cekinmezyucel.springboot.poc.repository.AccountRepository;
import org.cekinmezyucel.springboot.poc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class UserServiceTest extends BaseUnitTest {
  @Mock private UserRepository userRepository;
  @Mock private AccountRepository accountRepository;
  @InjectMocks private UserService userService;

  @Test
  void testLinkUserToAccountWithMembership() {
    UserEntity user = new UserEntity();
    user.setId(1L);
    user.setAccounts(new HashSet<>());
    AccountEntity account = new AccountEntity();
    account.setId(42L);
    account.setUsers(new HashSet<>());
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(accountRepository.findById(42L)).thenReturn(Optional.of(account));
    when(userRepository.save(user)).thenReturn(user);
    when(accountRepository.save(account)).thenReturn(account);
    userService.linkUserToAccountWithMembership(1L, 42L);
    assertTrue(user.getAccounts().contains(account));
    assertTrue(account.getUsers().contains(user));
  }

  @Test
  void testUnlinkUserFromAccountWithMembership() {
    UserEntity user = new UserEntity();
    user.setId(1L);
    AccountEntity account = new AccountEntity();
    account.setId(42L);
    user.setAccounts(new HashSet<>());
    user.getAccounts().add(account);
    account.setUsers(new HashSet<>());
    account.getUsers().add(user);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(accountRepository.findById(42L)).thenReturn(Optional.of(account));
    when(userRepository.save(user)).thenReturn(user);
    when(accountRepository.save(account)).thenReturn(account);
    userService.unlinkUserFromAccountWithMembership(1L, 42L);
    assertFalse(user.getAccounts().contains(account));
    assertFalse(account.getUsers().contains(user));
  }
}
