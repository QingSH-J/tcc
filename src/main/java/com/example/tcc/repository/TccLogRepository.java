package com.example.tcc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tcc.entity.TccLog;

public interface TccLogRepository extends JpaRepository<TccLog, String> {
}
