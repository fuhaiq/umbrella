package com.riot.percentiles;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class StartupRunner implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) throws Exception {}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(StartupRunner.class, args);
	}


}
