package com.umbrella.document;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Component
public class ReferenceService {
	
	@Autowired
	private Supplier<QueryParser> searchQueryParser;
	
	@Autowired
	private IndexSearcher searcher;

	@Autowired
	private ReferenceConfig config;
	
	public List<Map<String, String>> search(String value) throws IOException, ParseException {
		if(Strings.isNullOrEmpty(value)) {
			throw new IllegalArgumentException("query is not exist");
		}
		QueryParser parser = searchQueryParser.get();
		return search(value, parser);
	}
	
	private List<Map<String, String>> search(String value, QueryParser parser) throws IOException, ParseException {
		List<Map<String, String>> result = Lists.newArrayList();
		TopDocs results = searcher.search(parser.parse(value), config.getNum());
		ScoreDoc[] hits = results.scoreDocs;
		for (int i = 0; i < hits.length; i++) {
			Map<String, String> map = Maps.newHashMap();
			Document doc = searcher.doc(hits[i].doc);
			map.put("title", doc.get("title"));
			map.put("uri", doc.get("uri"));
			map.put("label", doc.get("label"));
			map.put("summary", doc.get("summary"));
			result.add(map);
		}
		return result;
	}

}
