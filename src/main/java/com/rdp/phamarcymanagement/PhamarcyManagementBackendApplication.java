
package com.rdp.phamarcymanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.rdp")
@EnableJpaRepositories(basePackages = {"com.rdp.repository", "com.rdp.audit"})
@EntityScan (basePackages = {"com.rdp.model", "com.rdp.audit"})
public class PhamarcyManagementBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhamarcyManagementBackendApplication.class, args);
	}

}
