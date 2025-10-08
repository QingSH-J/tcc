package com.example.tcc.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.tcc.anno.TccGlobalTransaction;

@Service
public class PaymentService {
    @Autowired
    private WalletService walletService;

    @TccGlobalTransaction
    public void processPayment(String userId, String merchantId, BigDecimal amount){
        //
        System.out.println("Processing payment of " + amount + " from user " + userId + " to merchant " + merchantId);
        walletService.tryDeduct(userId, amount);
        walletService.tryCredit(merchantId, amount);

        System.out.println("Payment processed successfully");
        System.out.println("wait Aop to commit");
    }
}
