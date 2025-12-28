package run.ikaros.plugin.pan115.repository;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.plugin.pan115.model.Pan115Attachment;
import run.ikaros.plugin.pan115.model.Pan115Folder;

import java.util.List;

public interface Pan115Repository {
    boolean assertDomainReachable();

    void refreshHttpHeaders(String accessToken);

    List<Pan115Attachment> openUFileFiles(String cid, Integer limit, Integer cur,
                                          Integer showDir);

    Pan115Folder openFolderGetInfo(String fileId);

    void refreshToken(AttachmentDriver driver);

    String openUFileDownUrl(String pickCode);

    String openVideoPlay(String pickCode);

    Flux<DataBuffer> openUFileSteam(String pickCode);

    Flux<DataBuffer> openUFileSteamWithRange(String pickCode, long start, long end);
}
