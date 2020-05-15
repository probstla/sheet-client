package de.probstl.ausgaben;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import de.probstl.ausgaben.data.Expense;
import de.probstl.ausgaben.data.HomeForm;
import de.probstl.ausgaben.mail.CityInfo;
import de.probstl.ausgaben.mail.MailInfo;

/**
 * Controller for web-access
 */
@Controller
public class ExpensesWebResource implements WebMvcConfigurer {

	/** Service to access Google data */
	@Autowired
	private FirestoreConfigService m_FirestoreService;

	/** The Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ExpensesWebResource.class);

	/** Pattern for formatting and parsing month */
	private static final String MONTH_PATTERN = "MMMM yyyy";

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("redirect:/home");
		registry.addViewController("/login").setViewName("login");
	}

	/**
	 * Takes the input from the landing page and load the selected data
	 * 
	 * @param homeForm The form with the selected month if <b>gotoMonth</b> has been
	 *                 pressed
	 * @param req      The request for getting the pressed submit button
	 * @param model    The model for calling other methods
	 * @param auth     The model for calling other methods
	 * @return Template to show
	 */
	@PostMapping("/overview")
	public String gotoOverview(final HomeForm homeForm, final HttpServletRequest req, Model model,
			Authentication auth) {

		if (req.getParameter("gotoWeek") != null) {
			LOG.info("dispatch to current week");
			return showWeek(model, auth);
		} else if (req.getParameter("gotoMonth") != null) {
			try {
				// for parsing the day of month has to be added for some reason
				LocalDate localDate = LocalDate.parse("1 " + homeForm.getSelectedMonth(),
						DateTimeFormatter.ofPattern("d " + MONTH_PATTERN));
				LOG.info("dispatch to month: " + homeForm.getSelectedMonth());
				return showMonth(Integer.toString(localDate.getMonthValue()), Integer.toString(localDate.getYear()),
						model, auth);
			} catch (DateTimeParseException e) {
				LOG.error("unparseable selection in landing page: " + homeForm.getSelectedMonth(), e);
			}
		}

		return showHome(model);
	}

	/**
	 * Show the landing page
	 * 
	 * @param model Model for web view
	 * @return Template to show
	 */
	@GetMapping("/home")
	public String showHome(Model model) {

		final Collection<String> monthList = new ArrayList<String>();

		LocalDate startDate = LocalDate.of(2020, Month.APRIL, 1);

		String month = null;
		while (startDate.isBefore(LocalDate.now())) {

			month = startDate.format(DateTimeFormatter.ofPattern(MONTH_PATTERN));
			LOG.debug("Adding month for selection {}", month);
			monthList.add(month);

			startDate = startDate.plusMonths(1);
		}

		// Initialize Form with latest month
		HomeForm homeForm = new HomeForm();
		homeForm.setSelectedMonth(month);

		model.addAttribute("monthSelection", monthList);
		model.addAttribute("homeForm", homeForm);
		return "home";
	}

	/**
	 * Shows the expenses by month/year
	 * 
	 * @param month The month
	 * @param year  The year
	 * @param model Model for web view
	 * @return Template to show
	 */
	@GetMapping("/view/{month}/{year}")
	public String showMonth(@PathVariable(name = "month") String month, @PathVariable(name = "year") String year,
			Model model, Authentication auth) {

		LocalDateTime begin;

		try {
			begin = LocalDateTime.of(LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1),
					LocalTime.of(0, 0, 0));
		} catch (NumberFormatException e) {
			LOG.warn("Illegal month/year in requeset", e);
			begin = LocalDateTime.of(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()), LocalTime.of(0, 0, 0));
		}

		Date beginDate = Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant());
		model.addAttribute("beginDate", beginDate);

		LocalDateTime end = begin.plusMonths(1);
		Date endDate = Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant());
		model.addAttribute("endDate", endDate);
		model.addAttribute("currency", "€");

		final MailInfo mailInfo = new MailInfo(beginDate, endDate);
		final Map<String, CityInfo> cityMapping;

		Optional<? extends GrantedAuthority> authority = auth.getAuthorities().stream().findFirst();
		if (authority.isPresent()) {
			String collection = authority.get().getAuthority();
			LOG.info("Using collection {} for user {}", collection, auth.getName());
			cityMapping = findExpensesByDate(collection, beginDate, endDate);
		} else {
			LOG.warn("No authority for user {} found. Showing empty data!", auth.getName());
			cityMapping = Collections.emptyMap();
		}

		cityMapping.values().stream().forEach(x -> mailInfo.addCityInfo(x));

		model.addAttribute("cities", mailInfo.getCityList());
		model.addAttribute("sumShops", mailInfo.getSumShops());
		model.addAttribute("sum", mailInfo.getSum());

		return "email";
	}

	/**
	 * Returns the data of the current week
	 * 
	 * @param model Model for web view
	 * @return Template to show
	 */
	@GetMapping("/view/currentWeek")
	public String showWeek(Model model, Authentication auth) {

		LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		LocalDateTime begin = LocalDateTime.of(monday, LocalTime.of(0, 0, 0));
		Date beginDate = Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant());
		model.addAttribute("beginDate", beginDate);

		LocalDateTime end = begin.plusDays(7);
		Date endDate = Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant());
		model.addAttribute("endDate", endDate);
		model.addAttribute("currency", "€");

		final MailInfo mailInfo = new MailInfo(beginDate, endDate);
		final Map<String, CityInfo> cityMapping;

		Optional<? extends GrantedAuthority> authority = auth.getAuthorities().stream().findFirst();
		if (authority.isPresent()) {
			String collection = authority.get().getAuthority();
			LOG.info("Using collection {} for user {}", collection, auth.getName());
			cityMapping = findExpensesByDate(collection, beginDate, endDate);
		} else {
			LOG.warn("No authority for user {} found. Showing empty data!", auth.getName());
			cityMapping = Collections.emptyMap();
		}

		cityMapping.values().stream().forEach(x -> mailInfo.addCityInfo(x));

		model.addAttribute("cities", mailInfo.getCityList());
		model.addAttribute("sumShops", mailInfo.getSumShops());
		model.addAttribute("sum", mailInfo.getSum());

		return "email";
	}

	/**
	 * Find the expenses for an interval from begin (inclusive) to end (exclusive)
	 * 
	 * @param beginDate Begin date
	 * @param endDate   End date
	 * @return Map of cities and expenses by city
	 */
	private Map<String, CityInfo> findExpensesByDate(String collection, Date beginDate, Date endDate) {
		final Map<String, CityInfo> cityMapping = new HashMap<>();

		LOG.info("Find expenses from {} to {} in collection {}", beginDate, endDate, collection);

		ZoneId systemZone = ZoneId.systemDefault();
		ZoneId homeZone = ZoneId.of("Europe/Berlin");

		LOG.info("System ZoneId is {}, Home ZoneId {}.", systemZone, homeZone);

		try {
			ApiFuture<QuerySnapshot> future = m_FirestoreService.getService().collection(collection)
					.whereGreaterThan("timestamp", beginDate).whereLessThan("timestamp", endDate).orderBy("timestamp")
					.get();

			QuerySnapshot queryResult = future.get();
			LOG.info("Read time {}", queryResult.getReadTime());
			for (DocumentSnapshot document : queryResult.getDocuments()) {

				String city = document.getString("city");

				CityInfo cityInfo;
				if (cityMapping.containsKey(city)) {
					cityInfo = cityMapping.get(city);
				} else {
					cityInfo = new CityInfo(city);
					cityMapping.put(city, cityInfo);
				}

				Expense expense = new Expense();
				expense.setShop(document.getString("shop"));
				expense.setCity(city);
				expense.setMessage(document.getString("message"));
				expense.setAmountDouble(document.getDouble("amount"));

				Instant originalTimestamp = document.getDate("timestamp").toInstant();
				LocalDateTime ldt = LocalDateTime.ofInstant(originalTimestamp, homeZone);
				ZonedDateTime zdt = ZonedDateTime.of(ldt, systemZone);

				expense.setTimestamp(Date.from(zdt.toInstant()));
				expense.setPayment(document.getString("payment"));

				LOG.debug("Expense id '{}' created at '{}' with date '{}' from shop '{}' and amount '{}' EUR.",
						document.getId(), document.getCreateTime(), expense.getTimestamp(), expense.getShop(),
						expense.getAmountDouble());

				cityInfo.addExpense(expense);
			}
		} catch (InterruptedException e) {
			LOG.error("Unterbrochen", e);
		} catch (ExecutionException e) {
			LOG.error("Fehler in der Ausführung", e);
		}
		return cityMapping;
	}

}
