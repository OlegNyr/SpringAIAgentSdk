package ru.nyrk.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record MediaResponse(    // Размер файла в байтах
                                Integer bytes,

                                // Время создания файла в формате unix timestamp.
                                Long createdAt,

                                // Имя файла
                                String filename,

                                // Идентификатор файла. Добавляется к сообщению пользователя для работы с файлом
                                String id,

                                // Тип объекта. Всегда равен file.
                                String object,

        /*
         * Назначение файла
         * Возможные значения: general
         * */
                                String purpose,

        /*
         * Доступность файла
         * Возможные значения: public, private
         * */
                                String accessPolicy) {
}
