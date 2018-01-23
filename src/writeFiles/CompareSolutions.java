package writeFiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CompareSolutions {

	public static void main(String[] args) throws FileNotFoundException {
		Scanner sc = new Scanner(new File("results.txt"));
		Scanner sc_2 = new Scanner(new File("java_results.txt"));

		ArrayList<String> from1 = new ArrayList<String>();
		ArrayList<String> time = new ArrayList<String>();
		while (sc.hasNext()) {
			sc.next();
			String line = sc.next();
			from1.add(line);
			time.add(sc.next());
		}
		ArrayList<String> from2 = new ArrayList<String>();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> heur_time = new ArrayList<String>();
		while (sc_2.hasNext()) {
			names.add(sc_2.next());
			String line = sc_2.next();
			from2.add(line);
			heur_time.add(sc_2.next());
			
		}
		
		for(int i=0; i<from1.size();i++) {
			double val_c = Double.parseDouble(from1.get(i));
			double val_j = Double.parseDouble(from2.get(i));
			System.out.println(val_j - val_c);
		}
		
		writeTex(names,from1,time,from2,heur_time);
	}
	
	public static void writeTex(List<String> names,List<String> val_c,List<String> time, List<String> val_j, List<String> heur_time) {
		Charset utf8 = StandardCharsets.UTF_8;
		
		DecimalFormat df = new DecimalFormat("####.###");
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("\\documentclass[a4paper]{article}");
		lines.add("\\usepackage[T1]{fontenc}");
		lines.add("\\usepackage[utf8]{inputenc}");
		lines.add("\\usepackage[english]{babel}");
		lines.add("\\usepackage{amssymb}");
		lines.add("\\usepackage[fleqn]{amsmath}");
		lines.add("\\usepackage{amsfonts}");
		lines.add("\\usepackage{float}");
		lines.add("\\usepackage{geometry}");
		lines.add("\\usepackage{booktabs}");
		lines.add("\\usepackage{tabularx,ragged2e}");
		lines.add("\\newcolumntype{L}{>{\\RaggedRight\\arraybackslash}X}");
		lines.add("\\usepackage{longtable}");
		lines.add("\\begin{document}");
		
	//	lines.add("\\begin{table}[H]");
		lines.add("\\begin{center}");
	 
		lines.add("\\begin{longtable}{ccccccc}");
		lines.add("\\caption{Results (MBit/s)"+" [ heur\\_time: "+df.format(Double.parseDouble(heur_time.get(0))/1000)+"s ] "+" [ cplex absmipgap=0.1 ]}");
		lines.add("\\tabularnewline");
		lines.add("\\hline");
		lines.add("Instance & best\\_known & heur\\_value & rel\\_gap & abs\\_gap & cplex\\_time"+"\\"+"\\");
		lines.add("\\hline");
		for(int i=0;i<names.size();i++) {
			String ln = "";
			ln += names.get(i).replaceAll("_", "\\\\_");
			ln += " & ";
			ln += df.format((Double.parseDouble(val_c.get(i))*1000));
			ln += " & ";
			ln += df.format((Double.parseDouble(val_j.get(i))*1000));
			ln += " & ";
			if(Double.parseDouble(val_c.get(i)) > 0) {
			    ln += df.format(((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i))) / Double.parseDouble(val_c.get(i))));
			}else {
				ln += "-";
			}
			ln += " & ";
			ln += df.format(((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i)))*1000));
			ln += " & ";
			ln += df.format(Double.parseDouble(time.get(i)));
			ln += "\\"+"\\";
			lines.add(ln);
			lines.add("\\hline");
		}
		lines.add("\\end{longtable}");
		lines.add("\\end{center}");
		//lines.add("\\end{table}");
		lines.add("\\end{document}");
		try {
			Files.write(Paths.get(
					"results_table.tex"),
					lines, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
