import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.international.pennchinese.UniversalChineseGrammaticalRelations;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class DependencyGenerator {
	public void generate(File file) {
		String text = this.preporcess(file.getPath());
		Annotation document = new Annotation(text);
		// Setup Chinese Properties by loading them from classpath resources
		Properties props = new Properties();
		try {
			props.load(IOUtils.readerFromString("StanfordCoreNLP-chinese.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		StanfordCoreNLP corenlp = new StanfordCoreNLP(props);
		corenlp.annotate(document);
		try (OutputStreamWriter writer = new OutputStreamWriter(new 
				FileOutputStream(file.getName() + ".txt"), StandardCharsets.UTF_8)){
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			for(CoreMap sentence: sentences) {
				SemanticGraph dependencies = sentence.get
						(EnhancedPlusPlusDependenciesAnnotation.class);
				ArrayList<GrammaticalRelation> criteria = new ArrayList<>();
				criteria.add(UniversalChineseGrammaticalRelations.NOUN_COMPOUND);
				criteria.add(UniversalChineseGrammaticalRelations.ADJECTIVAL_MODIFIER);
				criteria.add(UniversalChineseGrammaticalRelations.CASE);
				criteria.add(UniversalChineseGrammaticalRelations.MARK);
				writer.write(this.extractPhrases(dependencies, "subject", criteria));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String preporcess(String path) {
		StringBuilder sb = new StringBuilder();
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new 
				FileInputStream(path), StandardCharsets.UTF_8))) {
			String line = null;
			while((line = reader.readLine()) != null)
				sb.append(line);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString().replace(" ", "");
	}
	
	private String listToPhrase(List<String> phrase) {
		ListIterator<String> it = phrase.listIterator();
		StringBuilder sb = new StringBuilder();
		while(it.hasNext())
			sb.append(it.next());
		return sb.toString();
	}
	
	private String extractPhrases(SemanticGraph graph, String relation,
			List<GrammaticalRelation> criteria) {
		/* Get all specified relationships from graph */
		List<SemanticGraphEdge> list = null;
		if(relation.equals("subject")) {
			list = graph.findAllRelns(
					UniversalChineseGrammaticalRelations.NOMINAL_SUBJECT);
			list.addAll(graph.findAllRelns(
					UniversalChineseGrammaticalRelations.NOMINAL_PASSIVE_SUBJECT));
		} else if(relation.equals("object")) {
			list = graph.findAllRelns(
					GrammaticalRelation.valueOf("dobj"));
		}
		if(list == null || list.size() == 0) return ""; //error condition
		
		ListIterator<SemanticGraphEdge> it = list.listIterator();
		StringBuilder sb = new StringBuilder();
		while(it.hasNext()) {
			IndexedWord subject = it.next().getDependent();
			Set<IndexedWord> children = graph.getChildrenWithRelns(subject, 
					criteria);
			if(!children.isEmpty()) {
				IndexedWord[] words = new IndexedWord[children.size()];
				words = children.toArray(words);
				Arrays.sort(words, (a, b) -> a.index() > b.index() ? 1 : -1);
				List<String> phrase = new LinkedList<>();
				int countDown = words[words.length - 1].index();
				for(IndexedWord word: words) { //Iterate from the end
					if(word.index() != countDown) break; //Check continuity
						phrase.add(0, word.word());
					countDown--;
				}
				sb.append(this.listToPhrase(phrase));
			}
			sb.append(subject.word() + " ");
			
			/* Find subjects linked by conjunctions */
			sb.append(this.findConjunctPhrases(graph, subject, criteria));
		}
		return sb.toString();
	}
	
	private String findConjunctPhrases(SemanticGraph graph, IndexedWord parent,
			List<GrammaticalRelation> criteria) {
		Set<IndexedWord> children = graph.getChildrenWithReln(parent,
				UniversalChineseGrammaticalRelations.CONJUNCT);
		Iterator<IndexedWord> it = children.iterator();
		StringBuilder sb = new StringBuilder();
		while(it.hasNext()) {
			IndexedWord candidate = it.next();
			Set<IndexedWord> components = graph.getChildrenWithRelns(candidate, 
					criteria);
			if(!components.isEmpty()) {
				IndexedWord[] words = new IndexedWord[components.size()];
				words = components.toArray(words);
				Arrays.sort(words, (a, b) -> a.index() > b.index() ? 1 : -1);
				List<String> phrase = new LinkedList<>();
				int countDown = words[words.length - 1].index();
				for(IndexedWord word: words) { //Iterate from the end
					if(word.index() != countDown) break; //Check continuity
						phrase.add(0, word.word());
					countDown--;
				}
				sb.append(this.listToPhrase(phrase));
			}
			sb.append(candidate.word() + " ");
		}
		return sb.toString();
	}
	
	public static void main(String[] args) {
		File dir = new File("news");
		DependencyGenerator gen = new DependencyGenerator();
		for(final File file : dir.listFiles())
			gen.generate(file);
	}
}