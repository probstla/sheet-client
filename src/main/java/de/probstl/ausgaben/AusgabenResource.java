package de.probstl.ausgaben;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;

import de.probstl.ausgaben.data.Ausgabe;

@RestController
public class AusgabenResource {

	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(AusgabenResource.class);

	@Autowired
	private FirestoreConfigService m_FirestoreService;

	/**
	 * Erstellt eine neue Ausgabe
	 * 
	 * @param ausgabe
	 */
	@PostMapping("/ausgaben/new")
	public ResponseEntity<?> createAusgabe(@Valid @RequestBody Ausgabe ausgabe, Locale requestLocale) {
		DocumentReference docRef = m_FirestoreService.getService().collection("ausgaben").document();

		LOG.info("New Ausgabe with description '{}' in shop '{}' with amount '{}' in locale {}.", ausgabe.getMessage(),
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
