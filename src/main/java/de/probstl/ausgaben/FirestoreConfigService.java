package de.probstl.ausgaben;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

/**
 * Service that caches the connection to the Google service
 */
@Component
public class FirestoreConfigService {

	/** The reference holds the connection */
	private final AtomicReference<Firestore> m_Ref = new AtomicReference<>();

	/**
	 * Returns a existing or cached connection to the service
	 * 
	 * @return Service connection
	 */
	public Firestore getService() {

		if (m_Ref.get() != null) {
			return m_Ref.get();
		}

		FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId("probstl")
				.build();
		Firestore service = firestoreOptions.getService();

		if (m_Ref.compareAndSet(null, service)) {
			return service;
		}

		return m_Ref.get();
	}
}
