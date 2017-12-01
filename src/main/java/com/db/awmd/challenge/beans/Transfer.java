package com.db.awmd.challenge.beans;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class Transfer {

    @NotNull
    @NotEmpty
    private final String fromAccountId;

    @NotNull
    @NotEmpty
    private final String toAccountId;

    @NotNull
    @Min(value = 0, message = "Amount should not be negative.")
    private BigDecimal amount;

    @JsonCreator
    public Transfer(@JsonProperty("fromAccountId") String fromAccountId,
                    @JsonProperty("toAccountId") String toAccountId,
                    @JsonProperty("amount") BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }
}
