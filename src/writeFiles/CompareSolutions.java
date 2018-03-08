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
		Scanner sc = new Scanner(new File("CMPcplex_results.txt"));
		Scanner sc_2 = new Scanner(new File("CMPjava_resultsPR.txt"));

		ArrayList<String> from1 = new ArrayList<String>();
		ArrayList<String> time = new ArrayList<String>();
		while (sc.hasNext()) {
			sc.next();
			String line = sc.next();
			from1.add(line);
		//	sc.next();
			time.add(sc.next());
			
		}
		ArrayList<String> from2 = new ArrayList<String>();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> heur_time = new ArrayList<String>();
		ArrayList<String> iter = new ArrayList<String>();
		while (sc_2.hasNext()) {
			names.add(sc_2.next());
			String line = sc_2.next();
			from2.add(line);
			heur_time.add(sc_2.next());
			iter.add(sc_2.next());
			
		}
		
		for(int i=0; i<from1.size();i++) {
			double val_c = Double.parseDouble(from1.get(i));
			double val_j = Double.parseDouble(from2.get(i));
			System.out.println(val_j - val_c);
		}
		
		writeTex(names,from1,time,from2,heur_time,iter);
	}
	
	public static void writeTex(List<String> names,List<String> val_c,List<String> time, List<String> val_j, List<String> heur_time,List<String> iter) {

		Charset utf8 = StandardCharsets.UTF_8;
		DecimalFormat df = new DecimalFormat("####.###");
		
		
		double tot_abs = 0;
		double tot_rel = 0;
		int count_inf_abs = 0;
		int count_inf_rel = 0;
		System.out.println(names.size());
		System.out.println(val_j.size());
		System.out.println(val_c.size());

		for(int i=0; i<names.size();i++) {
			double tmp_abs = ((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i))));//*1000);
			if(tmp_abs == Double.POSITIVE_INFINITY) {
				count_inf_abs +=1;
				count_inf_rel +=1;
				continue;
			}
			double tmp_rel = 0;	
					
			if(Double.parseDouble(val_c.get(i)) != 0) {
			    tmp_rel = ((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i))) / Double.parseDouble(val_c.get(i)));    // REL GAP
			}else {
				count_inf_rel +=1		;
			}
			
			tot_abs += tmp_abs;
			tot_rel += tmp_rel;
			
		}
		double avg_abs = tot_abs / (names.size() - count_inf_abs);
		double avg_rel = tot_rel / (names.size() - count_inf_rel);
		
		
		
		
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
		
		lines.add("\\begin{center}");
	 
		lines.add("\\begin{longtable}{cccccccc}");
	//	lines.add("\\caption{Results (MBit/s)"+" [ heur\\_iter: "+df.format(Integer.parseInt(iter.get(0)))+"s ] "+" [ cplex mipgap=0.00000001 ]}");
		lines.add("\\caption{Results (MBit/s)"+" [ heur\\_time: "+df.format(Double.parseDouble(heur_time.get(0))/1000)+"s ] "+" [ cplex mipgap=0.00000001 ]}");
		lines.add("\\tabularnewline");
		lines.add("\\hline");
	//	lines.add("Instance & best\\_known & heur\\_value & rel\\_gap & abs\\_gap & cplex\\_time \\"+"\\");
	//	lines.add("Instance & best\\_known & heur\\_value & rel\\_gap & abs\\_gap & cplex\\_time & heur\\_iter\\"+"\\");
		lines.add("Instance & best\\_known & heur\\_value & rel\\_gap & abs\\_gap & cplex\\_time & heur\\_time\\"+"\\");
		lines.add("\\hline");
		for(int i=0;i<names.size();i++) {
			String ln = "";
			ln += names.get(i).replaceAll("_", "\\\\_");
			ln += " & ";
			ln += df.format((Double.parseDouble(val_c.get(i))));//*1000));
			ln += " & ";
			ln += df.format((Double.parseDouble(val_j.get(i))));//*1000));
			ln += " & ";
			if(Double.parseDouble(val_c.get(i)) != 0) {
			    ln += df.format(((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i))) / Double.parseDouble(val_c.get(i))));    // REL GAP
			}else {
				ln += "-";
			}
			ln += " & ";
			ln += df.format(((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i)))));//*1000));  // ABS GAP
			ln += " & ";
			ln += df.format(Double.parseDouble(time.get(i)));
		//	ln += " & ";
		//	ln += iter.get(i);
			ln += " & ";
			ln += df.format(Double.parseDouble(heur_time.get(i)));
			ln += "\\"+"\\";
			lines.add(ln);
			lines.add("\\hline");
		}
		lines.add("\\hline");
		lines.add("Average Gaps & & & "+df.format(avg_rel)+" & "+df.format(avg_abs)+" & & \\\\");
		lines.add("\\hline");
		lines.add("\\hline");
		lines.add("\\end{longtable}");
		lines.add("\\end{center}");
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
