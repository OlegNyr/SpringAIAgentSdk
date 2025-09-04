package ru.nyrk.agents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

@Slf4j
public class CustomTools {
    @Tool
    public String getWeather(String city) {
        log.info(">>>>>>>>  getting weather for {}", city);
        return "The weather in %s is sunny.".formatted(city);
    }
}
