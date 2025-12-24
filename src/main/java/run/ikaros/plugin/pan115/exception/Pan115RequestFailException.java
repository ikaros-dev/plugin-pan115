package run.ikaros.plugin.pan115.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class Pan115RequestFailException extends HttpClientErrorException {
    public Pan115RequestFailException(HttpStatusCode statusCode) {
        super(statusCode);
    }

    public Pan115RequestFailException(HttpStatusCode statusCode, String statusText) {
        super(statusCode, statusText);
    }

    public Pan115RequestFailException(HttpStatusCode statusCode, String statusText, byte[] body, Charset responseCharset) {
        super(statusCode, statusText, body, responseCharset);
    }

    public Pan115RequestFailException(HttpStatusCode statusCode, String statusText, HttpHeaders headers, byte[] body, Charset responseCharset) {
        super(statusCode, statusText, headers, body, responseCharset);
    }

    public Pan115RequestFailException(String message, HttpStatusCode statusCode, String statusText, HttpHeaders headers, byte[] body, Charset responseCharset) {
        super(message, statusCode, statusText, headers, body, responseCharset);
    }

    public Pan115RequestFailException(String msg, Integer code) {
        super(HttpStatusCode.valueOf(401), "code:" + code + ";msg:" + msg);
    }
}
