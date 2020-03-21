package de.probstl.ausgaben;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * The main spring application
 */
@SpringBootApplication
public class ExpensesApplication {

	/**
	 * Main method start the application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(ExpensesApplication.class, args);
	}

	/**
	 * Used for the i18n strings from the email
	 * 
	 * @return Resource Bundle containing the i18n strings
	 */
	@Bean(name = "messageSource")
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
		source.setBasename("classpath:i18n/email");
		source.setUseCodeAsDefaultMessage(true);
		source.setDefaultEncoding("UTF-8");
		source.setCacheSeconds(0);
		return source;
	}

}
