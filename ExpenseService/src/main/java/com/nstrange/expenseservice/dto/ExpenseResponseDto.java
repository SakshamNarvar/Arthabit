package com.nstrange.expenseservice.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExpenseResponseDto {

    private String externalId;
    private BigDecimal amount;
    private String userId;
    private String merchant;
    private String currency;
    private Timestamp createdAt;
}