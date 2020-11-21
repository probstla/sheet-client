package de.probstl.ausgaben.data;

import java.util.Date;

import javax.validation.constraints.NotNull;

/**
 * A expense
 */
public class Expense {

	/** Default payment if no payment was defined */
	public static final String DEFAULT_PAYMENT = "cash";

	/** The shop where the expense was made */
	@NotNull(message = "shop must be provided")
	private String m_Shop;

	/** The description message of the expense */
	@NotNull(message = "message must be provided")
	private String m_Message;

	/** The amount as string (parsed later */
	@NotNull(message = "amount must be provided")
	private String m_Amount;

	/** The amount as double value */
	private Double m_AmountDouble;

	/** The city where the expense was made */
	private String m_City;

	/** The timestamp */
	private Date m_Timestamp;

	/** The payment (cash or card) of the expense */
	private String m_Payment;

	/** The unique id of this expense */
	private final String m_Id;

	/** The Budget this expense is for (submitted by Client) */
	private String m_Budget;

	/**
	 * Constructor with the unique id of this expense
	 * 
	 * @param id The document id
	 */
	public Expense(String id) {
		m_Id = id;
	}

	/**
	 * Non-arg constructor is necessary because of the RESTful http-post to create a
	 * new expense from JSON!
	 */
	public Expense() {
		this("0");
	}

	/**
	 * @return id
	 */
	public String getId() {
		return m_Id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_Id == null) ? 0 : m_Id.hashCode());
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
		Expense other = (Expense) obj;
		if (m_Id == null) {
			if (other.m_Id != null)
				return false;
		} else if (!m_Id.equals(other.m_Id))
			return false;
		return true;
	}

	/**
	 * Returns the shop
	 * 
	 * @return Shop
	 */
	public String getShop() {
		return m_Shop;
	}

	/**
	 * Set the shop
	 * 
	 * @param shop Shop
	 */
	public void setShop(String shop) {
		m_Shop = shop;
	}

	/**
	 * Returns the message (description)
	 * 
	 * @return Message
	 */
	public String getMessage() {
		return m_Message;
	}

	/**
	 * Set the message (description)
	 * 
	 * @param message Message
	 */
	public void setMessage(String message) {
		m_Message = message;
	}

	/**
	 * Returns the amount as string
	 * 
	 * @return Amount (text)
	 */
	public String getAmount() {
		return m_Amount;
	}

	/**
	 * Set the amount as String
	 * 
	 * @param amount Amount
	 */
	public void setAmount(String amount) {
		m_Amount = amount;
	}

	/**
	 * Returns the amount as double value
	 * 
	 * @return Amount (number)
	 */
	public Double getAmountDouble() {
		return m_AmountDouble;
	}

	/**
	 * Set the amount as double value
	 * 
	 * @param amount Amount
	 */
	public void setAmountDouble(Double amount) {
		m_AmountDouble = amount;
	}

	/**
	 * Returns the timestamp
	 * 
	 * @return Timestamp
	 */
	public Date getTimestamp() {
		return m_Timestamp;
	}

	/**
	 * Set the timestamp
	 * 
	 * @param timestamp Timestamp
	 */
	public void setTimestamp(Date timestamp) {
		m_Timestamp = timestamp;
	}

	/**
	 * Returns the city
	 * 
	 * @return City
	 */
	public String getCity() {
		return m_City;
	}

	/**
	 * Set the city
	 * 
	 * @param city City
	 */
	public void setCity(String city) {
		m_City = city;
	}

	/**
	 * @return the payment - e.g. cash or credit card
	 */
	public String getPayment() {
		return m_Payment == null ? DEFAULT_PAYMENT : m_Payment;
	}

	/**
	 * @param m_Payment the payment to set - e.g. cash or credit card
	 */
	public void setPayment(String payment) {
		m_Payment = payment;
	}

	/**
	 * @return Is <code>true</code> if the payment is 'cash' otherwise it's card
	 */
	public boolean isCash() {
		return DEFAULT_PAYMENT.equals(getPayment());
	}

	/**
	 * @param budget the budget for this expense
	 */
	public void setBudget(String budget) {
		m_Budget = budget;
	}

	/**
	 * @return the budget submitted by the client
	 */
	public String getBudget() {
		return m_Budget;
	}
}
