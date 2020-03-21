package de.probstl.ausgaben.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

/**
 * Mail client service for sending expences by mail
 */
@Service
public class ExpensesMailer {

	/** The spring mail sender */
	private JavaMailSender m_MailSender;

	/** The content builder that creates the email */
	private ExpensesContentBuilder m_MailContentBuilder;

	/**
	 * Constructor
	 * 
	 * @param sender  The spring mail sender
	 * @param builder The email content builder
	 */
	@Autowired
	public ExpensesMailer(JavaMailSender sender, ExpensesContentBuilder builder) {
		m_MailSender = sender;
		m_MailContentBuilder = builder;
	}

	/**
	 * Preapre a new email and send the message
	 * 
	 * @param recipient The recipient of the mail
	 * @param mailInfo  The expences data that should be sent
	 */
	public void prepareAndSend(String recipient, MailInfo mailInfo) {
		MimeMessagePreparator messagePreparator = mimeMessage -> {
			MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
			messageHelper.setFrom("flo@probstl.de");
			messageHelper.setTo(recipient);
			messageHelper.setSubject("Ausgaben");
			String content = m_MailContentBuilder.build(mailInfo);
			messageHelper.setText(content, true);
		};
		
		try {
			m_MailSender.send(messagePreparator);
		} catch (MailException e) {
			// runtime exception; compiler will not force you to handle it
		}
	}

}
