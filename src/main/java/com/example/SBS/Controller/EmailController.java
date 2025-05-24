package com.example.SBS.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/customers")
public class EmailController {

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/email-transaction-history")
    public Response emailTransactionHistory(@RequestBody EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getEmail());
            helper.setSubject("Transaction History");
            helper.setText("Please find your transaction history attached for the period from "
                    + request.getFromDate() + " to " + request.getToDate() + ".");

            // Attach CSV content
            byte[] csvBytes = request.getCsvContent().getBytes(StandardCharsets.UTF_8);
            InputStreamSource csvResource = new ByteArrayResource(csvBytes);
            helper.addAttachment("transaction_history.csv", csvResource, "text/csv");

            mailSender.send(message);
            return new Response("Transaction history emailed successfully!");
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    static class EmailRequest {
        private String accountNumber;
        private String email;
        private String csvContent;
        private String fromDate;
        private String toDate;

        // Getters and setters
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCsvContent() { return csvContent; }
        public void setCsvContent(String csvContent) { this.csvContent = csvContent; }
        public String getFromDate() { return fromDate; }
        public void setFromDate(String fromDate) { this.fromDate = fromDate; }
        public String getToDate() { return toDate; }
        public void setToDate(String toDate) { this.toDate = toDate; }
    }

    static class Response {
        private String message;

        public Response(String message) { this.message = message; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}