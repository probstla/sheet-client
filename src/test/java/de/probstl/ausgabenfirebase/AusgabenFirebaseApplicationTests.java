package de.probstl.ausgabenfirebase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;

import de.probstl.ausgaben.data.Expense;

class AusgabenFirebaseApplicationTests {

	@Test
	void testFindCurrentWeek() {

		LocalDate now = LocalDate.now();
		System.out.println(now);

		LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDateTime begin = LocalDateTime.of(monday, LocalTime.of(0, 0, 0));
		LocalDateTime end = begin.plusDays(7);
		System.out.println(begin + " >= t <= " + end);
	}

	@Disabled
	@Test
	void findAusgaben() {

		LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDateTime begin = LocalDateTime.of(monday, LocalTime.of(0, 0, 0));
		LocalDateTime end = begin.plusDays(7);

		FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId("probstl")
				.build();
		Firestore service = firestoreOptions.getService();

		try {
			ApiFuture<QuerySnapshot> querySnapshot = service.collection("ausgaben")
					.whereGreaterThan("timestamp",
							Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant()))
					.whereLessThan("timestamp", Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant()))
					.orderBy("timestamp").get();

			for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
				Expense ausgabe = new Expense(document.getId());
				ausgabe.setShop(document.getString("shop"));
				ausgabe.setCity(document.getString("city"));
				ausgabe.setMessage(document.getString("message"));
				ausgabe.setAmountDouble(document.getDouble("amount"));
				ausgabe.setTimestamp(document.getDate("timestamp"));
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
