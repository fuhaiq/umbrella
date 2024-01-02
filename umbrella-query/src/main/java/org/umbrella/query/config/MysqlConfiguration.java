package org.umbrella.query.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.jooq.TransactionProvider;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.tools.StopWatchListener;
import org.springframework.boot.autoconfigure.jooq.SpringTransactionProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
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

  @Bean(name = "mysql_ds_trx")
  public TransactionAwareDataSourceProxy mysql_ds_trx(HikariDataSource mysql_ds) {
    return new TransactionAwareDataSourceProxy(mysql_ds);
  }

  @Bean(name = "mysql_trx_mgr")
  public PlatformTransactionManager mysql_trx_mgr(TransactionAwareDataSourceProxy mysql_ds_trx) {
    return new DataSourceTransactionManager(mysql_ds_trx);
  }

  @Bean(name = "mysql_trx_pro")
  public TransactionProvider mysql_trx_pro(PlatformTransactionManager mysql_trx_mgr) {
    return new SpringTransactionProvider(mysql_trx_mgr);
  }

  @Bean(name = "mysql_jooq_conf")
  public DefaultConfiguration mysql_jooq_conf(
      TransactionAwareDataSourceProxy mysql_ds_trx, TransactionProvider mysql_trx_pro) {
    DefaultConfiguration configuration = new DefaultConfiguration();
    configuration.set(SQLDialect.MYSQL);
    configuration.set(new DataSourceConnectionProvider(mysql_ds_trx));
    configuration.set(mysql_trx_pro);
    configuration.set(StopWatchListener::new);
    return configuration;
  }

  @Bean(name = "mysql")
  public DefaultDSLContext mysql(org.jooq.Configuration mysql_jooq_conf) {
    return new DefaultDSLContext(mysql_jooq_conf);
  }
}
