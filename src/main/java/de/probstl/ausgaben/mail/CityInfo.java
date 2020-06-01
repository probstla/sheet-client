package de.probstl.ausgaben.mail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.probstl.ausgaben.data.Expense;

/**
 * A class that holds all expenses for a city
 */
public class CityInfo implements Comparable<CityInfo> {

	/** The name of the city */
	private final String m_Name;

	/** The list of all expenses */
	private final List<Expense> m_Expenses = new ArrayList<Expense>();

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
}
