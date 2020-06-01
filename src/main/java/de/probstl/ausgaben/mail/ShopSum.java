package de.probstl.ausgaben.mail;

/**
 * Class with the sum of a shop
 */
public class ShopSum implements Comparable<ShopSum> {

	/** The name of the shop */
	private final String m_Shop;

	/** The amount of the shop */
	private final Double m_Sum;

	/** The expenses count for this shop */
	private final int m_Count;

	/**
	 * Constructor for new Object
	 * 
	 * @param shop The name of the shop 
	 * @param sum The sum of expenses in the time period
	 * @param count The count of expenses in the time period
	 */
	public ShopSum(String shop, Double sum, int count) {
		this.m_Shop = shop;
		this.m_Sum = sum;
		this.m_Count = count;
	}

	@Override
	public int compareTo(ShopSum o) {
		return -1 * getSum().compareTo(o.getSum());
	}

	/**
	 * @return the m_Shop
	 */
	public String getShop() {
		return m_Shop;
	}

	/**
	 * @return the m_Sum
	 */
	public Double getSum() {
		return m_Sum;
	}

	/**
	 * @return the m_Count
	 */
	public int getCount() {
		return m_Count;
	}
}
