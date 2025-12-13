package run.ikaros.plugin.pan115;
import run.ikaros.api.custom.Custom;
import run.ikaros.api.custom.Name;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Custom(group = "run.ikaros.plugin.pan115", version = "v1",
        kind = "AttachmentPanCustom", singular = "attachment_pan", plural = "attachment_pans")
public class AttachmentPanCustom {
    @Name
    private String title;
    private Long attId;
    private String fid;
    private String pid;
    private String fc;
    private String fn;
    private String fs;
    private String ico;
    private String pc;
    private String sha1;
    private String path;
    private Long playLong;
    private Long count;
}
