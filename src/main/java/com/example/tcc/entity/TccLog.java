package com.example.tcc.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class TccLog {
    
    @Id
    private String txId;

    private String status;

    private LocalDateTime createdTime;
}