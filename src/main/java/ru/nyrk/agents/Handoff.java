package ru.nyrk.agents;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import ru.nyrk.agents.process.HandoffInputData;

import java.util.Locale;
import java.util.Objects;
import java.util.function.*;

/// **Handoff** (передача) - это когда агент делегирует задачу другому агенту.
/// Например, в сценарии поддержки клиентов может быть "агент триажа", который определяет,
/// какой агент должен обработать запрос пользователя, и под-агенты, специализирующиеся в различных
/// областях, таких как биллинг, управление аккаунтами и т.д.
@Value
@Builder(builderClassName = "HandoffBuilder")
public class Handoff {
    private static final String EMPTY_SCHEMA = """
            {
                "additionalProperties": false,
                "type": "object",
                "properties": {},
                "required": []
            }
            """;

    /// Название инструмента, представляющего передачу.
    /// По умолчанию `"transfer_to_" + agent.getName()`
    String toolName;

    /// Описание инструмента, представляющего передачу.
    /// По умолчанию `"Handoff to the %s agent to handle the request. ".formatted(agent.getName());`
    String toolDescription;


    /**
     * JSON-схема для входных данных передачи. Может быть пустой, если передача не принимает входные данные.
     */
    String inputJsonSchema;

    /// Функция, которая вызывает передачу. Передаваемые параметры:
    ///
    /// 1. Контекст выполнения передачи
    /// 2. Аргументы от LLM, в виде JSON-строки. Пустая строка, если input_json_schema пуста.
    ///
    ///  Должна возвращать агента.
    BiFunction<AgentContext, String, Agent> onInvokeHandoff;

    /**
     * Название агента, которому передается задача.
     */
    String agentName;

    /// Функция, которая фильтрует входные данные, передаваемые следующему агенту. По умолчанию новый
    /// агент видит всю историю разговора. В некоторых случаях может потребоваться
    /// отфильтровать входные данные, например,
    /// чтобы удалить более старые входные данные или удалить инструменты из существующих входных данных.
    ///
    /// Функция получит всю историю разговора на данный момент, включая элемент входных данных,
    /// который вызвал передачу, и элемент вывода инструмента, представляющий вывод инструмента передачи.
    ///
    /// Вы можете свободно изменять историю входных данных или новые элементы по своему усмотрению.
    /// Следующий агент, который
    /// запустится, получит `handoffInput_data.allItems`.
    ///
    /// ВАЖНО: в потоковом режиме мы не будем передавать ничего в результате работы этой функции.
    /// Элементы, сгенерированные до этого, уже будут переданы.
    ///
    /// Пример `InputFiltersUtils::removeAllTools`
    Function<HandoffInputData, HandoffInputData> inputFilter;

    /**
     * Включена ли передача. Может быть boolean или Callable, который принимает контекст выполнения и
     * агента и возвращает, включена ли передача. Вы можете использовать это для динамического включения/отключения
     * передачи на основе вашего контекста/состояния(пока не реализованно).
     */
    @NonNull
    BiPredicate<AgentContext, Agent> enabled;


    public String getTransferMessage(Agent agent) {
        return "{\"assistant\": \"%s\"}".formatted(agent.getName());
    }

    public static class HandoffBuilder<T> {
        /// Агент, которому передается задача, или функция, возвращающая агента.
        private Agent agent;
        private BiConsumer<AgentContext, T> onHandoffInput;
        private Class<T> inputType;
        private Consumer<AgentContext> onHandoff;

        /// Функция обратного вызова, которая выполняется при вызове передачи управления.
        /// Это полезно, например, для запуска процесса получения данных, как только вы узнаете,
        /// что вызывается передача управления. Эта функция получает контекст агента,
        /// а также может получать входные данные, сгенерированные LLM.
        /// Входные данные управляются параметром input_type
        public HandoffBuilder<T> inputFunc(BiConsumer<AgentContext, T> onHandoff, Class<T> inputType) {
            this.onHandoffInput = onHandoff;
            this.inputType = Objects.requireNonNull(inputType);
            return this;
        }

        public HandoffBuilder<T> inputFunc(Consumer<AgentContext> onHandoff) {
            this.onHandoff = onHandoff;
            return this;
        }

        public HandoffBuilder<T> inputType(Class<T> inputType) {
            this.inputType = inputType;
            return this;
        }

        public HandoffBuilder<T> agent(Agent agent) {
            this.agent = agent;
            return this;
        }

        public HandoffBuilder<T> enabled(BiPredicate<AgentContext, Agent> predicate) {
            this.enabled = predicate;
            return this;
        }

        public HandoffBuilder<T> enabled(final boolean vl) {
            enabled((c, a) -> vl);
            return this;
        }

        public Handoff build() {

            if (inputJsonSchema == null) {
                if (inputType == null) {
                    inputJsonSchema = EMPTY_SCHEMA;
                } else {
                    inputJsonSchema = JsonSchemaGenerator.generateForType(inputType);
                }
            }

            if (this.enabled == null) {
                this.enabled = (context, agent) -> true;
            }
            if (this.agentName == null) {
                Objects.requireNonNull(agent, "Agent and agentName not null");
                this.agentName = agent.getName();
            }
            if (this.toolName == null) {
                this.toolName = defaultToolName(agentName);
            }

            if (this.toolDescription == null) {
                this.toolDescription = defaultToolDescription(agent);
            }

            BiFunction<AgentContext, String, Agent> onLocalHandoff;
            if (onInvokeHandoff == null) {
                Objects.requireNonNull(agent, "Agent and onInvokeHandoff not null");
                onLocalHandoff = (AgentContext agentContext, String argument) -> {
                    if (inputType != null && onHandoffInput != null) {
                        T param = JsonParser.fromJson(argument, inputType);
                        onHandoffInput.accept(agentContext, param);
                    }
                    if (onHandoff != null) {
                        onHandoff.accept(agentContext);
                    }
                    return agent;
                };
            } else {
                onLocalHandoff = onInvokeHandoff;
            }

            return new Handoff(this.toolName,
                    this.toolDescription,
                    this.inputJsonSchema,
                    onLocalHandoff,
                    this.agentName,
                    this.inputFilter,
                    this.enabled);
        }

        private static String defaultToolDescription(Agent agent) {
            Objects.requireNonNull(agent, "Agent and description not null");
            //Передача агенту %s для обработки запроса.
            String res = " Handoff to the %s agent to handle the request. ".formatted(agent.getName());
            if (agent.getHandoffDescription() != null) {
                return res + agent.getHandoffDescription();
            }
            return res;
        }

        private static String defaultToolName(String agentName) {
            Objects.requireNonNull(agentName, "Agent and toolName not null");
            return transformStringFunctionStyle("transfer_to_" + agentName);
        }


        private static String transformStringFunctionStyle(String name) {
            // Replace spaces with underscores
            name = name.replace(" ", "_");
            // Replace non-alphanumeric characters with underscores
            name = name.replaceAll("[^a-zA-Z0-9]", "_");
            return name.toLowerCase(Locale.ROOT);
        }

    }
}
