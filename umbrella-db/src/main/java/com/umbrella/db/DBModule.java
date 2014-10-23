package com.umbrella.db;

import static com.google.inject.util.Providers.guicify;
import static org.apache.ibatis.session.SqlSessionManager.newInstance;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionManager;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class DBModule extends AbstractModule {

	private final String config;

	public DBModule(String config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		SqlSessionFactory factory = null;
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(config)) {
			factory = new SqlSessionFactoryBuilder().build(in);
			bind(SqlSessionFactory.class).toInstance(factory);
		} catch (IOException e) {
			addError(e);
		}
		bind(SqlSessionManager.class).toInstance(newInstance(factory));
		factory.getConfiguration().getMapperRegistry().getMappers().stream().forEach(r -> bindMapper(r));
	}

	private <T> void bindMapper(Class<T> mapperType) {
		bind(mapperType).toProvider(guicify(new MapperProvider<T>(mapperType))).in(Scopes.SINGLETON);
	}
	

}
