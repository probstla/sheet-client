package de.probstl.ausgaben.mail;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import de.probstl.ausgaben.data.Expense;

/**
 * Class holding the information needed to fill an email with content
 */
public class MailInfo {

	/** The begin date of the report */
	private final Date m_From;

	/** The end date of the report */
	private final Date m_To;

	/** Sum of all expenses */
	private BigDecimal m_Sum = new BigDecimal(0);

	/** List of cities where the expenses have been made */
	private final Collection<CityInfo> m_CityList = new TreeSet<CityInfo>();

	/** Map with all shops and their sums */
	private final Map<String, Double> m_ShopSum = new HashMap<String, Double>();

	/**
	 * Constructor
	 * 
	 * @param from The begin of the report
	 * @param to   The end of the report
	 */
	public MailInfo(Date from, Date to) {
		m_From = from;
		m_To = to;
	}

	/**
	 * Add expenses for a city
	 * 
	 * @param info The expenses of a city
	 */
	public void addCityInfo(CityInfo info) {
		double sumCity = info.getExpenses().stream().mapToDouble(x -> x.getAmountDouble().doubleValue()).sum();
		m_Sum = m_Sum.add(BigDecimal.valueOf(sumCity));

		Map<String, List<Expense>> shopMap = info.getExpenses().stream()
				.collect(Collectors.groupingBy(Expense::getShop));
		Map<String, Double> shopSumMap = shopMap.entrySet().stream().collect(Collectors.toMap(x -> x.getKey(),
				x -> x.getValue().stream().mapToDouble(Expense::getAmountDouble).sum()));
		m_ShopSum.putAll(shopSumMap);

		m_CityList.add(info);
	}

	/**
	 * Returns all the cities where expenses have been made
	 * 
	 * @return Unmodifiable collection of all cities
	 */
	public Collection<CityInfo> getCityList() {
		return Collections.unmodifiableCollection(m_CityList);
	}

	/**
	 * Returns the summary of all shops sorted by amount desc
	 * 
	 * @return Collection of entries with the shop name as key and the amount sum as
	 *         value
	 */
	public Collection<ShopSum> getSumShops() {

		return m_ShopSum.entrySet().stream().map(x -> new ShopSum(x.getKey(), x.getValue())).sorted()
				.collect(Collectors.toList());
	}

	/**
	 * Returns the begin date
	 * 
	 * @return Begin date of the report
	 */
	public Date getFrom() {
		return m_From;
	}

	/**
	 * Returns the end date
	 * 
	 * @return End date of the report
	 */
	public Date getTo() {
		return m_To;
	}

	/**
	 * Return the sum of all expenses
	 * 
	 * @return Sum of all expenses
	 */
	public Double getSum() {
		return Double.valueOf(m_Sum.doubleValue());
	}
}
