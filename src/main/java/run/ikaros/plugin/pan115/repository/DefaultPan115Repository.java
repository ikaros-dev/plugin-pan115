package run.ikaros.plugin.pan115.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.plugin.pan115.Pan115Const;
import run.ikaros.plugin.pan115.exception.Pan115RequestFailException;
import run.ikaros.plugin.pan115.model.Pan115Attachment;
import run.ikaros.plugin.pan115.model.Pan115Folder;
import run.ikaros.plugin.pan115.model.Pan115Path;
import run.ikaros.plugin.pan115.model.Pan115Result;
import run.ikaros.plugin.pan115.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static run.ikaros.plugin.pan115.Pan115Const.*;

@Slf4j
@Component
public class DefaultPan115Repository implements Pan115Repository {
    private final RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
    private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
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
        log.debug("refresh rest template headers...");
        headers.clear();
        headers.set(HttpHeaders.USER_AGENT, REST_TEMPLATE_USER_AGENT);
        headers.set(HttpHeaders.COOKIE, "chii_searchDateLine=0");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(accessToken)) {
            log.debug("update http head access token");
            headers.set(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + accessToken);
        }
    }

    @Override
    public List<Pan115Attachment> openUFileFiles(String cid, Integer limit, Integer cur, Integer showDir) {
        final String url = API_BASE + "/open/ufile/files";
        try {
            UriComponentsBuilder uriComponentsBuilder =
                    UriComponentsBuilder.fromUri(URI.create(url))
                            .queryParam("cid", cid)
                            .queryParam("limit", limit)
                            .queryParam("show_dir", showDir)
                            .queryParam("cur", cur);
            String result = restTemplate.exchange(uriComponentsBuilder.toUriString(), HttpMethod.GET,
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

    @Override
    public Pan115Folder openFolderGetInfo(String fileId) {
        final String url = API_BASE + "/open/folder/get_info";
        try {
            UriComponentsBuilder uriComponentsBuilder =
                    UriComponentsBuilder.fromUri(URI.create(url))
                            .queryParam("file_id", fileId);
            Pan115Result<LinkedHashMap<String, Object>> result = restTemplate.exchange(
                    uriComponentsBuilder.toUriString(), HttpMethod.GET,
                    new HttpEntity<>(null, headers), Pan115Result.class).getBody();
            if (result == null || !result.getState()) {
                throw new Pan115RequestFailException("Req pan115 openFolderGetInfo fail: " + result.getMessage(), result.getCode());
            }
            LinkedHashMap<String, Object> data = result.getData();
            Object pathsObj = data.remove("paths");
            Pan115Path[] paths = JsonUtils.json2ObjArr(JsonUtils.obj2Json(pathsObj), new TypeReference<>() {
            });
            Pan115Folder pan115Folder = JsonUtils.json2obj(JsonUtils.obj2Json(data), Pan115Folder.class);
            if (paths != null && pan115Folder != null) {
                pan115Folder.setPaths(List.of(paths));
            }

            log.debug("Pull [{}] result is [{}] with fileId=[{}].", url, pan115Folder, fileId);
            return pan115Folder;
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("pan115 folder not found for fileId={}", fileId);
                return null;
            }
            throw exception;
        }
    }

    @Override
    public void refreshToken(AttachmentDriver driver) {
        final String refreshToken = driver.getRefreshToken();
        refreshHttpHeaders(driver.getAccessToken());
        Assert.hasText(refreshToken, "'refreshToken' must has text.");
        final String url = "https://passportapi.115.com/open/refreshToken";
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.put("refresh_token", List.of(refreshToken));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            Pan115Result<LinkedHashMap<String, Object>> result = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Pan115Result.class).getBody();
            if (result == null || !result.getState()) {
                log.error("Refresh pan115 token fail for result:{}", result);
                throw new Pan115RequestFailException("Refresh pan115 token fail: " + result.getMessage(), result.getCode());
            }
            Map<String, Object> dataMap = result.getData();
            driver.setAccessToken(dataMap.get("access_token").toString());
            driver.setRefreshToken(dataMap.get("refresh_token").toString());
            driver.setExpireTime(LocalDateTime.now().plusSeconds(Long.parseLong(dataMap.get("expires_in").toString())));
            log.debug("Pull [{}] result is [{}].", url, result);
            log.info("Refresh pan115 token for driver={}", driver.getMountName());
        } catch (HttpClientErrorException exception) {
            log.warn("pan115 request fail for refresh token with driver={}", driver);
            throw exception;
        } finally {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
    }

    @Override
    public String openUFileDownUrl(String pickCode) {
        Assert.hasText(pickCode, "'pickCode' must has text.");
        final String url = API_BASE + "/open/ufile/downurl";
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("pick_code", String.valueOf(pickCode));
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            Pan115Result<LinkedHashMap<String, Object>> result = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Pan115Result.class).getBody();
            if (result == null || !result.getState()) {
                throw new Pan115RequestFailException("Req openUFileDownUrl fail: " + result.getMessage(), result.getCode());
            }
            Map<String, Object> dataMap = result.getData();
            String key = dataMap.keySet().stream().findFirst().get();
            Map<String, LinkedHashMap> map = JsonUtils.json2obj(JsonUtils.obj2Json(dataMap.get(key)), LinkedHashMap.class);
            LinkedHashMap url1 = map.get("url");
            Object url2 = url1.get("url");
            log.debug("Pull [{}] result is [{}].", url, result);
            return url2.toString();
        } catch (HttpClientErrorException exception) {
            log.warn("pan115 request fail for openUFileDownUrl with pickCode={}", pickCode);
            throw exception;
        } finally {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
    }

    @Override
    public String openVideoPlay(String pickCode) {
        Assert.hasText(pickCode, "'pickCode' must has text.");
        final String url = API_BASE + "/open/video/play";
        try {
            UriComponentsBuilder uriComponentsBuilder =
                    UriComponentsBuilder.fromUri(URI.create(url))
                            .queryParam("pick_code", pickCode);
            Pan115Result<LinkedHashMap<String, Object>> result = restTemplate.exchange(
                    uriComponentsBuilder.toUriString(), HttpMethod.GET,
                    new HttpEntity<>(null, headers), Pan115Result.class).getBody();
            if (result == null || !result.getState()) {
                throw new Pan115RequestFailException("Req openUFileDownUrl fail: " + result.getMessage(), result.getCode());
            }
            log.debug("Pull [{}] result is [{}].", url, result);
            Map<String, Object> dataMap = result.getData();
            Object videoUrls = dataMap.get("video_url");
            LinkedHashMap[] videoUrlMaps = JsonUtils.obj2Arr(videoUrls, new TypeReference<>() {
            });
            if (videoUrlMaps == null || videoUrlMaps.length == 0) return "";
            Object url1 = videoUrlMaps[0].get("url");
            return url1.toString();
        } catch (HttpClientErrorException exception) {
            log.warn("pan115 request fail for openUFileDownUrl with pickCode={}", pickCode);
            throw exception;
        } finally {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
    }

    @Override
    public Flux<DataBuffer> openUFileSteam(String pickCode) {
        Assert.hasText(pickCode, "'pickCode' must has text.");
        final String uFileDownUrl = openUFileDownUrl(pickCode);
        return streamFileWithRestTemplate(uFileDownUrl);
        // return Flux.create(sink -> {
        //     Schedulers.boundedElastic().schedule(() -> {
        //         try {
        //             ResponseEntity<byte[]> response = restTemplate.getForEntity(
        //                     uFileDownUrl, byte[].class);
        //             if (response.getStatusCode() == HttpStatus.OK &&
        //                     response.getBody() != null) {
        //                 // 一次性发送所有数据（适合小文件）
        //                 DataBuffer dataBuffer = dataBufferFactory.allocateBuffer(
        //                         response.getBody().length);
        //                 dataBuffer.write(response.getBody());
        //                 sink.next(dataBuffer);
        //                 sink.complete();
        //             } else {
        //                 sink.error(new RuntimeException(
        //                         "请求失败，状态码: " + response.getStatusCode()));
        //             }
        //         } catch (Exception e) {
        //             sink.error(e);
        //         }
        //     });
        // });
    }


    /**
     * 使用 RestTemplate 获取文件流，转换为 Flux<DataBuffer>
     *
     * @param fileUrl   文件URL
     * @param chunkSize 分块大小（字节），默认为 8192
     * @return Flux<DataBuffer>
     */
    public Flux<DataBuffer> streamFileWithRestTemplate(String fileUrl, int chunkSize) {
        if (chunkSize <= 0) {
            chunkSize = 8192; // 默认8KB
        }

        final int finalChunkSize = chunkSize;

        return Flux.create(sink -> {
            // 使用 boundedElastic 调度器执行阻塞操作
            Schedulers.boundedElastic().schedule(() -> {
                try {
                    String newUrl = fileUrl;
                    if (StringUtils.isNoneBlank(newUrl)) {
                        newUrl = URLDecoder.decode(newUrl, StandardCharsets.UTF_8);
                    }
                    log.debug("Open stream for url: {}", newUrl);
                    restTemplate.execute(newUrl, HttpMethod.GET, new RequestCallback() {
                                @Override
                                public void doWithRequest(ClientHttpRequest request) throws IOException {
                                    request.getHeaders().addAll(headers);
                                }
                            },
                            (ResponseExtractor<Void>) response -> {
                                if (response.getStatusCode() != HttpStatus.OK) {
                                    sink.error(new RuntimeException(
                                            "请求失败，状态码: " + response.getStatusCode()));
                                    return null;
                                }

                                try (InputStream inputStream = response.getBody()) {
                                    byte[] buffer = new byte[finalChunkSize];
                                    int bytesRead;
                                    AtomicBoolean isCanceled = new AtomicBoolean(false);

                                    sink.onCancel(() -> {
                                        isCanceled.set(true);
                                        try {
                                            inputStream.close();
                                        } catch (IOException e) {
                                            // 忽略关闭异常
                                        }
                                    });

                                    while (!isCanceled.get() &&
                                            (bytesRead = inputStream.read(buffer)) != -1) {

                                        // 创建 DataBuffer
                                        DataBuffer dataBuffer = dataBufferFactory.allocateBuffer(bytesRead);
                                        dataBuffer.write(buffer, 0, bytesRead);

                                        // 发布数据块
                                        sink.next(dataBuffer);

                                        // 检查是否取消
                                        if (sink.isCancelled()) {
                                            log.debug("Close stream for url: {}", fileUrl);
                                            break;
                                        }
                                    }

                                    if (!isCanceled.get()) {
                                        sink.complete();
                                    }
                                } catch (Exception e) {
                                    if (!sink.isCancelled()) {
                                        sink.error(e);
                                    }
                                }
                                return null;
                            });
                } catch (Exception e) {
                    if (!sink.isCancelled()) {
                        sink.error(e);
                    }
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    /**
     * 简化版本：使用默认分块大小
     */
    public Flux<DataBuffer> streamFileWithRestTemplate(String fileUrl) {
        return streamFileWithRestTemplate(fileUrl, 8192);
    }
}
