package de.probstl.ausgaben.aws;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.probstl.ausgaben.data.Expense;
import lombok.Data;

/**
 * POJO for creating the JSON data for AWS request
 */
@Data
public class AwsDataValue {

    /** Name of the DynamoDB Table */
    @JsonProperty("TableName")
    private final String tableName;

    /** The expense transported as Item */
    @JsonProperty("Item")
    private final Expense data;

    /**
     * Constructor needs an expense. The expense is copied with a unique UUID as id
     * 
     * @param expense The expense that is being sent as item
     */
    AwsDataValue(Expense expense) {
        this.tableName = "Ausgaben";

        UUID id = UUID.randomUUID();
        this.data = new Expense(id.toString());
        this.data.setAmountDouble(expense.getAmountDouble());
        this.data.setBudget(expense.getBudget());
        this.data.setCity(expense.getCity());
        this.data.setMessage(expense.getMessage());
        this.data.setPayment(expense.getPayment());
        this.data.setShop(expense.getShop());
        this.data.setTimestamp(expense.getTimestamp());
    }
}
