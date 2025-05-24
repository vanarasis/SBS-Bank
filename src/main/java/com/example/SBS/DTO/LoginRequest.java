// DTO/LoginRequest.java
package com.example.SBS.DTO;

public class LoginRequest {
    private String phoneNumber;
    private String password;

    // Getters and setters
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
