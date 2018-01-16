package prova;

import general.*;
import lpsolve.*;
import general.DataCenter;
import general.Link;
import general.Server;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.*;
import org.jgrapht.alg.flow.EdmondsKarpMFImpl;
import org.jgrapht.alg.flow.PushRelabelMFImpl;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import cpp_heuristics.TreeIndex;
import cpp_heuristics.TreeNode;
import cpp_heuristics.TreeNodeExplorer;


public class Hello {

	public static void main(String [] args) {
		
		provaTree();
		/*
		double [] coeff = new double[10];
		try {
			LpSolve lp = LpSolve.makeLp(10, 10);
			lp.setObjFn(coeff);
			lp.solve();
			
		} catch (LpSolveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	/*	
		double inf = Double.NaN;
		double test = 23409.23988470;
		
		System.out.println((test < inf));
		
		Server s1 = new Server(Catalog.buyServer());
		Server s2 = new Server(Catalog.buyServer());
		Server s3 = new Server(Catalog.buyServer());
	
		DefaultDirectedGraph<Server,Link> g = new DefaultDirectedGraph<Server,Link> (Link.class);
		g.addVertex(s1);
		g.addVertex(s2);
		g.addVertex(s3);
		g.addEdge(s1, s2, new Link(s1,s2,10));
		g.addEdge(s3, s2, new Link(s3,s2,10));
		
		System.out.println(g.degreeOf(s2));
		System.out.println(g.incomingEdgesOf(s2));
		*/
		
		/*
		SecureRandom rng = new SecureRandom();
		Catalog.setRNG(rng);
		int n_pods = 4;
		DataCenter dc = new DataCenter("FatTree",n_pods);
		SimpleDirectedWeightedGraph<Node,Link> g = new SimpleDirectedWeightedGraph<Node,Link> (Link.class);
		for(Pod p: dc.getPods()) {
			for(Switch sw: p.getCore()) {
				g.addVertex(sw);
			}
			for(Switch sw: p.getAggregation()) {
				g.addVertex(sw);
			}
			for(Switch sw: p.getEdge()) {
				g.addVertex(sw);
			}
			
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					g.addVertex(s);
				}
			}
		}
		
		for(Pod p: dc.getPods()) {
			int tmp =0;
			for(Switch ag: p.getAggregation()) {
				for(int i=tmp; i<tmp+n_pods/2;i++) {
					g.addEdge(ag, p.getCore().get(i), new Link(ag,p.getCore().get(i),10));
					g.setEdgeWeight(g.getEdge(ag, p.getCore().get(i)), 10);
					g.addEdge(p.getCore().get(i), ag, new Link(p.getCore().get(i),ag,10));
					g.setEdgeWeight(g.getEdge(p.getCore().get(i),ag), 10);
				}
				tmp += n_pods/2;
			}
			for(Switch sw: p.getEdge()) {
				for(Switch sw2: p.getAggregation()) {
					g.addEdge(sw, sw2, new Link(sw,sw2,10));
					g.setEdgeWeight(g.getEdge(sw, sw2),10);
					g.addEdge(sw2, sw, new Link(sw2,sw,10));
					g.setEdgeWeight(g.getEdge(sw2, sw),10);
				}
			}
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					g.addEdge(s, r.getSwitches().get(0), s.getOut_connection());
					g.setEdgeWeight(s.getOut_connection(), 10);
					g.addEdge(r.getSwitches().get(0),s, s.getIn_connection());
					g.setEdgeWeight(s.getIn_connection(), 10);

				}
			}
		}
	//	PushRelabelMFImpl<Node,Link> al = new PushRelabelMFImpl<Node,Link>(g);
		EdmondsKarpMFImpl<Node,Link> al = new EdmondsKarpMFImpl<Node,Link>(g);
		
		
		
		
		
		
		ArrayList<Server> hosts = new ArrayList<Server>();
		for(Pod p:dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s:r.getHosts()) {
					hosts.add(s);
				}
			}
		}
		Date d1= new Date();
		for(Server s1: hosts) {
			//System.out.println(s1.getId()+" done");
			for(Server s2: hosts) {
				if( s1 != s2){
				al.calculateMaximumFlow(s1, s2);
				}
			}
			
		}
		
		Date d2 = new Date();
		System.out.println(d2.getTime()-d1.getTime());
		*/
	}
	
	
	public static void provaTree() {
		TreeIndex tree = new TreeIndex();
		SecureRandom rng = new SecureRandom();
		for(int i=0; i<30000; i++) {
			tree.insert(rng.nextInt(30000), i);
		}
		
		
		TreeNodeExplorer iter = new TreeNodeExplorer(tree);
		iter.setStart(Double.NEGATIVE_INFINITY);
		
		while(iter.hasNext()) {
			
			iter.next();
		
		}
		
		Date d_a = new Date();
		TreeNode n = tree.find(rng.nextInt(30000));
		Date d_b = new Date();
		System.out.println(n.getIndex()+ "  time = "+(d_b.getTime() - d_a.getTime()));
	}
}
