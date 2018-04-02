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
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class CompareSolutions {

	static int COSTANT = 1;
	static int T_COSTANT = 1000;
	public static void main(String[] args) throws FileNotFoundException {
		Scanner sc = new Scanner(new File("CMPjava_results.txt"));
		Scanner sc_2 = new Scanner(new File("CMPjava_resultsPR.txt"));

	/*	ArrayList<String> opt = new ArrayList<String>();
		ArrayList<String> time = new ArrayList<String>();
		while (sc.hasNext()) {
			sc.next();
			
			opt.add(sc.next());
		//	sc.next();
			time.add(sc.next());
			
		}
		
		
		
		*/
	
		ArrayList<String> opt = new ArrayList<String>();
		ArrayList<String> time = new ArrayList<String>();
		
		
		while (sc.hasNext()) {
			sc.next();			
			sc.next();
			opt.add(sc.next());
			time.add(sc.next());
			sc.next();
			sc.next();
			
		}
			
		
		
		
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> b_init = new ArrayList<String>();
		ArrayList<String> b_end = new ArrayList<String>();
		ArrayList<String> b_time = new ArrayList<String>();
		ArrayList<String> heur_time = new ArrayList<String>();
		ArrayList<String> iter = new ArrayList<String>();
		while (sc_2.hasNext()) {
			names.add(sc_2.next());			
		//	b_init.add(sc_2.next());
			b_end.add(sc_2.next());
		//	b_time.add(sc_2.next());
			heur_time.add(sc_2.next());
			iter.add(sc_2.next());
			
		}
		
		
		
		
		writeTex(names,opt,time,b_end,heur_time,iter);
	//	writeTexDUE(names,b_init,b_end,b_time);
	//	drawPlot(names,b_end, readPLOT(100, "java_resultsPLOT3min.txt"));
		
	}
	
	public static void drawPlot(List<String> names, List<String> best_known, HashMap<String,List<ValueTime>> map ) {
		Charset utf8 = StandardCharsets.UTF_8;
		DecimalFormat df = new DecimalFormat("####.###");
		
		
		HashMap<String,List<ValueTime>> rel_map = new HashMap<String,List<ValueTime>>();
	  for(int i=0;i<names.size();i++) {
			if(map.get(names.get(i)) == null) continue;
			double best = Double.parseDouble(best_known.get(i));
			List<ValueTime> ls = map.get(names.get(i));
			List<ValueTime> n_ls = new ArrayList<ValueTime>();
			for(ValueTime vt : ls) {
				double tmp_rel;
				if(best != 0 && best != Double.POSITIVE_INFINITY) {
					
				    tmp_rel = (( vt.getValue() - best*COSTANT ) / (best*COSTANT));    // REL GAP
				    System.out.println(tmp_rel +"="+vt.getValue() +" - "+(best*COSTANT));
					n_ls.add(new ValueTime(tmp_rel,vt.getTime()));
				}else {
				//	tmp_rel = Double.NaN;
				}
			
				
			}
			rel_map.put(names.get(i), n_ls);
		}
		
		List<ValueTime> avg_rel = new ArrayList<ValueTime>();
		int stop = 0;
		int j = 0;
		while(stop < rel_map.keySet().size() ) {
			double tot = 0;
			int count = 0;
			double time = -1;
			for(int i=0; i<names.size();i++) {
				if(rel_map.get(names.get(i))== null) continue;
				if(j >= rel_map.get(names.get(i)).size() ) {
					stop +=1;
					continue;
				}else {
					time = rel_map.get(names.get(i)).get(j).getTime();
					stop = 0;
				}
				if(rel_map.get(names.get(i)).get(j).getValue() != Double.POSITIVE_INFINITY && rel_map.get(names.get(i)).get(j).getValue() != Double.NaN) {
				tot += rel_map.get(names.get(i)).get(j).getValue();// - COSTANT*Double.parseDouble( best_known.get(i))) / (COSTANT*Double.parseDouble(best_known.get(i)));
			  //  System.out.println(map.get(names.get(i)).get(j).getValue() +"\t"+COSTANT*Double.parseDouble( best_known.get(i)));  	
				count++;
				}
			}
			
			
			if(count == 0) {
				j+=1;
				continue;
			}
			double avg = tot  / count;
		//System.out.println(avg + "="+tot+ "/"+count);
			avg_rel.add(new ValueTime(avg,time));
			j+=1;
		}
		
		
		
		int k= 4;
		List<String> nameToPlot = new ArrayList<String>();
		List<List<ValueTime>> toPlot= new ArrayList< List<ValueTime>>();
		
	/*		for(int i = 0; i<names.size(); i++) {
				double d = Double.parseDouble(best_known.get(i));
				if(d == Double.POSITIVE_INFINITY) continue;
				
				if(nameToPlot.size() < 4) {
				nameToPlot.add(names.get(i));
				toPlot.add(rel_map.get(names.get(i)));
				}
			}*/
		
		nameToPlot.add("AVG");
		toPlot.add(avg_rel);
		List<String> colors = new ArrayList<String>();
	//	colors.add("red");
	//	colors.add("green");
	//	colors.add("blue");
	//	colors.add("orange");
		colors.add("black");
		
		List<String> lines = new ArrayList<String>();
		lines.add("\\documentclass{article}");

		lines.add("\\usepackage{tikz}");
	    lines.add("\\usepackage{pgfplots}");
	    lines.add("\\begin{document}");

	    lines.add("\\begin{tikzpicture}");
	 		lines.add("\\begin{axis}[xlabel=$time$, ylabel=$rel\\_gap$]");
	 		for(int i=0; i<nameToPlot.size(); i++) {
		    lines.add("\\addplot[mark=*,"+colors.get(i)+"] plot coordinates {");
		    for(ValueTime vt : toPlot.get(i)) {
		    	lines.add("("+vt.getTime()+","+vt.getValue()+")");
		    	
		    }
		    lines.add("};");
		    lines.add("\\addlegendentry{"+nameToPlot.get(i).replaceAll("_", "\\\\_")+"}");

	 		}
		   lines.add("\\end{axis}");
		   lines.add("\\end{tikzpicture}");
		   lines.add("\\end{document}");

		   try {
			   System.out.println("PLOTTING");
				Files.write(Paths.get(
						"PLOT.tex"),
						lines, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}
	
	
	
	public static HashMap<String, List<ValueTime>> readPLOT(int granular, String file) {
		
		Scanner sc = null;
		try {
			 sc = new Scanner(new File(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		HashMap<String, List<ValueTime>> map = new HashMap <String,List<ValueTime>>();
		while(sc.hasNext()) {
		
			String name = sc.next();
			String test = "";
			List<Double> nums = new ArrayList<Double>();
			while (!test.equals("END")) {
				test = sc.next();
				if(!test.equals("END")) {
					nums.add(new Double(Double.parseDouble(test)));
				}
			}
			List<ValueTime> entry = new ArrayList<ValueTime>();
			double tmp = 0;
			for(int i=0; i<nums.size()-1; i+=2) {
				if(Math.abs(tmp - nums.get(i)*COSTANT) < 1 ) {
					continue;
				}else {
			//		System.out.println(tmp + " " + (nums.get(i)*COSTANT));
					tmp = nums.get(i)*COSTANT;
					
					
				}
				entry.add(new ValueTime(nums.get(i)*COSTANT,nums.get(i+1)/T_COSTANT));
			}
			
			map.put(name, entry);
			
		}
		
		double maxTime = 0;
		for(List<ValueTime> vt : map.values()) {
			if(vt.isEmpty())continue;
			double tmp = vt.get(vt.size()-1).getTime();
			maxTime = Math.max(tmp, maxTime);
		}
		System.out.println(maxTime);
		
		double t_step = maxTime / granular;
		
		if(t_step ==0) return map;
		
		HashMap<String, List<ValueTime>> n_map = new HashMap<String,List<ValueTime>>();
		for(String str : map.keySet()) {
			List<ValueTime> vt = map.get(str);
			List<ValueTime> n_vt = new ArrayList<ValueTime>();
			if(vt.isEmpty()) continue;
			int offset = (int)(vt.get(0).getTime() / t_step)+1;
			double tmp = 0;
			for(int j=0;j<offset;j++) {
				n_vt.add(new ValueTime(Double.POSITIVE_INFINITY,tmp));
				tmp += t_step;
				
			}
			
			for(int j=0; j<vt.size()-1;j++) {
				if(vt.get(j+1).getTime() > tmp + t_step) {
					n_vt.add(new ValueTime(vt.get(j).getValue(),tmp+t_step));
					tmp+= t_step;
					
				}else {
					continue;
				}
			}
			/*
			while(n_vt.size() <= granular) {
				n_vt.add(new ValueTime(vt.get(vt.size()-1).getValue(),tmp+t_step));
				tmp+= t_step;
				
			}
			*/
			n_map.put(str, n_vt);
		}
		
		return n_map;
		
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
			double tmp_abs = ((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i)))*COSTANT);
			if(tmp_abs == Double.POSITIVE_INFINITY) {
				count_inf_abs +=1;
				count_inf_rel +=1;
				continue;
			}
			double tmp_rel = 0;	
					
			if(Double.parseDouble(val_c.get(i)) != 0) {
			    tmp_rel = ((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i))) / Math.abs( Double.parseDouble(val_c.get(i))));    // REL GAP
			}else {
				count_inf_rel +=1		;
			}
			
			tot_abs += tmp_abs;
			tot_rel += tmp_rel;
			
			
		}
		double avg_abs = ( tot_abs / (names.size() - count_inf_abs));
		double avg_rel =(tot_rel / (names.size() - count_inf_rel));
		
		System.out.println(tot_abs);
		System.out.println(tot_rel);
		
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
		lines.add("\\caption{Results (MBit/s)"+" [ heur\\_time: "+df.format(Double.parseDouble(heur_time.get(0))/COSTANT)+"s ] "+" [ cplex mipgap=0.00000001 ]}");
		lines.add("\\tabularnewline");
		lines.add("\\hline");
	//	lines.add("Instance & best\\_known & heur\\_value & rel\\_gap & abs\\_gap & cplex\\_time \\"+"\\");
		lines.add("Instance & best\\_known & heur\\_value & rel\\_gap & abs\\_gap & cplex\\_time & heur\\_iter\\"+"\\");
	//	lines.add("Instance & best\\_known & heur\\_value & rel\\_gap & abs\\_gap & cplex\\_time & heur\\_time\\"+"\\");
		lines.add("\\hline");
		for(int i=0;i<names.size();i++) {
			String ln = "";
			ln += names.get(i).replaceAll("_", "\\\\_");
			ln += " & ";
			ln += df.format((Double.parseDouble(val_c.get(i))*COSTANT));
			ln += " & ";
			ln += df.format((Double.parseDouble(val_j.get(i))*COSTANT));
			ln += " & ";
			if(Double.parseDouble(val_c.get(i)) != 0) {
			    ln += df.format(((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i))) / Math.abs(Double.parseDouble(val_c.get(i)))));    // REL GAP
			}else {
				ln += "-";
			}
			ln += " & ";
			ln += df.format(((Double.parseDouble(val_j.get(i)) - Double.parseDouble(val_c.get(i)))*COSTANT));  // ABS GAP
			ln += " & ";
			ln += df.format(Double.parseDouble(time.get(i))/1);
			ln += " & ";
			ln += iter.get(i);
		//	ln += " & ";
		//	ln += df.format(Double.parseDouble(heur_time.get(i)));
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
	
	
	public static void writeTexDUE(List<String> names,List<String> b_init,List<String> b_end, List<String> b_time) {

		Charset utf8 = StandardCharsets.UTF_8;
		DecimalFormat df = new DecimalFormat("####.###");
		
		
		double tot_abs = 0;
		double tot_rel = 0;
		int count_inf_abs = 0;
		int count_inf_rel = 0;
		System.out.println(names.size());
		System.out.println(b_init.size());
		System.out.println(b_end.size());

		for(int i=0; i<names.size();i++) {
			if(Double.parseDouble(b_end.get(i))  == Double.POSITIVE_INFINITY ) {
				count_inf_abs +=1;
				count_inf_rel +=1;
				continue;
			}
			double tmp_abs = ((Double.parseDouble(b_init.get(i)) - Double.parseDouble(b_end.get(i)))*COSTANT);
			if(tmp_abs == Double.POSITIVE_INFINITY) {
				count_inf_abs +=1;
				count_inf_rel +=1;
				continue;
			}
			double tmp_rel = 0;	
					
			if(Double.parseDouble(b_end.get(i)) != 0) {
			    tmp_rel = ((Double.parseDouble(b_init.get(i)) - Double.parseDouble(b_end.get(i))) / Math.abs(Double.parseDouble(b_end.get(i))));    // REL GAP
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
	
		lines.add("\\tabularnewline");
		lines.add("\\hline");
		lines.add("Instance & Greedy\\_Rand & Local\\_Search & rel\\_gap & abs\\_gap & best\\_time\\"+"\\");
		lines.add("\\hline");
		for(int i=0;i<names.size();i++) {
			String ln = "";
			ln += names.get(i).replaceAll("_", "\\\\_");
			ln += " & ";
			ln += df.format((Double.parseDouble(b_init.get(i))*COSTANT));
			ln += " & ";
			ln += df.format((Double.parseDouble(b_end.get(i))*COSTANT));
			ln += " & ";
			if(Double.parseDouble(b_end.get(i)) != 0) {
			    ln += df.format(((Double.parseDouble(b_init.get(i)) - Double.parseDouble(b_end.get(i))) / Math.abs(Double.parseDouble(b_end.get(i)))));    // REL GAP
			}else {
				ln += "-";
			}
			ln += " & ";
			ln += df.format(((Double.parseDouble(b_init.get(i)) - Double.parseDouble(b_end.get(i)))*COSTANT));  // ABS GAP
			ln += " & ";
			ln += df.format(Double.parseDouble(b_time.get(i))/T_COSTANT);
		
			ln += "\\"+"\\";
			lines.add(ln);
			lines.add("\\hline");
		}
		lines.add("\\hline");
		lines.add("Average Gaps & & & "+df.format(avg_rel)+" & "+df.format(avg_abs)+" &  \\\\");
		lines.add("\\hline");
		lines.add("\\hline");
		lines.add("\\end{longtable}");
		lines.add("\\end{center}");
		lines.add("\\end{document}");
		try {
			Files.write(Paths.get(
					"results_tableDUE.tex"),
					lines, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	


}
