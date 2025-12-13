package run.ikaros.plugin.pan115.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.plugin.pan115.Pan115Const;
import run.ikaros.plugin.pan115.utils.JsonUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static run.ikaros.plugin.pan115.Pan115Const.*;

@Slf4j
@Component
public class DefaultPan115Repository implements Pan115Repository {
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders headers = new HttpHeaders();

    @Override
    public boolean assertDomainReachable() {
        try {
            restTemplate
                    .exchange(Pan115Const.API_BASE, HttpMethod.GET,
                            new HttpEntity<>(null, headers), Map.class);
            return true;
        } catch (HttpClientErrorException exception) {
            return exception.getStatusCode() == HttpStatus.NOT_FOUND;
        }
    }

    @Override
    public void refreshHttpHeaders(String accessToken) {
        log.info("refresh rest template headers...");
        headers.clear();
        headers.set(HttpHeaders.USER_AGENT, REST_TEMPLATE_USER_AGENT);
        headers.set(HttpHeaders.COOKIE, "chii_searchDateLine=0");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(accessToken)) {
            log.info("update http head access token");
            headers.set(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + accessToken);
        }
    }

    @Override
    public List<Pan115Attachment> openUFileFiles(String cid, Integer limit, Integer cur, Integer showDir) {
        final String url = API_BASE + " /open/ufile/files";
        try {
            String result = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(null, headers), String.class).getBody();
            log.debug("Pull [{}] result is [{}].", url, result);
            if (StringUtils.isBlank(result)) {
                return null;
            }
            Map map = JsonUtils.json2obj(result, Map.class);
            Object data = map.remove("data");
            log.debug("Pull [{}] result data is [{}].", url, data);
            Pan115Attachment[] pan115Attachments =
                    JsonUtils.json2ObjArr(JsonUtils.obj2Json(data), new TypeReference<>() {
                    });
            if (pan115Attachments == null) {
                return List.of();
            }

            return List.of(pan115Attachments);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("pan115 attachment not found for cid={}", cid);
                return null;
            }
            throw exception;
        }
    }

}
