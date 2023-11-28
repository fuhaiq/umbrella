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
public class MysqlConfiguration {

    @Bean(name = "mysql_hikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public HikariConfig mysql_hikariConfig() {
        return new HikariConfig();
    }

    @Bean(name = "mysql_ds", destroyMethod = "close")
    public HikariDataSource mysql_ds(HikariConfig mysql_hikariConfig) {
        return new HikariDataSource(mysql_hikariConfig);
    }

    @Bean(name = "mysql_ds_conn_provider")
    public DataSourceConnectionProvider mysql_ds_conn_provider(HikariDataSource mysql_ds){
        return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(mysql_ds));
    }

    @Bean(name = "mysqlTx")
    public PlatformTransactionManager mysqlTx(HikariDataSource mysql_ds) {
        return new DataSourceTransactionManager(mysql_ds);
    }

    @Bean(name = "mysql_tx_provider")
    public SpringTransactionProvider mysql_tx_provider(PlatformTransactionManager mysqlTx) {
        return new SpringTransactionProvider(mysqlTx);
    }

    @Bean(name = "mysql_listener_provider")
    public ExecuteListenerProvider mysql_listener_provider() {
        return StopWatchListener::new;
    }

    @Bean(name = "mysql_jooq_conf")
    public DefaultConfiguration mysql_jooq_conf(ConnectionProvider mysql_ds_conn_provider, TransactionProvider mysql_tx_provider,
             ObjectProvider<ExecuteListenerProvider> mysql_listener_provider) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.MYSQL);
        configuration.set(mysql_ds_conn_provider);
        configuration.set(mysql_tx_provider);
        configuration.set(mysql_listener_provider.orderedStream().toArray(ExecuteListenerProvider[]::new));
        return configuration;
    }

    @Bean(name = "mysql")
    public DefaultDSLContext mysql(org.jooq.Configuration mysql_jooq_conf) {
        return new DefaultDSLContext(mysql_jooq_conf);
    }
}
