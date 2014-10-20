package com.umbrella.db;

import java.io.IOException;
import java.io.InputStream;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

public class DBModule extends AbstractModule{

	private final String config;
	
	public DBModule(String config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		install(ThrowingProviderBinder.forModule(this)); 
	}

	@CheckedProvides(SqlSessionFactoryProvider.class)
	@Singleton
	SqlSessionFactory provideSqlSessionFactory() throws IOException {
		SqlSessionFactoryBuilder factory = new SqlSessionFactoryBuilder();
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(config)) {
			return factory.build(in);
		}
	}
}
