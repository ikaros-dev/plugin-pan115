package run.ikaros.plugin.pan115.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pan115Path {
    private Long file_id;
    private String file_name;
    private String iss;
}
