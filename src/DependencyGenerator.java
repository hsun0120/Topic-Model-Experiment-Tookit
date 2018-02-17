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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
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
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Haoran Sun
 * @since 01-22-2018
 * 
 * A class that generates Universal Chinese Dependencies using Stanford parser
 * and extracts subject phrases, verbs, object phrases, subject-verb pairs,
 * verb-object pairs, and subject-object pairs.
 */
public class DependencyGenerator {
	private StanfordCoreNLP corenlp;
	
	/**
	 * Default constructor. Initialize Stanford parser using settings in
	 * "chinese.properties"
	 */
	public DependencyGenerator() {
		Properties props = new Properties();
		try {
			props.load(IOUtils.readerFromString("chinese.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.corenlp = new StanfordCoreNLP(props);
	}
	
	/**
	 * Generate phrases and dependencies using Stanford parser.
	 * @param file - input file to process
	 * @param criteria - phrase extraction criteria
	 * @param outputDirs - a list of output directory names
	 */
	public void generate(File file, List<GrammaticalRelation> criteria, String[]
			outputDirs) {
		String text = this.preporcess(file.getPath());
		Annotation document = new Annotation(text);
		this.corenlp.annotate(document);
		
		OutputStreamWriter[] writers = new OutputStreamWriter[outputDirs.length];
		for(int i = 0; i < outputDirs.length; i++)
			try {
				writers[i] = new OutputStreamWriter(new 
						FileOutputStream(outputDirs[i] + file.getName()), 
						StandardCharsets.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			SemanticGraph dependencies = sentence.get
					(EnhancedDependenciesAnnotation.class);
			this.extractPhrases(dependencies, criteria, writers);
		}
		
		for(OutputStreamWriter writer: writers)
			try {
				writer.close();
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
	
	private void extractPhrases(SemanticGraph graph, List<GrammaticalRelation>
	criteria, OutputStreamWriter[] writers) {
		/* Get all specified relationships from graph */
		List<SemanticGraphEdge> list = graph.findAllRelns(
				UniversalChineseGrammaticalRelations.NOMINAL_SUBJECT);
		list.addAll(graph.findAllRelns(
				UniversalChineseGrammaticalRelations.NOMINAL_PASSIVE_SUBJECT));
		if(list == null || list.size() == 0) return; //error condition
		
		ListIterator<SemanticGraphEdge> it = list.listIterator();
		List<SemanticGraphEdge> allObjEdges = graph.findAllRelns
				(UniversalChineseGrammaticalRelations.DIRECT_OBJECT);
		HashSet<SemanticGraphEdge> allVerbObjs = new HashSet<>();
		allVerbObjs.addAll(allObjEdges);
		HashMap<IndexedWord, String> dict = new HashMap<>();
		
		try {
			while(it.hasNext()) { //Extract word pairs connect to subjects
				LinkedList<SemanticGraphEdge> verbObjects = new LinkedList<>();
				LinkedList<String> subjects = new LinkedList<>();
				SemanticGraphEdge currEdge = it.next();
				IndexedWord subject = currEdge.getDependent(); //Get subject
				subjects.add(this.extendToPhrase(graph, subject, criteria));

				/* Find subjects linked by conjunctions */
				this.findConjunctPhrases(graph, subject, criteria, subjects);

				LinkedList<String> objPhrases = new LinkedList<>();
				this.findObjs(graph, currEdge.getGovernor(), criteria, verbObjects,
						objPhrases); //Find all verb-object relations
				allVerbObjs.removeAll(verbObjects); //Remove all dobj edges found
				this.writeOutput(subjects, objPhrases, verbObjects, writers);
				ListIterator<String> itPhrase = objPhrases.listIterator();
				for(SemanticGraphEdge edge : verbObjects) //Store all verb-object pairs
					dict.put(edge.getGovernor(), itPhrase.next());

				this.handleCopulaSentence(graph, currEdge, subjects, criteria,
						writers);
			}

			/* Write verb and object afterward to avoid duplication */
			Set<Entry<IndexedWord, String>> voPairs = dict.entrySet();
			for(Entry<IndexedWord, String> entry : voPairs) {
				writers[1].write(entry.getKey().word() + " "); //verb
				writers[2].write(entry.getValue() + " "); //object
			}

			/* Extract the remaining verb-object pairs */
			for(SemanticGraphEdge verbObject: allVerbObjs) {
				writers[1].write(verbObject.getGovernor().word() + " "); //verb
				writers[2].write(verbObject.getDependent().word() + " "); //object
				writers[4].write(verbObject.getGovernor().word() + "-" + 
						verbObject.getDependent().word() + " "); //verb-object
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private void writeOutput(LinkedList<String> subjects, LinkedList<String>
	objPhrases, LinkedList<SemanticGraphEdge> verbObjects, OutputStreamWriter[]
			writers) {
		HashSet<String> printed = new HashSet<>();
		for(String subject: subjects) {
			try {
				writers[0].write(subject + " ");
				ListIterator<String> it = objPhrases.listIterator();
				for(SemanticGraphEdge verbObject: verbObjects) {
					String objPhrase = it.next();
					String sv = subject + "-" + verbObject.getGovernor().word() + " ";
					if(printed.add(sv))
						writers[3].write(sv); //subject-verb
					String vo = verbObject.getGovernor().word() + "-" + objPhrase + " ";
					if(printed.add(vo))
						writers[4].write(vo); //verb-object
					String so = subject + "-" + objPhrase + " ";
					if(printed.add(so))
						writers[5].write(so); //subject-object
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void findObjs(SemanticGraph graph, IndexedWord parent, 
			List<GrammaticalRelation> criteria, List<SemanticGraphEdge> verbObjects,
			List<String> objPhrases) {
		Iterator<SemanticGraphEdge> it = graph.outgoingEdgeIterator(parent);
		while(it.hasNext()) {
			SemanticGraphEdge currEdge = it.next();
			if(currEdge.getRelation().equals(UniversalChineseGrammaticalRelations.
					DIRECT_OBJECT)) {
				LinkedList<SemanticGraphEdge> edges = new LinkedList<>();
				this.findConjunctVerbs(graph, currEdge.getGovernor(), 
						currEdge.getDependent(), edges);
				for(SemanticGraphEdge edge : edges) {
					verbObjects.add(edge);
					objPhrases.add(this.extendToPhrase(graph, edge.getDependent(),
							criteria));
					int count = objPhrases.size();
					this.findConjunctPhrases(graph, edge.getDependent(), criteria,
							objPhrases);
					/* Include corresponding number of verbs */
					for(int i = 0; i < objPhrases.size() - count; i++)
						verbObjects.add(edge);
				}
			} else
				this.findObjs(graph, currEdge.getDependent(), criteria, verbObjects,
						objPhrases);
		}
	}
	
	private void handleCopulaSentence(SemanticGraph graph, SemanticGraphEdge
			currEdge, LinkedList<String> subjects, List<GrammaticalRelation>
	criteria, OutputStreamWriter[] writers) throws IOException {
		IndexedWord cop = graph.getChildWithReln(currEdge.getGovernor(), 
				UniversalChineseGrammaticalRelations.COPULA);
		if(cop == null) return;
		
		LinkedList<String> objPhrases = new LinkedList<>();
		writers[1].write(cop.word() + " ");
		objPhrases = new LinkedList<>();
		objPhrases.add(this.extendToPhrase(graph, currEdge.getGovernor(),
				criteria));
		this.findConjunctPhrases(graph, currEdge.getGovernor(), criteria,
				objPhrases);
		for(String phrase: subjects) {
			writers[3].write(phrase + "-" + cop.word() + " ");
			for(String objPhrase : objPhrases)
				writers[5].write(phrase + "-" + objPhrase + " ");
		}
		for(String objPhrase : objPhrases) {
			writers[2].write(objPhrase + " ");
			writers[4].write(cop.word() + "-" + objPhrase + " ");
		}
	}
	
	private void handlePassiveSentence(SemanticGraph graph, 
			List<GrammaticalRelation> criteria, List<String> subjects) {
		/* Passive subjects should be treated as objects in normal sentence */
		List<SemanticGraphEdge> passSbjs = graph.findAllRelns
				(UniversalChineseGrammaticalRelations.NOMINAL_PASSIVE_SUBJECT);
		LinkedList<String> nsubjs = new LinkedList<>();
		ListIterator<SemanticGraphEdge> it = passSbjs.listIterator();
		while(it.hasNext()) {
			LinkedList<SemanticGraphEdge> verbObjects = new LinkedList<>();
			LinkedList<String> objPhrases = new LinkedList<>();
			SemanticGraphEdge nsubjPassReln = it.next();
		}
	}
	
	private String extendToPhrase(SemanticGraph graph, IndexedWord candidate,
			List<GrammaticalRelation> criteria) {
		LinkedList<IndexedWord> result = new LinkedList<>();
		this.DFS(graph, candidate, criteria, result);
		Collections.sort(result, (a, b) -> a.index() > b.index() ? 1 : -1);
		ListIterator<IndexedWord> iter = result.listIterator(result.size());
		int countDown = result.getLast().index();
		StringBuilder sb = new StringBuilder();
		while(iter.hasPrevious()) { //Iterate from the end
			IndexedWord word = iter.previous();
			if(word.index() != countDown) break; //Check continuity
			sb.insert(0, word.word());
			countDown--;
		}
		return sb.toString();
	}
	
	private void DFS(SemanticGraph graph, IndexedWord parent, 
			List<GrammaticalRelation> criteria, List<IndexedWord> result) {
		result.add(parent);
		Set<IndexedWord> children = graph.getChildrenWithRelns(parent, 
				criteria);
		if(children.size() == 0) return; //Base case
		Iterator<IndexedWord> it = children.iterator();
		while(it.hasNext()) {
			this.DFS(graph, it.next(), criteria, result);
		}
	}
	
	private void findConjunctPhrases(SemanticGraph graph, IndexedWord parent,
			List<GrammaticalRelation> criteria, List<String> result) {
		Set<IndexedWord> children = graph.getChildrenWithReln(parent,
				UniversalChineseGrammaticalRelations.CONJUNCT);
		Iterator<IndexedWord> it = children.iterator();
		while(it.hasNext()) {
			IndexedWord candidate = it.next();
			LinkedList<IndexedWord> phrases = new LinkedList<>();
			this.DFS(graph, candidate, criteria, phrases);
			Collections.sort(phrases, (a, b) -> a.index() > b.index() ? 1 : -1);
			ListIterator<IndexedWord> iter = phrases.listIterator(phrases.size());
			int countDown = phrases.getLast().index();
			StringBuilder sb = new StringBuilder();
			while(iter.hasPrevious()) { //Iterate from the end
				IndexedWord word = iter.previous();
				if(word.index() != countDown) break; //Check continuity
				sb.insert(0, word.word());
				countDown--;
			}
			result.add(sb.toString());
			
			this.findConjunctPhrases(graph, candidate, criteria, result);
		}
	}
	
	private void findConjunctVerbs(SemanticGraph graph, IndexedWord verb, 
			IndexedWord object, List<SemanticGraphEdge> results) {
		results.add(new SemanticGraphEdge(verb, object,
				UniversalChineseGrammaticalRelations.DIRECT_OBJECT, 0, true));
		Set<IndexedWord> set = graph.getParentsWithReln(verb, 
				UniversalChineseGrammaticalRelations.CONJUNCT);
		for(IndexedWord conjVerb: set) {
			this.findConjunctVerbs(graph, conjVerb, object, results);
		}
	}
	
	public static void main(String[] args) {
		File dir = new File("news");
		DependencyGenerator gen = new DependencyGenerator();
		ArrayList<GrammaticalRelation> criteria = new ArrayList<>();
		criteria.add(UniversalChineseGrammaticalRelations.NOUN_COMPOUND);
		criteria.add(UniversalChineseGrammaticalRelations.ADJECTIVAL_MODIFIER);
		criteria.add(UniversalChineseGrammaticalRelations.CASE);
		criteria.add(UniversalChineseGrammaticalRelations.MARK);
		criteria.add(UniversalChineseGrammaticalRelations.CLAUSAL_MODIFIER);
		criteria.add(UniversalChineseGrammaticalRelations.ASSOCIATIVE_MODIFIER);
		String[] outputDirs = {"S/", "V/", "O/", "SV/", "VO/", "SO/"};
		for(final File file : dir.listFiles())
			gen.generate(file, criteria, outputDirs);
	}
}