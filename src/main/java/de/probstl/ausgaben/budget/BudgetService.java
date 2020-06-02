package de.probstl.ausgaben.budget;

import java.io.IOException;
import java.io.InputStream;
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
	public Map<Budget, Set<Expense>> createFromExpenses(Authentication auth, Collection<Expense> expenses) {

		final Map<Budget, Set<Expense>> toReturn = new TreeMap<Budget, Set<Expense>>();

		final Collection<Budget> budgets = readDefinition(auth.getName());
		for (Budget budget : budgets) {
			Set<Expense> sum = findMatching(budget, expenses);
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
	public Map<Budget, Set<Expense>> createFromCities(Authentication auth, Collection<CityInfo> cities) {
		final Collection<Expense> allCities = new ArrayList<Expense>();
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
		for (Budget budget : budgets) {
			Collection<Expense> matching = findMatching(budget, Collections.singletonList(expense));
			if (matching.isEmpty()) {
				return budget.getName();
			}
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

		final Set<Expense> toReturn = new HashSet<Expense>();

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
			m_Log.error("invalid pattern: " + regex, e);
		}

		if (budget.getShops() != null && budget.getShops().length > 0) {
			Set<String> shopsLowerCase = Arrays.asList(budget.getShops()).stream().map(x -> x.toLowerCase())
					.collect(Collectors.toSet());
			byShop = expenses.stream().filter(x -> shopsLowerCase.contains(x.getShop().toLowerCase()))
					.collect(Collectors.toList());
		}

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

		final Collection<Expense> toReturn = new ArrayList<Expense>();
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
				m_Log.info("expense {} with message '{}' matches regular expression '{}' with token: '{}'",
						expense.getId(), expense.getMessage(), pattern.pattern(), token);
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
			InputStream stream = BudgetService.class.getResourceAsStream("/budget/" + user + ".json");
			if (stream == null) {
				return Collections.emptyList();
			}
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(stream, new TypeReference<List<Budget>>() {
			});
		} catch (IOException e) {
			m_Log.error("error reading json for budget of user", e);
		}

		return Collections.emptyList();
	}

}
