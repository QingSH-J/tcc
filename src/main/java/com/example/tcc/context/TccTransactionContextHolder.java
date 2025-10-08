package com.example.tcc.context;

public class TccTransactionContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setContext(String context) {
        CONTEXT.set(context);
    }

    public static String getContext() {
        return CONTEXT.get();
    }

    public static void clearContext() {
        CONTEXT.remove();
    }
}
