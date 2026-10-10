package com.bank.simulator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Deposit amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    public DepositRequest() {
    }

    public DepositRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
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

    public static DepositRequestBuilder builder() {
        return new DepositRequestBuilder();
    }

    public static class DepositRequestBuilder {
        private BigDecimal amount;
        private String description;

        public DepositRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public DepositRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DepositRequest build() {
            return new DepositRequest(amount, description);
        }
    }
}
