package dev.ai.alibaba.samples.executor.function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

@Slf4j
public class WeatherFunction implements Function<WeatherFunction.Request, WeatherFunction.Response> {

    private final RestClient restClient;
    private final WeatherConfig weatherProps;

    public WeatherFunction(WeatherConfig props) {
        this.weatherProps = props;
        log.debug("Weather API URL: {}", weatherProps.apiUrl());
        log.debug("Weather API Key: {}", weatherProps.apiKey());
        this.restClient = RestClient.create(weatherProps.apiUrl());
    }

    @Override
    public Response apply(Request weatherRequest) {
        log.info("Weather Request: {}",weatherRequest);
        var response = restClient.get()
                .uri("/current.json?key={key}&q={q}", weatherProps.apiKey(), weatherRequest.city())
                .retrieve()
                .body(Response.class);
        log.info("Weather API Response:\n{}", response);


        return response;
    }

    // mapping the response of the Weather API to records. I only mapped the information I was interested in.
    public record Request(String city) {}
    public record Response(Location location,Current current) {}
    public record Location(String name, String region, String country, Long lat, Long lon){}
    public record Current(String temp_f, Condition condition, String wind_mph, String humidity) {}
    public record Condition(String text){}

}