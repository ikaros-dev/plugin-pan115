package run.ikaros.plugin.pan115;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.util.Assert;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentOperate;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.utils.ReactiveBeanUtils;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.AttachmentDriverType;

import java.util.List;

@Slf4j
@Extension
public class Pan115AttachmentDriverFetcher implements AttachmentDriverFetcher {
    private final ReactiveCustomClient reactiveCustomClient;
    private final AttachmentOperate attachmentOperate;
    private AttachmentDriver driver;

    public Pan115AttachmentDriverFetcher(ReactiveCustomClient reactiveCustomClient,
                                         AttachmentOperate attachmentOperate) {
        this.reactiveCustomClient = reactiveCustomClient;
        this.attachmentOperate = attachmentOperate;
    }

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
        Assert.notNull(driver, "Attachment driver is null");
        Assert.isTrue(pid >= 0, "pid is negative");
        // 115 网盘挂载的这个属性其实就是目录ID(cid/fid)
        String cid = StringUtils.isEmpty(remotePath) ? "0" : remotePath;



        return List.of();
    }

}
