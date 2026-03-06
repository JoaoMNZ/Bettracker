package io.github.joaomnz.bettracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BettrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BettrackerApplication.class, args);
	}

}