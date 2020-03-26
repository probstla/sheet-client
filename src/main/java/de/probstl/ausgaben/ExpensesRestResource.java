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
import org.springframework.web.bind.annotation.RestController;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;

import de.probstl.ausgaben.data.Expense;

@RestController
public class ExpensesRestResource {

	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ExpensesRestResource.class);

	@Autowired
	private FirestoreConfigService m_FirestoreService;

	@PostMapping("/expense/create")
	public ResponseEntity<?> createAusgabe(@Valid @RequestBody Expense ausgabe, Locale requestLocale,
			Authentication auth) {

		Optional<? extends GrantedAuthority> authority = auth.getAuthorities().stream().findFirst();
		if (!authority.isPresent()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		String collection = authority.get().getAuthority();
		LOG.info("Using collection {} for user {}", collection, auth.getName());

		DocumentReference docRef = m_FirestoreService.getService().collection(collection).document();

		LOG.info("New expense with description '{}' in shop '{}' with amount '{}' in locale {}.", ausgabe.getMessage(),
				ausgabe.getShop(), ausgabe.getAmount(), requestLocale);

		NumberFormat format = NumberFormat.getNumberInstance(requestLocale);
		Number betrag = null;
		try {
			betrag = format.parse(ausgabe.getAmount());
		} catch (ParseException e1) {
			LOG.error("No valid number: " + ausgabe.getAmount(), e1);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		LOG.info("Formatted value of {} is {}", ausgabe.getAmount(), betrag);

		Map<String, Object> data = new HashMap<>();
		data.put("message", ausgabe.getMessage());
		data.put("shop", ausgabe.getShop());
		data.put("amount", betrag);
		data.put("city", ausgabe.getCity());

		if (ausgabe.getTimestamp() == null) {
			data.put("timestamp", new Date());
		} else {
			data.put("timestamp", ausgabe.getTimestamp());
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
