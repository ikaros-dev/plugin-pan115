package run.ikaros.plugin.pan115;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentDriverOperate;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.plugin.pan115.model.Pan115Attachment;
import run.ikaros.plugin.pan115.model.Pan115Folder;
import run.ikaros.plugin.pan115.model.Pan115Path;
import run.ikaros.plugin.pan115.repository.Pan115Repository;
import run.ikaros.plugin.pan115.utils.FileUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Extension
@Component
public class Pan115AttachmentDriverFetcher implements AttachmentDriverFetcher {

    private final Pan115Repository pan115Repository;
    private final AttachmentDriverOperate driverOperate;

    public Pan115AttachmentDriverFetcher(Pan115Repository pan115Repository, AttachmentDriverOperate driverOperate) {
        this.pan115Repository = pan115Repository;
        this.driverOperate = driverOperate;
    }

    @Override
    public AttachmentDriverType getDriverType() {
        return AttachmentDriverType.CUSTOM;
    }

    @Override
    public String getDriverName() {
        return "PAN115";
    }

    private Mono<AttachmentDriver> checkoutToken(Long driverId) {
        Assert.isTrue(driverId >= 0, "driverId is negative");
        return driverOperate.findById(driverId)
                .flatMap(driver -> {
                    if (driver.getExpireTime() == null
                            || driver.getExpireTime().plusSeconds(10).isBefore(LocalDateTime.now())) {
                        applyPan115Token(driver);
                    }

                    pan115Repository.refreshHttpHeaders(driver.getAccessToken());
                    return driverOperate.save(driver);
                });
    }

    @Override
    public Flux<Attachment> getChildren(Long driverId, Long pid, String remotePath) {
        Assert.isTrue(driverId >= 0, "driverId is negative");
        Assert.isTrue(pid >= 0, "pid is negative");
        // 115 网盘挂载的这个属性其实就是目录ID(cid/fid)
        String cid = StringUtils.isEmpty(remotePath) ? "0" : remotePath;

        return checkoutToken(driverId)
                .flatMapMany(driver -> {
                    Long listPageSize = driver.getListPageSize();
                    if (listPageSize == null) {
                        listPageSize = 100L;
                    }

                    pan115Repository.refreshHttpHeaders(driver.getAccessToken());
                    List<Pan115Attachment> pan115Attachments =
                            pan115Repository.openUFileFiles(cid, listPageSize.intValue(), 1, 1);

                    return Flux.fromStream(pan115Attachments.stream());
                })
                .parallel(5)
                .runOn(Schedulers.boundedElastic())
                .map(att -> {
                    final String fid = att.getFid();
                    Pan115Folder pan115Folder = pan115Repository.openFolderGetInfo(fid);
                    String path = "";
                    for (Pan115Path pan115Path : pan115Folder.getPaths()) {
                        path += pan115Path.getFile_name();
                        path += "/";
                    }

                    AttachmentType type = "1".equals(pan115Folder.getFile_category())
                            ? AttachmentType.Driver_File
                            : AttachmentType.Driver_Directory;
                    Attachment attachment = new Attachment();
                    attachment.setSize(pan115Folder.getSize_byte());
                    attachment.setType(type);
                    attachment.setName(pan115Folder.getFile_name());
                    attachment.setUpdateTime(
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(pan115Folder.getUtime())),
                                    ZoneId.systemDefault()));
                    attachment.setDriverId(driverId);
                    attachment.setDeleted(false);
                    attachment.setSha1(pan115Folder.getSha1());
                    attachment.setParentId(pid);
                    attachment.setFsPath(pan115Folder.getFile_id()); // @see core: AttachmentDriverServiceImpl#refreshRemoteFileSystem
                    String url = pan115Folder.getPick_code();
                    if (StringUtils.isNotBlank(att.getIco())
                            && FileUtils.isImage(att.getIco())) {
                        url = att.getUo();
                    }
                    attachment.setUrl(url);
                    attachment.setPath(path);
                    return attachment;
                })
                .sequential();
    }

    @Override
    public Mono<String> parseReadUrl(Attachment attachment) {
        Assert.notNull(attachment, "'attachment' must not null.");
        AttachmentType type = attachment.getType();
        if (AttachmentType.Driver_Directory.equals(type)) return Mono.just(attachment.getUrl());
        String name = attachment.getName();
        Long driverId = attachment.getDriverId();
        Mono<AttachmentDriver> checkoutMono = checkoutToken(driverId);
        if (FileUtils.isImage(name)) {
            return checkoutMono.map(driver -> attachment.getUrl());
        } else if (FileUtils.isVideo(name)) {
            return checkoutMono.map(driver -> pan115Repository.openVideoPlay(attachment.getUrl()));
        } else if (FileUtils.isVoice(name)) {
            return parseDownloadUrl(attachment);
        } else {
            // doc
            return checkoutMono.map(driver -> attachment.getUrl());
        }
    }

    @Override
    public Mono<String> parseDownloadUrl(Attachment attachment) {
        Assert.notNull(attachment, "'attachment' must not null.");
        AttachmentType type = attachment.getType();
        if (AttachmentType.Driver_Directory.equals(type)) return Mono.just(attachment.getUrl());
        String name = attachment.getName();
        Long driverId = attachment.getDriverId();
        Mono<AttachmentDriver> checkoutMono = checkoutToken(driverId);
        if (FileUtils.isImage(name)) {
            return checkoutMono.map(driver -> attachment.getUrl());
        } else {
            // not image
            return checkoutMono.map(driver -> pan115Repository.openUFileDownUrl(attachment.getUrl()));
        }
    }

    private void applyPan115Token(AttachmentDriver driver) {
        Assert.notNull(driver, "'driver' must not null.");
        if (driver.getExpireTime() != null
                && driver.getExpireTime().isAfter(LocalDateTime.now().plusSeconds(10))) return;
        pan115Repository.refreshToken(driver);
    }

}
