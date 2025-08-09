package server.vpn.com.vpnmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VpnManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VpnManagementServiceApplication.class, args);
    }

}
