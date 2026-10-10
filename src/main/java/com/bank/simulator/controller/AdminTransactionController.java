package com.bank.simulator.controller;

import com.bank.simulator.dto.DepositRequest;
import com.bank.simulator.dto.PageResponse;
import com.bank.simulator.dto.TransactionResponse;
import com.bank.simulator.dto.WithdrawalRequest;
import com.bank.simulator.service.AdminTransactionService;
import com.bank.simulator.service.TransactionQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;
    private final TransactionQueryService transactionQueryService;

    public AdminTransactionController(AdminTransactionService adminTransactionService, TransactionQueryService transactionQueryService) {
        this.adminTransactionService = adminTransactionService;
        this.transactionQueryService = transactionQueryService;
    }

    @PostMapping("/accounts/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            Authentication authentication,
            @PathVariable Long accountId,
            @Valid @RequestBody DepositRequest request
    ) {
        String adminUsername = authentication.getName();
        TransactionResponse response = adminTransactionService.processDeposit(accountId, request, adminUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/accounts/{accountId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            Authentication authentication,
            @PathVariable Long accountId,
            @Valid @RequestBody WithdrawalRequest request
    ) {
        String adminUsername = authentication.getName();
        TransactionResponse response = adminTransactionService.processWithdrawal(accountId, request, adminUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(transactionQueryService.getAdminTransactions(page, size));
    }

    @GetMapping("/transactions/{transactionReference}")
    public ResponseEntity<TransactionResponse> getTransactionByReference(@PathVariable String transactionReference) {
        return ResponseEntity.ok(transactionQueryService.getAdminTransactionByReference(transactionReference));
    }
}
