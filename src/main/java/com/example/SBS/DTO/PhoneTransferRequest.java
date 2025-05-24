package com.example.SBS.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PhoneTransferRequest {
    public String getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public void setRecipientPhoneNumber(String recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    private String recipientPhoneNumber;
    private BigDecimal amount;
}
