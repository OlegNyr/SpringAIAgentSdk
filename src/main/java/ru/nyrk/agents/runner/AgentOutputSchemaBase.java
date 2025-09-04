package ru.nyrk.agents.runner;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.converter.StructuredOutputConverter;

@RequiredArgsConstructor
public class AgentOutputSchemaBase<T> implements StructuredOutputConverter<T> {

    private final StructuredOutputConverter<T> structuredOutputConverter;

    public boolean isPlanText() {
        return structuredOutputConverter == null;
    }

    @Override
    public String getFormat() {
        if (isPlanText()) {
            return null;
        }
        return structuredOutputConverter.getFormat();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convert(String source) {
        if (isPlanText()) {
            return (T) source;
        }
        return structuredOutputConverter.convert(source);
    }

}
