import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class DependencyGenerator {
	public static void generate(File file) {
		String text = DependencyGenerator.preporcess(file.getPath());
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
		try (FileOutputStream os = new FileOutputStream(file.getName() + ".txt")){
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			for(CoreMap sentence: sentences) {
				SemanticGraph dependencies = sentence.get
						(EnhancedDependenciesAnnotation.class);
				List<SemanticGraphEdge> list = dependencies.edgeListSorted();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String preporcess(String path) {
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
	
	private static void extractSubject(List<SemanticGraphEdge> list) {
		ListIterator<SemanticGraphEdge> it = list.listIterator();
		while(it.hasNext()) {
			SemanticGraphEdge token = it.next();
			if(token.getRelation().getShortName().equals("nsubj")) continue;
		}
	}
	
	private static void extractObject(List<SemanticGraphEdge> list) {
		
	}
	
	public static void main(String[] args) {
		File dir = new File("news");
		for(final File file : dir.listFiles())
			DependencyGenerator.generate(file);
	}
}