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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import de.probstl.ausgaben.budget.Budget;
import de.probstl.ausgaben.budget.BudgetService;
import de.probstl.ausgaben.data.Expense;
import de.probstl.ausgaben.data.ExpensesRequest;
import de.probstl.ausgaben.data.HomeForm;
import de.probstl.ausgaben.mail.BudgetInfo;
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

	/** The budget service for tracking expenses by budget */
	@Autowired
	private BudgetService m_BudgetService;

	/**
	 * Takes the input from the landing page and load the selected data
	 * 
	 * @param homeForm      The form with the selected month if <b>gotoMonth</b> has
	 *                      been pressed
	 * @param req           The request for getting the pressed submit button
	 * @param requestLocale The locale from the http request
	 * @return Template to show
	 */
	@PostMapping("/overview")
	public String gotoOverview(final HomeForm homeForm, final HttpServletRequest req, Locale requestLocale) {

		if (req.getParameter("gotoWeek") != null) {
			LOG.info("dispatch to current week");
			return "redirect:/view/currentWeek";
		}

		LOG.info("dispatch to month {}", homeForm.getSelectedMonth());

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

		final Collection<String> monthList = new ArrayList<>();

		// Here's when we started to collect the expenses
		LocalDate startDate = LocalDate.of(2020, Month.APRIL, 1);

		String month = null;
		while (startDate.isBefore(LocalDate.now()) || startDate.equals(LocalDate.now())) {

			month = startDate.format(DateTimeFormatter.ofPattern(MONTH_PATTERN, requestLocale));
			LOG.debug("Adding month for selection {}", month);
			monthList.add(month);

			startDate = startDate.plusMonths(1);
		}

		// Initialize Form with latest month
		HomeForm homeForm = new HomeForm();
		homeForm.setSelectedMonth(month);

		List<Double> weeksList = new ArrayList<>();
		double currentMonth = 0.0;
		double lastMonth = 0.0;
		double percentOfLastMonth = 0.0;
		double currentWeek = 0.0;

		String collection = m_FirestoreService.extractCollection(auth);
		if (collection != null) {
			lastMonth = m_FirestoreService.findAmount(ExpensesRequest.forLastMonth(), collection);

			final NavigableMap<Integer, Double> monthWeeks = m_FirestoreService
					.findByWeek(ExpensesRequest.forCurrentMonth(), collection);
			currentMonth = monthWeeks.values().stream().mapToDouble(Double::doubleValue).sum();
			percentOfLastMonth = (currentMonth > 0 && lastMonth > 0) ? (currentMonth / lastMonth) * 100.0 : 0.0;

			final double m = currentMonth;
			if (monthWeeks != null && !monthWeeks.isEmpty()) {
				currentWeek = monthWeeks.lastEntry().getValue().doubleValue();
				weeksList = monthWeeks.values().stream()
						.mapToDouble(x -> (x.doubleValue() > 0 && m > 0) ? (x.doubleValue() / m) * 100.0 : 0.0).boxed()
						.collect(Collectors.toList());
			}
		}

		model.addAttribute("monthSelection", monthList);
		model.addAttribute("homeForm", homeForm);
		model.addAttribute("amountLastMonth", Double.valueOf(lastMonth));
		model.addAttribute("amountCurrentMonth", Double.valueOf(currentMonth));
		model.addAttribute("amountCurrentWeek", Double.valueOf(currentWeek));
		model.addAttribute("percentOfLastMonth", Double.valueOf(percentOfLastMonth));
		model.addAttribute("weeksList", weeksList);

		return "home";
	}

	/**
	 * Shows the expenses by month/year
	 * 
	 * @param month The month
	 * @param year  The year
	 * @param model Model for web view
	 * @param auth  Auth for choosing the collection
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
	 * Edit the expense with the given id and forward to the edit page
	 * 
	 * @param id    The id of the expense that will be modified
	 * @param auth  Auth for choosing the collection
	 * @param model The model to store the loaded expense
	 * @return Template to show
	 */
	@GetMapping("/edit/{id}")
	public String editExpense(@PathVariable(name = "id") String id, Model model, Authentication auth) {
		// http://localhost:8080/money/edit/cOhvwl5ktsrAzAqoRi1D
		String collection = m_FirestoreService.extractCollection(auth);
		Expense expense = m_FirestoreService.getExpense(id, collection);
		if (expense != null) {
			model.addAttribute("expense", expense);
			return "edit";
		}

		return "home";
	}

	/**
	 * Save the data from the form
	 * 
	 * @param id            The Id that was modified
	 * @param req           The HTTP request
	 * @param requestLocale The locale from the browser
	 * @return Template to show
	 */
	@PostMapping("/save/{id}")
	public String saveExpense(@PathVariable(name = "id") String id, final HttpServletRequest req,
			Locale requestLocale) {

		if (req.getParameter("save") != null) {
			// return "redirect:/view/" + localDate.getMonthValue() + "/" +
			// localDate.getYear();
			LOG.info("Saving expense with id {}", id);
		} else if (req.getParameter("delete") != null) {
			// return "redirect:/export/" + localDate.getMonthValue() + "/" +
			// localDate.getYear();
			LOG.info("Deleting expense with id {}", id);
		}

		return "home";
	}

	/**
	 * Allows the export of a month into a CSV file
	 * 
	 * @param month         The month that should be exported
	 * @param year          The year
	 * @param response      The response to write the CSV data
	 * @param auth          Auth for choosing the collection
	 * @param requestLocale The locale of the logged in user
	 * @throws Exception Thrown on error
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
		final String filename = MessageFormat.format("expenses_{0,number,00}-{1,number,0000}.csv",
				Integer.valueOf(month), Integer.valueOf(year));

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
		String budget = m_BudgetService.findBudget(auth, expense);
		return budget == null ? "" : budget;
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

		final Map<Budget, Set<Expense>> budgetExpenses = m_BudgetService.createFromCities(auth, cityMapping.values());
		final Map<Budget, Double> budgetSum = budgetExpenses.entrySet().stream().collect(Collectors
				.toMap(x -> x.getKey(), y -> y.getValue().stream().mapToDouble(Expense::getAmountDouble).sum()));
		for (Entry<Budget, Set<Expense>> budgetEntry : budgetExpenses.entrySet()) {
			budgetEntry.getValue().stream().forEach(x -> {
				if (cityMapping.containsKey(x.getCity())) {
					cityMapping.get(x.getCity()).setBudget(x, budgetEntry.getKey());
				}
			});
		}

		final List<BudgetInfo> budgetInfo = budgetSum.entrySet().stream().filter(x -> x.getValue().doubleValue() > 0)
				.map(x -> new BudgetInfo(x.getKey(), x.getValue())).sorted().collect(Collectors.toList());
		model.addAttribute("budgets", budgetInfo);

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
	 * @param auth  Authentication for choosing the collection
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
