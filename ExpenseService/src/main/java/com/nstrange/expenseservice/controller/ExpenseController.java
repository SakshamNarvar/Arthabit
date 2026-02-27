package com.nstrange.expenseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstrange.expenseservice.dto.CreateExpenseRequestDto;
import com.nstrange.expenseservice.dto.ExpenseResponseDto;
import com.nstrange.expenseservice.entities.Expense;
import com.nstrange.expenseservice.service.ExpenseService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expense/v1")
public class ExpenseController
{

    private final ExpenseService expenseService;
    private final ObjectMapper objectMapper;

    @Autowired
    ExpenseController(ExpenseService expenseService, ObjectMapper objectMapper){
        this.expenseService = expenseService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(path = "/getExpense")
    public ResponseEntity<List<ExpenseResponseDto>> getExpense(
            @RequestHeader("X-User-Id") String userId){

        List<Expense> expenses = expenseService.getExpenses(userId);

        List<ExpenseResponseDto> response =
                expenses.stream()
                        .map(this::mapToDto)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping(path="/addExpense")
    public ResponseEntity<ExpenseResponseDto> addExpenses(
            @RequestHeader(value = "X-User-Id") @NonNull String userId,
            @RequestBody CreateExpenseRequestDto requestDto){
        Expense createdExpense = expenseService.createExpense(requestDto, userId);

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