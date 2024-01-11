package telegram.bot;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.TimeZone;

@SpringBootApplication
public class BotApplication {

	public static void main(String[] args) {

		System.out.println("Time Now: " + LocalDateTime.now());

		SpringApplication.run(BotApplication.class, args);
	}

	@PostConstruct
	public void init() {

		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"));
	}
}