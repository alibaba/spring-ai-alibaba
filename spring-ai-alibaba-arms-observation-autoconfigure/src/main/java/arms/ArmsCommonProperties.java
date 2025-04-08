package arms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Lumian
 * */
@ConfigurationProperties(ArmsCommonProperties.CONFIG_PREFIX)
public class ArmsCommonProperties {

  /**
   * Spring AI Alibaba ARMS extension configuration prefix.
   * */
  public static final String CONFIG_PREFIX = "spring.ai.alibaba.arms";

  /**
   * Enable Arms instrumentations and conventions.
   */
  private boolean enabled = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
