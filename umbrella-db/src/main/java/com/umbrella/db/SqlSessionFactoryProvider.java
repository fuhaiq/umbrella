package com.umbrella.db;

import java.io.IOException;

import org.apache.ibatis.session.SqlSessionFactory;

import com.google.inject.throwingproviders.CheckedProvider;

public interface SqlSessionFactoryProvider extends CheckedProvider<SqlSessionFactory>{

	@Override
	SqlSessionFactory get() throws IOException;
	
}
