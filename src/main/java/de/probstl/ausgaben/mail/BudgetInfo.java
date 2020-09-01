package de.probstl.ausgaben.mail;

import de.probstl.ausgaben.budget.Budget;

/**
 * Class for rendering a budget
 */
public class BudgetInfo implements Comparable<BudgetInfo> {

	/** Name of the Budget */
	private final String m_Name;

	/** The description of the budget */
	private final String m_Description;

	/** Sum of all expenses matching the budget */
	private final Double m_Sum;

	/** Remaining budget */
	private final Double m_Remaining;

	public BudgetInfo(Budget budget, Double sum) {
		m_Name = budget.getName();
		m_Description = budget.getDescription();
		m_Sum = sum == null ? Double.valueOf(0.0) : sum.doubleValue();
		if (budget.getAmount().intValue() <= 0) {
			m_Remaining = Double.valueOf(0.0d);
		} else {
			m_Remaining = Double.valueOf(budget.getAmount() - m_Sum.doubleValue());
		}
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(BudgetInfo o) {
		return getName().compareTo(o.getName());
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return m_Name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return m_Description;
	}

	/**
	 * @return the sum
	 */
	public Double getSum() {
		return m_Sum;
	}

	/**
	 * @return the remaining
	 */
	public Double getRemaining() {
		return m_Remaining;
	}

	/**
	 * @return if <code>true</code> the amount is overridden
	 */
	public boolean isNegative() {
		return m_Remaining != null && m_Remaining.doubleValue() < 0;
	}

}
