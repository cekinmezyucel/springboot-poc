package com.cekinmezyucel.springboot.poc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cekinmezyucel.springboot.poc.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {}
