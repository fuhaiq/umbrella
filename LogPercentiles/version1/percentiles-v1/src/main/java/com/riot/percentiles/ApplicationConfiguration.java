package com.riot.percentiles;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Lists;
import com.opencsv.bean.CsvToBeanBuilder;
import com.riot.percentiles.pojo.AccessLog;

@Configuration
public class ApplicationConfiguration {

	@Value("${log.dir:/var/log/httpd}")
	private String dir;

	/**
	 * reads all access logs from each file under the target folder, and parses each log to AccessLog</br>
	 * time-complexity = O(N)</br>
	 * space-complexity = O(N)</br>
	 * <i>N refers to the total size of all logs under target folder</i>
	 * @return list of access logs
	 * @throws IOException
	 */
	@Bean
	public List<AccessLog> readFromDir() throws IOException {
		List<AccessLog> result = Lists.newArrayList();
		Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
				try (Reader reader = new FileReader(path.toFile())) {
					List<AccessLog> logs = new CsvToBeanBuilder<AccessLog>(reader).withType(AccessLog.class).build()
							.parse();
					result.addAll(logs);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return result;
	}

}
