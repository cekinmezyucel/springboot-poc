package com.cekinmezyucel.springboot.poc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cekinmezyucel.springboot.poc.entity.AccountEntity;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {}
