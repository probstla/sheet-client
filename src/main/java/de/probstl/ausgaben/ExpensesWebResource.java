package de.probstl.ausgaben;

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
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import de.probstl.ausgaben.data.Expense;
import de.probstl.ausgaben.mail.CityInfo;
import de.probstl.ausgaben.mail.MailInfo;

/**
 * Controller for web-access
 */
@Controller
public class ExpensesWebResource {

	/** Service to access Google data */
	@Autowired
	private FirestoreConfigService m_FirestoreService;

	/** The Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ExpensesWebResource.class);

	/**
	 * Returns the data of the current week
	 * 
	 * @return currentWeek
	 */
	@GetMapping("/expenses/show/currentWeek")
	public String showWeek(Model model) {

		LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		LocalDateTime begin = LocalDateTime.of(monday, LocalTime.of(0, 0, 0));
		Date beginDate = Date.from(ZonedDateTime.of(begin, ZoneId.systemDefault()).toInstant());
		model.addAttribute("beginDate", beginDate);

		LocalDateTime end = begin.plusDays(7);
		Date endDate = Date.from(ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant());
		model.addAttribute("endDate", endDate);
		model.addAttribute("currency", "€");

		final Map<String, CityInfo> cityMapping = new HashMap<>();

		LOG.info("Find expenses from {} to {}", begin, end);

		try {
			ApiFuture<QuerySnapshot> future = m_FirestoreService.getService().collection("ausgaben")
					.whereGreaterThan("timestamp", beginDate).whereLessThan("timestamp", endDate).orderBy("timestamp")
					.get();

			QuerySnapshot queryResult = future.get();
			LOG.info("Read time {}", queryResult.getReadTime());
			for (DocumentSnapshot document : queryResult.getDocuments()) {

				String city = document.getString("city");

				CityInfo cityInfo;
				if (cityMapping.containsKey(city)) {
					cityInfo = cityMapping.get(city);
				} else {
					cityInfo = new CityInfo(city);
					cityMapping.put(city, cityInfo);
				}

				Expense expense = new Expense();
				expense.setShop(document.getString("shop"));
				expense.setCity(city);
				expense.setMessage(document.getString("message"));
				expense.setAmountDouble(document.getDouble("amount"));
				expense.setTimestamp(document.getDate("timestamp"));
				cityInfo.addExpense(expense);
			}
		} catch (InterruptedException e) {
			LOG.error("Unterbrochen", e);
		} catch (ExecutionException e) {
			LOG.error("Fehler in der Ausführung", e);
		}

		MailInfo mailInfo = new MailInfo(beginDate, endDate);
		cityMapping.values().stream().forEach(x -> mailInfo.addCityInfo(x));

		model.addAttribute("cities", mailInfo.getCityList());
		model.addAttribute("sum", mailInfo.getSum());

		return "email";
	}

}
