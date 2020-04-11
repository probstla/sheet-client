package de.probstl.ausgaben.mail;

/**
 * Class with the sum of a shop
 */
public class ShopSum implements Comparable<ShopSum> {

	/** The name of the shop */
	private String m_Shop;

	/** The amount of the shop */
	private Double m_Sum;

	/**
	 * Construcotr for new Object
	 * 
	 * @param m_Shop
	 * @param m_Sum
	 */
	public ShopSum(String shop, Double sum) {
		this.m_Shop = shop;
		this.m_Sum = sum;
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
	 * @param m_Shop the m_Shop to set
	 */
	public void setShop(String shop) {
		this.m_Shop = shop;
	}

	/**
	 * @return the m_Sum
	 */
	public Double getSum() {
		return m_Sum;
	}

	/**
	 * @param m_Sum the m_Sum to set
	 */
	public void setSum(Double sum) {
		this.m_Sum = sum;
	}
}
