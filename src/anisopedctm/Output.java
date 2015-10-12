package anisopedctm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;

/**
 * Output class
 *
 * @author Flurin Haenseler, Gael Lederrey
 *
 */

public class Output {


	// Writes travel time distribution and mean travel time for each group.
	public void writeTravelTime(Hashtable<Integer, Group> groupList, Parameter param) {

		//groupID, routeName, groupSize, depTime, travelTime, fragSize
		String curTTDist;

		//groupID, routeName, groupSize, depTime, weightedTravelTime, rel_loss
		String curTTMean;

		//travel time distribution log book
		ArrayList<String> travelTimeDist = new ArrayList<String>();

		//mean travel time log book
		ArrayList<String> travelTimeMean = new ArrayList<String>();

		//add header to travel time log books
		travelTimeDist.add("# groupID, routeName, groupSize, depTime, travelTime, fragSize \n");
		travelTimeMean.add("# groupID, routeName, groupSize, depTime, weightedTravelTime, rel_loss \n");

		//enumeration of groups
		Enumeration<Integer> groupKeys = groupList.keys();

		//current group and corresponding travel time distribution
		Group curGroup;
		Hashtable<Integer, Double> curTravelTimes;

		//key parameters of each group
		int groupID;
		String routeName;
		double groupSize;
		int depTime; //departure time interval

		double DeltaT = param.getDeltaT();

		//parameters of travel time distribution
		int travelTimeINT; //arrival time - travel time interval
		double travelTime; //double version of travelTimeINT
		double fragSize; //fragment size (fraction of group)

		//parameters of mean travel time
		double meanTravelTime; //weighted travel time
		double relLoss; //ratio of total number of people arrived and initial group size

		//iterate over groups
		while(groupKeys.hasMoreElements()) {
		     groupID = groupKeys.nextElement();
		     curGroup = groupList.get(groupID);

		     routeName = curGroup.getRouteName();
		     groupSize = curGroup.getNumPeople();
		     depTime = curGroup.getDepTime();

		     curTravelTimes = curGroup.getTravelTimes();

		     //enumerate travel times
		     Enumeration<Integer> travelTimeKeys = curTravelTimes.keys();

		     while (travelTimeKeys.hasMoreElements()) {
		    	 travelTimeINT = travelTimeKeys.nextElement();
		    	 fragSize = curTravelTimes.get(travelTimeINT);

		    	 travelTime = travelTimeINT*DeltaT;

		    	 //generate entry for travel time distribution log book
		    	 curTTDist = String.valueOf(groupID) + ", " + routeName + ", " +
		    			 String.valueOf(groupSize) + ", " + String.valueOf(depTime) + ", " +
		    			 String.valueOf(travelTime) + ", " + String.valueOf(fragSize) + "\n";

		    	 //add entry to travel time distribution log book
		    	 travelTimeDist.add(curTTDist);
		     }

		     //compute mean travel time and relative loss
		     meanTravelTime = curGroup.getMeanTTSimulated();
		     relLoss = curGroup.getRelLoss();

		     //generate entry for mean travel time log book
		     curTTMean = String.valueOf(groupID) + ", " + routeName + ", " +
	    			 String.valueOf(groupSize) + ", " + String.valueOf(depTime) + ", " +
	    			 String.valueOf(meanTravelTime) + ", " + String.valueOf(relLoss) + "\n";

		    //add entry to mean travel time log book
		     travelTimeMean.add(curTTMean);
		}

		// Invert the array of travelTimeMean
		ArrayList<String> travelTimeMean_Inverted = new ArrayList<String>();

		travelTimeMean_Inverted.add(travelTimeMean.get(0)); //keep the header

		for(int i=travelTimeMean.size()-1; i > 0; i--)
		{
			travelTimeMean_Inverted.add(travelTimeMean.get(i));
		}

		//write arrays to file
		writeArrayToFile(travelTimeDist, param.getFileNameTTDist(), param, false);
		writeArrayToFile(travelTimeMean_Inverted, param.getFileNameTTMean(), param, false);
	}

	//write system state at a given time interval
	//state variables: size of each group on each link
	public void writeSystemState(int timeInterval, Hashtable<Integer, Link> linkList,
			Parameter param) {
		//timeInterval, linkID, cellName, groupID, groupSizeOnLink
		String curSystemStateEntry;

		//system state
		ArrayList<String> systemState = new ArrayList<String>();

		//at beginning of simulation, initialize output file
		if (timeInterval == 0) {
			ArrayList<String> headerSystemState = new ArrayList<String>();
			headerSystemState.add("# timeInterval, linkID, cellName, groupID, groupSizeOnLink \n");
			writeArrayToFile(headerSystemState, param.getFileNameSystemState(), param, false);
		}

		//loop over links
		Enumeration<Integer> linkKeys = linkList.keys();
		int curLinkID;
		Link curLink;
		String curCellName; //corresponding cell name
		Hashtable<Integer, Fragment> curFragList; //fragment list on current link

		while(linkKeys.hasMoreElements()) {
			curLinkID = linkKeys.nextElement();
			curLink = linkList.get(curLinkID);
			curCellName = curLink.cellName;
			curFragList = curLink.getFragList();

			//loop over fragments on each link
			Enumeration<Integer> fragKeys = curFragList.keys();
			int groupID; //fragment key equals group ID
			double fragSize;

			while (fragKeys.hasMoreElements()) {
				groupID = fragKeys.nextElement();
				fragSize = curFragList.get(groupID).getNumPeople();

				//timeInterval, linkID, cellName, groupID, groupSizeOnLink
				curSystemStateEntry = String.valueOf(timeInterval) + ", " +
						String.valueOf(curLinkID) + ", " + curCellName + ", " +
						String.valueOf(groupID) + ", " + fragSize + "\n";

				//add entry to system state log book
				systemState.add(curSystemStateEntry);
			}
		}

		//append system state to file
		writeArrayToFile(systemState, param.getFileNameSystemState(), param, true);
	}
	
	//write calibration statistics
	public void writeCalibStatistics(Calibration calib, Parameter param,
			Hashtable<Integer, Pedestrian> pedList, String fileNameOutput) {
		//number of parameters
		int numParam = param.getNumParam();
		
		//content of output
		ArrayList<String> statContent = new ArrayList<String>();
		
		//write optimal parameters and log-likelihood
		String header = "Optimal parameters found (numCalibRuns: " + calib.getNumIter() + "):\n";
		header += "Fundamental diagram: " + param.getFunDiagName() + "\n";
		for(int i=0; i<numParam; i++) {
			header += param.getParamName(i) + ": " + calib.getBestParam()[i] + "\n"; 
		}
		
		
		header += "log-Likelihood: " + calib.getBestLogLikelihood() + "\n\n";
		statContent.add(header);
		
		//write Hessian
		header = "Hessian of LogLikelihood:\n";
		statContent.add(header);
		
		String varianceLine;
		
		for(int i=0; i<numParam; i++)
		{
			varianceLine = "";
			
			for(int j=0; j<numParam; j++)
			{
				
				varianceLine += calib.getHessianLL().getEntry(i,j);
				
				if (j < numParam-1) {
					varianceLine += ", ";
				}
				else {
					varianceLine += "\n";
				}
			}
			statContent.add(varianceLine);
		}
		
		
		if ( calib.invertHessianSuccessful() ) {
			//write eigenvalues of Hessian
			header = "\nReal eigenvalues of Hessian: ";
			boolean nonNegativeEigenvalues = false;
			
			for (int i=0; i < calib.getEigValuesHessianLL().length; i++){
				header += calib.getEigValuesHessianLL()[i];
				
				if (calib.getEigValuesHessianLL()[i] > -Parameter.absTol){
					nonNegativeEigenvalues = true;
				}
				
				if (i < calib.getEigValuesHessianLL().length - 1){
					header += ", ";
				} else {
					//comment on validity of eigenvalues of Hessian
					if (calib.hasComplexEigenvalues() | nonNegativeEigenvalues) {
						header += "\nWARNING: The Hessian is not negative definite. The model is not correctly identified. \n";
					} else {
						header += "\nHessian is negative definite. Calibration successful.";
					}
					
					if (calib.hasComplexEigenvalues()){
						 header += "Hessian contains one or several complex eigenvalues.";
					} else if (nonNegativeEigenvalues){
						header += "Hessian contains one or several non-negative eigenvalues.";
					} 
					
					header += "\n\n";
				}
			}
			
			statContent.add(header);
			
			// write covariance of parameters		
			header = "Covariance of parameters (Cramer-Rao lower bound)\n";
			statContent.add(header);
			
			for(int i=0; i<numParam; i++)
			{
				varianceLine = "";
				
				for(int j=0; j<numParam; j++)
				{
					
					varianceLine += calib.getCramerRaoLL().getEntry(i,j);
					
					if (j < numParam-1) {
						varianceLine += ", ";
					}
					else {
						varianceLine += "\n";
					}
				}
				statContent.add(varianceLine);
			}
			
			//write standard errors
			header = "\nStandard errors (square root of diagonal entries of Cramer-Rao Matrix):\n";
			
			for(int i=0; i<numParam; i++)
			{
				header += param.getParamName(i) + ": ";
				header += calib.getStandErrLL()[i];
				header += "\n";
			}
			
			statContent.add(header);
			
			
			//write correlation matrix
			header = "\nCorrelation matrix (derived from variance-covariance matrix):\n";
			statContent.add(header);
			
			String correlationLine;
			
			for(int i=0; i<numParam; i++)
			{
				correlationLine = "";
				
				for(int j=0; j<numParam; j++)
				{
					
					correlationLine += calib.getCorrMatrixLL().getEntry(i,j);
					
					if (j < numParam-1) {
						correlationLine += ", ";
					}
					else {
						correlationLine += "\n";
					}
				}
				statContent.add(correlationLine);
			}	
		}
		else {
			header = "\nWarning: Computation of eigenvalues or inversion of Hessian failed.\n";
			statContent.add(header);
		}
		
		/*
		 * Parameter analysis of top 20% of runs
		 */
		double[] runLogLikelihood = new double[calib.getNumIter()];
		PointValuePair[] calibSeries = calib.getCalibSeries();
		PointValuePair curCalib;
		
		
		for (int i=0; i<calib.getNumIter(); i++) {
			runLogLikelihood[i] = -calibSeries[i].getValue();
		}
		
		Arrays.sort(runLogLikelihood);
		
		int numTopRuns = (int) Math.round(calib.getNumIter()*0.2);
		
		if (numTopRuns >= 3) { //if enough runs to have meaningful statistics
			
			//get the threshold likelihood
			double logLikelihoodThreshold = -runLogLikelihood[numTopRuns-1];
			
			header = "\nParameter statistics of top " + numTopRuns + " runs (top 20%)\n";
			header += "paramName (optimum of calibration): mean, min, max\n";
			statContent.add(header);
			
			double[] paramMin = new double[param.getNumParam()];
			double[] paramMax = new double[param.getNumParam()];
			double[] paramMean = new double[param.getNumParam()];
			
			for (int i=0; i<param.getNumParam(); i++) {
				paramMin[i] = Double.POSITIVE_INFINITY;
				paramMax[i] = Double.NEGATIVE_INFINITY;
				paramMean[i] = 0.0;
			}
						
			for (int i=0; i<calib.getNumIter(); i++) {
				
				curCalib = calibSeries[i];
				
				if (curCalib.getValue() >= logLikelihoodThreshold) {
										
					for (int j=0; j<param.getNumParam(); j++) {
												
						if (paramMin[j] > curCalib.getPoint()[j]) {
							paramMin[j] = curCalib.getPoint()[j];
						}
						
						if (paramMax[j] < curCalib.getPoint()[j]) {
							paramMax[j] = curCalib.getPoint()[j];
						}
						
						paramMean[j] += curCalib.getPoint()[j]/numTopRuns;
					}
				}
			}
			
			String paramLine;
			
			for (int j=0; j<param.getNumParam(); j++) {
				paramLine = param.getParamName(j) + " (" + calib.getBestParam()[j] + "): ";
				paramLine += paramMean[j] + ", " + paramMin[j] + ", " + paramMax[j] + "\n";
				
				statContent.add(paramLine);
			}

		}
		
		/*
		 * List of calibration runs
		 */
		
		header = "\nCalibration runs sorted by decreasing log-likelihood\n";
		statContent.add(header);

		String calibLine;
		
		for (int i=0; i<calib.getNumIter(); i++) {
			double curLogLikelihood = - runLogLikelihood[i];
			
			for (int j=0; j<calib.getNumIter(); j++) {
				
				curCalib = calibSeries[j];
				
				if (curCalib.getValue() == curLogLikelihood) {
					
					calibLine = Double.toString( curCalib.getValue() );
					
					for (int k=0; k<param.getNumParam(); k++) {
						calibLine += ", " + curCalib.getPoint()[k];
					}
					calibLine += "\n";
					statContent.add(calibLine);
					
					break;
				}
					
			}
			
			
		}
		
		//write to file
		writeArrayToFile(statContent, fileNameOutput, param, false);
		System.out.println("Calibration results printed to: " + fileNameOutput);
	}
	
	//write travel time analysis
	public void writeTravelTimeAnalysis(Calibration calib, Parameter param, 
			Hashtable<Integer, Pedestrian> pedList, String fileNameOutput) {
		
		ArrayList<String> ttContent = new ArrayList<String>();
		
		String header;
		String travTimeLine;
		
		HashSet<String> activeRouteSet = new HashSet<String>();
		for (int pedID = 0; pedID<pedList.size(); pedID++) {
			activeRouteSet.add( pedList.get(pedID).getRouteName() );
		}
		/*
		 * Travel time per route
		 */
		header = "\nTravel time per route (routeName, numPed, travelTimeMeanObs (stDev), travelTimeMeanSim (stDev)):\n";
		ttContent.add(header);	
		
		List<Double> travelTimeObs = new ArrayList<Double>();
		List<Double> travelTimeSim = new ArrayList<Double>();
		
		double travelTimeObsMean, travelTimeObsStDev;
		double travelTimeSimMean, travelTimeSimStDev;
		
		Pedestrian curPed;
		int numPed;
		
		for (String route : activeRouteSet) {
			travelTimeObs.clear();
			travelTimeSim.clear();
			
			for (int pedID=0; pedID < pedList.size(); pedID++) {
				curPed = pedList.get(pedID);
				
				if ( route.equals( curPed.getRouteName() ) ) {
					travelTimeObs.add( curPed.getTravelTime() );
					travelTimeSim.add( calib.getBestTravelTimeMeanSim()[pedID] );
				}
			}
			
			numPed = travelTimeObs.size();
			
			//compute mean and standard deviation
			travelTimeObsMean = 0;
			travelTimeSimMean = 0;
			
			for (double travTimeObs : travelTimeObs) {
				travelTimeObsMean += travTimeObs/numPed;
			}
			
			for (double travTimeSim : travelTimeSim) {
				travelTimeSimMean += travTimeSim/numPed;
			}
			
			travelTimeObsStDev = 0;
			travelTimeSimStDev = 0;
			
			for (double travTimeObs : travelTimeObs) {
				travelTimeObsStDev += Math.pow(travTimeObs - travelTimeObsMean, 2) / numPed;
			}
			
			travelTimeObsStDev = Math.sqrt(travelTimeObsStDev);
			
			for (double travTimeSim : travelTimeSim) {
				travelTimeSimStDev += Math.pow(travTimeSim - travelTimeSimMean, 2) / numPed;
			}
			
			travelTimeSimStDev = Math.sqrt(travelTimeSimStDev);
			
			travTimeLine = route + ", " + numPed + ", " +
					travelTimeObsMean + " (" + travelTimeObsStDev +"), " +
					travelTimeSimMean + " (" + travelTimeSimStDev +")\n";
			
			ttContent.add(travTimeLine);
		}
		
		/*
		 * Disaggregate OD table for best calibration run
		 */
		header = "\nDisaggregate OD table (depTime, routeName, travelTimeObs, travelTimeMeanSim, travelTimeStdDevSim):\n";
		ttContent.add(header);
		
		String pedLine;
		//Pedestrian curPed;
		numPed = pedList.size(); 
				
		for(int i=0; i<numPed; i++)
		{
			curPed = pedList.get(i);
			
			pedLine = curPed.getDepTime() + ", " + curPed.getRouteName() + ", " +
					curPed.getTravelTime() + ", " + calib.getBestTravelTimeMeanSim()[i] + 
					", " + calib.getBestTravelTimeStdDevSim()[i] + "\n";
						
			ttContent.add(pedLine);
		}
		
		/*
		 * Travel time distribution for each route
		 */
		header = "\nTravel time distribution for each route (time interval, numPedObs, fragmentSizeEst):\n";
		header += "deltaT = " + param.getDeltaT() + "s\n";
		
		ttContent.add(header);
		
		for (String route : activeRouteSet) {
			ttContent.add( calib.getTravTimeDistEntry(route) );
		}
		
		//write to file
		writeArrayToFile(ttContent, fileNameOutput, param, false);
		System.out.println("Calibration results printed to: " + fileNameOutput);
		
	}
	
	//write cross-validation statistics
		public void writeCrossValidStatistics(Calibration calib, Parameter param,
				String fileName) {
			//number of parameters
			int numParam = param.getNumParam();
			
			//content of output
			ArrayList<String> statContent = new ArrayList<String>();
			
			String header;
			
			/*
			 * statistics of calibration on full data set
			 */
			header = "Results of calibration on full data set (after " + calib.getNumIter() + " runs):\n";
			statContent.add(header);
			
			String statLine;
			
			statLine = "log-likelihood: " + calib.getBestLogLikelihood() + "\n";
			
			for (int i=0; i<numParam; i++) {
				statLine += param.getParamName(i) + ": " + calib.getBestParam()[i] + "\n";
			}
			
			statContent.add(statLine);
			
			/*
			 * statistics of random calibration and validation samples
			 */
			header = "\nResults of cross-validation samples (numCrossValidRuns: " + calib.getNumIter() + "):\n";
			header += "llValid, llCalib";
			for (int i=0; i<numParam; i++) {
				header += ", " + param.getParamName(i);
			}
			header += "\n";
			statContent.add(header);
			
			for (int i=0; i<calib.getNumIter(); i++) {
				statLine = calib.getCrossValTableEntry(i);
				statContent.add(statLine);
			}
			
			//write to file
			writeArrayToFile(statContent, fileName, param, false);
			
			System.out.println("Results of full cross-validation printed to: " + fileName);
		}

	// This function is called when we create an aggregated table form a disaggregate table
	public void writeDemand(Hashtable<Integer, Group> groupList,Parameter param)
	{
		String fileNameDemand = param.getFileNameDemand();

		ArrayList<String> demand = new ArrayList<String> ();

		String headerDemand = "#demand: routeName, depTime, numPeople\n";

		demand.add(headerDemand);

		for(int i=0; i<groupList.size(); i++)
		{
			demand.add(groupList.get(i).demandGroup());
		}

		writeArrayToFile(demand, fileNameDemand, param, false);

	}

	// This function will print in a file the aggregated table after the simulation
	public void writeAggregatedTable(Hashtable<Integer, Group> groupList, Parameter param, double logLikelihood)
	{
		String fileNameAggregatedTable = param.getFileNameAggTable();

		ArrayList<String> aggTable = new ArrayList<String> ();

		String headerAgg_TT = "#aggregated table: routeName, depTimeInt, numPeople, travelTimeObs, travelTimeSim\n";

		aggTable.add(headerAgg_TT);

		headerAgg_TT = "#Log-likelihood = " + logLikelihood + "\n";

		aggTable.add(headerAgg_TT);

		for(int i=0; i<groupList.size(); i++)
		{
			aggTable.add(groupList.get(i).aggregatedTableGroup());
		}

		writeArrayToFile(aggTable, fileNameAggregatedTable, param, false);
	}
	
	// This function will print in a file the aggregated table after the simulation
	public void writeDisAggTable(Hashtable<Integer, Group> groupList,
			Hashtable<Integer, Pedestrian> pedList, Parameter param, double logLikelihood)
	{
		
		String fileNameDisAggTable = param.getFileNameDisAggTable();

		ArrayList<String> disAggTable = new ArrayList<String> ();

		String headerDisAggTable = "#disaggregate table: routeName, depTime, travelTimeObs, travelTimeSim\n";

		disAggTable.add(headerDisAggTable);

		headerDisAggTable = "#Log-likelihood = " + logLikelihood + "\n";

		disAggTable.add(headerDisAggTable);

		for(int i=0; i<pedList.size(); i++)
		{
			disAggTable.add(pedList.get(i).disAggTableEntry(groupList, param));
		}

		writeArrayToFile(disAggTable, fileNameDisAggTable, param, false);
	}

	//creates output directory unless existing
	private void createOutputDir(Parameter param) {
		//create output directory as specified in parameters
		try {
		File outputDir = new File(param.getOutputDir());
		outputDir.mkdirs();
		}
		//raise exception if unable to create directory
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	//generates buffered writer from path name
	private BufferedWriter bufferedWriterFromPath(String curPath, boolean appendToFile) throws IOException {
		//generate file
		File curFile = new File(curPath);

		//generate file writer
		FileWriter curFileWriter = new FileWriter(curFile, appendToFile);

		//generate buffered writer
		BufferedWriter curBufferedWriter = new BufferedWriter(curFileWriter);

		return curBufferedWriter;
	}

	//writes string array to file
	private void writeArrayToFile(ArrayList<String> arList, String filePath,
			Parameter param, boolean appendToFile) {
		try {
			//ensure output directory exists
			createOutputDir(param);

			//generate buffered writer
			BufferedWriter bufWriter = bufferedWriterFromPath(filePath, appendToFile);

			//write string array to file
			for (String lineEntry : arList) {
				bufWriter.write(lineEntry);
			}

			//close buffered writer
			bufWriter.close();
		}
		//if necessary, catch exception
		catch (Exception e) {
			e.printStackTrace();
		}
	}



}
