package com.alvinskylers.tablesync.exception;

public class TableNumberExistsException extends RuntimeException {
    public TableNumberExistsException(String message) {
        super(message);
    }
}
