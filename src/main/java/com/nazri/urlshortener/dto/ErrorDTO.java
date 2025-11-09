package com.nazri.urlshortener.dto;

import java.util.Objects;

/**
 * Data Transfer Object for standardized error responses.
 * Provides a consistent structure for error messages across the application.
 */
public class ErrorDTO {
    private String error;
    private String details;
    private int statusCode;
    
    public ErrorDTO() {
    }
    
    public ErrorDTO(String error, String details, int statusCode) {
        this.error = error;
        this.details = details;
        this.statusCode = statusCode;
    }
    
    public ErrorDTO(String error, int statusCode) {
        this.error = error;
        this.statusCode = statusCode;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorDTO errorDTO = (ErrorDTO) o;
        return statusCode == errorDTO.statusCode &&
                Objects.equals(error, errorDTO.error) &&
                Objects.equals(details, errorDTO.details);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(error, details, statusCode);
    }
    
    @Override
    public String toString() {
        return "ErrorDTO{" +
                "error='" + error + '\'' +
                ", details='" + details + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
