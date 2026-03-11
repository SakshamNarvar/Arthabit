package com.nstrange.expenseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstrange.expenseservice.dto.CreateExpenseRequestDto;
import com.nstrange.expenseservice.dto.ExpenseDto;
import com.nstrange.expenseservice.entities.Expense;
import com.nstrange.expenseservice.exception.ExpenseServiceException;
import com.nstrange.expenseservice.exception.InvalidExpenseRequestException;
import com.nstrange.expenseservice.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Service
public class ExpenseService
{

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenseRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ExpenseService(ExpenseRepository expenseRepository){
        this.expenseRepository = expenseRepository;
    }

    public Expense createExpense(CreateExpenseRequestDto requestDto, String userId){
        if (Objects.isNull(requestDto)) {
            throw new InvalidExpenseRequestException("Expense request body must not be null");
        }
        if (Objects.isNull(userId) || userId.isBlank()) {
            throw new InvalidExpenseRequestException("User ID must not be null or blank");
        }

        log.debug("Converting request DTO to Expense entity for userId={}", userId);
        Expense expense;
        try {
            expense = objectMapper.convertValue(requestDto, Expense.class);
        } catch (IllegalArgumentException ex) {
            log.error("Failed to convert CreateExpenseRequestDto to Expense for userId={}", userId, ex);
            throw new InvalidExpenseRequestException("Invalid expense data: " + ex.getMessage(), ex);
        }

        expense.setUserId(userId);

        if (Objects.isNull(expense.getCurrency())) {
            expense.setCurrency("inr");
        }

        if (Objects.isNull(expense.getCreatedAt())) {
            expense.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }

        try {
            Expense saved = expenseRepository.save(expense);
            log.info("Expense persisted successfully with id={} for userId={}", saved.getId(), userId);
            return saved;
        } catch (DataAccessException ex) {
            log.error("Database error while saving expense for userId={}", userId, ex);
            throw new ExpenseServiceException("Failed to persist expense for user " + userId, ex);
        }
    }

    public void createExpense(ExpenseDto expenseDto){
        if (Objects.isNull(expenseDto)) {
            throw new InvalidExpenseRequestException("ExpenseDto must not be null");
        }
        if (Objects.isNull(expenseDto.getUserId()) || expenseDto.getUserId().isBlank()) {
            throw new InvalidExpenseRequestException("User ID in ExpenseDto must not be null or blank");
        }

        log.debug("Creating expense from ExpenseDto for userId={}", expenseDto.getUserId());

        Expense expense = new Expense();
        expense.setUserId(expenseDto.getUserId());
        expense.setAmount(expenseDto.getAmount());
        expense.setMerchant(expenseDto.getMerchant());
        expense.setCurrency(Objects.nonNull(expenseDto.getCurrency()) ? expenseDto.getCurrency() : "inr");
        expense.setCreatedAt(Objects.nonNull(expenseDto.getCreatedAt()) ? expenseDto.getCreatedAt() : new Timestamp(System.currentTimeMillis()));

        try {
            expenseRepository.save(expense);
            log.info("Expense from Kafka event persisted successfully for userId={}", expenseDto.getUserId());
        } catch (DataAccessException ex) {
            log.error("Database error while saving expense from Kafka event for userId={}", expenseDto.getUserId(), ex);
            throw new ExpenseServiceException("Failed to persist expense from event for user " + expenseDto.getUserId(), ex);
        }
    }

//    public boolean updateExpense(ExpenseDto expenseDto){
//        setCurrency(expenseDto);
//        Optional<Expense> expenseFoundOpt = expenseRepository.findByUserIdAndExternalId(expenseDto.getUserId(), expenseDto.getExternalId());
//        if(expenseFoundOpt.isEmpty()){
//            return false;
//        }
//        Expense expense = expenseFoundOpt.get();
//        expense.setAmount(expenseDto.getAmount());
//        expense.setMerchant(Strings.isNotBlank(expenseDto.getMerchant())?expenseDto.getMerchant():expense.getMerchant());
//        expense.setCurrency(Strings.isNotBlank(expenseDto.getCurrency())?expenseDto.getMerchant():expense.getCurrency());
//        expenseRepository.save(expense);
//        return true;
//    }

    public List<Expense> getExpenses(String userId){
        if (Objects.isNull(userId) || userId.isBlank()) {
            throw new InvalidExpenseRequestException("User ID must not be null or blank");
        }

        log.debug("Fetching expenses for userId={}", userId);
        try {
            return expenseRepository.findByUserId(userId);
        } catch (DataAccessException ex) {
            log.error("Database error while fetching expenses for userId={}", userId, ex);
            throw new ExpenseServiceException("Failed to fetch expenses for user " + userId, ex);
        }
    }
}