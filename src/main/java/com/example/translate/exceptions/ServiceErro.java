package com.example.translate.exceptions;

public class ServiceErro extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public ServiceErro(String message){
        super(message);
    }

    public ServiceErro(String message, Throwable t){
        super(message, t);
    }
}
