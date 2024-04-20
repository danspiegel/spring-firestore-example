package br.com.firestore.example.exception;

public class BusinessException extends RuntimeException {

    public BusinessException(String msg){
        super(msg);
    }

}
