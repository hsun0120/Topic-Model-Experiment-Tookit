package utils;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import au.com.bytecode.opencsv.CSVReader;

/**
 * A simple class to extract columns from a table to text files.
 * @author hsun9
 * @since 02-25-2018
 */
public class TableReader {
	public static void tableToFiles(String table, String output) {
		String row[] = null;
		try(CSVReader reader = new CSVReader((new InputStreamReader(new 
				FileInputStream(table), StandardCharsets.UTF_8)))) {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
					FileOutputStream("compensation.txt"), StandardCharsets.UTF_8));
			int count = 0;
			while((row = reader.readNext()) != null) {
				if(row.length != 3 || !row[1].trim().equals("Åâ³¥°¸¼þ")) continue;
				writer.write(row[2]); //Write original text
				count++;
				if(count == 200) break;
			}
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}