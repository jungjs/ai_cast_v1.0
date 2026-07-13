package com.aicast.common.exception;

public class AzureServiceException extends RuntimeException {
    
    private final String serviceType;

    public AzureServiceException(String serviceType, String message) {
        super(message);
        this.serviceType = serviceType;
    }

    public AzureServiceException(String serviceType, String message, Throwable cause) {
        super(message, cause);
        this.serviceType = serviceType;
    }

    public String getServiceType() {
        return serviceType;
    }
}
