package de.probstl.ausgaben;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
	public void createAusgabe(@Valid @RequestBody Ausgabe ausgabe) {
		DocumentReference docRef = m_FirestoreService.getService().collection("ausgaben").document();
		Map<String, Object> data = new HashMap<>();
		data.put("beschreibung", ausgabe.getBeschreibung());
		data.put("geschäft", ausgabe.getGeschaeft());
		data.put("betrag", ausgabe.getBetrag());
		data.put("zeitpunkt", new Date());

		ApiFuture<WriteResult> result = docRef.set(data);

		try {
			LOG.info("Update time : " + result.get().getUpdateTime());
		} catch (InterruptedException e) {
			LOG.error("Unterbrochen", e);
		} catch (ExecutionException e) {
			LOG.error("Fehler in der Ausführung", e);
		}
	}
}
