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
public class UpdateExpenseDto {

    private BigDecimal amount;

    private String notes;
    private String category;
    private String fundSource;

    private String merchant;

    private String currency;

    private Timestamp createdAt;
}
