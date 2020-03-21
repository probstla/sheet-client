package de.probstl.ausgaben;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

import de.probstl.ausgaben.data.Expense;

@RestController
public class ExpensesRestResource {

	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ExpensesRestResource.class);

	@Autowired
	private FirestoreConfigService m_FirestoreService;

	@GetMapping("/ausgaben/week")
	public List<Expense> currentWeek() {

		LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDateTime begin = LocalDateTime.of(monday, LocalTime.of(0, 0, 0));
		LocalDateTime end = begin.plusDays(7);

		LOG.info("Find expenses from {} to {}", begin, end);

		final List<Expense> toReturn = new ArrayList<>();

		try {
			ApiFuture<QuerySnapshot> future = m_FirestoreService.getService().collection("ausgaben")
					.whereGreaterThan("timestamp",
							Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant()))
					.whereLessThan("timestamp", Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant()))
					.orderBy("timestamp").get();

			QuerySnapshot queryResult = future.get();
			LOG.info("Read time {}", queryResult.getReadTime());
			for (DocumentSnapshot document : queryResult.getDocuments()) {
				Expense ausgabe = new Expense();
				ausgabe.setShop(document.getString("shop"));
				ausgabe.setCity(document.getString("city"));
				ausgabe.setMessage(document.getString("message"));
				ausgabe.setAmountDouble(document.getDouble("amount"));
				ausgabe.setTimestamp(document.getDate("timestamp"));
				toReturn.add(ausgabe);
			}
		} catch (InterruptedException e) {
			LOG.error("Unterbrochen", e);
		} catch (ExecutionException e) {
			LOG.error("Fehler in der Ausführung", e);
		}

		return toReturn;
	}

	@PostMapping("/ausgaben/new")
	public ResponseEntity<?> createAusgabe(@Valid @RequestBody Expense ausgabe, Locale requestLocale) {
		DocumentReference docRef = m_FirestoreService.getService().collection("ausgaben").document();

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
