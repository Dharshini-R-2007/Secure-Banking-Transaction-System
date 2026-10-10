package com.bank.simulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateClientRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    public CreateClientRequest() {
    }

    public CreateClientRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static CreateClientRequestBuilder builder() {
        return new CreateClientRequestBuilder();
    }

    public static class CreateClientRequestBuilder {
        private String username;
        private String password;

        public CreateClientRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public CreateClientRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public CreateClientRequest build() {
            return new CreateClientRequest(username, password);
        }
    }
}
