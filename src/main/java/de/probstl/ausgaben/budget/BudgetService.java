package de.probstl.ausgaben.budget;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.probstl.ausgaben.data.Expense;
import de.probstl.ausgaben.mail.CityInfo;

@Component
public class BudgetService {

	/** Logger */
	private static Logger m_Log = LoggerFactory.getLogger(BudgetService.class);

	/**
	 * Read the budget definition and try to match the given expenses.
	 * 
	 * @param auth     Authenticated user
	 * @param expenses The expenses. Can be <code>null</code> or empty
	 * @return A Map with the budget and the corresponding amount
	 */
	public Map<Budget, Double> createFromExpenses(Authentication auth, Collection<Expense> expenses) {

		final Map<Budget, Double> toReturn = new TreeMap<Budget, Double>();

		final Collection<Budget> budgets = readDefinition(auth.getName());
		for (Budget budget : budgets) {
			Double sum = findMatching(budget, expenses).stream().mapToDouble(x -> x.getAmountDouble()).sum();
			toReturn.put(budget, sum);
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
	public Map<Budget, Double> createFromCities(Authentication auth, Collection<CityInfo> cities) {
		final Collection<Expense> allCities = new ArrayList<Expense>();
		cities.stream().forEach(x -> allCities.addAll(x.getExpenses()));
		return createFromExpenses(auth, allCities);
	}

	/**
	 * Find the matching expenses by the given budget
	 * 
	 * @param budget   the budget definition
	 * @param expenses the found expenses that are checked against the budget
	 * @return matching expenses, never <code>null</code>
	 */
	Collection<Expense> findMatching(Budget budget, Collection<Expense> expenses) {

		Collection<Expense> filtered = expenses;

		String regex = budget.getMessageRegex();
		if (regex != null && !regex.trim().isEmpty()) {
			Pattern pattern = null;
			try {
				pattern = Pattern.compile(regex);
				filtered = findByRegex(pattern, expenses);
			} catch (PatternSyntaxException e) {
				m_Log.error("invalid pattern: " + regex, e);
			}
		}

		if (budget.getShops() == null || budget.getShops().length == 0) {
			return filtered; // Without shops just filter by regex
		}

		Set<String> shopsLowerCase = Arrays.asList(budget.getShops()).stream().map(x -> x.toLowerCase())
				.collect(Collectors.toSet());
		return filtered.stream().filter(x -> shopsLowerCase.contains(x.getShop().toLowerCase()))
				.collect(Collectors.toList());
	}

	/**
	 * Find the expenses matching the given regular expression
	 * 
	 * @param pattern  The regular expression from the budget
	 * @param expenses The expenses that are checked against the budget
	 * @return matching expenses, never <code>null</code>
	 */
	private Collection<Expense> findByRegex(Pattern pattern, Collection<Expense> expenses) {

		final Collection<Expense> toReturn = new ArrayList<Expense>();
		for (Expense expense : expenses) {
			Matcher matcher = pattern.matcher(expense.getMessage());
			if (matcher.matches()) {
				toReturn.add(expense);
			}
		}

		return toReturn;
	}

	/**
	 * Read the budget definition for the authenticated user
	 * 
	 * @param auth The authenticated user
	 * @return List of budgets or an empty list
	 */
	Collection<Budget> readDefinition(String user) {

		try {
			File definition = ResourceUtils.getFile("classpath:budget/" + user.concat(".json"));
			if (definition == null) {
				return Collections.emptyList();
			}
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(definition, new TypeReference<List<Budget>>() {
			});
		} catch (IOException e) {
			m_Log.error("error reading json for budget of user", e);
		}

		return Collections.emptyList();
	}

}
