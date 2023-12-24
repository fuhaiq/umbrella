package org.umbrella.query.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.arrow.flight.CallOption;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightClientMiddleware;
import org.apache.arrow.flight.Location;
import org.apache.arrow.flight.auth2.BasicAuthCredentialWriter;
import org.apache.arrow.flight.auth2.ClientBearerHeaderHandler;
import org.apache.arrow.flight.auth2.ClientIncomingAuthHeaderMiddleware;
import org.apache.arrow.flight.client.ClientCookieMiddleware;
import org.apache.arrow.flight.grpc.CredentialCallOption;
import org.apache.arrow.memory.BufferAllocator;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.tools.StopWatchListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DremioConfiguration {

    @Value("${spring.datasource.dremio.flight.host:localhost}")
    private String flightHost;
    @Value("${spring.datasource.dremio.flight.port:32010}")
    private int flightPort;
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
    @Bean
    public ClientIncomingAuthHeaderMiddleware.Factory authFactory() {
        return new ClientIncomingAuthHeaderMiddleware.Factory(new ClientBearerHeaderHandler());
    }

    /**
     * Dremio 只读,不用配置连接池,单例即可
     */
    @Bean(destroyMethod = "close")
    public FlightClient flightClient(
            BufferAllocator allocator,
            FlightClientMiddleware.Factory authFactory,
            @Qualifier("dremio_hikariConfig") HikariConfig dremio_hikariConfig) {
        var flightClientBuilder = FlightClient.builder()
                .allocator(allocator)
                .location(Location.forGrpcInsecure(flightHost, flightPort));

        var cookieFactory = new ClientCookieMiddleware.Factory();
        flightClientBuilder.intercept(cookieFactory);
        flightClientBuilder.intercept(authFactory);

        var flightClient = flightClientBuilder.build();
        List<CallOption> callOptions = new ArrayList<>();
        callOptions.add(new CredentialCallOption(new BasicAuthCredentialWriter(dremio_hikariConfig.getUsername(), dremio_hikariConfig.getPassword())));
        flightClient.handshake(callOptions.toArray(new CallOption[callOptions.size()]));

        return flightClient;
    }
}
