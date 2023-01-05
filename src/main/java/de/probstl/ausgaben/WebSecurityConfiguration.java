package de.probstl.ausgaben;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures access to new expenses and overviews
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

	private final Logger LOG = LoggerFactory.getLogger(WebSecurityConfiguration.class);

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf().disable().authorizeRequests().anyRequest().authenticated().and().formLogin().loginPage("/login")
				.permitAll().and().httpBasic();
		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		Properties fromFile = new Properties();
		try (FileInputStream in = new FileInputStream("/userAccounts.properties")) {
			fromFile.load(in);
		} catch (IOException ioe) {
			fromFile.put("flo", "{noop}test,enabled,ausgaben");
			LOG.warn("Properties file not found. Using defaults!");
		}
		return new InMemoryUserDetailsManager(fromFile);
	}
}
