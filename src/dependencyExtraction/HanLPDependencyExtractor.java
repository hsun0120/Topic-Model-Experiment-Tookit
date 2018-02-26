package dependencyExtraction;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Scanner;

import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import com.hankcs.hanlp.dependency.IDependencyParser;
import com.hankcs.hanlp.dependency.nnparser.NeuralNetworkDependencyParser;
import com.hankcs.hanlp.seg.CRF.CRFSegment;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.tokenizer.IndexTokenizer;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;

/**
 * A class that generates and output dependency pairs based on HanLP
 * dependency parsers. Refactored from previous project.
 * @author Haoran Sun
 * @since 02-25-2018
 */
public class HanLPDependencyExtractor {
  static final String PUNCT= "(?<=[¡££¿£» ])";
  static final String SV = "SBV";
  static final String VO = "VOB";
  static final String CORE = "HED";

  private LinkedList<CoNLLWord[]> doc;
  
  /**
   * Generate dependency graphs from input file.
   * @param path - path to input file
   * @param option - name of segmenter, by default use neural network
   * dependency parser with NLPTokenizer; use "index" to use IndexTokenizer;
   * use "NShort" to use NShortSegment; use "CRF" to use CRF dependency parser.
   */
	public void buildDep(String path, String option) {
		  try {
			  Scanner sc = new Scanner(new FileInputStream(path), 
					  StandardCharsets.UTF_8.toString());
			  IDependencyParser parser = 
					  new NeuralNetworkDependencyParser().enableDeprelTranslator(false);
			  sc.useDelimiter("\\Z");
			  if(option.equals("index"))
			  	parser.setSegment(IndexTokenizer.SEGMENT);
			  else if(option.equals("NShort"))
			  	parser.setSegment(new NShortSegment());
			  else if(option.equals("CRF"))
			  	parser.setSegment(new CRFSegment());
			  else
			  	parser.setSegment(NLPTokenizer.SEGMENT);
			  while(sc.hasNext()) {
				  String[] sentences = sc.next().split(PUNCT);
				  LinkedList<CoNLLWord[]> doc = new LinkedList<>();
				  this.doc = doc;
				  /* Add dependency graph of each sentence*/
				  for(int j = 0; j < sentences.length; j++) {
				  	CoNLLSentence sentence = parser.parse(sentences[j].trim());
					  CoNLLWord[] wordArray = sentence.getWordArray();
					  doc.add(wordArray);
				  }
			  }
			  sc.close();
		  } catch (FileNotFoundException e) {
			  e.printStackTrace();
		  }
  }
  
  /**
   * Read a single component of sentence, i.e. subject, verb, or object.
   * @param outDir - output directory
   * @param fileName - output filename
   * @param component - "S," "V," or "O"
   */
  public void getSingleComp(String outDir, String fileName, String component) {
  	ListIterator<CoNLLWord[]> docIt = this.doc.listIterator();
  	try {
  		PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
  				FileOutputStream(outDir + "/" + fileName), 
  				StandardCharsets.UTF_8.toString()));
  		while(docIt.hasNext()) {
  			CoNLLWord[] wordArray = docIt.next();
  			for(CoNLLWord term: wordArray) {
  				switch(component) {
  				case "S":
  					if(term.DEPREL.equals(SV)) {
  						writer.write(this.formPhrase(wordArray, term.ID - 1));
  						writer.write(" ");
  					}
  					break;
  				case "V":
  					if(term.DEPREL.equals(SV)) {
  						writer.write(term.HEAD.LEMMA);
  						writer.write(" ");
  					} else if(term.DEPREL.equals(VO)) {
  						writer.write(term.HEAD.LEMMA);
  						writer.write(" ");
  					}
  					break;
  				case "O":
  					if(term.DEPREL.equals(VO)) {
  						writer.write(this.formPhrase(wordArray, term.ID - 1));
  						writer.write(" ");
  					}
  					break;
  				}
  			}
  			writer.write("\n");
  		}
  		writer.close();
  	}catch (UnsupportedEncodingException | FileNotFoundException e) {
  		e.printStackTrace();
  	}
  }
  
  /**
   * Output subject-object pairs.
   * @param outDir - output directory
   * @param fileName - output filename
   */
  public void getSO(String outDir, String fileName) {
	  ListIterator<CoNLLWord[]> docIt = this.doc.listIterator();
	  try {
	  	PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
	  			FileOutputStream(outDir + "/" + fileName), 
	  			StandardCharsets.UTF_8.toString()));
	  	while(docIt.hasNext()) {
	  		CoNLLWord[] wordArray = docIt.next();
	  		HashMap<CoNLLWord, CoNLLWord> map = new HashMap<>();
	  		for(CoNLLWord term: wordArray) {
	  			if(term.DEPREL.equals(SV))
	  				map.put(term.HEAD, term);
	  		}

	  		for(int k = wordArray.length - 1; k >= 0; k--) {
	  			if(!wordArray[k].DEPREL.equals(VO)) continue;
	  			if(wordArray[k].HEAD == null) continue;

	  			CoNLLWord subject = this.findSbj(wordArray[k].HEAD,
	  					map);
	  			if(subject == null) continue;
	  			writer.write(this.formPhrase(wordArray, subject.ID - 1));
	  			writer.write("-");
	  			writer.write(this.formPhrase(wordArray, k));
	  			writer.write(" ");
	  		}
	  	}
	  	writer.write("\n");
	  	writer.close();
	  } catch (UnsupportedEncodingException | FileNotFoundException e) {
	  	e.printStackTrace();
	  }
  }
  
  /**
   * Output verb-object pairs.
   * @param outDir - output directory
   * @param fileName - output filename
   */
  public void getVO(String outDir, String fileName) {
  	ListIterator<CoNLLWord[]> docIt = this.doc.listIterator();
  	try {
  		PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
  				FileOutputStream(outDir + "/" + fileName), 
  				StandardCharsets.UTF_8.toString()));
  		while(docIt.hasNext()) {
  			CoNLLWord[] wordArray = docIt.next();
  			HashMap<CoNLLWord, CoNLLWord> map = new HashMap<>();
  			for(CoNLLWord term: wordArray) {
  				if(term.DEPREL.equals(SV))
  					map.put(term.HEAD, term);
  			}

  			for(int k = wordArray.length - 1; k >= 0; k--) {
  				if(!wordArray[k].DEPREL.equals(VO)) continue;
  				if(wordArray[k].HEAD == null || 
  						(!wordArray[k].HEAD.CPOSTAG.startsWith("v")))
  					continue;

  				CoNLLWord subject = this.findSbj(wordArray[k].HEAD,
  						map);
  				if(subject == null) continue;
  				writer.write(wordArray[k].HEAD.LEMMA);
  				writer.write("-");
  				writer.write(this.formPhrase(wordArray, k));
  				writer.write(" ");
  			}
  		}
  		writer.write("\n");
  		writer.close();
  	} catch (UnsupportedEncodingException | FileNotFoundException e) {
  		e.printStackTrace();
  	}
  }
  
  /**
   * Output subject-verb pairs.
   * @param outDir - output directory
   * @param fileName - output filename
   */
  public void getSV(String outDir, String fileName) {
  	ListIterator<CoNLLWord[]> docIt = this.doc.listIterator();
  	try {
  		PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
  				FileOutputStream(outDir + "/" + fileName), 
  				StandardCharsets.UTF_8.toString()));
  		while(docIt.hasNext()) {
  			CoNLLWord[] wordArray = docIt.next();
  			HashMap<CoNLLWord, CoNLLWord> map = new HashMap<>();
  			for(CoNLLWord term: wordArray) {
  				if(term.DEPREL.equals(SV))
  					map.put(term.HEAD, term);
  			}

  			for(int k = wordArray.length - 1; k >= 0; k--) {
  				if(!wordArray[k].CPOSTAG.startsWith("v")) continue;
  				if(wordArray[k].HEAD == null) continue;

  				CoNLLWord subject = this.findSbj(wordArray[k].HEAD,
  						map);
  				if(subject == null) continue;
  				writer.write(this.formPhrase(wordArray, subject.ID - 1));
  				writer.write("-");
  				writer.write(wordArray[k].LEMMA);
  				writer.write(" ");
  			}
  		}
  		writer.write("\n");
  		writer.close();
  	} catch (UnsupportedEncodingException | FileNotFoundException e) {
  		e.printStackTrace();
  	}
  }
  
  public boolean isPunc(String str) {
	  if(str.equals("£¬") || str.equals("¡¢") || str.equals("¡£") ||
			  str.equals("¡°") || str.equals("¡±") || str.equals("¡®") ||
			  str.equals("¡¯") || str.equals(" ") || str.equals("£¨")
			  || str.endsWith("£©"))
		  return true;
	  return false;
  }
  
  private CoNLLWord findSbj(CoNLLWord object, HashMap<CoNLLWord, CoNLLWord> map) {
	  while(object.HEAD != null) {
		  if(map.containsKey(object))
			  return map.get(object);
		  if(object.DEPREL.equals("COO") || object.DEPREL.equals(VO))
			  object = object.HEAD;
		  else
			  return null;
	  }
	return null;
  }
  
  private String formPhrase(CoNLLWord[] wordArray, int k) {
	  StringBuilder sb = new StringBuilder();
	  int i = k;
	  while(i > 0) {
		  if(wordArray[i - 1].POSTAG.startsWith("n"))
			  i--;
		  else break;
	  }
	  while(i <= k) {
		  sb.append(wordArray[i].LEMMA);
		  i++;
	  }
	  return sb.toString();
  }
}