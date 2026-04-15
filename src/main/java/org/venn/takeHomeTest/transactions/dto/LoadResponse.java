package org.venn.takeHomeTest.transactions.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class LoadResponse {
    private String id;
    private String customerId;
    private boolean accepted;
}
