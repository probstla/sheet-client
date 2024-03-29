package de.probstl.ausgaben.data;

import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for creating request intervals with begin and end dates
 */
public class ExpensesRequest {

	/** The logger */
	private static final Logger LOG = LoggerFactory.getLogger(ExpensesRequest.class);

	/** Begin date for the expenses request interval */
	private final @Nonnull Date m_BeginDate;

	/** End date for the expenses request interval */
	private final @Nonnull Date m_EndDate;

	/**
	 * Create a new object spanning the current Month
	 * 
	 * @return New expenses request with the first of the current month as the
	 *         beginning and the current timestamp as end
	 */
	public static ExpensesRequest forCurrentMonth() {
		LocalDate currentMonth = LocalDate.now();
		LocalDateTime begin = LocalDateTime.of(LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 1),
				LocalTime.of(0, 0, 0));
		Date beginDate = Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant());
		LocalDateTime end = LocalDateTime.now();
		Date endDate = Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant());

		if (beginDate == null || endDate == null) {
			throw new IllegalStateException("illegal begin or end date");
		}

		return new ExpensesRequest(beginDate, endDate);
	}

	/**
	 * Create a new object for the last month
	 * 
	 * @return New expenses request with the first of the last month as the
	 *         beginning and the first of the current month as end
	 */
	public static ExpensesRequest forLastMonth() {
		LocalDate lastMonth = LocalDate.now().minusMonths(1);
		LocalDateTime begin = LocalDateTime.of(LocalDate.of(lastMonth.getYear(), lastMonth.getMonth(), 1),
				LocalTime.of(0, 0, 0));
		Date beginDate = Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant());
		LocalDateTime end = begin.plusMonths(1).minusMinutes(1);
		Date endDate = Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant());

		if (beginDate == null || endDate == null) {
			throw new IllegalStateException("illegal begin or end date");
		}

		return new ExpensesRequest(beginDate, endDate);
	}

	/**
	 * Create a new request based on the given month
	 * 
	 * @param month The month as integer string
	 * @param year  The year as integer string
	 * @return New expenses request with the given begin- and end dates
	 */
	public static ExpensesRequest forMonth(String month, String year) {
		LocalDateTime begin;
		try {
			begin = LocalDateTime.of(LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1),
					LocalTime.of(0, 0, 0));
		} catch (NumberFormatException e) {
			LOG.warn("Illegal month/year in requeset", e);
			begin = LocalDateTime.of(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()), LocalTime.of(0, 0, 0));
		}

		Date beginDate = Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant());

		LocalDateTime end = begin.plusMonths(1).minusMinutes(1);
		Date endDate = Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant());

		if (beginDate == null || endDate == null) {
			throw new IllegalStateException("illegal begin or end date");
		}

		return new ExpensesRequest(beginDate, endDate);
	}

	/**
	 * Create a new request based on current week
	 * 
	 * @return New expenses request with the given begin- and end dates
	 */
	public static ExpensesRequest forCurrentWeek() {
		LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		LocalDateTime begin = LocalDateTime.of(monday, LocalTime.of(0, 0, 0));
		Date beginDate = Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant());

		LocalDateTime end = begin.plusDays(7).minusMinutes(1);
		Date endDate = Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant());

		if(beginDate == null || endDate == null){
			throw new IllegalStateException("illegal begin or end date");
		}

		return new ExpensesRequest(beginDate, endDate);
	}

	/**
	 * Private constructor must provide begin- and end dates
	 * 
	 * @param begin Begin date of the request interval
	 * @param end   End date of the request interval
	 */
	private ExpensesRequest(@Nonnull Date begin, @Nonnull Date end) {
		m_BeginDate = begin;
		m_EndDate = end;
	}

	/**
	 * @return the m_BeginDate
	 */
	public @Nonnull Date getBeginDate() {
		return m_BeginDate;
	}

	/**
	 * @return the m_EndDate
	 */
	public @Nonnull Date getEndDate() {
		return m_EndDate;
	}

	/**
	 * @return returns the next month 
	 */
	public LocalDateTime getNextMonth() {
		LocalDateTime dateTime = LocalDateTime.ofInstant(m_BeginDate.toInstant(), ZoneId.systemDefault());
		if(dateTime.plusMonths(1).isAfter(LocalDateTime.now())) {
			return dateTime;
		}
		return dateTime.plusMonths(1);
	} 

	/**
	 * @return returns the previous month
	 */
	public LocalDateTime getPreviousMonth() {
		return LocalDateTime.ofInstant(m_BeginDate.toInstant(), ZoneId.systemDefault()).minusMonths(1);
	}

	@Override
	public String toString() {
		return MessageFormat.format(
				"time interval [{0,date,yyyy-MM-dd} {0,time,HH:mm:ss}] => [{1,date,yyyy-MM-dd} {1,time,HH:mm:ss}]",
				getBeginDate(), getEndDate());
	}
}
