package run.ikaros.plugin.pan115;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentOperate;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.exception.FeatureNotImplException;
import run.ikaros.api.infra.utils.ReactiveBeanUtils;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.plugin.pan115.repository.Pan115Attachment;
import run.ikaros.plugin.pan115.repository.Pan115Repository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Extension
public class Pan115AttachmentDriverFetcher implements AttachmentDriverFetcher {
    private final ReactiveCustomClient reactiveCustomClient;
    private final AttachmentOperate attachmentOperate;
    private final Pan115Repository pan115Repository;
    private AttachmentDriver driver;

    public Pan115AttachmentDriverFetcher(ReactiveCustomClient reactiveCustomClient,
                                         AttachmentOperate attachmentOperate,
                                         Pan115Repository pan115Repository) {
        this.reactiveCustomClient = reactiveCustomClient;
        this.attachmentOperate = attachmentOperate;
        this.pan115Repository = pan115Repository;
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

        if (driver.getExpireTime() == null
                || driver.getExpireTime().isBefore(LocalDateTime.now().plusHours(1L))) {
            applyPan115Token(driver);
        }

        Long listPageSize = driver.getListPageSize();
        if (listPageSize == null) {
            listPageSize = 20L;
        }

        pan115Repository.refreshHttpHeaders(driver.getAccessToken());
        List<Pan115Attachment> pan115Attachments =
                pan115Repository.openUFileFiles(cid, listPageSize.intValue(), 1, 1);


        Flux.fromStream(pan115Attachments.stream())
                .filter(pa -> "1".equals(pa.getPc()))
                .map(Pan115Attachment::getFid)// 文件

                .switchIfEmpty(Mono.error(new FeatureNotImplException("Save dir attachment."))) // 目录
        ;
        return List.of();
    }

    private void applyPan115Token(AttachmentDriver driver) {

    }

}
