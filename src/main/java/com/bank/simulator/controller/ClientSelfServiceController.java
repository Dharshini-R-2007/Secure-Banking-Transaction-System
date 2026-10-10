package com.bank.simulator.controller;

import com.bank.simulator.dto.AccountBalanceResponse;
import com.bank.simulator.dto.AccountResponse;
import com.bank.simulator.dto.ClientProfileResponse;
import com.bank.simulator.service.ClientSelfService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client")
public class ClientSelfServiceController {

    private final ClientSelfService clientSelfService;

    public ClientSelfServiceController(ClientSelfService clientSelfService) {
        this.clientSelfService = clientSelfService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ClientProfileResponse> getProfile(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(clientSelfService.getClientProfile(username));
    }

    @GetMapping("/account")
    public ResponseEntity<AccountResponse> getAccount(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(clientSelfService.getClientAccount(username));
    }

    @GetMapping("/account/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(clientSelfService.getClientBalance(username));
    }
}
