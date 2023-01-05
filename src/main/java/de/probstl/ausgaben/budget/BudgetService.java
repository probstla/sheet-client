package de.probstl.ausgaben.budget;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.probstl.ausgaben.data.Expense;
import de.probstl.ausgaben.mail.CityInfo;

@Component
public class BudgetService {

	/** Logger */
	private static Logger m_Log = LoggerFactory.getLogger(BudgetService.class);

	/** Once read the budgets for each user are stored in this map */
	private final Map<String, Collection<Budget>> m_Budgets = Collections
			.synchronizedMap(new HashMap<String, Collection<Budget>>());

	/**
	 * Read the budget definition and try to match the given expenses.
	 * 
	 * @param auth     Authenticated user
	 * @param expenses The expenses. Can be <code>null</code> or empty
	 * @return A Map with the budget and the corresponding amount
	 */
	public Map<Budget, Set<Expense>> createFromExpenses(Authentication auth, Collection<Expense> expenses) {

		final Map<Budget, Set<Expense>> toReturn = new TreeMap<>();

		final Collection<Budget> budgets = readDefinition(auth.getName());
		final Optional<Budget> fallback = findFallback(budgets);

		final Set<Expense> remaining = new HashSet<>();
		remaining.addAll(expenses);

		for (Budget budget : budgets) {
			Set<Expense> sum = findMatching(budget, expenses);
			toReturn.put(budget, sum);

			// Remove the found expenses from the list
			remaining.removeAll(sum);
		}

		if (fallback.isPresent()) {
			// these are all expenses that does not have any budget - they match the
			// fallback budget
			toReturn.put(fallback.get(), remaining);
		}

		return toReturn;
	}

	/**
	 * Create the budgets from the city info object
	 * 
	 * @param auth   Authenticated user
	 * @param cities Collection of cities which contains expenses
	 * @return A Map with the budget and the corresponding amount
	 */
	public Map<Budget, Set<Expense>> createFromCities(Authentication auth, Collection<CityInfo> cities) {
		final Collection<Expense> allCities = new ArrayList<>();
		cities.stream().forEach(x -> allCities.addAll(x.getExpenses()));
		return createFromExpenses(auth, allCities);
	}

	/**
	 * Find a budget for the given expense
	 * 
	 * @param auth    Authenticated user
	 * @param expense The expense where the budget is asked
	 * @return Name of the budget or <code>null</code> if there is no matching
	 *         budget
	 */
	public String findBudget(Authentication auth, Expense expense) {
		final Collection<Budget> budgets = readDefinition(auth.getName());
		final Optional<Budget> fallback = findFallback(budgets);

		if(budgets == null) {
			return null;
		}

		if (expense.getBudget() != null) {
			Optional<Budget> foundBudget = budgets.stream().filter(x -> x.getName().equalsIgnoreCase(expense.getBudget())).findFirst();
			if(foundBudget.isPresent()) {
				return foundBudget.get().getName();
			}
		}

		for (Budget budget : budgets) {
			Collection<Expense> matching = findMatching(budget, Collections.singletonList(expense));
			if (!matching.isEmpty()) {
				return budget.getName();
			}
		}

		if (fallback.isPresent()) {
			return fallback.get().getName();
		}

		return null;
	}

	/**
	 * Find the matching expenses by the given budget. The expenses are first
	 * checked against a regular expression. If the budget has no regular expression
	 * a hash-tag based regular expression is created. After that the expeses are
	 * checked against the shops given in the budget. All unique expenses (based on
	 * id) are returned.
	 * 
	 * @param budget   the budget definition
	 * @param expenses the found expenses that are checked against the budget
	 * @return matching expenses, never <code>null</code>
	 */
	Set<Expense> findMatching(Budget budget, Collection<Expense> expenses) {

		final Set<Expense> toReturn = new HashSet<>();

		String regex = budget.getMessageRegex();
		if (regex == null || regex.trim().isEmpty()) {
			regex = ".*(#".concat(budget.getName().replaceAll("\\s", "").toLowerCase()).concat(").*");
		}

		Collection<Expense> byRegex = Collections.emptyList();
		Collection<Expense> byShop = Collections.emptyList();

		Pattern pattern = null;
		try {
			pattern = Pattern.compile(regex);
			byRegex = findByRegex(pattern, expenses);
		} catch (PatternSyntaxException e) {
			m_Log.error(String.format("invalid pattern: %s", regex), e);
		}

		if (budget.getShops() != null && budget.getShops().length > 0) {
			Set<String> shopsLowerCase = Arrays.stream(budget.getShops()).map(String::toLowerCase)
					.collect(Collectors.toSet());
			byShop = expenses.stream().filter(x -> shopsLowerCase.contains(x.getShop().toLowerCase()))
					.collect(Collectors.toList());
		}

		// Add the expenses with a direct budget match to the returning set
		toReturn.addAll(expenses.stream().filter(x -> budget.getName().equals(x.getBudget())).collect(Collectors.toSet()));
		
		// Add all unique expenses to the result
		toReturn.addAll(byRegex);
		toReturn.addAll(byShop);
		return toReturn;
	}

	/**
	 * Find the expenses matching the given regular expression
	 * 
	 * @param pattern  The regular expression from the budget
	 * @param expenses The expenses that are checked against the budget
	 * @return matching expenses, never <code>null</code>
	 */
	private Collection<Expense> findByRegex(Pattern pattern, Collection<Expense> expenses) {

		final Collection<Expense> toReturn = new ArrayList<>();
		for (Expense expense : expenses) {
			if (expense.getMessage() == null) {
				continue;
			}

			Matcher matcher = pattern.matcher(expense.getMessage());
			if (matcher.matches()) {
				String token = "";
				if (matcher.groupCount() > 0) {
					token = matcher.group(1);
				}
				String regex = pattern.pattern();
				m_Log.info("expense {} with message '{}' matches regular expression '{}' with token: '{}'",
						expense.getId(), expense.getMessage(), regex, token);
				toReturn.add(expense);
			}
		}

		return toReturn;
	}

	/**
	 * Find a fallback Budget of the given Budget List
	 * 
	 * @param budgetList The list of budgets to search
	 * @return A optional with the given fallback or an empty optional
	 */
	private Optional<Budget> findFallback(Collection<Budget> budgetList) {
		if (budgetList == null) {
			return Optional.empty();
		}

		return budgetList.stream().filter(Budget::isFallback).findFirst();
	}

	/**
	 * Read the budget definition for the authenticated user
	 * 
	 * @param auth The authenticated user
	 * @return List of budgets or an empty list
	 */
	Collection<Budget> readDefinition(String user) {

		Collection<Budget> budgets = m_Budgets.get(user);
		if (budgets != null) {
			return budgets;
		}

		final InputStream stream = BudgetService.class.getResourceAsStream("/budget/" + user + ".json");
		if (stream == null) {
			return Collections.emptyList(); // No such file
		}

		final ObjectMapper mapper = new ObjectMapper();
		try {
			budgets = mapper.readValue(stream, new TypeReference<List<Budget>>() {
			});
		} catch (IOException e) {
			m_Log.error("error reading json for budget of user", e);
		}

		if (budgets != null) {
			// Store the Budgets in the Map - if there was already a value return this value
			Collection<Budget> oldValue = m_Budgets.putIfAbsent(user, budgets);
			return oldValue != null ? oldValue : budgets;
		}

		return Collections.emptyList();
	}

}
