package de.probstl.ausgaben;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

@Configuration
public class RequestLoggingConfiguration {

	@Bean
	public Filter loggingFilter() {
		return new AbstractRequestLoggingFilter() {
			private final Logger log = LoggerFactory.getLogger(RequestLoggingConfiguration.class);
			{
				setIncludeClientInfo(true);
				setIncludeQueryString(true);
				setIncludePayload(true);
			}

			@Override
			protected void beforeRequest(HttpServletRequest request, String message) {
				// not needed
			}

			@Override
			protected void afterRequest(HttpServletRequest request, String message) {
				log.info(message);
			}
		};
	}
}
