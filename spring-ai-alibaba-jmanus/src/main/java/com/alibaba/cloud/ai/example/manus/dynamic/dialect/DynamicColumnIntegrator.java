package com.alibaba.cloud.ai.example.manus.dynamic.dialect;

import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecordEntity;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.stereotype.Component;

/**
 * auth: dahua time: 20250809
 */
@Component
public class DynamicColumnIntegrator implements Integrator {

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		JdbcServices jdbcServices = serviceRegistry.getService(JdbcServices.class);
		Dialect dialect = jdbcServices.getDialect();
		PersistentClass persistentClass = metadata.getEntityBinding(PlanExecutionRecordEntity.class.getName());
		if (persistentClass == null) {
			return;
		}
		Property contentProperty = persistentClass.getProperty("planExecutionRecord");
		if (contentProperty == null) {
			return;
		}
		contentProperty.getColumns().forEach(column -> {
			if (dialect instanceof PostgreSQLDialect) { // pg
				column.setSqlType("TEXT");
			}
			else if (dialect instanceof MySQLDialect) { // mysql
				column.setSqlType("LONGTEXT");
			}
			else if (dialect instanceof H2Dialect) { // h2
				column.setSqlType("LONGTEXT");
			}
		});
	}

	@Override
	public void disintegrate(@UnknownKeyFor @NonNull @Initialized SessionFactoryImplementor sessionFactory,
			@UnknownKeyFor @NonNull @Initialized SessionFactoryServiceRegistry serviceRegistry) {
	}

}
