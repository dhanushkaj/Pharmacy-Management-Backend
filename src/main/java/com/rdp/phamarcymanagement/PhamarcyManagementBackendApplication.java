
package com.rdp.phamarcymanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.rdp")
@EnableJpaRepositories(basePackages = "com.rdp.repository")
@EntityScan (basePackages = "com.rdp.model")
public class PhamarcyManagementBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhamarcyManagementBackendApplication.class, args);
	}

}
