package de.probstl.ausgaben;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
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
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
import de.probstl.ausgaben.data.ExpensesRequest;
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

		// Here's when we started to collect the expenses
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
		final ExpensesRequest request = ExpensesRequest.forMonth(month, year);
		loadData(request, model, auth);
		return "email";
	}

	/**
	 * Allows the export of a month into a CSV file
	 * 
	 * @param month    The month that should be exported
	 * @param year     The year
	 * @param response The response to write the CSV data
	 * @param auth     Auth for choosing the collection
	 * @throws Exception
	 */
	@GetMapping("/export/{month}/{year}")
	public void exportMonth(@PathVariable(name = "month") String month, @PathVariable(name = "year") String year,
			HttpServletResponse response, Authentication auth) throws Exception {

		final ExpensesRequest request = ExpensesRequest.forMonth(month, year);
		final Map<String, CityInfo> cityMapping;

		Optional<? extends GrantedAuthority> authority = auth.getAuthorities().stream().findFirst();
		if (authority.isPresent()) {
			String collection = authority.get().getAuthority();
			LOG.info("Using collection {} for user {}", collection, auth.getName());
			cityMapping = findExpensesByDate(collection, request);
		} else {
			LOG.warn("No authority for user {} found. Showing empty data!", auth.getName());
			cityMapping = Collections.emptyMap();
		}

		// set file name and content type
		String filename = MessageFormat.format("expenses_{0,number,00}-{1,number,0000}.csv", Integer.valueOf(month),
				Integer.valueOf(year));

		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
		final String lineSeparator = System.getProperty("line.separator");
		final PrintWriter printWriter = response.getWriter();

		for (CityInfo cityInfo : cityMapping.values()) {
			for (Expense expense : cityInfo.getExpenses()) {
				ZonedDateTime dt = expense.getTimestamp().toInstant().atZone(ZoneId.systemDefault());
				printWriter.append(formatter.format(dt));
				printWriter.append(",");
				printWriter.append(quote(expense.getMessage()));
				printWriter.append(",,");
				printWriter.append(MessageFormat.format("\"{0,number,0.00} €\"", expense.getAmountDouble()));
				printWriter.append(lineSeparator);
			}
		}
		printWriter.flush();
	}

	/**
	 * Quotes the message. If there area double quotes they are escaped by adding
	 * additional double quotes to escape them. If there are comma in the message,
	 * the message is surrounded by double quotes
	 * 
	 * @param message The message that should be quoted
	 * @return Quoted message
	 */
	private String quote(String message) {

		String toReturn = message + "\"BUG\" for testing";

		boolean hasDoubleQuotes = toReturn.indexOf("\"") >= 0;
		boolean hasComma = message.indexOf(",") >= 0;
		if (hasDoubleQuotes) {
			toReturn = toReturn.replaceAll("\"", "\"\"");
		}
		if (hasComma) {
			toReturn = "\"" + toReturn + "\"";
		}

		return toReturn;
	}

	/**
	 * Load all expenses during the given request interval and fill the given model
	 * with data
	 * 
	 * @param request The request providing begin and end dates
	 * @param model   The model that must be filled with data
	 * @param auth    Authentication for choosing the collection
	 */
	private void loadData(ExpensesRequest request, Model model, Authentication auth) {
		final Map<String, CityInfo> cityMapping;

		Optional<? extends GrantedAuthority> authority = auth.getAuthorities().stream().findFirst();
		if (authority.isPresent()) {
			String collection = authority.get().getAuthority();
			LOG.info("Using collection {} for user {}", collection, auth.getName());
			cityMapping = findExpensesByDate(collection, request);
		} else {
			LOG.warn("No authority for user {} found. Showing empty data!", auth.getName());
			cityMapping = Collections.emptyMap();
		}

		final MailInfo mailInfo = new MailInfo(request.getBeginDate(), request.getEndDate());
		cityMapping.values().stream().forEach(x -> mailInfo.addCityInfo(x));

		model.addAttribute("beginDate", request.getBeginDate());
		model.addAttribute("endDate", request.getEndDate());
		model.addAttribute("cities", mailInfo.getCityList());
		model.addAttribute("sumShops", mailInfo.getSumShops());
		model.addAttribute("sum", mailInfo.getSum());
		model.addAttribute("currency", "€");
	}

	/**
	 * Returns the data of the current week
	 * 
	 * @param model Model for web view
	 * @return Template to show
	 */
	@GetMapping("/view/currentWeek")
	public String showWeek(Model model, Authentication auth) {
		final ExpensesRequest request = ExpensesRequest.forCurrentWeek();
		loadData(request, model, auth);
		return "email";
	}

	/**
	 * Find the expenses for an interval from begin (inclusive) to end (exclusive)
	 * 
	 * @param beginDate Begin date
	 * @param endDate   End date
	 * @return Map of cities and expenses by city
	 */
	private Map<String, CityInfo> findExpensesByDate(String collection, ExpensesRequest request) {
		final Map<String, CityInfo> cityMapping = new HashMap<>();

		LOG.info("Find expenses from {} to {} in collection {}", request.getBeginDate(), request.getEndDate(),
				collection);

		ZoneId systemZone = ZoneId.systemDefault();
		ZoneId homeZone = ZoneId.of("Europe/Berlin");

		LOG.info("System ZoneId is {}, Home ZoneId {}.", systemZone, homeZone);

		try {
			ApiFuture<QuerySnapshot> future = m_FirestoreService.getService().collection(collection)
					.whereGreaterThan("timestamp", request.getBeginDate())
					.whereLessThan("timestamp", request.getEndDate()).orderBy("timestamp").get();

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
