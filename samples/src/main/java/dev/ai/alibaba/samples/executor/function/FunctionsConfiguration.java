package dev.ai.alibaba.samples.executor.function;

import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class FunctionsConfiguration {

    private final WeatherConfig props;

    public FunctionsConfiguration(WeatherConfig props) {
        this.props = props;
    }

    @Bean
    @Description("Get the current weather conditions for the given city.")
    public Function<WeatherFunction.Request, WeatherFunction.Response> currentWeatherFunction() {
        return new WeatherFunction(props);
    }

    @Bean
    public FunctionCallback weatherFunctionCallback() {

        return AgentFunctionCallbackWrapper.<WeatherFunction.Request, WeatherFunction.Response>builder( new WeatherFunction(props) )
                .withName("weatherFunctionCallback") // (1) function name
                .withDescription("Get the weather in location") // (2) function description
                .build();
    }

}