package de.probstl.ausgaben.mail;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
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
	private final Map<String, List<Expense>> m_ShopSum = new HashMap<String, List<Expense>>();

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
		m_ShopSum.putAll(info.getExpenses().stream().collect(Collectors.groupingBy(Expense::getShop)));
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
		final SortedSet<ShopSum> toReturn = new TreeSet<ShopSum>();
		for (Entry<String, List<Expense>> entry : m_ShopSum.entrySet()) {
			double sum = entry.getValue().stream().mapToDouble(Expense::getAmountDouble).sum();
			toReturn.add(new ShopSum(entry.getKey(), Double.valueOf(sum), entry.getValue().size()));
		}
		return toReturn;
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
