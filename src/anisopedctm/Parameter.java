package anisopedctm;

import java.util.Hashtable;

/**
 * Parameter class
 * 
 * @author Flurin Haenseler, Gael Lederrey
 */

public class Parameter {

	/*
	 * Contains input and derived parameters for
	 * fundamental diagram and route choice
	 */

	// numerical tolerance
	public static double absTol = 1e-6;

	public static final Hashtable<String,Integer> linkAngles = new Hashtable<String,Integer>() {/**
		 *
		 */
		private static final long serialVersionUID = 1L;

	{
		put("N->E", 315); put("N->S", 270); put("N->W", 225);
		put("E->N", 135); put("E->W", 180); put("E->S", 225);
		put("S->E", 45); put("S->N", 90); put("S->W", 135);
		put("W->S", 315); put("W->E", 0); put("W->N", 45); }};

	// parameters of fundamental diagram (a priori known)
	private Double freeSpeed; //free-flow speed

	// Shape parameters
	private Double[] shapeParam;

	// parameter of route choice model (a priori known)
	private Double mu; //weight in logit model
	
	// parameter names
	private String[] paramNames;

	// parameter for CFL conditions
	private double cflFactor;

	// derived parameters
	private Double minLinkLength; //shortest link length
	private Double deltaT; // deltaT=minLinkLength/freeSpeed

	//numerical parameters associated with FunDiag
	public static double velNdInit = 0.5; //initial guess of non-dimensional speed
	public static int maxEvaluations = 1000; //parameter of Levenberg-Marquardt and Newton-Raphson algorithm
	public static int maxIterations = 1000; //parameter of Levenberg-Marquardt algorithm
	public static double maxDensity = 5.4; //upper density bound in Brent's algorithm (1D FD CP)
	public static double Tolerance = 1e-6; //absolute and relative tolerance of various solvers

	public static double stepSizeHessian = 1e-4; //step size for computation of Hessian

	// maximum number of lines and maximum line length in layout file
	public static final int LimitLinesNumber = 20000;
	public static final int LimitLineLength = 1000;

	// maximum travel time (simulation stops MaxTravelTime after the last departure of a group)
	public static final int MaxTravelTime = 1000;

	// Name of the fundamental diagrams in use
	private final String funDiagName;

	// visualization
	public final boolean visualization;
	public final boolean displayNumbers;
	public final boolean displayCellNames;

	// write output
	public final boolean writeOutput;

	// write debug of initialization
	public final boolean writeDebug;
	
	// format of the demand
	public final String demandFormat;

	// Write the aggregated table at the end of the simulation
	public final boolean writeAggTable;

	//file paths input
	public final String inputDir;
	public final String paramFilePath;
	public final String paramRangeFilePath;
	public final String linkFilePath;
	public final String cellFilePath;
	public final String routeFilePath;
	public final String demandFilePath;
	public final String correspFilePath;
	public final String disaggTableFilePath;

	//file paths output
	public final String outputDir;
	public final String fileNameTTDist;
	public final String fileNameTTMean;
	public final String fileNameSystemState;
	public final String fileNameDebug;
	public final String fileNameAggTT;
	public final String fileNameDisAggTT;
	public final String fileNameCalibStat;
	public final String fileNameCalibFromDefaultStat;
	public final String fileNameTravelTimeStat;
	public final String fileNameCrossValidStat;
	
	//parameter for calibration
	public static int numThreads = Runtime.getRuntime().availableProcessors();
	private double[] paramLowerBound;
	private double[] paramUpperBound;
	private final String calibrationMode;
	private double aggPeriodCalib;

	// Constructor
	public Parameter(String inDir, String outDir, String paramFile, String paramRangeFile,
			String linkFile, String cellFile, String routeFile, String funDiag, double cflFact,
			boolean textOutput, boolean textDebug, boolean visualOut, boolean numbers, boolean cellNames, String correspFile,
			String demandFormat, String demandFile, boolean writeAggTable, String calibMode, double aggPerCalib) {

		outputDir = outDir;
		inputDir = inDir;

		paramFilePath = inputDir + paramFile;
		paramRangeFilePath = inputDir + paramRangeFile;
		linkFilePath = inputDir + linkFile;
		cellFilePath = inputDir + cellFile;
		routeFilePath = inputDir + routeFile;
		demandFilePath = inputDir + demandFile;
		correspFilePath = inputDir + correspFile;

		funDiagName = funDiag;
		cflFactor = cflFact;

		visualization = visualOut;
		displayNumbers = numbers;
		displayCellNames = cellNames;
		writeOutput = textOutput;
		writeDebug = textDebug;
		this.writeAggTable = writeAggTable;
		this.demandFormat = demandFormat;
		
		calibrationMode = calibMode;
		aggPeriodCalib = aggPerCalib;

		fileNameTTDist = outputDir + "travelTimeDist.txt";
		fileNameTTMean = outputDir + "travelTimeMean.txt";
		fileNameSystemState = outputDir + "systemState.txt";
		fileNameDebug = outputDir + "DebugInitialization.txt";
		fileNameDisAggTT = outputDir + "disaggODTT_autogenerated.txt";
		fileNameCalibStat = outputDir + "calibStatistics";
		fileNameTravelTimeStat = outputDir + "travelTimeStatistics";
		fileNameCalibFromDefaultStat = outputDir + "calibFromDefaultStatistics";
		fileNameCrossValidStat =  outputDir + "crossValidStatistics";
		
		
		if(demandFormat.equals("aggregated"))
		{
			fileNameAggTT = demandFilePath;
			disaggTableFilePath = "";
		} else { // demandFormat.equals("disaggregated")
			disaggTableFilePath = demandFilePath;
			if(writeAggTable == true)
			{
				fileNameAggTT = outputDir + "aggDemand.txt";
			} else {
				fileNameAggTT = "";
			}
		}

		minLinkLength = Double.NaN;
	}

	//set parameters associated with fundamental diagram and route choice model
	public void setFDRChParam(double vf, Double[] shapeParam, double mu) {

		this.shapeParam = new Double[shapeParam.length];
		this.shapeParam = shapeParam;
		
		this.freeSpeed = vf;
		
		this.mu = mu;

		// Recaculate the deltaT (Used for the Calibration)
		if(!this.minLinkLength.isNaN())
		{
			this.deltaT = cflFactor*this.minLinkLength/vf;
		}
	}
	
	public void setParamNames(String[] parNames) {
		paramNames = parNames.clone();
	}
	
	public void setCalibSearchRange(double freeSpeedMin, double freeSpeedMax,
			Double[] shapeParamMin, Double[] shapeParamMax, double muMin,
			double muMax) {
		
		// number of shape parameters
		int numShapeParam = shapeParamMin.length;
		
		//initialize lower and upper bound
		paramLowerBound = new double[2+numShapeParam];
		paramUpperBound = new double[2+numShapeParam];
		
		paramLowerBound[0] = freeSpeedMin;
		paramUpperBound[0] = freeSpeedMax;
		
		for (int i=0; i < numShapeParam; i++){
			paramLowerBound[i+1] = shapeParamMin[i];
			paramUpperBound[i+1] = shapeParamMax[i];
		}
		
		paramLowerBound[numShapeParam+1] = muMin;
		paramUpperBound[numShapeParam+1] = muMax;
	}

	/*
	 * Getters and Setters for private variables
	 */

	public double getCFL() {
		return cflFactor;
	}
	
	public double getFreeSpeed() {
		return freeSpeed;
	}

	public Double[] getShapeParam() {
		return shapeParam;
	}

	public String getParamName(int i) {
		return paramNames[i];
	}
	
	public double getMu() {
		return mu;
	}

	public double[] getParamLowerBound() {
		return paramLowerBound;
	}

	public double[] getParamUpperBound() {
		return paramUpperBound;
	}

	public String getFunDiagName() {
		return funDiagName;
	}

	public String getFileNameDemand() {
		return demandFilePath;
	}

	public String getFileNameTTDist() {
		return fileNameTTDist;
	}

	public String getFileNameTTMean() {
		return fileNameTTMean;
	}

	public String getFileNameSystemState() {
		return fileNameSystemState;
	}

	public String getFileNameDebug() {
		return fileNameDebug;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public double getMinLinkLength() {
		return minLinkLength;
	}
	
	public String getCalibMode() {
		return calibrationMode;
	}
	
	public double getAggPeriodCalib() {
		return aggPeriodCalib;
	}

	//set minimum link length and length of time interval
	public void setMinLinkLength(double d) {
		this.minLinkLength = d;
		this.deltaT = cflFactor*d/this.freeSpeed;

		if (!(this.deltaT > 0.0))
		{
			throw new IllegalArgumentException("Invalid cfl (" + cflFactor + ") or vf (" +
					+ this.freeSpeed + ") resulting in invalid deltaT (" + this.deltaT + ")");
		}
	}

	public double getDeltaT() {
		return deltaT;
	}
	
	public String getDemandFormat() {
		return demandFormat;
	}

	public String getFileNameDisaggTable() {
		return disaggTableFilePath;
	}

	public String getFileNameAggTable() {
		return fileNameAggTT;
	}
	
	public String getFileNameDisAggTable() {
		return fileNameDisAggTT;
	}

	public boolean getWriteAggTable() {
		return writeAggTable;
	}
	
	public int getNumParam() {
		return 2+shapeParam.length; //mu+vf+shape parameters
	}
	
	public double[] getRandomParameter(){
		int numParam = getNumParam();
		
		double[] paramVec = new double[numParam];
		
		for(int i=0; i<numParam; i++){
			double r = Math.random();
			paramVec[i] = r*(paramUpperBound[i] - paramLowerBound[i]) + paramLowerBound[i];
		}
		
		return paramVec;
	}

	
}
