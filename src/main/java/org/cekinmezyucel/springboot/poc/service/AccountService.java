package org.cekinmezyucel.springboot.poc.service;

import java.util.Arrays;
import java.util.List;

import org.cekinmezyucel.springboot.poc.model.Account;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    public List<Account> getAccounts() {
        return  Arrays.asList(
            new Account().id(1).name("Acme Corp").industry("Technology"),
            new Account().id(2).name("Beta LLC").industry("Finance")
        );
    }

    public Account createAccount(Account account) {
        account.setId(100);
        return account;
    }
}
