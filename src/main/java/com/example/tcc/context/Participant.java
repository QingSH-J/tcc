package com.example.tcc.context;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Participant implements Serializable {
    private String beanName;
    private String confirmMethod;
    private String cancelMethod;
    
    // Add type information to preserve types during serialization/deserialization
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private Object[] args;
    
    // Store parameter types explicitly
    private Class<?>[] parameterTypes;
    
    // Constructor that captures parameter types
    public Participant(String beanName, String confirmMethod, String cancelMethod, Object[] args) {
        this.beanName = beanName;
        this.confirmMethod = confirmMethod;
        this.cancelMethod = cancelMethod;
        this.args = args;
        
        // Capture parameter types
        if (args != null) {
            this.parameterTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    this.parameterTypes[i] = args[i].getClass();
                }
            }
        }
    }
}