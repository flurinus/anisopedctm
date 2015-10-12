package anisopedctm;

import java.util.Hashtable;

/**
 * Class representing Drake Fundamental diagram
 * 
 * @author Flurin Haenseler, Gael Lederrey
 * 
 */

/*
 * Zero fundamental diagram.
 */
public class FunDiagZero extends FunDiag {

	// constructor
	public FunDiagZero(Parameter param, double areaSize) {
		super (param, areaSize);
	}

	//update derived quantities
	protected void updateDerivedQuantities() {
		actLinkKeys = getActLinks();
		numActLinks = actLinkKeys.size();
		actLinkIndex = getActLinkIndex(); //generate indices of active links for solving FD
		totAcc = getTotAcc(linkAcc);
	}

	//set critical accumulations and velocities for all links
	public void setCritLinkAccVel()
	{
		for (String lnk : linkNames) {
			//add critical accumulation and velocity to corresponding hash tables
			critLinkAcc.put(lnk, Double.POSITIVE_INFINITY);
			critLinkVel.put(lnk, 1.0);
		}
	}

	public void setLinkVel()
	{
		//clear previous record of link velocities
		linkVel.clear();

		//update various derived quantities
		updateDerivedQuantities();

		Double velocity = 1.0;

		//loop over all links
		for (String curStr : linkNames) { //current link
			//store current link speed
			linkVel.put(curStr, velocity);
		}
	}


	// compute the critical density and critical speed (for the visualization)
	public Hashtable<String, Double> critValues()
	{
		Hashtable<String, Double> critVal = new Hashtable<String, Double>();

		critVal.put("critDens", Double.POSITIVE_INFINITY);
		critVal.put("critVel", 1.0);

		return critVal;
	}

}
