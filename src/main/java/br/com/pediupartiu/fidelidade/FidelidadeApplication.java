package br.com.pediupartiu.fidelidade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FidelidadeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FidelidadeApplication.class, args);
	}

}
