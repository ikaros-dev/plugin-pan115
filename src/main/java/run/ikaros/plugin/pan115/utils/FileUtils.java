package run.ikaros.plugin.pan115.utils;

import run.ikaros.api.constant.FileConst;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class FileUtils extends run.ikaros.api.infra.utils.FileUtils {
    static final Set<String> IMAGES =
            Arrays.stream(FileConst.Postfix.IMAGES).collect(Collectors.toSet());

    public static boolean isImage(String url) {
        return IMAGES.contains(parseFilePostfix(url));
    }
}
