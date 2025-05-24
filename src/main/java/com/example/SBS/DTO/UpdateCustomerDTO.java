// com.example.SBS.DTO.UpdateCustomerDTO.java

package com.example.SBS.DTO;

public class UpdateCustomerDTO {
    private String name;
    private String address;
    // Add other fields that can be updated (exclude phone/account number)

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
