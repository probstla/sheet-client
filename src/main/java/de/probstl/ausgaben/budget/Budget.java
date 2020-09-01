package de.probstl.ausgaben.budget;

/**
 * A class representing a budget defined in JSON file
 */
public class Budget implements Comparable<Budget> {

	/** Short name of the budget (e.g. Lebensmittel) */
	private String m_Name;

	/** Description of the budget */
	private String m_Description;

	/** Amount per month */
	private Double m_Amount;

	/** A regular expression for matching the expense description */
	private String m_MessageRegex;

	/** The shops when the regular expression does not match */
	private String[] m_Shops;

	/** The budget is a fallback if no other budget is found for an expense */
	private boolean m_Fallback;

	/**
	 * Default constructor
	 */
	public Budget() {
	}

	/**
	 * Constructor providing the values
	 * 
	 * @param name         short name of the budget
	 * @param description  description
	 * @param amount       amount per month
	 * @param messageRegex regular expression applied to the message
	 */
	public Budget(String name, String description, Double amount, String messageRegex, String[] shops) {
		m_Name = name;
		m_Description = description;
		m_Amount = amount;
		m_MessageRegex = messageRegex;
		m_Shops = shops;
	}

	@Override
	public int compareTo(Budget o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_Name == null) ? 0 : m_Name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Budget other = (Budget) obj;
		if (m_Name == null) {
			if (other.m_Name != null)
				return false;
		} else if (!m_Name.equals(other.m_Name))
			return false;
		return true;
	}

	/**
	 * @return the m_Name
	 */
	public String getName() {
		return m_Name;
	}

	/**
	 * @param name the m_Name to set
	 */
	public void setName(String name) {
		this.m_Name = name;
	}

	/**
	 * @return the m_Description
	 */
	public String getDescription() {
		return m_Description;
	}

	/**
	 * @param description the m_Description to set
	 */
	public void setDescription(String description) {
		this.m_Description = description;
	}

	/**
	 * @return the m_Amount
	 */
	public Double getAmount() {
		return m_Amount;
	}

	/**
	 * @param amount the m_Amount to set
	 */
	public void setAmount(Double amount) {
		this.m_Amount = amount;
	}

	/**
	 * @return the m_MessageRegex
	 */
	public String getMessageRegex() {
		return m_MessageRegex;
	}

	/**
	 * @param messageRegex the m_MessageRegex to set
	 */
	public void setMessageRegex(String messageRegex) {
		this.m_MessageRegex = messageRegex;
	}

	/**
	 * @return the m_Shops
	 */
	public String[] getShops() {
		return m_Shops;
	}

	/**
	 * @param shops the m_Shops to set
	 */
	public void setShops(String[] shops) {
		this.m_Shops = shops;
	}

	/**
	 * @return the fallback
	 */
	public boolean isFallback() {
		return m_Fallback;
	}

	/**
	 * @param fallback the fallback to set
	 */
	public void setFallback(boolean fallback) {
		this.m_Fallback = fallback;
	}
}
