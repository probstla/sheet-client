package de.probstl.ausgaben.budget;

import java.util.Collection;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test for the budget service
 */
@RunWith(SpringRunner.class)
public class TestBudgetService {

	/**
	 * Test for reading the budget definition from file
	 */
	@Test
	public void testReadDefinition() {
		BudgetService service = new BudgetService();
		Collection<Budget> definition = service.readDefinition("flo");
		Assert.assertNotNull(definition);
		Assert.assertTrue(!definition.isEmpty());
		
		Optional<String> budget = definition.stream().map(x -> x.getName()).filter(x -> x.equalsIgnoreCase("Lebensmittel")).findFirst();
		Assert.assertTrue(budget.isPresent());
	}
}
