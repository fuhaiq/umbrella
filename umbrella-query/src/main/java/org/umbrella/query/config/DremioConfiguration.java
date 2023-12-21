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
public class DremioConfiguration {

    @Bean(name = "dremio_hikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.dremio")
    public HikariConfig dremio_hikariConfig() {
        return new HikariConfig();
    }

    @Bean(name = "dremio_ds", destroyMethod = "close")
    public HikariDataSource dremio_ds(HikariConfig dremio_hikariConfig) {
        return new HikariDataSource(dremio_hikariConfig);
    }

    @Bean(name = "dremio_jooq_conf")
    public DefaultConfiguration dremio_jooq_conf(HikariDataSource dremio_ds) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.DEFAULT);
        configuration.set(new DataSourceConnectionProvider(dremio_ds));
        configuration.set(StopWatchListener::new);
        return configuration;
    }

    @Bean(name = "dremio")
    public DefaultDSLContext dremio(org.jooq.Configuration dremio_jooq_conf) {
        return new DefaultDSLContext(dremio_jooq_conf);
    }
}
