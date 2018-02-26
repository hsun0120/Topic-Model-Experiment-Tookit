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
		try(CSVReader reader = new CSVReader((new InputStreamReader(new 
				FileInputStream(table), StandardCharsets.UTF_8)))) {
			String row[] = null;
			while((row = reader.readNext()) != null) {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
						FileOutputStream(output + "/" + row[0] + ".txt"), 
						StandardCharsets.UTF_8));
				writer.write(row[6]); //Write original text
				writer.close();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}