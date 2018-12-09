package com.riot.percentiles.analyzer;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opencsv.bean.CsvToBeanBuilder;
import com.riot.percentiles.partitioner.RangePartitioner;
import com.riot.percentiles.pojo.AccessLog;
import com.riot.percentiles.pojo.ShuffleFile;
import com.riot.percentiles.utils.HttpMethod;

@Component
public class ResponseTimeAnalyzer {

	private final Log logger = LogFactory.getLog(ResponseTimeAnalyzer.class);

	@Value("${log.dir:/var/log/httpd}")
	private String dir;

	@Value("${shuffle.dir:/tmp/shuffles}")
	private String shuffle_dir;

	@Value("${shuffle.prefix:shuffle-}")
	private String shuffle_prefix;

	@Value("${shuffle.deleteOnExit:true}")
	private boolean deleteOnExit;

	@Autowired
	private RangePartitioner partitioner;

	// total number of READ access logs
	private long N;

	// list of shuffle file with number of access logs
	private List<ShuffleFile> shuffle_files = Lists.newArrayList();

	private final List<Double> percents = Lists.newArrayList(0.9d, 0.95d, 0.99d);

	/**
	 * calculates the 90%, 95% and 99% percentile response time for READ API requests</br>
	 * time-complexity = O(2P+(N/P))</br>
	 * space-complexity = O(2P)</br>
	 * @throws IOException
	 */
	@PostConstruct
	public void analyse() throws IOException {
		Path tempShuffleDir = shuffle();
		sortShuffleFiles(tempShuffleDir);

		// sort shuffle files by extension name
		shuffle_files = shuffle_files.stream().sorted((a, b) -> {
			int partitionA = Integer.valueOf(FilenameUtils.getExtension(a.getPath().toFile().getName()));
			int partitionB = Integer.valueOf(FilenameUtils.getExtension(b.getPath().toFile().getName()));
			return partitionA - partitionB;
		}).collect(Collectors.toList());

		for (Double percent : percents) {
			int target = new Double(percent * N).intValue();
			int current = 0;
			locate: for (ShuffleFile file : shuffle_files) {
				current += file.getNumberOfLog();
				// locate line number in shuffle file
				if (target <= current) {
					int line = file.getNumberOfLog() - (current - target);
					int responseTime = Integer.valueOf(Files.readAllLines(file.getPath()).get(line - 1));
					logger.info(percent * 100 + "% of requests return a response in " + responseTime + " ms ");
					break locate;
				}
			}
		}
	}

	/**
	 * shuffle all access log according range partitioner, generate shuffle files under temp shuffle dir</br>
	 * time-complexity = O(P + N)</br>
	 * space-complexity = O(2*(P+N))</br>
	 * @return
	 * @throws IOException
	 */
	private Path shuffle() throws IOException {
		// create temp shuffle dir
		Path tempShuffleDir = Files.createTempDirectory(Paths.get(shuffle_dir), shuffle_prefix);
		if (deleteOnExit) {
			tempShuffleDir.toFile().deleteOnExit();
		}

		// create temp shuffle files, map partition index with file writer
		Map<Integer, PrintWriter> partition2FileWriter = Maps.newHashMap();
		for (int i = 0; i < partitioner.numPartitions() + 1; i++) {
			Path tmpShufflefile = Files.createTempFile(tempShuffleDir, shuffle_prefix, "." + i);
			partition2FileWriter.put(i, new PrintWriter(tmpShufflefile.toFile()));
			if (deleteOnExit) {
				tmpShufflefile.toFile().deleteOnExit();
			}
		}

		// walk through log dir, read each access log file then filter all READ request
		Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
				try (Reader reader = new FileReader(path.toFile())) {
					List<AccessLog> logs = new CsvToBeanBuilder<AccessLog>(reader).withType(AccessLog.class).build()
							.parse().stream().filter(log -> log.getHttpMethod() == HttpMethod.GET)
							.collect(Collectors.toList());
					logs.forEach(log -> {
						// calculate partition index using range partitioner
						int partition = partitioner.getPartition(log);
						// write response time into corresponding shuffle file
						partition2FileWriter.get(partition).println(log.getResponseTime());
					});
				}
				return FileVisitResult.CONTINUE;
			}
		});

		// close all shuffle files' writers
		partition2FileWriter.forEach((k, writer) -> writer.close());
		return tempShuffleDir;
	}

	/**
	 * sort every shuffle file under temp shuffle dir</br>
	 * time-complexity = O(2N + P)</br>
	 * space-complexity = O(N + P)</br>
	 * <i>P refers to number of partitions.
	 * @param temp shuffle dir
	 * @throws IOException
	 */
	private void sortShuffleFiles(Path tempShuffleDir) throws IOException {
		Files.walkFileTree(tempShuffleDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
				ShuffleFile shuffle_file = new ShuffleFile(path);
				Stream<Integer> stream = Files.readAllLines(path, Charsets.UTF_8).stream().map(Integer::valueOf)
						.sorted();
				try (PrintWriter writer = new PrintWriter(path.toFile())) {
					stream.forEach(responseTime -> {
						writer.println(responseTime);
						shuffle_file.add();
						N++;
					});
				}
				shuffle_files.add(shuffle_file);
				return FileVisitResult.CONTINUE;
			}
		});
	}

}
