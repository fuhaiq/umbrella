package org.umbrella.query.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.tools.StopWatchListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DuckDBConfiguration {

    @Bean(name = "duckdb_hikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.duckdb")
    public HikariConfig duckdb_hikariConfig() {
        return new HikariConfig();
    }

    @Bean(name = "duckdb_ds", destroyMethod = "close")
    public HikariDataSource duckdb_ds(HikariConfig duckdb_hikariConfig) {
        return new HikariDataSource(duckdb_hikariConfig);
    }

    @Bean(name = "duckdb_jooq_conf")
    public DefaultConfiguration duckdb_jooq_conf(HikariDataSource duckdb_ds) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.DEFAULT);
        configuration.set(new DataSourceConnectionProvider(duckdb_ds));
        configuration.set(StopWatchListener::new);
        return configuration;
    }

    @Bean(name = "duckdb")
    public DefaultDSLContext duckdb(org.jooq.Configuration duckdb_jooq_conf) {
        return new DefaultDSLContext(duckdb_jooq_conf);
    }
}
