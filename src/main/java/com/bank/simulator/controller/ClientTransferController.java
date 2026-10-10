package com.bank.simulator.controller;

import com.bank.simulator.dto.PageResponse;
import com.bank.simulator.dto.TransactionResponse;
import com.bank.simulator.dto.TransferRequest;
import com.bank.simulator.service.TransactionQueryService;
import com.bank.simulator.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client")
public class ClientTransferController {

    private final TransferService transferService;
    private final TransactionQueryService transactionQueryService;

    public ClientTransferController(TransferService transferService, TransactionQueryService transactionQueryService) {
        this.transferService = transferService;
        this.transactionQueryService = transactionQueryService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transferMoney(
            Authentication authentication,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String username = authentication.getName();
        TransactionResponse response = transferService.processTransfer(username, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        String username = authentication.getName();
        PageResponse<TransactionResponse> response = transactionQueryService.getClientTransactions(username, page, size, type, status);
        return ResponseEntity.ok(response);
    }
}
