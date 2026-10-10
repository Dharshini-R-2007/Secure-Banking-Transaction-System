package com.bank.simulator.controller;

import com.bank.simulator.dto.ClientResponse;
import com.bank.simulator.dto.CreateClientRequest;
import com.bank.simulator.service.AdminClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/clients")
public class AdminClientController {

    private final AdminClientService adminClientService;

    public AdminClientController(AdminClientService adminClientService) {
        this.adminClientService = adminClientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse response = adminClientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        return ResponseEntity.ok(adminClientService.getAllClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(adminClientService.getClientById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ClientResponse>> searchClients(@RequestParam String username) {
        return ResponseEntity.ok(adminClientService.searchClientsByUsername(username));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<ClientResponse> blockClient(@PathVariable Long id) {
        return ResponseEntity.ok(adminClientService.blockClient(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ClientResponse> activateClient(@PathVariable Long id) {
        return ResponseEntity.ok(adminClientService.activateClient(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ClientResponse> deactivateClient(@PathVariable Long id) {
        return ResponseEntity.ok(adminClientService.deactivateClient(id));
    }
}
