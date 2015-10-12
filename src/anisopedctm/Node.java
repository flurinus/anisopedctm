package anisopedctm;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Node class
 *
 * @author Flurin Haenseler, Gael Lederrey
 *
 */

public class Node {
	
	private HashSet<Integer> inLinks;  // list of all in links
	private HashSet<Integer> outLinks;  // list of all out links
	
	// key = routeName, value = node potential
	private Hashtable<String,Double> routePotentials;
	
	// key = routeName, value = denominator of route choice model
	private Hashtable<String,Double> rChoiceDenom;
	
	// set of adjacent cells (contains two elements, one may be equal to "none")
	private HashSet<String> adjacentCells;
	
	//set of associated zones (can contain one or two elements)
	private HashSet<String> associatedZones;
	
	// constructor
	public Node(String neighborCellA, String neighborCellB) {
		inLinks = new HashSet<Integer>();
		outLinks = new HashSet<Integer>();
		
		routePotentials = new Hashtable<String,Double>();
		
		rChoiceDenom = new Hashtable<String,Double>();
		
		adjacentCells = new HashSet<String>();
		adjacentCells.add(neighborCellA);
		adjacentCells.add(neighborCellB);
		
		associatedZones = new HashSet<String>();
	}
	
	public void addInLink(int linkID) {
		this.inLinks.add(linkID);
	}
	
	public void addOutLink(int linkID) {
		this.outLinks.add(linkID);
	}
	
	public HashSet<Integer> getInLinks() {
		return this.inLinks;
	}
	
	public HashSet<Integer> getOutLinks() {
		return this.outLinks;
	}
	
	public void addAssociatedZone(String zoneName) {
		associatedZones.add(zoneName);
	}
	
	public boolean associatedWithZone(String zoneName) {
		return associatedZones.contains(zoneName);
	}

	public void setPotential(String routeName, double pot) {
		this.routePotentials.put(routeName, pot);
	}
	
	public double getPotential(String routeName) {
		return this.routePotentials.get(routeName);
	}
	
	//check if node potential is defined for a given route
	//useful to check if a route involves current node
	public boolean containsRoutePotential(String routeName){
		return routePotentials.containsKey(routeName);
	}
	
	//check if a link is available for a given route
	//rational: check if for destination node of link a potential is defined 
	public boolean containsOutLink(Hashtable<Integer, Link> linkList,
			Hashtable<Integer, Node> nodeList, String routeName, int targLinkID){
		
		Link curLink = linkList.get(targLinkID); //target link
		
		int curDestNodeID = curLink.getDestNode(); //ID of destination node of target link
		
		Node curDestNode = nodeList.get(curDestNodeID); //destination node of target link
		
		//if destination node of target link has a defined potential for the route,
		//the target link is a feasible choice. True is returned
		return curDestNode.containsRoutePotential(routeName);
	}
	
	public void computeRouteChoiceDenominator(Hashtable<Integer, Link> linkList,
			Hashtable<Integer, Node> nodeList, Parameter param) {
		
		String curRouteName; //container for current route
		int curDestNodeID; //ID of current destination node
		double nodePot; //container for current potential of current node and route
		double denom; //container for current denominator
		double eta = param.getMu(); //route choice parameter
		
		//iterate through routes that are available on current node
		Enumeration<String> routeKeys = routePotentials.keys();
		
		while(routeKeys.hasMoreElements()) {
			curRouteName = routeKeys.nextElement();
		    
			denom = 0.0; //reset denominator
			
			for (int outLinkID : outLinks){
				//check if outLink leads to a node with a defined potential for curRoute
				if (this.containsOutLink(linkList, nodeList, curRouteName, outLinkID)){
					//ID of current destination node
					curDestNodeID = linkList.get(outLinkID).getDestNode(); 
					
					//potential of destination node on outLink for curRoute
					nodePot = nodeList.get(curDestNodeID).getPotential(curRouteName);
					
					//add logit-summand to denominator
					denom += Math.exp(-eta*nodePot);
				}
			}
			
			//put pre-computed route choice denominator to corresponding hash table
			rChoiceDenom.put(curRouteName, denom);
		}
	}
	
	public double getRouteChoiceFrac(Hashtable<Integer, Link> linkList,
			Hashtable<Integer, Node> nodeList, String routeName, int targLink, Parameter param) {
		
		final double eta = param.getMu(); //route choice parameter
		Link tLink = linkList.get(targLink); //target link
		Node tNode = nodeList.get(tLink.getDestNode()); //corresponding target node
		
		//get route-specific potential of target node
		double tNodePot = tNode.getPotential(routeName);
		
		//retrieve pre-computed denominator of route choice model
		double rChDenom = rChoiceDenom.get(routeName);
				
		// compute logit-based route choice fraction
		if(Math.exp(-eta*tNodePot)/rChDenom < 1e-14)
		{
			return 0.0;
		}
		else
		{	
			return Math.exp(-eta*tNodePot)/rChDenom;
		}
	}
	
	public boolean adjacentToCells(String cellNameA, String cellNameB) {
		return (adjacentCells.contains(cellNameA) & adjacentCells.contains(cellNameB));
	}
	
	public HashSet<String> getadjacentCells()
	{
		return adjacentCells;
	}

	
}
