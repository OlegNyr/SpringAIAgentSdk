package ru.nyrk.client.giga;

import chat.giga.springai.api.auth.GigaChatApiProperties;
import chat.giga.springai.api.chat.GigaChatApi;
import chat.giga.springai.api.chat.file.DeleteFileResponse;
import chat.giga.springai.api.chat.file.UploadFileResponse;
import org.springframework.ai.content.Media;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import ru.nyrk.client.ChatApi;
import ru.nyrk.client.dto.DeleteMediaResponse;
import ru.nyrk.client.dto.MediaResponse;
import ru.nyrk.client.dto.UploadMediaResponse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

public class GigaChatMoreApi extends GigaChatApi implements ChatApi<GigaChatMoreApi> {
    private final RestClient restClient;

    public GigaChatMoreApi(GigaChatApiProperties properties,
                           RestClient.Builder restClientBuilder,
                           WebClient.Builder webClientBuilder,
                           ResponseErrorHandler responseErrorHandler) {
        super(properties, restClientBuilder, webClientBuilder, responseErrorHandler);
        Field field = ReflectionUtils.findField(GigaChatApi.class, "restClient");
        Objects.requireNonNull(field);
        ReflectionUtils.makeAccessible(field);
        this.restClient = (RestClient) ReflectionUtils.getField(field, this);
    }

    public ResponseEntity<ListFileResponse> listFiles() {

        return this.restClient
                .get()
                .uri("/files")
                .header(HttpHeaders.USER_AGENT, USER_AGENT_SPRING_AI_GIGACHAT)
                .retrieve()
                .toEntity(ListFileResponse.class);
    }

    @Override
    public UploadMediaResponse uploadMedia(Media media) {
        ResponseEntity<UploadFileResponse> entity = this.uploadFile(media);
        if (entity.getStatusCode().is2xxSuccessful()) {
            UploadFileResponse body = entity.getBody();
            return new UploadMediaResponse(body.bytes(), body.createdAt(), body.filename(), body.id(), body.object(), body.purpose(), body.accessPolicy());
        } else {
            throw new RuntimeException(entity.getStatusCode().toString());
        }
    }

    @Override
    public DeleteMediaResponse deleteMedia(String fileId) {
        ResponseEntity<DeleteFileResponse> entity = super.deleteFile(fileId);
        if (entity.getStatusCode().is2xxSuccessful()) {
            DeleteFileResponse body = entity.getBody();
            return new DeleteMediaResponse(body.id(), body.deleted(), body.accessPolicy());
        } else {
            throw new RuntimeException(entity.getStatusCode().toString());
        }
    }

    @Override
    public GigaChatMoreApi origin() {
        return this;
    }

    @Override
    public List<MediaResponse> listMedia() {
        ResponseEntity<ListFileResponse> entity = listFiles();
        if (entity.getStatusCode().is2xxSuccessful()) {
            List<FileResponse> data = entity.getBody().data();
            return data.stream()
                    .map(f -> new MediaResponse(f.bytes(), f.createdAt(), f.filename(), f.id().toString(), f.object(), f.purpose(), f.accessPolicy()))
                    .toList();
        } else {
            throw new RuntimeException(entity.getStatusCode().toString());
        }
    }
}
