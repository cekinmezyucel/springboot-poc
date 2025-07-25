package org.cekinmezyucel.springboot.poc.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cekinmezyucel.springboot.poc.model.Account;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final Map<Integer, Account> accounts = new HashMap<>();
    private int accountIdSeq = 1;

    public List<Account> getAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public Account createAccount(Account account) {
        account.setId(accountIdSeq++);
        accounts.put(account.getId(), account);
        return account;
    }

    public void linkAccountToUser(int accountId, int userId) {
        Account account = accounts.get(accountId);
        if (account != null && !account.getUserIds().contains(userId)) {
            account.getUserIds().add(userId);
        }
    }

    public void unlinkAccountFromUser(int accountId, int userId) {
        Account account = accounts.get(accountId);
        if (account != null) {
            account.getUserIds().remove(Integer.valueOf(userId));
        }
    }
}
