package com.rdp.phamarcymanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.rdp")
@EnableJpaRepositories(basePackages = {"com.rdp.repository", "com.rdp.audit"})
@EntityScan (basePackages = {"com.rdp.model", "com.rdp.audit"})
@EnableScheduling
public class PhamarcyManagementBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhamarcyManagementBackendApplication.class, args);
	}

}
