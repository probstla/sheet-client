package de.probstl.ausgaben;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

import de.probstl.ausgaben.data.Expense;
import de.probstl.ausgaben.data.ExpensesRequest;

/**
 * Service that caches the connection to the Google service
 */
@Component
public class FirestoreService {

	/** Constant for the field message */
	private static final String FIELD_MESSAGE = "message";

	/** Constant for the field shop */
	private static final String FIELD_SHOP = "shop";

	/** Constant for the field amount */
	private static final String FIELD_AMOUNT = "amount";

	/** Constant for the field city */
	private static final String FIELD_CITY = "city";

	/** Constant for the field payment */
	private static final String FIELD_PAYMENT = "payment";

	/** Constant for the field timestamp */
	private static final String FIELD_TIMESTAMP = "timestamp";

	/** Constant for the field budget */
	private static final String FIELD_BUDGET = "budget";

	/** The reference holds the connection */
	private final AtomicReference<Firestore> m_Ref = new AtomicReference<>();

	/** Logger for this class */
	private static final Logger LOG = LoggerFactory.getLogger(FirestoreService.class);

	/** The system time zone where the application is running */
	private final ZoneId m_SystemTimezone = ZoneId.systemDefault();

	/** The time zone where the data was created */
	private final ZoneId m_HomeTimezone = ZoneId.of("Europe/Berlin");

	/**
	 * Returns a existing or cached connection to the service
	 * 
	 * @return Service connection
	 */
	protected Firestore getFirestoreService() {

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

	/**
	 * Extract the used collection from the authentication
	 * 
	 * @param auth The authenticated user
	 * @return The name of the collection or <code>null</code> if there is no role
	 *         associated
	 */
	public String extractCollection(Authentication auth) {
		Optional<? extends GrantedAuthority> authority = auth.getAuthorities().stream().findFirst();
		if (authority.isPresent()) {
			String collection = authority.get().getAuthority();
			LOG.info("Using collection {} for user {}", collection, auth.getName());
			return collection;
		}

		LOG.warn("No authority for user {} found. Showing empty data!", auth.getName());
		return null;
	}

	/**
	 * Update the expense
	 * 
	 * @param expense The expense to be updated
	 * @param auth    Auth for getting the collection
	 * @return if successfull true otherwise false
	 */
	public boolean updateExpense(Expense expense, Authentication auth) {

		String collection = extractCollection(auth);
		if (collection == null) {
			return false;
		}

		if (expense.getId() == null) {
			LOG.warn("Expense has no Id");
			return false;
		}

		Map<String, Object> fields = new HashMap<>();
		fields.put(FIELD_AMOUNT, expense.getAmountDouble());

		if (expense.getBudget() != null && !expense.getBudget().isEmpty()) {
			fields.put(FIELD_BUDGET, expense.getBudget()); // new field with version 1.2.0
		}

		fields.put(FIELD_CITY, expense.getCity());
		fields.put(FIELD_MESSAGE, expense.getMessage());
		fields.put(FIELD_PAYMENT, expense.getPayment());
		fields.put(FIELD_SHOP, expense.getShop());
		fields.put(FIELD_TIMESTAMP, expense.getTimestamp());

		DocumentReference docRef = getFirestoreService().collection(collection).document(expense.getId());
		ApiFuture<WriteResult> result = docRef.update(fields);

		try {
			WriteResult writeResult = result.get();
			LOG.info("Document {} created at {}", docRef.getId(), writeResult.getUpdateTime());
			return true;
		} catch (InterruptedException e) {
			LOG.warn("Interrupt while updating expense {}", expense.getId());
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			LOG.error("Error while updating expense", e.getCause());
		}

		return false;
	}

	/**
	 * Create a new document and store it in the firebase database
	 * 
	 * @param expense    The expense that must be saved
	 * @param locale     The locale of the client to parse the amount
	 * @param collection The collection in which the expense must be saved
	 * @return Returns <code>true</code> if the creation was successful otherwise
	 *         <code>false</code>
	 */
	public boolean createExpense(Expense expense, Locale locale, String collection) {

		NumberFormat format = NumberFormat.getNumberInstance(locale);
		Number betrag = null;
		try {
			betrag = format.parse(expense.getAmount());
		} catch (ParseException e1) {
			LOG.error(String.format("No valid number: %s", expense.getAmount()), e1);
			return false;
		}

		LOG.info("Formatted value of {} is {}", expense.getAmount(), betrag);

		Map<String, Object> data = new HashMap<>();
		data.put(FIELD_MESSAGE, expense.getMessage());
		data.put(FIELD_SHOP, expense.getShop());
		data.put(FIELD_AMOUNT, betrag);
		data.put(FIELD_CITY, expense.getCity());
		data.put(FIELD_PAYMENT, expense.getPayment());

		if (expense.getBudget() != null && !expense.getBudget().isEmpty()) {
			data.put(FIELD_BUDGET, expense.getBudget()); // new field with version 1.2.0
		}

		if (expense.getTimestamp() == null) {
			data.put(FIELD_TIMESTAMP, new Date());
		} else {
			data.put(FIELD_TIMESTAMP, expense.getTimestamp());
		}

		DocumentReference docRef = getFirestoreService().collection(collection).document();
		ApiFuture<WriteResult> result = docRef.set(data);

		try {
			WriteResult writeResult = result.get();
			LOG.info("Document {} created at {}", docRef.getId(), writeResult.getUpdateTime());
			return true;
		} catch (InterruptedException e) {
			LOG.warn("Interrupt while creating new expense");
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			LOG.error("Error while creating expense", e.getCause());
		}

		return true;
	}

	/**
	 * Load an expense with an specific Id from the collection
	 * 
	 * @param id         The Id of the expense
	 * @param collection The collection from which the expense is loaded
	 * @return An expense or null if the id was not found
	 */
	public Expense getExpense(String id, String collection) {
		final Instant start = Instant.now();
		Duration queryTime = null;

		LOG.info("Find expense with id {} in collection {}", id, collection);

		DocumentSnapshot result = null;
		try {
			ApiFuture<DocumentSnapshot> future = getFirestoreService().collection(collection).document(id).get();

			try {
				result = future.get();
			} catch (InterruptedException e) {
				LOG.warn("waiting for result interrupted!");
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				LOG.error("could not retrieve result from firestore!", e.getCause());
			}
		} finally {
			queryTime = Duration.between(start, Instant.now());
		}

		if (result == null || !result.exists()) {
			LOG.warn("Expense with id {} not found!", id);
			return null;
		}

		LOG.info("Expense with id {} loaded in {} ms. Snapshot timestamp: {}", id, Long.valueOf(queryTime.toMillis()),
				result.getReadTime());

		return createFromDocument(result);
	}

	/**
	 * Create a expense object from the firestore data
	 * 
	 * @param document The document
	 * @return A new expense object
	 */
	protected Expense createFromDocument(DocumentSnapshot document) {

		Expense expense = new Expense(document.getId());
		expense.setShop(document.getString(FIELD_SHOP));
		expense.setCity(document.getString(FIELD_CITY));
		expense.setMessage(document.getString(FIELD_MESSAGE));
		expense.setAmountDouble(document.getDouble(FIELD_AMOUNT));
		expense.setPayment(document.getString(FIELD_PAYMENT));
		expense.setBudget(document.getString(FIELD_BUDGET));

		Instant originalTimestamp = document.getDate(FIELD_TIMESTAMP).toInstant();
		LocalDateTime ldt = LocalDateTime.ofInstant(originalTimestamp, m_HomeTimezone);
		ZonedDateTime zdt = ZonedDateTime.of(ldt, m_SystemTimezone);
		expense.setTimestamp(Date.from(zdt.toInstant()));

		LOG.debug(
				"Expense id '{}' created at '{}' with date '{}' from shop '{}' and amount '{}' EUR for budget '{}'. System ZoneId is {}, Home ZoneId {}.",
				document.getId(), document.getCreateTime(), expense.getTimestamp(), expense.getShop(),
				expense.getAmountDouble(), expense.getBudget(), m_SystemTimezone, m_HomeTimezone);

		return expense;
	}

	/**
	 * Find the sum of expenses in an specific time interval for a specific
	 * collection
	 * 
	 * @param request    The time interval
	 * @param collection The collection
	 * @return Amount of expenses in the interval
	 */
	public double findAmount(ExpensesRequest request, String collection) {
		final Instant start = Instant.now();
		Duration queryTime = null;

		LOG.info("Find amounts for {} in collection {}", request, collection);

		QuerySnapshot queryResult = null;
		try {
			ApiFuture<QuerySnapshot> future = getFirestoreService().collection(collection).select(FIELD_AMOUNT)
					.whereGreaterThanOrEqualTo(FIELD_TIMESTAMP, request.getBeginDate())
					.whereLessThanOrEqualTo(FIELD_TIMESTAMP, request.getEndDate()).orderBy(FIELD_TIMESTAMP).get();

			try {
				queryResult = future.get();
			} catch (InterruptedException e) {
				LOG.warn("waiting for result interrupted!");
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				LOG.error("could not retrieve result from firestore!", e.getCause());
			}
		} finally {
			queryTime = Duration.between(start, Instant.now());
		}

		if (queryResult == null || queryResult.isEmpty()) {
			return Double.valueOf(0);
		}

		LOG.info("Query executed in {} ms. Snapshot timestamp: {}", Long.valueOf(queryTime.toMillis()),
				queryResult.getReadTime());

		BigDecimal sum = new BigDecimal(0);
		for (DocumentSnapshot document : queryResult.getDocuments()) {
			Double amountValue = document.getDouble(FIELD_AMOUNT);
			if (amountValue != null) {
				sum = sum.add(BigDecimal.valueOf(amountValue.doubleValue()));
			}
		}

		return sum.doubleValue();
	}

	/**
	 * Find the Amounts by week
	 * 
	 * @param request    The time interval
	 * @param collection The collection
	 * @return Map of the week as key and the expense amount within the week
	 */
	public NavigableMap<Integer, Double> findByWeek(ExpensesRequest request, String collection) {
		final Instant start = Instant.now();
		Duration queryTime = null;

		LOG.info("Find expenses in week from {} in collection {}", request, collection);

		final NavigableMap<Integer, Double> toReturn = new TreeMap<>();

		QuerySnapshot queryResult = null;
		try {
			ApiFuture<QuerySnapshot> future = getFirestoreService().collection(collection)
					.select(FIELD_AMOUNT, FIELD_TIMESTAMP)
					.whereGreaterThanOrEqualTo(FIELD_TIMESTAMP, request.getBeginDate())
					.whereLessThanOrEqualTo(FIELD_TIMESTAMP, request.getEndDate()).orderBy(FIELD_TIMESTAMP).get();

			try {
				queryResult = future.get();
			} catch (InterruptedException e) {
				LOG.warn("waiting for result interrupted!");
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				LOG.error("could not retrieve result from firestore!", e.getCause());
			}
		} finally {
			queryTime = Duration.between(start, Instant.now());
		}

		if (queryResult == null || queryResult.isEmpty()) {
			return toReturn;
		}

		LOG.info("Query executed in {} ms. Snapshot timestamp: {}", Long.valueOf(queryTime.toMillis()),
				queryResult.getReadTime());

		for (QueryDocumentSnapshot documentSnapshot : queryResult.getDocuments()) {
			Instant timestamp = documentSnapshot.getTimestamp(FIELD_TIMESTAMP).toDate().toInstant();
			Integer week = Integer.valueOf(timestamp.atZone(m_SystemTimezone).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			BigDecimal value = BigDecimal.valueOf(documentSnapshot.getDouble(FIELD_AMOUNT));

			Double sum = toReturn.get(week);
			if (sum == null) {
				toReturn.put(week, Double.valueOf(value.doubleValue()));
			} else {
				double newValue = BigDecimal.valueOf(sum.doubleValue()).add(value).doubleValue();
				toReturn.put(week, Double.valueOf(newValue));
			}
		}

		// Fill weeks without found expenses
		ZonedDateTime currentDateTime = request.getBeginDate().toInstant().atZone(m_SystemTimezone);
		while (currentDateTime.isBefore(request.getEndDate().toInstant().atZone(m_SystemTimezone))) {
			int currentWeek = currentDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
			if (!toReturn.containsKey(Integer.valueOf(currentWeek))) {
				toReturn.put(Integer.valueOf(currentWeek), Double.valueOf(0.0));
			}
			currentDateTime = currentDateTime.plusDays(1);
		}

		return toReturn;
	}

	/**
	 * Find all expenses for a collection for the given time interval
	 * 
	 * @param request    The begin and end interval
	 * @param collection Name of the collection
	 * @return Collection of expenses. Never <code>null</code> but empty if there
	 *         was an error or no data was found
	 */
	public Collection<Expense> findBetween(ExpensesRequest request, String collection) {
		LOG.info("Find expenses from {} in collection {}", request, collection);

		final Instant start = Instant.now();
		Duration queryTime = null;

		QuerySnapshot queryResult = null;
		try {
			ApiFuture<QuerySnapshot> future = getFirestoreService().collection(collection)
					.whereGreaterThanOrEqualTo(FIELD_TIMESTAMP, request.getBeginDate())
					.whereLessThanOrEqualTo(FIELD_TIMESTAMP, request.getEndDate()).orderBy(FIELD_TIMESTAMP).get();

			try {
				queryResult = future.get();
			} catch (InterruptedException e) {
				LOG.warn("waiting for result interrupted!");
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				LOG.error("could not retrieve result from firestore!", e.getCause());
			}
		} finally {
			queryTime = Duration.between(start, Instant.now());
		}

		if (queryResult == null || queryResult.isEmpty()) {
			return Collections.emptyList();
		}

		LOG.info("Query executed in {} ms. Snapshot timestamp: {}", Long.valueOf(queryTime.toMillis()),
				queryResult.getReadTime());

		final Collection<Expense> toReturn = new ArrayList<>();

		for (DocumentSnapshot document : queryResult.getDocuments()) {
			toReturn.add(createFromDocument(document));
		}

		return toReturn;
	}
}
