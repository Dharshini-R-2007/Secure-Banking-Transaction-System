package com.bank.simulator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class WithdrawalRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Withdrawal amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    public WithdrawalRequest() {
    }

    public WithdrawalRequest(BigDecimal amount, String description) {
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

    public static WithdrawalRequestBuilder builder() {
        return new WithdrawalRequestBuilder();
    }

    public static class WithdrawalRequestBuilder {
        private BigDecimal amount;
        private String description;

        public WithdrawalRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public WithdrawalRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public WithdrawalRequest build() {
            return new WithdrawalRequest(amount, description);
        }
    }
}
