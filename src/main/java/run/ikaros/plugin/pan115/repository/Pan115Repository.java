package run.ikaros.plugin.pan115.repository;

import run.ikaros.api.core.attachment.Attachment;

import java.util.List;

public interface Pan115Repository {
    boolean assertDomainReachable();

    void refreshHttpHeaders(String accessToken);

    List<Pan115Attachment> openUFileFiles(String cid, Integer limit, Integer cur,
                                    Integer showDir);
}
