package anisopedctm;

import java.util.Hashtable;

/**
 * Class representing Drake Fundamental diagram
 * 
 * @author Flurin Haenseler, Gael Lederrey
 * 
 */

/*
 * Fundamental diagram inspired by Drake (1967).
 */
public class FunDiagDrake extends FunDiag {

	//parameters of fundamental diagram
	private double thetaDrake;

	// constructor
	public FunDiagDrake(Parameter param, double areaSize) {
		super (param, areaSize);

		thetaDrake = param.getShapeParam()[0];
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
		if (Double.isInfinite(cellArea)) {
			for (String lnk : linkNames) {
				//add critical accumulation and velocity to corresponding hash tables
				critLinkAcc.put(lnk, Double.POSITIVE_INFINITY);
				critLinkVel.put(lnk, 1.0);
			}
		}
		else
		{
			double critAcc; // critical accumulation for unidirectional flow
			double critVel; // critical velocity for unidirectional flow
			for (String lnk : linkNames) {
				critAcc = ComputeCritAcc(lnk);
				critLinkAcc.put(lnk, critAcc);

				critVel = computeVelNd(critAcc);
				critLinkVel.put(lnk, critVel);

				if (Double.isInfinite(critAcc) | Double.isNaN(critVel))
				{
					throw new IllegalArgumentException("Invalid critAcc (" + critAcc +
							"), or critVel ("+ critVel +") on link " + lnk);
				}
			}
		}
	}

	public void setLinkVel()
	{
		//clear previous record of link velocities
		linkVel.clear();

		//update various derived quantities
		updateDerivedQuantities();

		Double velocity = computeVelNd(totAcc);

		//loop over all links
		for (String curStr : linkNames) { //current link
			//store current link speed
			linkVel.put(curStr, velocity);
		}
	}

	private double computeVelNd(double acc)
	{

		return Math.exp(-thetaDrake*Math.pow(totAcc/cellArea,2.0));
	}

	// Compute the critical accumulation for a link Lambda
	private double ComputeCritAcc(String linkLambda)
	{
		double critAcc, accOtherLinks;

		if(thetaDrake!=0.0)
		{
			accOtherLinks = 0.0;

			//loop over all links
			for (String curLnk : linkNames) { //current link
				if(!curLnk.equals(linkLambda)) // If this is another link than the one the FD is computed for
				{
					accOtherLinks += linkAcc.get(curLnk);
				}
			}

			critAcc = -accOtherLinks/2.0 + Math.sqrt(Math.pow(accOtherLinks/2.0, 2) + Math.pow(cellArea,2.0)/(2.0*thetaDrake));
		}
		else
		{
			System.out.println("thetaDrake: " + thetaDrake);

			critAcc = Double.POSITIVE_INFINITY;
		}

		return critAcc;
	}

	// compute the critical density and critical speed (for the visualization)
	public Hashtable<String, Double> critValues()
	{
		Hashtable<String, Double> critVal = new Hashtable<String, Double>();

		double critDens = 0.0;

		if(thetaDrake!=0.0)
		{
			critDens = Math.sqrt(1/(2.0*thetaDrake));
		}
		else
		{
			critDens = Double.POSITIVE_INFINITY;
		}

		critVal.put("critDens", critDens);
		critVal.put("critVel", Math.exp(-1.0/2.0));

		return critVal;
	}

}
