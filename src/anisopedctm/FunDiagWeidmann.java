package anisopedctm;

import java.util.Hashtable;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BisectionSolver;

/**
 * Class representing Weidmann Fundamental diagram
 * 
 * @author Flurin Haenseler, Gael Lederrey
 * 
 */

/*
 * Fundamental diagram as proposed by Weidmann (1993).
 */
public class FunDiagWeidmann extends FunDiag{

	//parameters of fundamental diagram
	private double gamma;
	private double kj;

	// constructor
	public FunDiagWeidmann(Parameter param, double areaSize) {
		super (param, areaSize);

		gamma = param.getShapeParam()[0];
		kj = param.getShapeParam()[1];

	}

	//update derived quantities
	protected void updateDerivedQuantities()
	{
		actLinkKeys = getActLinks();
		numActLinks = actLinkKeys.size();
		actLinkIndex = getActLinkIndex(); //generate indices of active Links for solving FD
		totAcc = getTotAcc(linkAcc);
	}

	//set critical accumulations and velocities for all Links
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
			double critAcc;
			for (String lnk : linkNames) {
				critAcc = computeCritAcc(lnk);
				critLinkAcc.put(lnk, critAcc);							// Critical accumulation for unidirectional flow
				critLinkVel.put(lnk, computeFD(lnk, critAcc)); 			// Critical velocity for unidirectional flow
			}
		}
	}

	public void setLinkVel()
	{
		//clear previous record of Link velocities
		linkVel.clear();

		//update various derived quantities
		updateDerivedQuantities();

		Double velocity;

		//loop over all Links
		for (String curStr : linkNames) { //current Link

			velocity = computeFD(curStr, linkAcc.get(curStr));
			//store current Link speed
			linkVel.put(curStr, velocity);
		}
	}

	// Compute the value of Wong's FD with a Link lambda
	private double computeFD(String linkLambda, double accLambda)
	{
		double vLambda, totAcc;

		totAcc = accLambda;

		//loop over all Links
		for (String curLnk : linkNames) { //current Link
			if(!curLnk.equals(linkLambda)) // If this is another Link than the one the FD is computed for
			{
				totAcc += linkAcc.get(curLnk);
			}
		}

		if(totAcc == 0.0)
		{
			return Double.POSITIVE_INFINITY;
		}
		else if(totAcc/cellArea > kj)
		{
			return 0.0;
		}
		else
		{
			vLambda = 1.0 - Math.exp(-gamma*((cellArea/totAcc) - (1.0/kj)));

			return vLambda;
		}
	}

	// Compute the critical accumulation for a Link Lambda
	private double computeCritAcc(String linkLambda)
	{
		double critAcc, accOtherLinks;

		accOtherLinks = 0.0;

		//loop over all Links
		for (String curLnk : linkNames) { //current Link
			if(!curLnk.equals(linkLambda)) // If this is another Link than the one the FD is computed for
			{
				accOtherLinks += linkAcc.get(curLnk);
			}
		}

		FuncRacine funcRacine = new FuncRacine(accOtherLinks, cellArea, kj, gamma);

		final double tol = Parameter.Tolerance;

		BisectionSolver BisecSolv = new BisectionSolver(tol,tol);

		int maxEval = Parameter.maxEvaluations;

		critAcc = BisecSolv.solve(maxEval, funcRacine, 1.0, 100.0);

		if(critAcc > kj*cellArea)
		{
			critAcc = kj*cellArea;
		}

		return critAcc;

	}

	// Class to find the zero (For solving the critical accumulation)
	private static class FuncRacine implements UnivariateFunction {

		private double accGammaprime;
		private double cellArea;
		private double kj;
		private double gamma;

		public FuncRacine(double accOtherLinks, double cellArea, double kj, double gamma)
		{
			accGammaprime = accOtherLinks;
			this.cellArea = cellArea;
			this.kj = kj;
			this.gamma = gamma;
		}

		public double value(double accLambda)
		{
			double exp, mult, totAcc;

			totAcc = accGammaprime + accLambda;

			mult = (1.0 + accLambda*gamma*(cellArea/Math.pow(totAcc,2)));

			exp = Math.exp(-gamma*((cellArea/totAcc) - (1.0/kj)));

			if(totAcc == 0.0)
			{
				return Double.POSITIVE_INFINITY;
			}
			else
			{
				return 1.0 - mult*exp;
			}
		}
	}

	// compute the critical density and critical speed (for the visualization)
	public Hashtable<String, Double> critValues()
	{
		Hashtable<String, Double> critVal = new Hashtable<String, Double>();

		FuncRacine1D funcRacine = new FuncRacine1D(gamma,kj);

		final double tol = Parameter.Tolerance;

		BisectionSolver BisecSolv = new BisectionSolver(tol, tol);

		int maxEval = Parameter.maxEvaluations;

		double x_crit = BisecSolv.solve(maxEval, funcRacine, 0.0, kj);

		double k_crit = gamma/x_crit;

		critVal.put("critDens", k_crit);
		critVal.put("critVel", funcFunDiag(k_crit));

		return critVal;
	}

	//Function for the FunDiag of Weidmann (1993)
	private double funcFunDiag(double k)
	{
		if(k < kj)
		{
			return (1-Math.exp(-gamma*(1.0/k - 1.0/kj)));
		}
		else
		{
			return 0.0;
		}


	}

	//Function in which we have to find the zero.
	private static class FuncRacine1D implements UnivariateFunction {

		private double xj;

		public FuncRacine1D(double gamma, double kj)
		{
			xj = gamma/kj;
		}

	    public double value(double x) {

	    	return 1-(1+x)*Math.exp(xj-x);
	    }
	}


}
