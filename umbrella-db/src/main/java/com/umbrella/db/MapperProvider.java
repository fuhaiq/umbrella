package com.umbrella.db;

import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.ibatis.session.SqlSessionManager;

public class MapperProvider<T> implements Provider<T> {

	private final Class<T> mapperType;
	
	@Inject
	private SqlSessionManager sqlSessionManager;

	public MapperProvider(Class<T> mapperType) {
		this.mapperType = mapperType;
	}

	@Override
	public T get() {
		return this.sqlSessionManager.getMapper(mapperType);
	}

}
