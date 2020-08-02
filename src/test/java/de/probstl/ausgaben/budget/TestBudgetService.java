package de.probstl.ausgaben.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import de.probstl.ausgaben.data.Expense;

/**
 * Test for the budget service
 */
@SpringBootTest
public class TestBudgetService {

	/**
	 * Test for reading the budget definition from file
	 */
	@Test
	public void testReadDefinition() {
		final BudgetService service = new BudgetService();
		Collection<Budget> definition = service.readDefinition("flo");
		assertNotNull(definition);
		assertTrue(!definition.isEmpty());

		Optional<String> budget = definition.stream().map(x -> x.getName())
				.filter(x -> x.equalsIgnoreCase("Lebensmittel")).findFirst();
		assertTrue(budget.isPresent());
	}

	/**
	 * Test for detecting expenses by regex
	 */
	@Test
	public void testRegex() {

		Budget budget = new Budget();
		budget.setName("Auto");
		budget.setMessageRegex(".*Auto.*");

		Collection<Expense> expenses = new ArrayList<Expense>();
		Expense e1 = new Expense("0");
		e1.setMessage("Autowaschmittel");
		expenses.add(e1);

		final BudgetService service = new BudgetService();
		Collection<Expense> matching = service.findMatching(budget, expenses);
		assertFalse(matching.isEmpty());
	}

	/**
	 * Testcase for automatic regex generator based on budget name
	 */
	@Test
	public void testEmptyRegex() {
		Budget budget = new Budget();
		budget.setName("Auto");

		Collection<Expense> expenses = new ArrayList<Expense>();
		Expense e1 = new Expense("0");
		e1.setMessage("Waschstraße #auto");
		expenses.add(e1);

		final BudgetService service = new BudgetService();
		Collection<Expense> matching = service.findMatching(budget, expenses);
		assertFalse(matching.isEmpty());

	}

	/**
	 * Testcase for finding expenses by a given shop
	 */
	@Test
	public void testShops() {
		Budget budget = new Budget();
		budget.setName("Bäcker");
		budget.setShops(new String[] { "Betz", "Wackerl", "Mareis", "Frühmorgen" });

		Collection<Expense> expenses = new ArrayList<Expense>();
		Expense e1 = new Expense("0");
		e1.setShop("Betz");
		e1.setAmountDouble(2.0);
		expenses.add(e1);

		e1 = new Expense("1");
		e1.setShop("EDEKA");
		e1.setAmountDouble(2.0);
		expenses.add(e1);

		e1 = new Expense("2");
		e1.setShop("Wackerl");
		e1.setAmountDouble(2.0);
		expenses.add(e1);

		final BudgetService service = new BudgetService();
		Collection<Expense> matching = service.findMatching(budget, expenses);
		assertFalse(matching.isEmpty());
		assertEquals(4.0, matching.stream().mapToDouble(x -> x.getAmountDouble()).sum());
	}
}
