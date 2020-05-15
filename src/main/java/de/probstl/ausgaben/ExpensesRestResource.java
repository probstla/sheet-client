package de.probstl.ausgaben;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;

import de.probstl.ausgaben.data.Expense;

@RestController
@RequestMapping("/rest")
public class ExpensesRestResource {

	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ExpensesRestResource.class);

	@Autowired
	private FirestoreConfigService m_FirestoreService;

	@PostMapping(path = "/create")
	public ResponseEntity<?> createAusgabe(@Valid @RequestBody Expense expense, Locale requestLocale,
			Authentication authentication) {

		Optional<? extends GrantedAuthority> authority = authentication.getAuthorities().stream().findFirst();
		if (!authority.isPresent()) {
			LOG.warn("No authority for user {} found. Showing empty data!", authentication.getName());
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		String collection = authority.get().getAuthority();

		LOG.info("Using collection {} for user {}", collection, authentication.getName());

		DocumentReference docRef = m_FirestoreService.getService().collection(collection).document();

		String payment = expense.getPayment();
		if (payment == null) {
			payment = "cash";
		}

		LOG.info("New expense with description '{}' in shop '{}' with amount '{}' payed with {} in locale {}.",
				expense.getMessage(), expense.getShop(), expense.getAmount(), payment, requestLocale);

		NumberFormat format = NumberFormat.getNumberInstance(requestLocale);
		Number betrag = null;
		try {
			betrag = format.parse(expense.getAmount());
		} catch (ParseException e1) {
			LOG.error("No valid number: " + expense.getAmount(), e1);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		LOG.info("Formatted value of {} is {}", expense.getAmount(), betrag);

		Map<String, Object> data = new HashMap<>();
		data.put("message", expense.getMessage());
		data.put("shop", expense.getShop());
		data.put("amount", betrag);
		data.put("city", expense.getCity());
		data.put("payment", payment);

		if (expense.getTimestamp() == null) {
			data.put("timestamp", new Date());
		} else {
			data.put("timestamp", expense.getTimestamp());
		}

		ApiFuture<WriteResult> result = docRef.set(data);

		try {
			LOG.info("Update time : " + result.get().getUpdateTime());
		} catch (InterruptedException e) {
			LOG.error("Unterbrochen", e);
		} catch (ExecutionException e) {
			LOG.error("Fehler in der Ausführung", e);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
