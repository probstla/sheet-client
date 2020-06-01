package de.probstl.ausgaben.budget;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.probstl.ausgaben.data.Expense;

public class BudgetService {

	/** Logger */
	private static Logger m_Log = LoggerFactory.getLogger(BudgetService.class);

	/**
	 * Read the budget definition and try to match the given expenses.
	 * 
	 * @param expenses The expenses. Can be <code>null</code> or empty
	 * @return A Map with the budget and the corresponding amount
	 */
	public Map<Budget, Double> createFromExpenses(Authentication auth, Collection<Expense> expenses) {
		return null;
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
