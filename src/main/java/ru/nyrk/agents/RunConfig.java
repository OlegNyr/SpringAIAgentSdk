package ru.nyrk.agents;

import io.micrometer.observation.ObservationRegistry;
import lombok.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.template.TemplateRenderer;
import ru.nyrk.agents.process.HandoffInputData;

import java.util.Map;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunConfig {

    /**
     * Модель для использования во всем запуске агента. Если установлена, переопределяет модель,
     * установленную для каждого агента.
     */
    @With
    private ChatModel model;

    /**
     * Настройка глобальных параметров модели. Любые ненулевые значения переопределяют
     * специфичные для агента настройки модели.
     */
    @With
    private ChatOptions chatOptions;


    /**
     * Глобальный фильтр входных данных для применения ко всем передачам управления. Если
     * установлен `Handoff.inputFilter`, он будет иметь приоритет. Фильтр входных данных позволяет
     * редактировать входные данные, отправляемые новому агенту.
     */
    @With
    private Function<HandoffInputData, HandoffInputData> handoffInputFilter;

    /**
     * Имя запуска, используемое для трассировки. Должно быть логическим именем для запуска,
     * таким как "Рабочий процесс генерации кода" или "Агент поддержки клиентов".
     */
    @With
    @Builder.Default
    private String workflowName = "Agent workflow";

    /**
     * Пользовательский ID трассировки для использования. Если не предоставлен, будет сгенерирован новый ID трассировки.
     */
    @With
    private String traceId;

    /**
     * Идентификатор группировки для использования в трассировке, для связывания нескольких трассировок
     * из одного разговора или процесса. Например, можно использовать ID чат-треда.
     */
    @With
    private String groupId;

    /**
     * Необязательный словарь дополнительных метаданных для включения в трассировку.
     */
    @With
    private Map<String, Object> traceMetadata;


    @With
    ObservationRegistry observationRegistry;

    @With
    TemplateRenderer templateRenderer;
}
