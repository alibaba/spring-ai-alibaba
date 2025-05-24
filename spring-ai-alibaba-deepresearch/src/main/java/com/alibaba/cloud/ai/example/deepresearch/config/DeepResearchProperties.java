package com.alibaba.cloud.ai.example.deepresearch.config;

import com.alibaba.cloud.ai.example.deepresearch.model.BackgroundInvestigationType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Allen Hu
 * @date 2025/5/24
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.deepreserch")
public class DeepResearchProperties {

	/**
	 * Set the type of background investigation node. Default is: just_web_search
	 */
	private BackgroundInvestigationType backgroundInvestigationType = BackgroundInvestigationType.JUST_WEB_SEARCH;

	public BackgroundInvestigationType getBackgroundInvestigationType() {
		return backgroundInvestigationType;
	}

	public void setBackgroundInvestigationType(BackgroundInvestigationType backgroundInvestigationType) {
		this.backgroundInvestigationType = backgroundInvestigationType;
	}

}
