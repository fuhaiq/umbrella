package org.umbrella.query.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.ConnectionProvider;
import org.jooq.ExecuteListenerProvider;
import org.jooq.SQLDialect;
import org.jooq.TransactionProvider;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.tools.StopWatchListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jooq.SpringTransactionProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

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

    @Bean(name = "duckdb_ds_conn_provider")
    public DataSourceConnectionProvider duckdb_ds_conn_provider(HikariDataSource duckdb_ds){
        return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(duckdb_ds));
    }

    @Bean(name = "duckdbTx")
    public PlatformTransactionManager duckdbTx(HikariDataSource duckdb_ds) {
        return new DataSourceTransactionManager(duckdb_ds);
    }

    @Bean(name = "duckdb_tx_provider")
    public SpringTransactionProvider duckdb_tx_provider(PlatformTransactionManager duckdbTx) {
        return new SpringTransactionProvider(duckdbTx);
    }

    @Bean(name = "duckdb_listener_provider")
    public ExecuteListenerProvider duckdb_listener_provider() {
        return StopWatchListener::new;
    }

    @Bean(name = "duckdb_jooq_conf")
    public DefaultConfiguration duckdb_jooq_conf(ConnectionProvider duckdb_ds_conn_provider, TransactionProvider duckdb_tx_provider,
             ObjectProvider<ExecuteListenerProvider> duckdb_listener_provider) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.DEFAULT);
        configuration.set(duckdb_ds_conn_provider);
        configuration.set(duckdb_tx_provider);
        configuration.set(duckdb_listener_provider.orderedStream().toArray(ExecuteListenerProvider[]::new));
        return configuration;
    }

    @Bean(name = "duckdb")
    public DefaultDSLContext duckdb(org.jooq.Configuration duckdb_jooq_conf) {
        return new DefaultDSLContext(duckdb_jooq_conf);
    }
}
