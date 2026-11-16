package com.identity.app.api.dto;

public record ResponseWrapperDTO<T>(int statusCode, String message, T data) { }
