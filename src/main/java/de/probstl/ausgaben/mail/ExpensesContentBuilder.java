package de.probstl.ausgaben.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Builder for creating an email
 */
@Service
public class ExpensesContentBuilder {

	/** The template engine that is being used */
	private TemplateEngine templateEngine;

	/**
	 * Constructor
	 * 
	 * @param templateEngine The injected template enging for rendering the email
	 *                       content
	 */
	@Autowired
	public ExpensesContentBuilder(TemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	/**
	 * Create the mail content
	 * 
	 * @param mailInfo The expenses that should be used to render the email
	 * @return content as a string
	 */
	public String build(MailInfo mailInfo) {
		Context context = new Context();
		context.setVariable("beginDate", mailInfo.getFrom());
		context.setVariable("endDate", mailInfo.getTo());
		context.setVariable("cities", mailInfo.getCityList());
		context.setVariable("sum", mailInfo.getSum());
		context.setVariable("currency", "€");
		return templateEngine.process("email", context);
	}
}