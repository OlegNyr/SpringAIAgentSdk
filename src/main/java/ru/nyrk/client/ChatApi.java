package ru.nyrk.client;

import org.springframework.ai.content.Media;
import ru.nyrk.client.dto.DeleteMediaResponse;
import ru.nyrk.client.dto.MediaResponse;
import ru.nyrk.client.dto.UploadMediaResponse;

import java.util.List;

public interface ChatApi<T> {
    UploadMediaResponse uploadMedia(Media media);

    DeleteMediaResponse deleteMedia(String fileId);

    T origin();

    List<MediaResponse> listMedia();

}
