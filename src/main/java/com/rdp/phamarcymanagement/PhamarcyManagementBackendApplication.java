package com.rdp.phamarcymanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication(scanBasePackages = "com.rdp")
@EnableJpaRepositories(basePackages = {"com.rdp.repository", "com.rdp.audit"})
@EntityScan (basePackages = {"com.rdp.model", "com.rdp.audit"})
@EnableScheduling
public class PhamarcyManagementBackendApplication {

	public static void main(String[] args) {

	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	System.out.println(""+encoder.encode("admin123"));
 	SpringApplication.run(PhamarcyManagementBackendApplication.class, args);
	
		System.out.println(" ++++++++++++++++++++++" );
	}

}
