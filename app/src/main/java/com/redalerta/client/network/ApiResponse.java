package com.redalerta.client.network;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    // getters

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
