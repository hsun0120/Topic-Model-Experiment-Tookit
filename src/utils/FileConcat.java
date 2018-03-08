package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class FileConcat {
	public static void main(String[] args) {
		File dir = new File(args[0]);
		try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
						FileOutputStream("data.txt"), StandardCharsets.UTF_8))) {
			for(final File file : dir.listFiles()) {
				Scanner sc = new Scanner(new InputStreamReader(new FileInputStream(file.getPath()), StandardCharsets.UTF_8));
				while(sc.hasNextLine())
					writer.write(sc.nextLine());
				sc.close();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}