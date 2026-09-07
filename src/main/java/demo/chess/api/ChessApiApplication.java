package demo.chess.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChessApiApplication {

    /**
     * Starts the application.
     * @param args the args
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ChessApiApplication.class);
        application.setHeadless(false);
        application.run(args);
    }
}
