package com.thehecklers.thing2;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;

@SpringBootApplication
public class Thing2Application {

    public static void main(String[] args) {
        SpringApplication.run(Thing2Application.class, args);
    }

    @Bean
    RSocketRequester requester(RSocketRequester.Builder builder) {
        return builder.tcp("192.168.86.168", 9091);
    }

    @Bean
    WebClient client() {
        return WebClient.create("http://192.168.86.168:9876/metar");
    }
}

@Component
@AllArgsConstructor
class Thing2Component {
    private final RSocketRequester requester;
    private final WebClient wxClient;

    void reqResp() {
        requester.route("reqresp")
                .data(Instant.now())
                .retrieveMono(Aircraft.class)
                .subscribe(ac -> System.out.println("🛩 " + ac));
    }

    void reqStream() {
        requester.route("reqstream")
                .data(Instant.now())
                .retrieveFlux(Aircraft.class)
                .subscribe(ac -> System.out.println("✈️ " + ac));
    }

    void fireAndForget() {
        requester.route("fireforget")
                .data(wxClient.get()
                        .retrieve()
                        .bodyToMono(Weather.class))
                .send()
                .subscribe();
    }

    @PostConstruct
    void channel() {
        requester.route("channel")
                .data(wxClient.get()
                        .retrieve()
                        .bodyToMono(Weather.class)
                        .repeat()
                        .delayElements(Duration.ofSeconds(2)))
                .retrieveFlux(Aircraft.class)
                .subscribe(ac -> System.out.println("🛫🛬 " + ac));
    }
}

@Data
class Weather {
    private String flight_rules, raw;
}

@Data
class Aircraft {
	private String callsign, reg, flightno, type;
    private int altitude, heading, speed;
    private double lat, lon;
}