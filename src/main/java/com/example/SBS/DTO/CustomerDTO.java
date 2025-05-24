package com.example.SBS.DTO;

import java.math.BigDecimal;

public class CustomerDTO {

    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private String bankAccountNumber;
   // private String bankName;
    private String ifscCode;
    private String accountType;  // SAVINGS, CHECKING
    private String passwordHash;
    private BigDecimal balance;  // Add balance field

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

 /*   public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

  */

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public BigDecimal getBalance() { return balance; } // Getter for balance
    public void setBalance(BigDecimal balance) { this.balance = balance; }  // Setter for balance
}
