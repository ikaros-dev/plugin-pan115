package run.ikaros.plugin.pan115.repository;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.plugin.pan115.model.Pan115Attachment;
import run.ikaros.plugin.pan115.model.Pan115Folder;

import java.util.List;

public interface Pan115Repository {
    Mono<Boolean> assertDomainReachable();

    void refreshHttpHeaders(String accessToken);

    Flux<Pan115Attachment> openUFileFiles(String cid, Integer limit, Integer cur,
                                          Integer showDir);

    Mono<Pan115Folder> openFolderGetInfo(String fileId);

    Mono<AttachmentDriver> refreshToken(AttachmentDriver driver);

    Mono<String> openUFileDownUrl(String pickCode);

    Mono<String> openVideoPlay(String pickCode);

    Flux<DataBuffer> openUFileSteam(String pickCode);

    Flux<DataBuffer> openUFileSteamWithRange(String pickCode, long start, long end);
}
