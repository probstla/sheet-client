package de.probstl.ausgaben.data;

import java.util.Date;

import javax.validation.constraints.NotNull;

public class Ausgabe {

	@NotNull(message = "shop must be provided")
	private String m_Shop;

	@NotNull(message = "message must be provided")
	private String m_Message;

	@NotNull(message = "amount must be provided")
	private String m_Amount;

	private Double m_AmountDouble;

	private String m_City;

	private Date m_Timestamp;

	public String getShop() {
		return m_Shop;
	}

	public void setShop(String shop) {
		m_Shop = shop;
	}

	public String getMessage() {
		return m_Message;
	}

	public void setMessage(String message) {
		m_Message = message;
	}

	public String getAmount() {
		return m_Amount;
	}

	public void setAmount(String amount) {
		m_Amount = amount;
	}

	public Double getAmountDouble() {
		return m_AmountDouble;
	}

	public void setAmountDouble(Double amount) {
		m_AmountDouble = amount;
	}

	public Date getTimestamp() {
		return m_Timestamp;
	}

	public void setTimestamp(Date timestamp) {
		m_Timestamp = timestamp;
	}

	public String getCity() {
		return m_City;
	}

	public void setCity(String city) {
		m_City = city;
	}
}
