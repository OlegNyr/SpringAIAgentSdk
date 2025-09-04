 package ru.nyrk.client.stub;

import org.springframework.ai.content.Media;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import ru.nyrk.client.ChatApi;
import ru.nyrk.client.dto.DeleteMediaResponse;
import ru.nyrk.client.dto.MediaResponse;
import ru.nyrk.client.dto.UploadMediaResponse;

import java.util.List;

public class StubApiMore implements ChatApi<DeepSeekApi> {

    private final DeepSeekApi deepSeekApi;

    public StubApiMore(DeepSeekApi deepSeekApi) {
        this.deepSeekApi = deepSeekApi;
    }

    @Override
    public UploadMediaResponse uploadMedia(Media media) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteMediaResponse deleteMedia(String fileId) {
        return null;
    }

    @Override
    public DeepSeekApi origin() {
        return deepSeekApi;
    }

    @Override
    public List<MediaResponse> listMedia() {
        return List.of();
    }
}
