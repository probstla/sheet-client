package de.probstl.ausgaben.aws;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.probstl.ausgaben.data.Expense;

@SpringBootTest
public class TestAwsService {

    @Autowired
    private AwsService toTest;

    /**
     * Test call against AWS API Gateway
     */
    @Test
    public void testSaveExpense() {
        Assertions.assertNotNull(toTest, "Service is null");

        final String id = UUID.randomUUID().toString();
        Assertions.assertNotNull(id);

        Expense expense = new Expense(id);
        expense.setAmount("3,49");
        expense.setAmountDouble(Double.valueOf(3.49));
        expense.setBudget("coffee");
        expense.setCity("Berlin");
        expense.setMessage("Cappuccino grande");
        expense.setPayment("visa");
        expense.setShop("Starbucks");
        expense.setTimestamp(Date.from(Instant.now()));

        Assertions.assertTrue(this.toTest.sendExpense(expense));
    }

}
