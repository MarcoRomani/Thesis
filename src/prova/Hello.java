package prova;

import general.Catalog;
import general.Link;
import general.Server;
import org.*;
import org.jgrapht.graph.*;
public class Hello {

	public static void main(String [] args) {
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
		
	}
}
