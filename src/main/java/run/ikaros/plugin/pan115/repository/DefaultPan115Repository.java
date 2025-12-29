package run.ikaros.plugin.pan115.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.plugin.pan115.Pan115Const;
import run.ikaros.plugin.pan115.exception.Pan115RequestFailException;
import run.ikaros.plugin.pan115.model.Pan115Attachment;
import run.ikaros.plugin.pan115.model.Pan115Folder;
import run.ikaros.plugin.pan115.model.Pan115Path;
import run.ikaros.plugin.pan115.model.Pan115Result;
import run.ikaros.plugin.pan115.utils.JsonUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static run.ikaros.plugin.pan115.Pan115Const.*;

@Slf4j
@Component
public class DefaultPan115Repository implements Pan115Repository, DisposableBean {
    private WebClient webClient = WebClient.create(Pan115Const.API_BASE);
    private final DelayQueueExpiringMap<String, String> downloadUrlCacheMap = new DelayQueueExpiringMap<>();

    @Override
    public Mono<Boolean> assertDomainReachable() {
        return webClient.get()
                .uri("/")
                .accept(MediaType.ALL)
                .exchangeToMono(rsp -> Mono.just(rsp.statusCode().equals(HttpStatus.NOT_FOUND)))
                .retry(3);
    }

    @Override
    public void refreshHttpHeaders(String accessToken) {
        log.debug("refresh webclient headers...");
        // 增大内存缓冲区限制，例如设为10MB
        final int size = 10 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(size))
                .build();
        webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .baseUrl(Pan115Const.API_BASE)
                .defaultHeader(HttpHeaders.USER_AGENT, REST_TEMPLATE_USER_AGENT)
                .defaultHeader(HttpHeaders.COOKIE, "chii_searchDateLine=0")
                .defaultHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + accessToken)
                .build();

    }

    @Override
    public Flux<Pan115Attachment> openUFileFiles(String cid, Integer limit, Integer cur, Integer showDir) {
        final String url = API_BASE + "/open/ufile/files";
        UriComponentsBuilder uriComponentsBuilder =
                UriComponentsBuilder.fromUri(URI.create(url))
                        .queryParam("cid", cid)
                        .queryParam("limit", limit)
                        .queryParam("show_dir", showDir)
                        .queryParam("cur", cur);
        return webClient.get()
                .uri(uriComponentsBuilder.toUriString())
                .exchangeToMono(rsp -> rsp.bodyToMono(String.class))
                .flatMapMany(result -> {
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
                    if (pan115Attachments == null) return Flux.empty();
                    Object pathObj = map.remove("path");
                    if (pathObj != null) {
                        Pan115Path[] parentPath = JsonUtils.json2ObjArr(JsonUtils.obj2Json(pathObj), new TypeReference<>() {
                        });
                        for (Pan115Attachment pan115Attachment : pan115Attachments) {
                            List<Pan115Path> newPaths = new ArrayList<>();
                            Pan115Path lastPath = new Pan115Path();
                            lastPath.setFile_id(Long.parseLong(pan115Attachment.getFid()));
                            lastPath.setFile_name(pan115Attachment.getFn());
                            lastPath.setIss(String.valueOf(pan115Attachment.getIss()));
                            for (Pan115Path pan115Path : parentPath) {
                                newPaths.add(pan115Path);
                            }
                            newPaths.add(lastPath);
                            pan115Attachment.setPath(newPaths);
                        }
                    }
                    return Flux.fromStream(Arrays.stream(pan115Attachments));
                })
                .doOnError(HttpClientErrorException.class, e ->
                        log.warn("pan115 attachment fail for cid={}", cid));
    }

    @Override
    public Mono<Pan115Folder> openFolderGetInfo(String fileId) {
        final String url = API_BASE + "/open/folder/get_info";
        UriComponentsBuilder uriComponentsBuilder =
                UriComponentsBuilder.fromUri(URI.create(url))
                        .queryParam("file_id", fileId);
        return webClient.get()
                .uri(url)
                .exchangeToMono(rsp -> rsp.bodyToMono(Pan115Result.class))
                .filter(Objects::nonNull)
                .filter(Pan115Result::getState)
                .flatMap(result -> {
                    if (!result.getState()) {
                        return Mono.error(new Pan115RequestFailException(result.getMessage(), result.getCode()));
                    }
                    LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) result.getData();
                    Object pathsObj = data.remove("paths");
                    Pan115Path[] paths = JsonUtils.json2ObjArr(JsonUtils.obj2Json(pathsObj), new TypeReference<>() {
                    });
                    Pan115Folder pan115Folder = JsonUtils.json2obj(JsonUtils.obj2Json(data), Pan115Folder.class);
                    if (paths != null && pan115Folder != null) {
                        pan115Folder.setPaths(List.of(paths));
                    }
                    log.debug("Pull [{}] result is [{}] with fileId=[{}].", url, pan115Folder, fileId);
                    return Mono.just(pan115Folder);
                })
                .doOnError(HttpClientErrorException.class, e ->
                        log.warn("req pan115 folder fail for fileId={}", fileId));
    }

    @Override
    public Mono<AttachmentDriver> refreshToken(AttachmentDriver driver) {
        final String refreshToken = driver.getRefreshToken();
        refreshHttpHeaders(driver.getAccessToken());
        Assert.hasText(refreshToken, "'refreshToken' must has text.");
        final String url = "https://passportapi.115.com/open/refreshToken";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.put("refresh_token", List.of(refreshToken));
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .exchangeToMono(rsp -> rsp.bodyToMono(Pan115Result.class))
                .filter(r -> Objects.nonNull(r) && r.getState())
                .flatMap(r -> {
                    if (!r.getState()) {
                        return Mono.error(new Pan115RequestFailException(r.getMessage(), r.getCode()));
                    }

                    Map<String, Object> dataMap = (Map<String, Object>) r.getData();
                    driver.setAccessToken(dataMap.get("access_token").toString());
                    driver.setRefreshToken(dataMap.get("refresh_token").toString());
                    driver.setExpireTime(LocalDateTime.now().plusSeconds(Long.parseLong(dataMap.get("expires_in").toString())));
                    log.debug("Pull [{}] result is [{}].", url, r);
                    log.info("Refresh pan115 token for driver={}", driver.getMountName());
                    return Mono.just(driver);
                })
                .doOnSuccess(driver1 -> refreshHttpHeaders(driver1.getAccessToken()))
                .doOnError(HttpClientErrorException.class, e ->
                        log.warn("req pan115 refresh token fail for drier={}", driver.getId()));
    }

    @Override
    public Mono<String> openUFileDownUrl(String pickCode) {
        Assert.hasText(pickCode, "'pickCode' must has text.");
        final String url = API_BASE + "/open/ufile/downurl";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("pick_code", String.valueOf(pickCode));
        return webClient.post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .exchangeToMono(rsp -> rsp.bodyToMono(Pan115Result.class))
                .filter(r -> Objects.nonNull(r) && r.getState())
                .flatMap(r -> {
                    if (!r.getState()) {
                        return Mono.error(new Pan115RequestFailException(r.getMessage(), r.getCode()));
                    }
                    Map<String, Object> dataMap = (Map<String, Object>) r.getData();
                    String key = dataMap.keySet().stream().findFirst().get();
                    Map<String, LinkedHashMap> map = JsonUtils.json2obj(JsonUtils.obj2Json(dataMap.get(key)), LinkedHashMap.class);
                    LinkedHashMap url1 = map.get("url");
                    Object url2 = url1.get("url");
                    log.debug("Pull [{}] result is [{}].", url, r);
                    return Mono.just(String.valueOf(url2));
                })
                .doOnError(HttpClientErrorException.class, e ->
                        log.warn("req pan115 openUFileDownUrl fail for pickCode={}", pickCode));
    }

    @Override
    public Mono<String> openVideoPlay(String pickCode) {
        Assert.hasText(pickCode, "'pickCode' must has text.");
        final String url = API_BASE + "/open/video/play";
        UriComponentsBuilder uriComponentsBuilder =
                UriComponentsBuilder.fromUri(URI.create(url))
                        .queryParam("pick_code", pickCode);
        return webClient.get()
                .uri(uriComponentsBuilder.toUriString())
                .exchangeToMono(rsp -> rsp.bodyToMono(Pan115Result.class))
                .filter(r -> Objects.nonNull(r) && r.getState())
                .flatMap(r -> {
                    if (!r.getState()) {
                        return Mono.error(new Pan115RequestFailException(r.getMessage(), r.getCode()));
                    }
                    log.debug("Pull [{}] result is [{}].", url, r);
                    Map<String, Object> dataMap = (Map<String, Object>) r.getData();
                    Object videoUrls = dataMap.get("video_url");
                    LinkedHashMap[] videoUrlMaps = JsonUtils.obj2Arr(videoUrls, new TypeReference<>() {
                    });
                    if (videoUrlMaps == null || videoUrlMaps.length == 0) return Mono.empty();
                    Object url1 = videoUrlMaps[0].get("url");
                    return Mono.just(String.valueOf(url1));
                })
                .doOnError(HttpClientErrorException.class, e ->
                        log.warn("pan115 request fail for openUFileDownUrl with pickCode={}", pickCode));
    }

    @Override
    public Flux<DataBuffer> openUFileSteam(String pickCode) {
        Assert.hasText(pickCode, "'pickCode' must has text.");
        if (downloadUrlCacheMap.containsKey(pickCode)) {
            return streamFile(downloadUrlCacheMap.get(pickCode));
        } else {
            return openUFileDownUrl(pickCode)
                    .map(url -> {
                        downloadUrlCacheMap.put(pickCode, url);
                        return url;
                    })
                    .flatMapMany(this::streamFile);
        }
    }

    @Override
    public Flux<DataBuffer> openUFileSteamWithRange(String pickCode, long start, long end) {
        Assert.hasText(pickCode, "'pickCode' must has text.");
        if (downloadUrlCacheMap.containsKey(pickCode)) {
            return streamFileWithRange(downloadUrlCacheMap.get(pickCode), start, end);
        } else {
            return openUFileDownUrl(pickCode)
                .map(url -> {
                    downloadUrlCacheMap.put(pickCode, url);
                    return url;
                })
                .flatMapMany(url -> streamFileWithRange(url, start, end));
        }

    }

    private Flux<DataBuffer> streamFileWithRange(String uFileDownUrl, Long start, Long end) {
        // 设置 Range 请求头
        String rangeHeader;
        if (start != null && end != null) {
            rangeHeader = String.format("bytes=%d-%d", start, end);
        } else if (start != null) {
            rangeHeader = String.format("bytes=%d-", start);
        } else {
            rangeHeader = "bytes=0-"; // 默认从头开始
        }

        String newUrl = uFileDownUrl;
        if (StringUtils.isNoneBlank(newUrl)) {
            newUrl = URLDecoder.decode(newUrl, StandardCharsets.UTF_8);
        }
        return webClient.get()
                .uri(newUrl)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.RANGE, rangeHeader)
                .retrieve()
                .bodyToFlux(DataBuffer.class);
    }


    public Flux<DataBuffer> streamFile(String fileUrl) {
        String newUrl = fileUrl;
        if (StringUtils.isNoneBlank(newUrl)) {
            newUrl = URLDecoder.decode(newUrl, StandardCharsets.UTF_8);
        }
        return webClient.get()
                .uri(newUrl)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class);
    }

    @Override
    public void destroy() throws Exception {
        downloadUrlCacheMap.shutdown();
    }

}
