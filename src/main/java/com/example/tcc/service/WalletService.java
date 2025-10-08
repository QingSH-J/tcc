package com.example.tcc.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.tcc.anno.TccAction;
import com.example.tcc.entity.Wallet;
import com.example.tcc.repository.WalletRepository;


@Service("walletService")
public class WalletService {
    
    @Autowired
    private WalletRepository walletRepository;

    @TccAction(confirmMethod = "confirmDeduct", cancelMethod = "cancelDeduct")
    @Transactional
    public void tryDeduct(String userId,BigDecimal amount){
        System.out.println("Trying to deduct " + amount + " from user " + userId);
        Wallet wallet = walletRepository.findByOwnerId(userId);
        if(wallet.getBalance().compareTo(amount) < 0){
            throw new RuntimeException("Insufficient balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(amount));
        walletRepository.save(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmDeduct(String userId,BigDecimal amount){
        System.out.println("Confirming deduct " + amount + " from user " + userId);
        Wallet wallet = walletRepository.findByOwnerId(userId);
        //check
        if(wallet.getFrozenBalance().compareTo(amount) >= 0){
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
            walletRepository.save(wallet);
        } else {
            throw new RuntimeException("No frozen balance to confirm");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelDeduct(String userId,BigDecimal amount){
        System.out.println("Cancelling deduct " + amount + " from user " + userId);
        Wallet wallet = walletRepository.findByOwnerId(userId);
        if(wallet.getFrozenBalance().compareTo(amount) >= 0){
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
            wallet.setBalance(wallet.getBalance().add(amount));
            walletRepository.save(wallet);
        } else {
            throw new RuntimeException("No frozen balance to cancel");
        }
    }


    @TccAction(confirmMethod = "confirmCredit", cancelMethod = "cancelCredit")
    @Transactional
    public void tryCredit(String userId, BigDecimal amount){
        System.out.println("Trying to credit " + amount + " to user " + userId);
        Wallet wallet = walletRepository.findByOwnerId(userId);
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(amount));
        walletRepository.save(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmCredit(String userId, BigDecimal amount){
        System.out.println("Confirming credit " + amount + " to user " + userId);
        Wallet wallet = walletRepository.findByOwnerId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelCredit(String userId, BigDecimal amount){
        System.out.println("Cancelling credit " + amount + " to user " + userId);
        Wallet wallet = walletRepository.findByOwnerId(userId);
        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
        walletRepository.save(wallet);
    }
}
