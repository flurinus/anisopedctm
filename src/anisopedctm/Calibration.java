package anisopedctm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;


public class Calibration {
	//parameter value and log-likelihood of optimum
	private double bestLogLikelihood;
	private double[] bestParam;
	
	//simulated travel time: mean and standard deviation
	private double[] bestTravelTimeMeanSim;
	private double[] bestTravelTimeStdDevSim;
	
	//corresponding statistics
	private RealMatrix hessianLL; //Hessian of log-likelihood at optimum
	private double[] eigValuesHessianLL; //corresponding eigenvalues
	private boolean complexEigenvalues;
	private RealMatrix cramerRaoLL; //lower Cramer-Rao bound
	private double[] standErrLL; //standard errors (square root of diagonal of CR)
	private RealMatrix corrMatrixLL; //correlation matrix obtained from Cramer-Rao bound
	
	//calibration parameters
	private int numIter;
	private int numParam;
	private Board board;
	private Parameter param;
	
	//result of calibration
	private PointValuePair[] calibSeries;
	private boolean invertHessianSuccessful;
	
	//result of cross-calibration
	private Hashtable<Integer, crossVal> crossValTable;
	
	//travel time distribution
	private Hashtable<String, travelTimeDist> routeTravelTimeDist;
	
	public Calibration(Board board, int numRun){
		this.board = board;
		this.param = board.getParam();
		this.numIter = numRun;
		
		numParam = param.getNumParam();
		
		bestLogLikelihood = Double.NEGATIVE_INFINITY;
		bestParam = new double[numParam];
		
		calibSeries = new PointValuePair[numRun];
	}
	
	public double getBestLogLikelihood() {
		return bestLogLikelihood;
	}

	public double[] getBestParam() {
		return bestParam;
	}
	
	public int getNumIter() {
		return numIter;
	}
	
	public PointValuePair[] getCalibSeries() {
		return calibSeries;
	}
	
	public double[] getBestTravelTimeMeanSim() {
		return bestTravelTimeMeanSim;
	}
	
	public double[] getBestTravelTimeStdDevSim() {
		return bestTravelTimeStdDevSim;
	}
	
	public boolean invertHessianSuccessful() {
		return invertHessianSuccessful;
	}
	
	public RealMatrix getHessianLL() {
		return hessianLL;
	}
	
	public double[] getEigValuesHessianLL() {
		return eigValuesHessianLL;
	}
	
	public boolean hasComplexEigenvalues() {
		return complexEigenvalues;
	}

	public RealMatrix getCramerRaoLL() {
		return cramerRaoLL;
	}
	
	public double[] getStandErrLL() {
		return standErrLL;
	}
	
	public RealMatrix getCorrMatrixLL() {
		return corrMatrixLL;
	}
	
	public String getCrossValTableEntry(int i) {
		crossVal curEntry = crossValTable.get(i);
		
		String entry = curEntry.logLikelihoodValid + ", " + curEntry.logLikelihoodCalib;
		
		for (int j=0; j<param.getNumParam(); j++) {
			entry += ", " + curEntry.paramCalib[j];
		}
	
		entry += "\n";
		
		return entry;
	}
	
	//derivative-free optimizer
	private PointValuePair bobyqaOptimizer(double[] initParam) {
			
		int numInterpoltationPoints = 2*initParam.length;
		
		double initRad = 10;
		double stoppingRad = 1e-3;
	
		MultivariateOptimizer multiVarOptimizer = new BOBYQAOptimizer(numInterpoltationPoints, initRad, stoppingRad);
	    
		MultivariateFunction logLikelihoodFunction = new LogLikelihoodCalculator(board);
	
		PointValuePair result = multiVarOptimizer.optimize(
				new MaxEval(1500),
		        GoalType.MAXIMIZE,
		        new InitialGuess(initParam),
		        new ObjectiveFunction(logLikelihoodFunction),
		        new SimpleBounds(param.getParamLowerBound(), param.getParamUpperBound()));	
			
		return result;
	}

	private class LogLikelihoodCalculator implements MultivariateFunction {
		//simulation board
		Board curBoard;
		
		//constructor
		public LogLikelihoodCalculator(Board board) {
			curBoard = board;
		}
		
		//returns the log-likelihood
		@Override
		public double value(double[] paramVec) {
	    	// update the board with new parameters
			curBoard.updateParam(paramVec);
	
			// simulate
			curBoard.simulate();
	    	
			// return log-likelihood
			Double logLikelihood = curBoard.getLogLikelihood(); 
			
			if (logLikelihood.isNaN()) {
				return Double.NEGATIVE_INFINITY;
			}else {
				return logLikelihood;
			}
		}
	}
	
	
	//calibrate using default parameters
	public void runDefault() {
		
		board.simulate();
		
		bestTravelTimeMeanSim = board.getMeanPedTravelTimeSim();
		bestTravelTimeStdDevSim = board.getStdDevPedTravelTimeSim();
	}
	
	
	
	//calibrate using default parameters
	public void calibFromDefault() {
		
		//extract default parameter values
		double[] paramDefault = new double[param.getNumParam()];
		
		paramDefault[0] = param.getFreeSpeed();
			
		for (int i=0; i<param.getNumParam()-2; i++) {
			paramDefault[i+1] = param.getShapeParam()[i];
		}
		
		paramDefault[param.getNumParam()-1] = param.getMu();
		
		//initialize log-likelihood calculator
		MultivariateFunction logLikelihoodFunction = new LogLikelihoodCalculator(board);
		
		//check log-likelihood of default parameters
		Double logLikelihoodDefault = logLikelihoodFunction.value(paramDefault);
		
		if (logLikelihoodDefault.isNaN() || logLikelihoodDefault.isInfinite()) {
			throw new IllegalArgumentException("Default parameters yield invalid log-likelihood.");
		}
		else {
			//find corresponding local optimum
			PointValuePair localCalib = bobyqaOptimizer(paramDefault);
			
			calibSeries[0] = localCalib;
			
			//update loglikelihood and parameters
			bestLogLikelihood = localCalib.getValue();
			bestParam = localCalib.getPoint();
		}
	}
	
	private PointValuePair calibRandInit() {
		//randomly generate a set of initial parameters, and use the best one
		int numDraws = 1;
		double[] paramDraw;
		double[] paramInit = new double[param.getNumParam()];
		Double logLikelihoodDraw;
		double logLikelihoodInit = Double.NEGATIVE_INFINITY;
		
		MultivariateFunction logLikelihoodFunction = new LogLikelihoodCalculator(board);
		
		PointValuePair paramLocOpt = null;
		
		//for some awkward initial parameters, some iteration runs fail and are redone.
		boolean runSuccessful = false;

		while (runSuccessful == false) {
			try{
				//draw initial parameter
				for (int i=0; i<numDraws; i++) {
					//randomly generate initial parameter estimate
					paramDraw = param.getRandomParameter();
					
					//check if initial solution is feasible
					logLikelihoodDraw = logLikelihoodFunction.value(paramDraw);
					
					//if infeasible, redraw
					while (logLikelihoodDraw.isNaN() || logLikelihoodDraw.isInfinite()) {
						paramDraw = param.getRandomParameter();
						logLikelihoodDraw = logLikelihoodFunction.value(paramDraw);
					}
					
					//if best draw so far, use as new init parameter set
					if (logLikelihoodDraw > logLikelihoodInit) {
						paramInit = paramDraw;
						logLikelihoodInit = logLikelihoodDraw;
					}
					
				}
				
				//compute corresponding local optimum
				paramLocOpt = bobyqaOptimizer(paramInit);
				
				//if no exception raised so far, local optimum search successful
				runSuccessful = true;
			}
			catch (Exception e) {
				System.out.println("Iteration raised " + e.toString() +". Run redone.");
			}
		}
		return paramLocOpt;
	}
	
	public double[] calibMultInit() {
		
		bestLogLikelihood = Double.NEGATIVE_INFINITY;
		
		PointValuePair curCalibration;
		double curLogLikelihood;
		
		for (int i=0; i<numIter; i++) {
			curCalibration = calibRandInit();
			
			calibSeries[i] = curCalibration;
			
			curLogLikelihood = curCalibration.getValue();
			
			//DEBUG
			System.out.print("Calib " + (i+1) + "/" + numIter + " -- log-likelihood: " + curLogLikelihood + "; param: ");
			for (int j=0; j<param.getNumParam(); j++) {
				System.out.print(curCalibration.getPoint()[j]);
				if (j < param.getNumParam() -1) {
					 System.out.print(", ");
				}
			}
			System.out.print("\n");
			//END DEBUG
			
			if (curLogLikelihood >= getBestLogLikelihood()) {
				bestLogLikelihood = curLogLikelihood;
				bestParam = curCalibration.getPoint();
			}
			
		}
		
		return bestParam;
	}
	
	public void generateCalibStatistics(String fileName) {
		System.out.println("Computing Hessian.");
		//compute calibration statistics
		double stepSize = Parameter.stepSizeHessian;
		computeCalibStatistics(bestParam, stepSize);
		
		String fileNameFull = fileName+getFileNameAppendix();
		
		//write statistics to file
		board.getOutput().writeCalibStatistics(this, param, board.getPedList(), fileNameFull );
	}
	
	//stores discrete observed and estimated travel time distribution (e.g. for a route)
	private class travelTimeDist {
		private Hashtable<Integer, Integer> distObs;
		private Hashtable<Integer, Double> distEst;
		
		private int minTravTimeInt;
		private int maxTravTimeInt;
		
		private int numPeopleObs;
		private double numPeopleEst;
		
		public travelTimeDist(double travTimeObs) {
			distObs = new Hashtable<Integer, Integer>();
			distEst = new Hashtable<Integer, Double>();
			
			minTravTimeInt = Integer.MAX_VALUE;
			maxTravTimeInt = Integer.MIN_VALUE;
			
			numPeopleObs = 0;
			numPeopleEst = 0.0;
			
			addObsTravelTime(travTimeObs); //add first pedestrian
		}
		
		public int getTotNumPeopleObs() {
			return numPeopleObs;
		}
		
		public double getTotNumPeopleEst() {
			return numPeopleEst;
		}
		
		public int getNumPeopleObs(int travTimeInt) {
			if (distObs.containsKey(travTimeInt)) {
				return distObs.get(travTimeInt);
			}
			else {
				return 0;
			}
		}
		
		public double getFragSizeEst(int travTimeInt) {
			if (distEst.containsKey(travTimeInt)) {
				return distEst.get(travTimeInt);
			}
			else {
				return 0.0;
			}
		}
		
		public void addObsTravelTime(double travTime) {
			
			//increment pedestrian count
			numPeopleObs++;
			
			//obtain corresponding travel time interval
			int travTimeInt = (int) Math.floor(travTime/param.getDeltaT());
			
			//update min and max travel time
			updateMinMaxTravTime(travTimeInt);
			
			//update observed travel time distribution
			if ( distObs.containsKey(travTimeInt) ) {
				distObs.put(travTimeInt, distObs.get(travTimeInt) + 1);
			}
			else {
				distObs.put(travTimeInt, 1);
			}
		}
		
		public void addEstTravelTimeDist(Hashtable<Integer, Double> travTimeDistEst){
			
			Enumeration<Integer> enumDist = travTimeDistEst.keys();
			int travTimeInt;
			double fragSize;
			
			while(enumDist.hasMoreElements()) {
				travTimeInt = enumDist.nextElement();
				fragSize = travTimeDistEst.get(travTimeInt);
				
				//increment pedestrian count
				numPeopleEst += fragSize;
				
				//update min and max travel time
				updateMinMaxTravTime(travTimeInt);
				
				if ( distEst.containsKey(travTimeInt) ) {
					distEst.put(travTimeInt, distEst.get(travTimeInt) + fragSize);
				}
				else {
					distEst.put(travTimeInt, fragSize);
				}
			}
		}
		
		public void updateMinMaxTravTime(int travTimeInt) {
			if (travTimeInt < minTravTimeInt) {
				minTravTimeInt = travTimeInt;
			}
			if (travTimeInt > maxTravTimeInt) {
				maxTravTimeInt = travTimeInt;
			}
		}
		
	}
	
	public void generateTravelTimeStat(String fileName) {
		
		 String fileNameFull = fileName + getFileNameAppendix(); 
		
		//compute travel time distributions
		computeRouteTravelTimeDist();
		
		//write statistics to file
		board.getOutput().writeTravelTimeAnalysis(this, param, board.getPedList(), fileNameFull);
	}
	
	private void computeRouteTravelTimeDist() {
		
		Hashtable<Integer,Pedestrian> pedList = board.getPedList();
		Hashtable<Integer,Group> groupList = board.getGroupList();
		
		//initialize hashtable of travel time distributions of all routes
		routeTravelTimeDist = new Hashtable<String, travelTimeDist>();
		
		//distribution of current route
		travelTimeDist curDist;
		
		String route;
		
		/*
		 * generate empirical travel time distribution
		 */
		Pedestrian curPed;
		double travTimeObs;
		
		for (int i=0; i<pedList.size(); i++) {
			curPed = pedList.get(i);
			
			route = curPed.getRouteName();
			travTimeObs = curPed.getTravelTime();
			
			if (routeTravelTimeDist.containsKey(route)) {
				curDist = routeTravelTimeDist.get(route);
				curDist.addObsTravelTime(travTimeObs);
			}
			else {
				curDist = new travelTimeDist(travTimeObs);
				routeTravelTimeDist.put(route, curDist);
			}
			
		}
		
		/*
		 * generate travel time distribution estimated by model
		 */
		Group curGroup;
		Hashtable<Integer, Double> travTimeDistEst;
		
		for (int j=0; j<groupList.size(); j++) {
			curGroup = groupList.get(j);
			
			route = curGroup.getRouteName();
			travTimeDistEst = curGroup.getTravelTimes();
			
			if (routeTravelTimeDist.containsKey(route)) {
				curDist = routeTravelTimeDist.get(route);
				curDist.addEstTravelTimeDist(travTimeDistEst);
			}
			else {
				throw new IllegalStateException(
						"Potential mismatch between groupList and pedList. "
						+ "All routes should already have been created.");
			}
			
		}
	}
	
	public String getTravTimeDistEntry(String route) {
		if (routeTravelTimeDist.containsKey(route)) {
			travelTimeDist curDist = routeTravelTimeDist.get(route);
			
			String ttDistEntry = "\nRoute: " + route + " (totNumObs: " +
					curDist.getTotNumPeopleObs() + ", totNumEst: " +
					curDist.getTotNumPeopleEst() + ")\n";
			
			for (int tInt=curDist.minTravTimeInt;tInt<curDist.maxTravTimeInt+1;tInt++) {
				ttDistEntry += tInt + ", " + curDist.getNumPeopleObs(tInt) + ", " +
						curDist.getFragSizeEst(tInt) + "\n";
			}
			
			ttDistEntry += "\n";
			
			return ttDistEntry;
		}
		else {
			throw new IllegalStateException("Route " + route + " not found in routeTravelTimeDist.");
		}
	}
	
	/*
	 * Compute Cramer-Rao bound by taking matrix inverse
	 */
	private void computeCalibStatistics(double[] paramVec, double stepSize)
	{

		MultivariateFunction logLikelihoodFun = new LogLikelihoodCalculator(board);

		/*
		 * Compute Hessian of log-likelihood and Cramer-Rao bound
		 */
		
		hessianLL = MatrixUtils.createRealMatrix(
					computeHessian(logLikelihoodFun, paramVec, stepSize)
				);
		
		try {
			//compute eigenvalues of Hessian
			EigenDecomposition hessEigenDecomp = new EigenDecomposition(hessianLL);
	
			//eigenvalues are invalid if they are complex
			complexEigenvalues = hessEigenDecomp.hasComplexEigenvalues();
	
			//compute eigenvalues
			eigValuesHessianLL = hessEigenDecomp.getRealEigenvalues();
	
			//compute matrix inverse using QR decomposition
			cramerRaoLL = MatrixUtils.inverse(hessianLL);
			cramerRaoLL.scalarMultiply(-1.0);
	
	
			/*
			 * compute correlation matrix from var-covar matrix
			 */
	
			//extract standard errors
			standErrLL = new double[numParam];
	
			for (int i=0; i<numParam; i++){
				standErrLL[i] = Math.sqrt(Math.abs(cramerRaoLL.getEntry(i, i)));
			}
	
			DiagonalMatrix standErrDiagMat = new DiagonalMatrix(standErrLL);
	
			DiagonalMatrix standErrDiagMatInv = standErrDiagMat.inverse();
	
			corrMatrixLL = (cramerRaoLL.preMultiply(standErrDiagMatInv)).multiply(standErrDiagMatInv);
	
			invertHessianSuccessful = true;
		}
		catch (TooManyEvaluationsException | SingularMatrixException e){
			System.out.println("Inversion of Hessian failed. Output of related statistics omitted.");
			
			invertHessianSuccessful = false;
		}

		
		/*
		 * extract simulated pedestrian travel times
		 */
		
		//re-run at optimal parameter point
		board.updateParam(bestParam);
		board.simulate();
	
		bestTravelTimeMeanSim = board.getMeanPedTravelTimeSim();
		bestTravelTimeStdDevSim = board.getStdDevPedTravelTimeSim();
				
	}
	
	/*
	 * Compute Hessian using central differences.
	 */
	private double[][] computeHessian(MultivariateFunction multiVarFunc, double[] paramVec, double stepSize)
	{		
		//initialize Hessian
		double[][] hessian = new double[numParam][numParam];
		
		//first point (left) and its derivative
		double[] x1;
		double[] df1;
		
		//second point (right) and its derivative
		double[] x2;
		double[] df2;
		
		//for each row of Hessian
		for (int i=0; i<numParam; i++)
		{	
					
			//derivative at first point (left)
			x1 = paramVec.clone();
			
			//ensure that parameter value remains positive
			while (x1[i] - stepSize <= 0.0) {
				stepSize = 0.5*stepSize;
			}
			x1[i] = x1[i] - stepSize;
			df1 = computeGrad(multiVarFunc, x1, stepSize);
			
			//derivative at second point (right)
			x2 = paramVec.clone();
			x2[i] = x2[i] + stepSize;
			df2 = computeGrad(multiVarFunc, x2, stepSize);
			
			//differentiate between the two derivatives and assign as row i to Hessian
			for (int j=0; j<numParam; j++){
				hessian[i][j] = (df2[j] - df1[j])/(2.0*stepSize);
			}
		}
		
		return hessian;
	}

	
	/*
	 * Compute gradient using central differences
	 */
	private double [] computeGrad(MultivariateFunction multiVarFunc, double[] paramVec, double stepSize)
	{		
		double[] df = new double[numParam];
		
		//left parameter point and corresponding function value
		double[] x1;
		double y1;
		
		//right parameter point and corresponding function value
		double[] x2;
		double y2;

		// for each dimension of objective function
		for (int i=0; i<numParam; i++)
		{	
			x1 = paramVec.clone();
			x2 = paramVec.clone();
			
			//vary variables by step size (left and right)
			//ensure that parameter value remains positive
			while (x1[i] - stepSize <= 0.0) {
				stepSize = 0.5*stepSize;
			}
			x1[i] = x1[i] - stepSize;
			x2[i] = x2[i] + stepSize;
			
			//evaluate objective function at the left and right points
			y1 = multiVarFunc.value(x1);
			y2 = multiVarFunc.value(x2);
			
			//calculate the slope for dimension i
			df[i] = (y2 - y1) / (2.0 * stepSize);
		}
		
		return df;
	}
	
	private String getFileNameAppendix() {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH)+1;
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		
		return "-" + year + "-" + month + "-" + day + "_" + numIter + ".txt";
	}

	
	
	/*
	 * Methods for cross-validation
	 */
	
	public void addPed(Hashtable<Integer,Pedestrian> pedListTarget,
			Hashtable<Integer,Pedestrian> pedListSource) {
		
		
		double timeShift = getFirstDepTime(pedListTarget);
		
		Pedestrian sourcePed;
		Pedestrian newPed;
		
		for (int i=0; i<pedListSource.size(); i++) {
			sourcePed = pedListSource.get(i);
			
			newPed = new Pedestrian(sourcePed.getRouteName(),
					sourcePed.getDepTime() + timeShift, sourcePed.getTravelTime());
			
			pedListTarget.put(pedListTarget.size(), newPed);
		}
		
	}
	
	private double getFirstDepTime(Hashtable<Integer,Pedestrian> pedList) {
		double minTimeDist = 30;
		
		if ( pedList.isEmpty() ) {
			return 0.0;
		} else {
			return pedList.get( pedList.size() - 1 ).getDepTime() + minTimeDist;
		}
	}
	
	/*
	 * Cross-Validation
	 */
	
	public void crossValidateMulti(ArrayList<String> disAggDemTables) {
		
		//sets of experiments, each represented by a disaggregate demand table
		Hashtable<Integer, Hashtable<Integer,Pedestrian>> expSets =
				new Hashtable<Integer, Hashtable<Integer,Pedestrian>>();
		
		//pedLists combining all pedestrians for complete calibration
		Hashtable<Integer,Pedestrian> pedListAll = new Hashtable<Integer,Pedestrian>();
				
		Hashtable<Integer,Pedestrian> curExp;
		
		//generate pedList from each demand table
		for (int i=0; i<disAggDemTables.size(); i++) {
			curExp = board.getInput().loadDisAggDemand( disAggDemTables.get(i) , board.getRouteList() );
			expSets.put(i, curExp);
			
			//add current experiment to list of all pedestrians
			addPed(pedListAll, curExp);
		}
		
		/*
		 * calibrate on full set
		 */
		board.updateDisAggDemand(pedListAll);
		
		double[] paramCalibAll = calibMultInit();
		
		generateCalibStatistics(param.fileNameCalibStat);
		
		generateTravelTimeStat(param.fileNameTravelTimeStat);
		
		/*
		 * cross-validation
		 */
		
		//DEACTIVATED: RECALIBRATION IS TOO LOCAL.
		
		//number of experiments for calibration and for validation
//		int numExpTot = disAggDemTables.size();
//		int numExpCalib = (int) Math.round( numExpTot*0.75 ); //calibrate on 75% of data
//		//int numExpValid = numExpTot - numExpCalib; //validate on the remaining 20%
//		
//		crossValTable = new Hashtable<Integer, crossVal>(); 
//		crossVal curCrossVal;
//		
//		for (int i = 0; i<numIter; i++) {
//			curCrossVal = crossValidSample(numExpTot, numExpCalib, expSets, paramCalibAll);
//			
//			crossValTable.put(i, curCrossVal);
//			
//			//DEBUG
//			System.out.print("Cross-Valid " + (i+1) + "/" + numIter + 
//					" -- param re-calib: ");
//			for (int j=0; j<param.getNumParam(); j++) {
//				System.out.print(curCrossVal.paramCalib[j]);
//				if (j < param.getNumParam() -1) {
//					 System.out.print(", ");
//				}
//			}
//			System.out.print("\n");
//			//END DEBUG
//			
//		}
//		
//		//write statistics to file
//		String fileName = param.fileNameCrossValidStat + getFileNameAppendix();
//		
//		board.getOutput().writeCrossValidStatistics(this, param, fileName);
		
	}
	
	//stores result of cross-validation sample
	private class crossVal {
	    
		//parameters and log-likelihood of calibration sample
	    double[] paramCalib;
	    double logLikelihoodCalib;
	    
	    //observed and simulated travel times of calibration sample
	    //double[] travelTimeCalibObs;
	    //double[] travelTimeCalibSim;
	    
	    //log-likelihood of validation sample
	    double logLikelihoodValid;
	    
		//observed and simulated travel times of validation sample
	    //double[] travelTimeValidObs;
	    //double[] travelTimeValidSim;
	    
	    public crossVal(PointValuePair calibRes, double[] ttCalibObs, double[] ttCalibSim,
	    		double logLikeHoodValid, double[] ttValidObs, double[] ttValidSim) {
	    	paramCalib = calibRes.getPoint();
	    	logLikelihoodCalib = calibRes.getValue();
	    	
	    	//travelTimeCalibObs = ttCalibObs;
	    	//travelTimeCalibSim = ttCalibSim;
	    	
	    	logLikelihoodValid = logLikeHoodValid;
	    	
	    	//travelTimeValidObs = ttValidObs;
	    	//travelTimeValidSim = ttValidSim;
	    }
	}
	
	private crossVal crossValidSample(int numExpTot, int numExpCalib,
			Hashtable<Integer, Hashtable<Integer,Pedestrian>> expSets,
			double[] paramCalibAll) {
		
		/*
		 * draw experiments for calibration and validation
		 */
		
		//indices of experiments used for calibration
		ArrayList<Integer> indexExpCalib = new ArrayList<Integer>(numExpCalib);
		
		//randomly draw experiments for calibration
		for(int i=0; i<numExpCalib; i++){
			Random rand = new Random();

			int RND = rand.nextInt( numExpTot );

			while(indexExpCalib.contains(RND))
			{
				RND = rand.nextInt( numExpTot );
			}

			indexExpCalib.add(RND);
		}
		
		//pedLists for complete calibration, calib and valid
		Hashtable<Integer,Pedestrian> pedListCalib = new Hashtable<Integer,Pedestrian>();
		Hashtable<Integer,Pedestrian> pedListValid = new Hashtable<Integer,Pedestrian>();
		
		Hashtable<Integer,Pedestrian> curExp;
		
		//generate pedestrian lists for calibration and validation
		for(int i=0; i<numExpTot; i++) {
			
			curExp = expSets.get(i);
			
			//use current experiment either for calibration or validation
			if (indexExpCalib.contains(i)) {
				addPed(pedListCalib, curExp);
			} else {
				addPed(pedListValid, curExp);
			}
		}
		
		/*
		 * calibrate on sample
		 */
		board.updateDisAggDemand(pedListCalib);
		//find corresponding local optimum
		PointValuePair sampleCalibRes = bobyqaOptimizer(paramCalibAll);
		
		int numPedCalib = pedListCalib.size();
		
		double[] ttCalibObs = new double[numPedCalib];
		double[] ttCalibSim = new double[numPedCalib];
		
		for (int i=0; i<numPedCalib; i++) {
			ttCalibObs[i] = pedListCalib.get(i).getTravelTime();
			ttCalibSim[i] = pedListCalib.get(i).getMeanTravelTimeSim( board.getGroupList(), param );
		}
		
		/*
		 * validate on remaining data
		 */
		board.updateDisAggDemand(pedListValid);
		board.updateParam(sampleCalibRes.getPoint());
		board.simulate();
		
		double LogLikelihoodValid = board.getLogLikelihood();
		
		int numPedValid = pedListValid.size();
		
		double[] ttValidObs = new double[numPedValid];
		double[] ttValidSim = new double[numPedValid];
		
		for (int i=0; i<numPedValid; i++) {
			ttValidObs[i] = pedListValid.get(i).getTravelTime();
			ttValidSim[i] = pedListValid.get(i).getMeanTravelTimeSim( board.getGroupList(), param );
		}
		
		/*
		 * store results
		 */
		
		crossVal res = new crossVal(sampleCalibRes, ttCalibObs, ttCalibSim,
				LogLikelihoodValid, ttValidObs, ttValidSim);
		
		return res;
		
	}
}


