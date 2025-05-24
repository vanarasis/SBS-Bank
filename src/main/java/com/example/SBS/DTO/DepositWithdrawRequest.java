// DepositWithdrawRequest.java
package com.example.SBS.DTO;

import java.math.BigDecimal;

public class DepositWithdrawRequest {
    private String accountNumber;
    private BigDecimal amount;


    // Getters and setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

}
