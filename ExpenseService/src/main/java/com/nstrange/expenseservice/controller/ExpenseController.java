package com.nstrange.expenseservice.controller;

import com.nstrange.expenseservice.client.AuthClient;
import com.nstrange.expenseservice.dto.CreateExpenseRequestDto;
import com.nstrange.expenseservice.dto.ExpenseResponseDto;
import com.nstrange.expenseservice.entities.Expense;
import com.nstrange.expenseservice.service.ExpenseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expense/v1")
public class ExpenseController
{

    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);

    private final ExpenseService expenseService;
    private final AuthClient authClient;

    @Autowired
    ExpenseController(ExpenseService expenseService, AuthClient authClient){
        this.expenseService = expenseService;
        this.authClient = authClient;
    }

    @GetMapping(path = "/getExpense")
    public ResponseEntity<List<ExpenseResponseDto>> getExpense(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader){

        String userId = authClient.authenticateAndGetUserId(authorizationHeader);
        log.info("Fetching expenses for userId={}", userId);
        List<Expense> expenses = expenseService.getExpenses(userId);

        List<ExpenseResponseDto> response =
                expenses.stream()
                        .map(this::mapToDto)
                        .toList();

        log.info("Returning {} expenses for userId={}", response.size(), userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path="/addExpense")
    public ResponseEntity<ExpenseResponseDto> addExpenses(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody CreateExpenseRequestDto requestDto){

        String userId = authClient.authenticateAndGetUserId(authorizationHeader);
        log.info("Creating expense for userId={}, merchant={}", userId, requestDto.getMerchant());
        Expense createdExpense = expenseService.createExpense(requestDto, userId);

        log.info("Expense created successfully with externalId={} for userId={}",
                createdExpense.getExternalId(), userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToDto(createdExpense));
    }

    private ExpenseResponseDto mapToDto(Expense expense) {
        return ExpenseResponseDto.builder()
                .externalId(expense.getExternalId())
                .amount(expense.getAmount())
                .userId(expense.getUserId())
                .merchant(expense.getMerchant())
                .currency(expense.getCurrency())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}