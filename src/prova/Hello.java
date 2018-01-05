package prova;

import general.*;
import general.DataCenter;
import general.Link;
import general.Server;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.*;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
public class Hello {

	public static void main(String [] args) {
		/*
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
		SecureRandom rng = new SecureRandom();
		Catalog.setRNG(rng);
		int n_pods = 34;
		DataCenter dc = new DataCenter("FatTree",n_pods);
		DefaultDirectedGraph<Node,Link> g = new DefaultDirectedGraph<Node,Link> (Link.class);
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
					g.addEdge(p.getCore().get(i), ag, new Link(p.getCore().get(i),ag,10));
				}
				tmp += n_pods/2;
			}
			for(Switch sw: p.getEdge()) {
				for(Switch sw2: p.getAggregation()) {
					g.addEdge(sw, sw2, new Link(sw,sw2,10));
					g.addEdge(sw2, sw, new Link(sw2,sw,10));
				}
			}
			for(Rack r: p.getRacks()) {
				for(Server s: r.getHosts()) {
					g.addEdge(s, r.getSwitches().get(0), s.getOut_connection());
					g.addEdge(r.getSwitches().get(0),s, s.getIn_connection());

				}
			}
		}
		
		DijkstraShortestPath<Node,Link> alg = new DijkstraShortestPath<Node,Link>(g);
		Date d1= new Date();
		SingleSourcePaths<Node,Link> paths = alg.getPaths(dc.getPods().get(0).getRacks().get(0).getHosts().get(0));
		Date d2 = new Date();
		
		System.out.println(d2.getTime()-d1.getTime());
		int count = 0;
	/*	for(Pod p:dc.getPods()) {
			for(Rack r: p.getRacks()) {
				for(Server s:r.getHosts()) {
					//System.out.println(paths.getPath(s).toString());
					count++;
				}
			}
		}
		System.out.println(count);
		*/
	}
	
}
