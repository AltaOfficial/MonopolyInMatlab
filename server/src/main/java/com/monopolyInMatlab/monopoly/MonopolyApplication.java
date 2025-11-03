package com.monopolyInMatlab.monopoly;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MonopolyApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(MonopolyApplication.class, args);
	}

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Server started");
    }
}
