package pw.avvero.ggt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pw.avvero.ggt")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
