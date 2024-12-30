package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.saver.AppSaver;

/**
 * DSLAdapter defined the mutual conversion between specific DSL(e.g.) and {@link App}
 * model.
 */
public interface DSLAdapter {

	/**
	 * Turn app into DSL
	 * @param app {@link App}
	 * @return the specific dialect DSL
	 */
	String exportDSL(App app);

	/**
	 * Turn DSL into app
	 * @param dsl a specific formatted string
	 * @return unified app model {@link AppSaver}
	 */
	App importDSL(String dsl);

	/**
	 * Judge if current implementation supports this dialect
	 * @param dialectType a specific dsl format, see {@link DSLDialectType}
	 * @return if supports return true, otherwise return false
	 */
	Boolean supportDialect(DSLDialectType dialectType);

}
