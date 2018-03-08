import java.io.File;
import java.util.ArrayList;

import dependencyExtraction.DependencyGenerator;
import dependencyExtraction.HanLPDependencyExtractor;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.international.pennchinese.UniversalChineseGrammaticalRelations;
import utils.TableReader;

/**
 * A class for demo phrase extraction using Stanford Parser and HanLP
 * @author Haoran Sun
 * @since 02-25-2018
 */
public class DemoParser {
	/**
	 * Execute Stanford Parser to get dependency pairs.
	 * @param dirPath - directory of input files
	 */
	public static void executeStanfordParser(String dirPath) {
		File dir = new File(dirPath);
		DependencyGenerator gen = new DependencyGenerator();
		ArrayList<GrammaticalRelation> criteria = new ArrayList<>();
		criteria.add(UniversalChineseGrammaticalRelations.NOUN_COMPOUND);
		criteria.add(UniversalChineseGrammaticalRelations.ADJECTIVAL_MODIFIER);
		criteria.add(UniversalChineseGrammaticalRelations.CLAUSAL_MODIFIER);
		criteria.add(UniversalChineseGrammaticalRelations.ASSOCIATIVE_MODIFIER);
		criteria.add(UniversalChineseGrammaticalRelations.ORDINAL_MODIFIER);
		String[] outputDirs = {"S/", "V/", "O/", "SV/", "VO/", "SO/"};
		for(String depType: outputDirs) {
			File directory = new File(depType.substring(0, depType.length() - 1));
			directory.mkdir();
		}
		for(final File file : dir.listFiles())
			gen.generate(file, criteria, outputDirs);
	}
	
	/**
	 * Execute one of HanLP dependency parser to get dependency pairs.
	 * @param dirPath - directory of input files
	 * @param option - name of segmenter, by default use neural network
   * dependency parser with NLPTokenizer; use "index" to use IndexTokenizer;
   * use "NShort" to use NShortSegment; use "CRF" to use CRF dependency parser.
	 */
	public static void executeHanLPDependencyParser(String dirPath, String 
			option) {
		File dir = new File(dirPath);
		HanLPDependencyExtractor extr = new HanLPDependencyExtractor();
		String[] outputDirs = {"S", "V", "O", "SV", "VO", "SO"};
		for(String depType: outputDirs) {
			File directory = new File(depType);
			directory.mkdir();
		}
		for(final File file : dir.listFiles()) {
			extr.buildDep(file.getPath(), option);
			extr.getSingleComp(outputDirs[0], file.getName(), "S");
			extr.getSingleComp(outputDirs[1], file.getName(), "V");
			extr.getSingleComp(outputDirs[2], file.getName(), "O");
			extr.getSV(outputDirs[3], file.getName());
			extr.getVO(outputDirs[4], file.getName());
			extr.getSO(outputDirs[5], file.getName());
		}
	}
	
	public static void main(String[] args) {
		TableReader.tableToFiles("courtDoc.csv", "courtDoc");
		//DemoParser.executeStanfordParser("courtDoc");
		//DemoParser.executeHanLPDependencyParser("courtDoc", "CRF");
	}
}