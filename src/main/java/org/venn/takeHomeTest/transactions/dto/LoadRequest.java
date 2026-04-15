package org.venn.takeHomeTest.transactions.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class LoadRequest {
    private String id;
    private String customerId;
    private String loadAmount;
    private String time;
}
