package org.umbrella.query.config;

import org.apache.arrow.driver.jdbc.ArrowFlightJdbcConnectionPoolDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.tools.StopWatchListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class DremioConfiguration {

    @Bean(name = "dremio_properties")
    @ConfigurationProperties(prefix = "spring.datasource.dremio")
    public Properties dremio_properties() {
        return new Properties();
    }

    @Bean(name = "dremio_ds", destroyMethod = "close")
    public ArrowFlightJdbcConnectionPoolDataSource dremio_ds(Properties dremio_properties) {
        return ArrowFlightJdbcConnectionPoolDataSource.createNewDataSource(dremio_properties);
    }

//    @Bean(name = "dremio_jooq_conf")
//    public DefaultConfiguration dremio_jooq_conf(ArrowFlightJdbcConnectionPoolDataSource dremio_ds) {
//        DefaultConfiguration configuration = new DefaultConfiguration();
//        configuration.set(SQLDialect.DEFAULT);
//        configuration.set(new DataSourceConnectionProvider(dremio_ds));
//        configuration.set(StopWatchListener::new);
//        return configuration;
//    }
//
//    @Bean(name = "dremio")
//    public DefaultDSLContext dremio(org.jooq.Configuration dremio_jooq_conf) {
//        return new DefaultDSLContext(dremio_jooq_conf);
//    }
}
