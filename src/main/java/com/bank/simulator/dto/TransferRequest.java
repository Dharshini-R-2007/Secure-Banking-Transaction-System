package com.bank.simulator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Receiver account number is required")
    private String receiverAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    public TransferRequest() {
    }

    public TransferRequest(String receiverAccountNumber, BigDecimal amount, String description) {
        this.receiverAccountNumber = receiverAccountNumber;
        this.amount = amount;
        this.description = description;
    }

    public String getReceiverAccountNumber() {
        return receiverAccountNumber;
    }

    public void setReceiverAccountNumber(String receiverAccountNumber) {
        this.receiverAccountNumber = receiverAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static TransferRequestBuilder builder() {
        return new TransferRequestBuilder();
    }

    public static class TransferRequestBuilder {
        private String receiverAccountNumber;
        private BigDecimal amount;
        private String description;

        public TransferRequestBuilder receiverAccountNumber(String receiverAccountNumber) {
            this.receiverAccountNumber = receiverAccountNumber;
            return this;
        }

        public TransferRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public TransferRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TransferRequest build() {
            return new TransferRequest(receiverAccountNumber, amount, description);
        }
    }
}
