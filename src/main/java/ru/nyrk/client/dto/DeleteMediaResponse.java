package ru.nyrk.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DeleteMediaResponse(
        // Идентификатор файла.
        UUID id,

        // Признак удаления файла.
        boolean deleted,

        /*
         * Доступность файла
         * Возможные значения: public, private
         * */
        @JsonProperty("access_policy") String accessPolicy) {}
