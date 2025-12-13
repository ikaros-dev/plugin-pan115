package run.ikaros.plugin.pan115;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Path115 {
    private String name;
    private Long aid;
    private Long cid;
    private Long pid;
    private Long isp;
    private String p_cid;
    private String fv;
}
