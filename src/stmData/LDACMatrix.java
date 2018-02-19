package stmData;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;


public class LDACMatrix {
	private HashSet<String> vocab;
	
	public LDACMatrix() {
		this.vocab = new HashSet<>();
	}
	
	public void populateVocab(String[]fileList,  String inDir,
			String vocabName) {
		  for(int i = 0; i < fileList.length; i++) {
			  try {
				  Scanner sc = new Scanner(new FileInputStream(inDir + "/" + fileList[i]), 
						  StandardCharsets.UTF_8.toString());
				  while(sc.hasNext()) {
					  vocab.add(sc.next());
				  }
				  sc.close();
			  } catch (FileNotFoundException e) {
				  e.printStackTrace();
			  }
		  }
		  /* Output vocabularies */
		  try {
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
					  FileOutputStream(vocabName), 
					  StandardCharsets.UTF_8.toString()));
			Iterator<String> it = vocab.iterator();
			while(it.hasNext()) {
				writer.write(it.next());
				writer.println();
			}
			writer.close();
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void getMatrix(String[]fileList,  String inDir, String vocabName,
			String matName) {
		HashMap<String, Integer> map = new HashMap<>();
		try {
			Scanner vocSc = new Scanner(new FileInputStream(vocabName), 
					  StandardCharsets.UTF_8.toString());
			int index = 0;
			while(vocSc.hasNext())
				map.put(vocSc.next(), index++);
			vocSc.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		ArrayList<HashMap<String, Integer>> list = new ArrayList<>(fileList.length);
		for(int i = 0; i < fileList.length; i++) {
			  try {
				  Scanner sc = new Scanner(new FileInputStream(inDir + "/" + fileList[i]), 
						  StandardCharsets.UTF_8.toString());
				  HashMap<String, Integer> locMap = new HashMap<>();
				  list.add(locMap);
				  while(sc.hasNext()) {
					  String term = sc.next();
					  if(locMap.containsKey(term))
						  locMap.put(term, locMap.get(term).intValue() + 1);
					  else
						  locMap.put(term, 1);
				  }
				  sc.close();
			  } catch (FileNotFoundException e) {
				  e.printStackTrace();
			  }
		  }
		
		/* Output dtm */
		  try {
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
					  FileOutputStream(matName), 
					  StandardCharsets.UTF_8.toString()));
			for(int i = 0; i < list.size(); i++) {
				HashMap<String, Integer> locMap = list.get(i);
				writer.write(locMap.size() + " "); //# of unique words
				Set<String> set = locMap.keySet();
				Iterator<String> it = set.iterator();
				while(it.hasNext()) {
					String key = it.next();
					writer.write(map.get(key).intValue() +
							":" + locMap.get(key));
					if(it.hasNext())
						writer.print(" ");
				}
				writer.println();
			}
			writer.close();
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void main (String[] args) {
			String[] combinations = {"S", "V", "O", "SV", "VO", "SO"};
			for(String combi : combinations) {
				File dir = new File(args[0] + "/" + combi);
		    String[] fileList = dir.list(); //Get all the files of the source folder
		    Arrays.sort(fileList);
		    LDACMatrix ldac = new LDACMatrix();
		    ldac.populateVocab(fileList, args[0] + "/" + combi, "vocab" + combi);
		    ldac.getMatrix(fileList, args[0] + "/" + combi, "vocab" + combi, "mat"
		    + combi + ".ldac");
			}
	  }
}