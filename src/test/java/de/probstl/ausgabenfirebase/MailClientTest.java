package de.probstl.ausgabenfirebase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import de.probstl.ausgaben.ExpensesApplication;
import de.probstl.ausgaben.data.Expense;
import de.probstl.ausgaben.mail.CityInfo;
import de.probstl.ausgaben.mail.ExpensesMailer;
import de.probstl.ausgaben.mail.MailInfo;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ExpensesApplication.class)
public class MailClientTest {

	@Autowired
	private ExpensesMailer m_ExpensesClient;

	private GreenMail smtpServer;

	@BeforeEach
	public void setUp() throws Exception {
		smtpServer = new GreenMail(new ServerSetup(1234, null, "smtp"));
		smtpServer.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		smtpServer.stop();
	}

	@Test
	public void shouldSendMail() throws Exception {
		String recipient = "john.doe@gmx.de";

		Calendar cal = Calendar.getInstance();
		Date to = cal.getTime();
		cal.add(Calendar.DAY_OF_YEAR, -7);
		Date from = cal.getTime();

		MailInfo info = new MailInfo(from, to);
		
		CityInfo landshut = new CityInfo("Landshut");
		Expense betz = new Expense();
		betz.setAmountDouble(Double.valueOf(5));
		betz.setCity("Landshut");
		betz.setMessage("Brezen und Zeitung");
		betz.setShop("Betz");
		betz.setTimestamp(new Date());
		landshut.addExpense(betz);
		Expense wochenmarkt = new Expense();
		wochenmarkt.setAmountDouble(Double.valueOf(7.6));
		wochenmarkt.setCity("Landshut");
		wochenmarkt.setMessage("Gemüse");
		wochenmarkt.setShop("Gründl");
		wochenmarkt.setTimestamp(new Date());
		landshut.addExpense(wochenmarkt);
		info.addCityInfo(landshut);

		CityInfo dingolfing = new CityInfo("Dingolfing");
		Expense maccy = new Expense();
		maccy.setAmountDouble(Double.valueOf(7.99));
		maccy.setCity("Dingolfing");
		maccy.setMessage("BigMac Sparmenü");
		maccy.setShop("Mc Donalds");
		maccy.setTimestamp(new Date());
		dingolfing.addExpense(maccy);
		info.addCityInfo(dingolfing);
		
		m_ExpensesClient.prepareAndSend(recipient, info);
		assertReceivedMessage();
	}

	private void assertReceivedMessage() throws IOException, MessagingException {
		MimeMessage[] receivedMessages = smtpServer.getReceivedMessages();
		Assert.assertEquals(1, receivedMessages.length);
		String content = (String) receivedMessages[0].getContent();
		Assert.assertNotNull(content);

		Path path = Paths.get("target", MessageFormat.format("Template_{0,time,HHmmss}.html", new Date()));
		Files.write(path, content.getBytes());
	}

}