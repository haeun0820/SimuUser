package com.example.simuuser;

import com.example.simuuser.config.DatabaseUrlNormalizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SimuuserApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(SimuuserApplication.class);
		application.addListeners(new DatabaseUrlNormalizer());
		application.run(args);
	}

}
