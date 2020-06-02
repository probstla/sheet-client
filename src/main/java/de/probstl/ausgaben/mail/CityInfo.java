package de.probstl.ausgaben.mail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.probstl.ausgaben.budget.Budget;
import de.probstl.ausgaben.data.Expense;

/**
 * A class that holds all expenses for a city
 */
public class CityInfo implements Comparable<CityInfo> {

	/** The name of the city */
	private final String m_Name;

	/** The list of all expenses */
	private final List<Expense> m_Expenses = new ArrayList<Expense>();

	/** The mapping of the Expenses to budgets */
	private final Map<Expense, Budget> m_ExpenseBudget = new HashMap<Expense, Budget>();

	/**
	 * Constructor
	 * 
	 * @param name The name of the city
	 */
	public CityInfo(String name) {
		m_Name = name;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CityInfo o) {
		return getName().compareTo(o.getName());
	}

	/**
	 * Add an expense
	 * 
	 * @param expense The expense that has been done in the city
	 */
	public void addExpense(Expense expense) {
		m_Expenses.add(expense);
	}

	/**
	 * Return all expenses from the city
	 * 
	 * @return Unmodifiable collection of expenses
	 */
	public Collection<Expense> getExpenses() {
		return Collections.unmodifiableList(m_Expenses);
	}

	/**
	 * Returns the name of the city
	 * 
	 * @return Name of the city
	 */
	public String getName() {
		return m_Name;
	}

	/**
	 * Returns the sum of all expenses in the city
	 * 
	 * @return Sum of all expenses
	 */
	public Double getSum() {
		return Double.valueOf(m_Expenses.stream().mapToDouble(x -> x.getAmountDouble().doubleValue()).sum());
	}

	/**
	 * Returns the sum of all expenses in the city payed by card
	 * 
	 * @return Sum of all expenses by card
	 */
	public Double getSumCard() {
		return Double.valueOf(
				m_Expenses.stream().filter(x -> !x.isCash()).mapToDouble(x -> x.getAmountDouble().doubleValue()).sum());
	}

	/**
	 * Returns the displayed message for the expense. If there is a budget for the
	 * expense the corresponding hashtag is filtered from the message. Otherwise the
	 * complete message is returned
	 * 
	 * @param expense The expense
	 * @return The message of the expense filtered by the budget hashtag
	 */
	public String getDisplayedMessage(Expense expense) {
		Budget budget = m_ExpenseBudget.get(expense);
		if (budget != null) {
			String hashTag = "#" + budget.getName().toLowerCase();
			return expense.getMessage().replaceAll(hashTag, "");
		}
		return expense.getMessage();
	}

	/**
	 * Get the budget for the given expense
	 * 
	 * @param expense The expense
	 * @return A hashtag string for the found budget or an empty string
	 */
	public String getBudgetInfo(Expense expense) {
		Budget budget = m_ExpenseBudget.get(expense);
		if (budget != null) {
			return " #" + budget.getName().toLowerCase();
		}
		return "";
	}

	/**
	 * A budget was found for the given expense
	 * 
	 * @param expense The expense
	 * @param budget  The found budget for the expense
	 */
	public void setBudget(Expense expense, Budget budget) {
		m_ExpenseBudget.put(expense, budget);
	}
}
