package de.probstl.ausgaben;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfiguration implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/img/**").addResourceLocations("classpath:/static/img/");
		registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/");
		registry.addResourceHandler("/apple-touch-icon.png").addResourceLocations("classpath:/static/");
		registry.addResourceHandler("/favicon.png").addResourceLocations("classpath:/static/");
	}
}
