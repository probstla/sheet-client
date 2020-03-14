package de.probstl.ausgaben;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

@Component
public class FirestoreConfigService {

	private final AtomicReference<Firestore> m_Ref = new AtomicReference<>();
	
	public Firestore getService() {
		
		if(m_Ref.get() != null)
		{
			return m_Ref.get();
		}
		
		FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId("probstl")
				.build();
		Firestore service = firestoreOptions.getService();
		
		if(m_Ref.compareAndSet(null, service))
		{
			return service;
		}
		
		return m_Ref.get();
	}
}
