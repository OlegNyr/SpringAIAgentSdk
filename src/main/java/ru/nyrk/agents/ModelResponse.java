package ru.nyrk.agents;

import org.springframework.ai.chat.metadata.Usage;

import java.util.List;

public record ModelResponse(

        /**
         * Список выходов (сообщений, инструментов и т. Д.), Сгенерированные моделью
         */
        List<ResponseOutputItem> output,

        /**
         * Информация об использовании для ответа.
         */
        Usage usage) {
}

