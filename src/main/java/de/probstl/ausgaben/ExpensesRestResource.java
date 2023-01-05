package de.probstl.ausgaben;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.probstl.ausgaben.data.Expense;

@RestController
@RequestMapping("/rest")
public class ExpensesRestResource {

	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ExpensesRestResource.class);

	/** The service for reading and writing data to firestore */
	@Autowired
	private FirestoreService m_FirestoreService;

	@PostMapping(path = "/create")
	public ResponseEntity<String> createAusgabe(@Valid @RequestBody Expense expense, Locale requestLocale,
			Authentication authentication) {

		Optional<? extends GrantedAuthority> authority = authentication.getAuthorities().stream().findFirst();
		if (!authority.isPresent()) {
			LOG.warn("No authority for user {} found. Showing empty data!", authentication.getName());
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		String collection = authority.get().getAuthority();

		LOG.info("Using collection {} for user {}", collection, authentication.getName());

		NumberFormat format = NumberFormat.getNumberInstance(requestLocale);
		try {
			double betrag = format.parse(expense.getAmount()).doubleValue();
			expense.setAmountDouble(Double.valueOf(betrag));
		} catch (ParseException e1) {
			LOG.error(String.format("No valid number: %s", expense.getAmount()), e1);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		LOG.info("New expense with description '{}' in shop '{}' with amount '{}' payed with {} in locale {}.",
				expense.getMessage(), expense.getShop(), expense.getAmountDouble(), expense.getPayment(),
				requestLocale);

		if (!m_FirestoreService.createExpense(expense, requestLocale, collection)) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (!"flo".equals(authentication.getName())) {
			return new ResponseEntity<>(HttpStatus.OK);
		}

		// AWS Export removed with version 1.2.3!
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
