package com.example.tcc.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tcc.service.PaymentService;


@RestController
public class TestController {
    
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/test")
    public String postMethodName(@RequestParam String userId,
                                 @RequestParam String merchantId,
                                 @RequestParam BigDecimal amount) {
        
        try {
            paymentService.processPayment(userId, merchantId, amount);
            return "阁下的 Payment processed successfully";
        } catch (Exception e) {
            return "Error processing payment: " + e.getMessage();
        }
    }
    
    
}
