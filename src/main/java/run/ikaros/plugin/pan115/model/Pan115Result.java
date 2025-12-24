package run.ikaros.plugin.pan115.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pan115Result<T> {
    private Boolean state;
    private String message;
    private T data;
    private Integer code;
}
