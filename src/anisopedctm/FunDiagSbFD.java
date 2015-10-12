package anisopedctm;

import java.util.Hashtable;

/**
 * Class representing SbFD (Stream-base fundamental diagram)
 * 
 * @author Flurin Haenseler, Gael Lederrey
 * 
 */

/*
 * Fundamental diagram inspired by Wong (2010) and Xie and Wong (2014).
 */
public class FunDiagSbFD extends FunDiag {

	//parameters of fundamental diagram
	private double beta;
	private double theta;

	//link angles (of all links)
	private final static Hashtable<String,Integer> linkAngles = Parameter.linkAngles;

	// constructor
	public FunDiagSbFD(Parameter param, double areaSize) {
		super (param, areaSize);

		theta = param.getShapeParam()[0];
		beta = param.getShapeParam()[1];
	}

	//update derived quantities
	protected void updateDerivedQuantities() {
		actLinkKeys = getActLinks();
		numActLinks = actLinkKeys.size();
		actLinkIndex = getActLinkIndex(); //generate indices of active links for solving FD
		totAcc = getTotAcc(linkAcc);
	}

	//returns intersection angle between link A and link B in radians
	private double interAngle(String strA, String strB) {

		// First, we take the absolute value
		double phi = Math.abs(linkAngles.get(strA) - linkAngles.get(strB));

		// We keep it between 0 and 360 degrees
		phi = phi % 360;

		return Math.toRadians(phi);
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
				critAcc = computeCritAcc(lnk);
				critLinkAcc.put(lnk, critAcc);

				critVel = computeFD(lnk, critAcc);
				critLinkVel.put(lnk, critVel);

				if (Double.isInfinite(critAcc) | Double.isNaN(critVel))
				{
					throw new IllegalArgumentException("Invalid critAcc (" + critAcc +
							"), or critVel ("+ critVel +") on link " + lnk +
							" (beta: " + beta + ", theta: " + theta + ")");
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

		Double velocity;

		//loop over all links
		for (String curStr : linkNames) { //current link

			velocity = computeFD(curStr, linkAcc.get(curStr));
			//store current link speed
			linkVel.put(curStr, velocity);
		}
	}

	//
	private double computeFD(String linkLambda, double accLambda)
	{
		double vLambda, totAcc;

		totAcc = accLambda;

		vLambda = 1.0;

		//loop over all links
		for (String curLnk : linkNames) { //current link
			if(!curLnk.equals(linkLambda)) // If this is another link than the one the FD is computed for
			{
				vLambda *= Math.exp(-beta*(1.0-Math.cos(interAngle(linkLambda,curLnk)))*(linkAcc.get(curLnk)/cellArea));
				totAcc += linkAcc.get(curLnk);
			}
		}

		vLambda *= Math.exp(-theta*Math.pow(totAcc/cellArea,2.0));

		return vLambda;
	}

	// Compute the critical accumulation for a link Lambda
	private double computeCritAcc(String linkLambda)
	{
		double critAcc, accOtherLinks;

		if(theta!=0.0)
		{
			accOtherLinks = 0.0;

			//loop over all links
			for (String curLnk : linkNames) { //current link
				if(!curLnk.equals(linkLambda)) // If this is another link than the one the FD is computed for
				{
					accOtherLinks += linkAcc.get(curLnk);
				}
			}

			critAcc = -accOtherLinks/2.0 + Math.sqrt(Math.pow(accOtherLinks/2.0, 2) + Math.pow(cellArea,2.0)/(2.0*theta));
		}
		else
		{
			System.out.println("theta: " + theta);

			critAcc = Double.POSITIVE_INFINITY;
		}

		return critAcc;
	}

	// compute the critical density and critical speed (for the visualization)
	public Hashtable<String, Double> critValues()
	{
		Hashtable<String, Double> critVal = new Hashtable<String, Double>();

		double critDens = 0.0;

		if(theta!=0.0)
		{
			critDens = Math.sqrt(1/(2.0*theta));
		}
		else
		{
			critDens = Double.POSITIVE_INFINITY;
		}

		critVal.put("critDens", critDens);
		//critVal.put("critVel", Math.exp(-theta*Math.pow(CritDens,2.0)));
		critVal.put("critVel", Math.exp(-1.0/2.0));

		return critVal;
	}

}
