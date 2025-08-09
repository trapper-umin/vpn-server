package server.vpn.com.servermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServerManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerManagementServiceApplication.class, args);
    }

}
