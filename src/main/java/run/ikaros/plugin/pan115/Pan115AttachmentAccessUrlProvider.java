package run.ikaros.plugin.pan115;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AccessUrlCondition;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentAccessUrlProvider;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.plugin.pan115.repository.Pan115Repository;
import run.ikaros.plugin.pan115.utils.FileUtils;

import java.util.List;
import java.util.Map;

/**
 * PAN115 VIP video play URL provider.
 * Provides direct VIP play URLs for 115 network disk video files
 * via the /open/video/play API endpoint.
 *
 * @author ikaros
 */
@Slf4j
@Extension
public class Pan115AttachmentAccessUrlProvider implements AttachmentAccessUrlProvider {

    private final Pan115Repository pan115Repository;

    public Pan115AttachmentAccessUrlProvider(Pan115Repository pan115Repository) {
        this.pan115Repository = pan115Repository;
    }

    @Override
    public boolean supports(Attachment attachment) {
        if (attachment == null) {
            return false;
        }
        // PAN115 stores pick_code in the url field for non-image files
        return AttachmentType.Driver_File.equals(attachment.getType())
            && StringUtils.isNotBlank(attachment.getUrl())
            && FileUtils.isVideo(attachment.getName());
    }

    @Override
    public Mono<String> getAccessUrl(Attachment attachment, Map<String, Object> conditions) {
        String pickCode = attachment.getUrl();
        log.debug("Getting VIP play URL for attachment: {}, pickCode: {}",
            attachment.getId(), pickCode);
        return pan115Repository.openVideoPlay(pickCode)
            .map(url -> appendConditions(url, conditions));
    }

    @Override
    public List<AccessUrlCondition> getConditionDefinitions() {
        // Audio track selection
        AccessUrlCondition audioTrack = new AccessUrlCondition()
            .setName("audio_track")
            .setType("integer")
            .setLabel("音轨选择")
            .setRequired(false)
            .setDefaultValue("0")
            .setDescription("选择视频的音轨，从0开始，默认为0");

        // Video definition/quality selection
        AccessUrlCondition definition = new AccessUrlCondition()
            .setName("definition")
            .setType("select")
            .setLabel("清晰度")
            .setRequired(false)
            .setDefaultValue("4")
            .setDescription("视频清晰度: 1=标清, 2=高清, 3=超清, 4=1080P, 5=4K, 100=原画")
            .setOptions(List.of("1-标清", "2-高清", "3-超清", "4-1080P", "5-4K", "100-原画"));

        return List.of(audioTrack, definition);
    }

    /**
     * Append condition parameters to the play URL.
     * Currently supports: audio_track
     */
    private String appendConditions(String url, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        if (conditions.containsKey("audio_track")) {
            sb.append(url.contains("?") ? "&" : "?")
                .append("audio_track=")
                .append(conditions.get("audio_track"));
        }
        String result = sb.toString();
        log.debug("Appended conditions, final URL: {}", result);
        return result;
    }
}
