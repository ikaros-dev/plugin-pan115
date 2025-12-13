package run.ikaros.plugin.pan115;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import run.ikaros.api.custom.scheme.CustomSchemeManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomSchemeRegister implements InitializingBean {
    private final CustomSchemeManager customSchemeManager;

    public CustomSchemeRegister(CustomSchemeManager customSchemeManager) {
        this.customSchemeManager = customSchemeManager;
    }

    @Override
    public void afterPropertiesSet() {
        customSchemeManager.register(AttachmentPanCustom.class);
    }
}
