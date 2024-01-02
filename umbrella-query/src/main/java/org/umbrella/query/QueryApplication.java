package org.umbrella.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;

@SpringBootApplication(
    exclude = {
      DataSourceAutoConfiguration.class,
      JooqAutoConfiguration.class,
      TransactionAutoConfiguration.class
    })
public class QueryApplication {
  public static void main(String[] args) {
    SpringApplication.run(QueryApplication.class, args);
  }
}
