package de.probstl.ausgaben;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
	private FirestoreService m_FirestoreService;

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
	public String gotoOverview(final HomeForm homeForm, final HttpServletRequest req, Locale requestLocale) {

		if (req.getParameter("gotoWeek") != null) {
			LOG.info("dispatch to current week");
			return "redirect:/view/currentWeek";
		}

		LOG.info("dispatch to month: " + homeForm.getSelectedMonth());

		LocalDate localDate = null;
		try {
			// for parsing the day of month has to be added for some reason
			localDate = LocalDate.parse("1 " + homeForm.getSelectedMonth(),
					DateTimeFormatter.ofPattern("d " + MONTH_PATTERN, requestLocale));
		} catch (DateTimeParseException e) {
			LOG.error("unparseable selection in landing page: " + homeForm.getSelectedMonth(), e);
			return "redirect:/home";
		}

		if (req.getParameter("gotoMonth") != null) {
			return "redirect:/view/" + localDate.getMonthValue() + "/" + localDate.getYear();
		} else if (req.getParameter("downloadMonth") != null) {
			return "redirect:/export/" + localDate.getMonthValue() + "/" + localDate.getYear();
		}

		return "redirect:/home";
	}

	/**
	 * Show the landing page
	 * 
	 * @param model         Model for web view
	 * @param auth          The logged in user
	 * @param requestLocale The locale of the logged in user
	 * @return Template to show
	 */
	@GetMapping("/home")
	public String showHome(Model model, Authentication auth, Locale requestLocale) {

		final Collection<String> monthList = new ArrayList<String>();

		// Here's when we started to collect the expenses
		LocalDate startDate = LocalDate.of(2020, Month.APRIL, 1);

		String month = null;
		while (startDate.isBefore(LocalDate.now())) {

			month = startDate.format(DateTimeFormatter.ofPattern(MONTH_PATTERN, requestLocale));
			LOG.debug("Adding month for selection {}", month);
			monthList.add(month);

			startDate = startDate.plusMonths(1);
		}

		// Initialize Form with latest month
		HomeForm homeForm = new HomeForm();
		homeForm.setSelectedMonth(month);

		double currentWeek = 0.0;
		double currentMonth = 0.0;
		double lastMonth = 0.0;
		double percentOfLastMonth = 0.0;
		double percentOfMonth = 0.0;

		String collection = m_FirestoreService.extractCollection(auth);
		if (collection != null) {
			currentWeek = m_FirestoreService.findAmount(ExpensesRequest.forCurrentWeek(), collection);
			currentMonth = m_FirestoreService.findAmount(ExpensesRequest.forCurrentMonth(), collection);
			lastMonth = m_FirestoreService.findAmount(ExpensesRequest.forLastMonth(), collection);
			percentOfLastMonth = (currentMonth > 0 && lastMonth > 0) ? (currentMonth / lastMonth) * 100.0 : 0.0;
			percentOfMonth = (currentMonth > 0 && currentWeek > 0) ? (currentWeek / currentMonth) * 100.0 : 0.0;
		}

		model.addAttribute("monthSelection", monthList);
		model.addAttribute("homeForm", homeForm);
		model.addAttribute("amountLastMonth", Double.valueOf(lastMonth));
		model.addAttribute("amountCurrentMonth", Double.valueOf(currentMonth));
		model.addAttribute("amountCurrentWeek", Double.valueOf(currentWeek));
		model.addAttribute("percentOfLastMonth", Double.valueOf(percentOfLastMonth));
		model.addAttribute("percentOfMonth", Double.valueOf(percentOfMonth));

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
			HttpServletResponse response, Authentication auth, Locale requestLocale) throws Exception {

		final ExpensesRequest request = ExpensesRequest.forMonth(month, year);
		final Map<String, CityInfo> cityMapping;

		String collection = m_FirestoreService.extractCollection(auth);
		if (collection != null) {
			cityMapping = findExpensesByDate(collection, request);
		} else {
			cityMapping = Collections.emptyMap();
		}

		// set file name and content type
		String filename = MessageFormat.format("expenses_{0,number,00}-{1,number,0000}.csv", Integer.valueOf(month),
				Integer.valueOf(year));

		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

		final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
		final DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(requestLocale);
		numberFormat.applyPattern("0.00");
		final String lineSeparator = System.getProperty("line.separator");
		final PrintWriter printWriter = response.getWriter();

		// print headers
		printWriter.append("Datum,Beschreibung,Kategorie,Betrag" + lineSeparator);

		for (CityInfo cityInfo : cityMapping.values()) {
			for (Expense expense : cityInfo.getExpenses()) {
				ZonedDateTime dt = expense.getTimestamp().toInstant().atZone(ZoneId.systemDefault());
				printWriter.append(dateFormatter.format(dt));
				printWriter.append(",");
				printWriter.append(quote(expense));
				printWriter.append(",");
				printWriter.append(getCategory(auth, expense));
				printWriter.append(",");
				printWriter.append("\"" + numberFormat.format(expense.getAmountDouble().doubleValue()) + " €\"");
				printWriter.append(lineSeparator);
			}
		}
		printWriter.flush();
	}

	/**
	 * Try to assign a Category for the expense. Possible categories:
	 * <ul>
	 * <li>Geschenkefond</li>
	 * <li>52 Wochenchallenge</li>
	 * <li>Lebensmittel</li>
	 * <li>Eigene Anschaffungen</li>
	 * <li>Verschiedenes</li>
	 * </ul>
	 * 
	 * @param auth    The currently logged on user
	 * @param expense The expense
	 * @return A Category or an empty string when the category could not be assigned
	 */
	private String getCategory(Authentication auth, Expense expense) {

		if (!"manu".equals(auth.getName())) {
			return "";
		}

		if (expense.getMessage() != null && expense.getMessage().indexOf("Geschenk") >= 0) {
			return "Geschenkefond";
		} else if (expense.getShop() != null) {
			if (expense.getShop().equals("Challenge")) {
				return "52 Wochenchallenge";
			} else if ("Gründl".equalsIgnoreCase(expense.getShop().trim())
					|| "Rewe".equalsIgnoreCase(expense.getShop().trim())
					|| "EDEKA".equalsIgnoreCase(expense.getShop().trim())
					|| "DM".equalsIgnoreCase(expense.getShop().trim())
					|| "Eierautomat".equalsIgnoreCase(expense.getShop().trim())
					|| "Betz".equalsIgnoreCase(expense.getShop().trim())) {
				return "Lebensmittel";
			}
		}

		return "";
	}

	/**
	 * Quotes the message. If there area double quotes they are escaped by adding
	 * additional double quotes to escape them. If there are comma in the message,
	 * the message is surrounded by double quotes
	 * 
	 * @param expense The message that should be quoted
	 * @return Quoted message
	 */
	private String quote(Expense expense) {

		String toReturn = expense.getMessage();
		if (expense.getShop() != null) {
			toReturn = toReturn + " (" + expense.getShop() + ")";
		}

		boolean hasDoubleQuotes = toReturn.indexOf("\"") >= 0;
		boolean hasComma = toReturn.indexOf(",") >= 0;
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

		String collection = m_FirestoreService.extractCollection(auth);
		if (collection != null) {
			cityMapping = findExpensesByDate(collection, request);
		} else {
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

		Collection<Expense> expenses = m_FirestoreService.findBetween(request, collection);

		for (Expense expense : expenses) {
			String city = expense.getCity();

			final CityInfo cityInfo;
			if (cityMapping.containsKey(city)) {
				cityInfo = cityMapping.get(city);
			} else {
				cityInfo = new CityInfo(city);
				cityMapping.put(city, cityInfo);
			}

			cityInfo.addExpense(expense);
		}
		return cityMapping;
	}
}
