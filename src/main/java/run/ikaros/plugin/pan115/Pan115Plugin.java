package run.ikaros.plugin.pan115;


import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.plugin.BasePlugin;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Component
public class Pan115Plugin extends BasePlugin {
    private final Pan115AttachmentDriverFetcher driverFetcher;

    public Pan115Plugin(PluginWrapper wrapper, Pan115AttachmentDriverFetcher driverFetcher) {
        super(wrapper);
        this.driverFetcher = driverFetcher;
    }

    @Override
    public void start() {
        log.info("plugin [Pan115Plugin] start success");
    }

    public Disposable startRefreshTokenTask() {
        return Flux.interval(Duration.ofMinutes(30))
                .flatMap(tick -> refreshTokenTask())
                .subscribeOn(Schedulers.newSingle("RefreshTokenTask", true))
                .subscribe();
    }

    private Mono<Void> refreshTokenTask() {
        return driverFetcher.checkoutAllDriverToken();
    }

    @Override
    public void stop() {
        log.info("plugin [Pan115Plugin] stop success");
    }

    @Override
    public void delete() {
        log.info("plugin [Pan115Plugin] delete success");
    }
}