package run.ikaros.plugin.pan115;


import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.ikaros.api.core.task.TaskOperate;
import run.ikaros.api.plugin.BasePlugin;

import java.util.Random;

@Slf4j
@Component
public class Pan115Plugin extends BasePlugin {
    private final TaskOperate taskOperate;

    public Pan115Plugin(PluginWrapper wrapper, TaskOperate taskOperate) {
        super(wrapper);
        this.taskOperate = taskOperate;
    }

    @Override
    public void start() {
        log.info("plugin [Pan115Plugin] start success");
        // submitParallelTask();
    }

    private void submitParallelTask() {
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            taskOperate.submit(getWrapper().getPluginId() + "-parallel-" + i,
                    new Runnable() {
                        @Override
                        public void run() {
                            log.info("Submit task with index:{}", finalI);
                            try {
                                Thread.sleep(new Random().nextLong(0, 5000));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            log.info("Finish task with index:{}", finalI);

                        }
                    })
                    .subscribe();
        }
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