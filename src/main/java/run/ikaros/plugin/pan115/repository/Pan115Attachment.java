package run.ikaros.plugin.pan115.repository;

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
    private int isp;
    private int iss;
    private String pc;
    private int upt;
    private int uet;
    private int uppt;
    private int cm;
    private String fdesc;
    private int ispl;
    private int fvs;
    private int fuuid;
    private int opt;
    private List<Object> fl;
    private int issct;
    private int is_top;
    private String sha1;
    private int fs;
    private String fta;
    private String ico;
    private String fatr;
    private int isv;
    private int def;
    private int def2;
    private long play_long;
    private String v_img;
    private String thumb;
    private String uo;
}
