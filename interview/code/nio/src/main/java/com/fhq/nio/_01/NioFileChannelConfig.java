package com.fhq.nio._01;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

@Configuration
public class NioFileChannelConfig {
	
	@Value("${fhq._01.path}")
	private String path;

	@Bean(destroyMethod = "close")
	public RandomAccessFile randomAccessFile() throws FileNotFoundException {
		return new RandomAccessFile(ResourceUtils.getFile(path), "rw");
	}
	
}
