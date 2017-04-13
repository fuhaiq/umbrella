package com.umbrella.document;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReferenceConfiguration {

	@Autowired
	@Bean(destroyMethod = "close")
	public Analyzer provideAnalyzer() {
		return new StandardAnalyzer(Version.LUCENE_36);
	}

	@Autowired
	@Bean(destroyMethod = "close")
	public Directory provideDirectory(ReferenceConfig config) throws IOException {
		return new SimpleFSDirectory(new File(config.getPath()));
	}

	@Autowired
	@Bean(destroyMethod = "close")
	public IndexReader provideIndexReader(Directory directory) throws IOException {
		return IndexReader.open(directory);
	}

	@Autowired
	@Bean(destroyMethod = "close")
	public IndexSearcher provideIndexSearcher(IndexReader reader) {
		return new IndexSearcher(reader);
	}
	
	@Autowired
	@Bean
	public Supplier<QueryParser> searchQueryParser(Analyzer analyzer) {
		return () -> {
			return new MultiFieldQueryParser(Version.LUCENE_36, new String[] 
					{ "title", "uri", "url", "label", "stemmed_title", "content"}
			, analyzer);
		};
	}
	
}
