package anisopedctm;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

/**
 * Abstract class representing a fundamental diagram
 * 
 * @author Flurin Haenseler, Gael Lederrey
 * 
 */

/*
 * Abstract class for the Fundamental Diagram. Any kind of Fundamental Diagram can be a child from this abstract class.
 * For the moment, we have 3 FunDiag : Weidmann, Drake and SbFD.
 *
 * WARNING : If you want to add a new Fundamental Diagram, you should create a new class that will inherit from this one.
 * Then, you have to change the conditions to send an error in the class Input.java. You will also have to add
 * a new condition in the constructor of the class Cell.java.
 */
abstract class FunDiag {

	//cell properties
	protected double cellArea;

	//names of links relevant for current cell
	protected List<String> linkNames;

	//properties of links (N->E, S->W, W->E, etc.)
	protected Hashtable<String, Double> linkAcc; //accumulation
	protected Hashtable<String, Double> linkVel; //non-dimensional velocity

	//theoretical properties of critical links
	protected Hashtable<String, Double> critLinkAcc; // critical accumulation
	protected Hashtable<String, Double> critLinkVel; // critical non-dimensional velocity

	//currently active (i.e., non-zero) links
	HashSet<String> actLinkKeys; //list of active (i.e., non-empty) links
	protected int numActLinks; //number of active links
	protected Hashtable<String,Integer> actLinkIndex; //indices of links in velNd (used for solving FD of active links)

	//derived properties
	protected double totAcc; //total accumulation

	// constructor
	public FunDiag(Parameter param, double areaSize) {
		cellArea = areaSize;

		linkNames = new ArrayList<String>();

		linkAcc =  new Hashtable<String, Double>();
		linkVel = new Hashtable<String, Double>();

		critLinkAcc = new Hashtable<String, Double>();
		critLinkVel = new Hashtable<String, Double>();
	}

	//add link name if not included
	public void addLinkName(String lnkName) {
		if (!linkNames.contains(lnkName)) {
			linkNames.add(lnkName);
		}
	}

	//clear linkAcc
	public void clearLinkAcc() {
		linkAcc.clear();
	}

	//add accumulation to link, generate new "link" if necessary
	public void addToLink(String lnkName, double lnkAcc) {
		if (linkAcc.containsKey(lnkName)) {
			linkAcc.put(lnkName, linkAcc.get(lnkName) + lnkAcc);
		} else {
			linkAcc.put(lnkName, lnkAcc);
		}
	}

	//get keys of active links
	public HashSet<String> getActLinks() {

		//enumeration
		HashSet<String> actLinks = new HashSet<String>();

		//loop over all links
		Enumeration<String> linkKeys = linkAcc.keys(); //enumeration of all links
		String curLink; //name of current link
		double curLinkAcc; //size of current link

		while(linkKeys.hasMoreElements()) {
			 curLink = linkKeys.nextElement();
			 curLinkAcc = linkAcc.get(curLink);

			 //if current link is active, add it
			 if (curLinkAcc > 0.0){
				 actLinks.add(curLink);
			}
		}

		return actLinks;
	}

	//returns indices of link in velNd (an array of double of variable length,
	//depending on whether links (N->E, S->W, W->E, etc.) are active)
	protected Hashtable<String,Integer> getActLinkIndex() {
		Hashtable<String,Integer> indexMap = new Hashtable<String,Integer>();

		int i = 0; //index
		for (String link : linkNames) {
			if (linkAcc.get(link) > 0) {
				indexMap.put(link, i);
				i++;
			}
		}
		return indexMap;
	}

	//computes total accumulation in cell
	public double getTotAcc(Hashtable<String, Double> lnkAcc) {
		double totAcc = 0.0;

		for (double lAcc : lnkAcc.values() ) {
			totAcc += lAcc;
		}

		return totAcc;
	}

	//returns accumulation of given link
	public double getLinkAcc(String lnkName) {
		return linkAcc.get(lnkName);
	}

	//returns speed of link
	public double getLinkVel(String lnkName) {
		return linkVel.get(lnkName);
	}

	//returns critical accumulation of link
	public double getCritLinkAcc(String lnkName) {
		return critLinkAcc.get(lnkName);
	}

	//returns critical velocity of link
	public double getCritLinkVel(String lnkName) {
		return critLinkVel.get(lnkName);
	}

	//ABSTRACT : update derived quantities
	protected abstract void updateDerivedQuantities();

	//ABSTRACT : compute and store non-dimensional link speeds
	public abstract void setLinkVel();

	//ABSTRACT : compute and set critical accumulations and velocities for all links
	public abstract void setCritLinkAccVel();

	// ABSTRACT : compute the critical density and critical speed (for the visualization)
	public abstract Hashtable<String, Double> critValues();


}
