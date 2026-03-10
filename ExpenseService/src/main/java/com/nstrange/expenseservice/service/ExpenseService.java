package com.nstrange.expenseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstrange.expenseservice.dto.CreateExpenseRequestDto;
import com.nstrange.expenseservice.dto.ExpenseDto;
import com.nstrange.expenseservice.entities.Expense;
import com.nstrange.expenseservice.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Service
public class ExpenseService
{

    private final ExpenseRepository expenseRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ExpenseService(ExpenseRepository expenseRepository){
        this.expenseRepository = expenseRepository;
    }

    public Expense createExpense(CreateExpenseRequestDto requestDto, String userId){
        Expense expense = objectMapper.convertValue(requestDto, Expense.class);

        expense.setUserId(userId);

        if (Objects.isNull(expense.getCurrency())) {
            expense.setCurrency("inr");
        }

        if (Objects.isNull(expense.getCreatedAt())) {
            expense.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }

        return expenseRepository.save(expense);
    }

    public void createExpense(ExpenseDto expenseDto){
        Expense expense = new Expense();
        expense.setUserId(expenseDto.getUserId());
        expense.setAmount(expenseDto.getAmount());
        expense.setMerchant(expenseDto.getMerchant());
        expense.setCurrency(Objects.nonNull(expenseDto.getCurrency()) ? expenseDto.getCurrency() : "inr");
        expense.setCreatedAt(Objects.nonNull(expenseDto.getCreatedAt()) ? expenseDto.getCreatedAt() : new Timestamp(System.currentTimeMillis()));

        expenseRepository.save(expense);
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
        return expenseRepository.findByUserId(userId);
    }
}