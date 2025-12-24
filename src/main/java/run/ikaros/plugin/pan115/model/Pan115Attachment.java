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
public class Pan115Attachment {
    private String fid;
    private String aid;
    private String pid;
    private String fc;
    private String fn;
    private String fco;
    private String ism;
    private Long isp;
    private Long iss;
    private String pc;
    private Long upt;
    private Long uet;
    private Long uppt;
    private Long cm;
    private String fdesc;
    private Long ispl;
    private Long fvs;
    private Long fuuid;
    private Long opt;
    private List<Object> fl;
    private Long issct;
    private Long is_top;
    private String sha1;
    private Long fs;
    private String fta;
    private String ico;
    private String fatr;
    private Long isv;
    private Long def;
    private Long def2;
    private Long play_Long;
    private String v_img;
    private String thumb;
    private String uo;
}
