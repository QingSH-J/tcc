package com.example.tcc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tcc.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    /**
     * 
     * @param ownerId
     * @return Wallet   
     */
    Wallet findByOwnerId(String ownerId);
}
