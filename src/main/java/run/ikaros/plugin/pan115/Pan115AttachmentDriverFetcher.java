package run.ikaros.plugin.pan115;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.store.enums.AttachmentDriverType;

import java.util.List;

@Slf4j
@Extension
public class Pan115AttachmentDriverFetcher implements AttachmentDriverFetcher {
    private AttachmentDriver driver;

    @Override
    public AttachmentDriverType getDriverType() {
        return AttachmentDriverType.CUSTOM;
    }

    @Override
    public String getDriverName() {
        return "PAN115";
    }

    @Override
    public void setDriver(AttachmentDriver attachmentDriver) {
        this.driver = attachmentDriver;
    }

    @Override
    public List<Attachment> getChildAttachments(Long pid, String remotePath) {
        return List.of();
    }

}
