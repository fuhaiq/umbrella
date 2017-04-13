package com.umbrella.document;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;

@Controller
@ComponentScan
@EnableAutoConfiguration
public class Reference {
	
	@Autowired
	private ReferenceService service;
	
	@RequestMapping("/")
    String home(@RequestParam(required = false) String q, Model model) throws IOException, ParseException {
		if(Strings.isNullOrEmpty(q)) {
			return "redirect:/language";
		}
		q = q.replaceAll("\\p{Punct}", "");
		if(Strings.isNullOrEmpty(q)) {
			return "redirect:/language";
		}
		List<Map<String, String>> result = service.search(q);
		if(result.size() > 0) {
			model.addAttribute("top", result.get(0));
			result.remove(0);
		}
		model.addAttribute("result", result);
        return "search";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Reference.class, args);
    }

}
