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
import com.riot.percentiles.partitioner.RangePartitioner;
import com.riot.percentiles.pojo.AccessLog;
import com.yahoo.sketches.sampling.ReservoirItemsSketch;

@Configuration
public class ApplicationConfiguration {

	@Value("${log.dir:/var/log/httpd}")
	private String dir;
	
	@Value("${shuffle.partitions:5}")
	private int partitions;
	
	@Bean
	public RangePartitioner getPartitioner() throws IOException {
		return new RangePartitioner(getReservoirSampling());
	}
	
	/**
	 * walk through all access logs then build sample using Reservoir Sampling algorithm</br>
	 * time-complexity = O(N)</br>
	 * space-complexity = O(N)</br>
	 * @return sample list
	 * @throws IOException
	 */
	public List<Integer> getReservoirSampling() throws IOException {
		ReservoirItemsSketch<Integer> sketch = ReservoirItemsSketch.newInstance(partitions);
		Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
				try (Reader reader = new FileReader(path.toFile())) {
					List<AccessLog> logs = new CsvToBeanBuilder<AccessLog>(reader).withType(AccessLog.class).build()
							.parse();
					logs.forEach(log -> sketch.update(log.getResponseTime()));
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return Lists.newArrayList(sketch.getSamples());
	}

}
