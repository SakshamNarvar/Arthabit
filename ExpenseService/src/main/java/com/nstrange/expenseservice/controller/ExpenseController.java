package com.nstrange.expenseservice.controller;

import com.nstrange.expenseservice.dto.CreateExpenseRequestDto;
import com.nstrange.expenseservice.dto.ExpenseDto;
import com.nstrange.expenseservice.dto.UpdateExpenseDto;
import com.nstrange.expenseservice.entities.Expense;
import com.nstrange.expenseservice.service.ExpenseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    ExpenseController(ExpenseService expenseService){
        this.expenseService = expenseService;
    }

    @GetMapping(path = "/getExpense")
    public ResponseEntity<List<ExpenseDto>> getExpense(
            @RequestHeader("X-User-ID") String userId){

        log.info("Fetching expenses for userId={}", userId);
        List<Expense> expenses = expenseService.getExpenses(userId);

        List<ExpenseDto> response =
                expenses.stream()
                        .map(this::mapToDto)
                        .toList();

        log.info("Returning {} expenses for userId={}", response.size(), userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path="/addExpense")
    public ResponseEntity<ExpenseDto> addExpense(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateExpenseRequestDto requestDto){

        log.info("Creating expense for userId={}, merchant={}", userId, requestDto.getMerchant());
        Expense createdExpense = expenseService.createExpense(requestDto, userId);

        log.info("Expense created successfully with externalId={} for userId={}",
                createdExpense.getExternalId(), userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToDto(createdExpense));
    }

    @PostMapping(path="/updateExpense")
    public ResponseEntity<ExpenseDto> updateExpense(
            @RequestHeader("X-External-ID") String expenseId,
            @Valid @RequestBody UpdateExpenseDto requestDto
            ) {
        log.info("Updating expense with expenseId={}", expenseId);
        Expense updatedExpense = expenseService.updateExpense(requestDto, expenseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mapToDto(updatedExpense));
    }

    private ExpenseDto mapToDto(Expense expense) {
        return ExpenseDto.builder()
                .externalId(expense.getExternalId())
                .amount(expense.getAmount())
                .userId(expense.getUserId())
                .merchant(expense.getMerchant())
                .currency(expense.getCurrency())
                .createdAt(expense.getCreatedAt())
                .fundSource(expense.getFundSource())
                .category(expense.getCategory())
                .notes(expense.getNotes())
                .build();
    }
}