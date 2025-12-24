package run.ikaros.plugin.pan115.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pan115Folder {
    private String count;
    private String size;
    private Long size_byte;
    private String folder_count;
    private Long play_long;
    private Long show_play_long;
    private String ptime;
    private String utime;
    private String file_name;
    private String pick_code;
    private String sha1;
    private String file_id;
    private String is_mark;
    private Long open_time;
    private String file_category;
    private List<Pan115Path> paths;
}
