package savemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SaveMateApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaveMateApplication.class, args);
    }
}