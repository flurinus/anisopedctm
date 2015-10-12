package anisopedctm;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Computation board
 *
 * @author Flurin Haenseler, Gael Lederrey
 *
 */

public class Board {

	private Parameter param;

	private Input input;
	private Output output;
	private Debug debug;
	private Visualization visualization;

	//hashtables
	private Hashtable<String, Cell> cellList;
	private Hashtable<Integer, Link> linkList;
	private Hashtable<Integer, Node> nodeList;
	private Hashtable<String, Route> routeList;
	private Hashtable<Integer, Group> groupList; //represents demand
	
	private Hashtable<Integer, Pedestrian> pedList; //disaggregate OD table (for calibration only)

	private HashSet<String> zoneList; //for consistency check

	private HashSet<Integer> sinkLinks; //for removing pedestrians at destinations

	private HashSet<Integer> sourceSinkNodes; 	// for setting the potential to infinity in the source/sink nodes which are not the
												// the destination node of the group.

	//potential field
	private PotentialField potField;
	
	// calibration
	private Calibration calib;

	//constructor
	public Board(String pathScenario) {

		input = new Input();
		output = new Output();
		debug = new Debug();

		zoneList = new HashSet<String>();
		potField = new PotentialField();
		sinkLinks = new HashSet<Integer>();

		//load scenario
		File scenarioFile = new File(pathScenario);
		param = input.loadScenario(scenarioFile);

		//load parameters associated with fundamental diagram and route choice
		input.loadParam(param);
		
		//load parameter names
		input.loadParamNames(param);

		//load parameter range (for calibration)
		input.loadParamRange(param);

		cellList = input.loadCells(zoneList, param);

		linkList = input.loadLinks(cellList, sinkLinks, param);

		nodeList = input.buildNodes(cellList, linkList);

		routeList = input.loadRoutes(cellList, zoneList, linkList, nodeList, param);
		
		//load demand either from disaggregate or from aggregated table
		if(param.getDemandFormat().equals("disaggregate")) {
			pedList = input.loadDisAggDemand(param.getFileNameDisaggTable(), routeList);
			groupList = input.generateAggDemand(pedList, param);
			
		} else {
			groupList = input.loadAggDemand(routeList, param);
		}		

		// Define the sourceSinkNodes
		sourceSinkNodes = input.setSourceSinkNodes(linkList, sinkLinks, param);

		// We write an output to debug the initialization
		if(param.writeDebug){
			// In this function, we will write all the cells, all the links,
			// all the nodes, all the routes and all the groups.
			debug.writeDebug(cellList, linkList, nodeList, routeList, groupList, param);
		}

		if(param.visualization){
			// We use the constructor of the class Visualization
			visualization = new Visualization(cellList, groupList, param, input);

			// We need to find the adjacent cells of all the cells
			visualization.setAdjCells(cellList, nodeList, param);

			// We need to find where the adjacent cells are
			visualization.setPosAdjCells(cellList);
		}
	}

	//main simulator
	public void simulate() {

		//perform simulation
		//simulation stops maxTravelTime after the last departure
		int maxTime = getLastDeparture() + Parameter.MaxTravelTime;

		double totAcc;

		for (int timeStep = 0; timeStep <= maxTime; timeStep++) {
			//perform an iteration step
			iterate(timeStep);

			if (param.writeOutput) {
				output.writeSystemState(timeStep, linkList, param);
			}

			if (param.visualization) {
				// Draw all the pictures
				visualization.drawPictures(timeStep, cellList, linkList, param);
			}

			// We had the condition to stop if there isn't any pedestrian in the cells

			totAcc = getTotAcc(cellList);

			if(totAcc < Parameter.absTol && timeStep > getLastDeparture())
			{
				break;
			}
		}

		// update simulated travel times
		updateGroupTravelTimesSim(groupList, param);

		//output (text)
		if (param.writeOutput) {
			output.writeTravelTime(groupList, param);
			
			if(param.getWriteAggTable() == true){
				output.writeAggregatedTable(groupList, param, getLogLikelihood());
			}
			
			if (param.getDemandFormat().equals("disaggregate") && param.getWriteAggTable()) {
				output.writeDisAggTable(groupList, pedList, param, getLogLikelihood());
			}
		}

	}

	public void iterate(int timeStep) {
		//fill sources with entering groups
		this.fillSources(timeStep);

		//compute prevailing and critical speed on all links
		for (Cell curCell : cellList.values()){
			curCell.computeAccVelCritVel(linkList);
		}

		//compute node potentials for all nodes for all routes, pre-compute route choice model
		potField.computeAllNodePotentials(linkList, nodeList, routeList, sourceSinkNodes, param);

		//reset flows (sending capacities, candidate inflow, total in- and outflows)
		for (Link curLink : linkList.values()) {
			curLink.resetFlows();
		}

		//compute sending capacities
		for (Link curLink : linkList.values()) {
			//computes sending capacity hash tables for all fragments on curLink, update candidate inflow
			curLink.setSendCap(linkList, nodeList, groupList, param);
		}

		//propagate people
		for (Link curLink: linkList.values()) {
			curLink.propagate(linkList);
		}

		//empty sink links and store travel times
		this.emptySinks(timeStep);
	}

	//empty sinks and store travel times
	public void emptySinks(int timeStep) {
		//iterate through sink links
		for (int linkID : sinkLinks) {
			Link curLink = linkList.get(linkID); //current link

			int groupID; //current group ID
			double fragSize; //current fragment size

			//iterate over all active groups on current link
			Enumeration<Integer> actGroupIDs = curLink.getActiveGroups();

			while(actGroupIDs.hasMoreElements()) {
				//get groupID and corresponding group size on link
				groupID = actGroupIDs.nextElement();
			    fragSize = curLink.getFragSize(groupID);

			    //add travel time in group-specific log book
			    if(!(timeStep == 0))
			    {
			    	groupList.get(groupID).addTravelTime(timeStep, fragSize);
			    }
			  
			    //remove fragment
			    curLink.removeFrag(groupID);
			}
		}
	}

	//fill sources
	public void fillSources(int timeStep) {

		int groupID; //current groupID
		int depTime; //departure time of current group
		double numPeople; //size of current group
		String routeName; //route of current group
		int sourceLinkID; //source link of current group

		//iterate over all groups
		Enumeration<Integer> enumGroups = groupList.keys();
		while (enumGroups.hasMoreElements()) {
			//retrieve groupID and departure time of current group
			groupID = enumGroups.nextElement();
			depTime = groupList.get(groupID).getDepTime();

			//if group departs in current time step, load them
			if (timeStep == depTime) {
				//get route and source link of current group
				routeName = groupList.get(groupID).getRouteName();
				sourceLinkID = routeList.get(routeName).getSourceLinkID();

				//get group size
				numPeople = groupList.get(groupID).getNumPeople();

				//add group to source link
				linkList.get(sourceLinkID).addFrag(groupID, numPeople);

				//output time step on screen
				//timeStepLog(timeStep);
			}

		}

	}

	//return latest departure time interval
	public int getLastDeparture() {
		int lastDep = 0; //time interval of last departure

		//loop over all groups and check if there is a later departure
		for (Group group : groupList.values() ) {
			if (lastDep <= group.getDepTime()) {
				lastDep = group.getDepTime();
			}
		}

		return lastDep;
	}

	//output time step on screen
	public void timeStepLog(int timeStep) {
		//output current time step every 100 steps
		if (timeStep % 100 == 0) {
			System.out.println("Time step : tau = " + timeStep + ", ");
		}
	}

	// Function that will calculate the total number of pedestrians on all the cells
	public double getTotAcc(Hashtable<String, Cell> cellList)
	{
		double totAcc = 0.0;

		Enumeration<String> cellKeys = cellList.keys(); //enumeration of all cells

		String curCell;

		while(cellKeys.hasMoreElements()) {
			curCell = cellKeys.nextElement();

			totAcc = totAcc + cellList.get(curCell).getTotAcc();
		}

		return totAcc;
	}

	// calculate simulated travel time for all groups
	public void updateGroupTravelTimesSim(Hashtable<Integer, Group> groupList, Parameter param) {
		
		//iterate over groups and update travel time statistics
		for (int groupID : groupList.keySet()) {
			groupList.get(groupID).computeTravelTimeStats(param);
		}
	}

	//////////////////////////////////////////////////////////////////////////
	//																		//
	//			Functions that are only used for the Calibration.			//
	//																		//
	//////////////////////////////////////////////////////////////////////////

	
	// change parameters in class Parameter
	private void changeParam(double[] parameters)
	{
		double vf = parameters[0];

		Double shapeParam[] = new Double[parameters.length-2];

		for(int j=1; j<parameters.length-1; j++)
		{
			shapeParam[j-1] = parameters[j];
		}

		double mu = parameters[parameters.length-1];

		param.setFDRChParam(vf, shapeParam, mu);
	}

	// update all components based on new parameters
	public void updateParam(double[] newParam)
	{
		cellList.clear();
		linkList.clear();
		nodeList.clear();
		routeList.clear();
		groupList.clear();
		sourceSinkNodes.clear();
		
		//updates field "param" by newParam
		changeParam(newParam);

		cellList = input.loadCells(zoneList, param);

		linkList = input.loadLinks(cellList, sinkLinks, param);

		nodeList = input.buildNodes(cellList, linkList);

		routeList = input.loadRoutes(cellList, zoneList, linkList, nodeList, param);
		
		//load demand either from disaggregate or from aggregated table
		if(param.getDemandFormat().equals("disaggregate")) {
			groupList = input.generateAggDemand(pedList, param);
			
		} else {
			groupList = input.loadAggDemand(routeList, param);
		}
		
		sourceSinkNodes = input.setSourceSinkNodes(linkList, sinkLinks, param);
	}
	
	public void updateDisAggDemand(Hashtable<Integer, Pedestrian> pList) {
		//update pedestrian list
		pedList.clear();
		pedList = pList;
		
		//recompute and update groupList
		groupList = input.generateAggDemand(pedList, param);
	}

	// return logLikelihood of travel times
	public double getLogLikelihood(){

		//calibration mode: based on mean travel times (avoid if possible)
		if (param.getCalibMode().equals("meantraveltime")) {
			double squaredErr = 0.0;
			
			for (int pedID = 0; pedID < pedList.size(); pedID++) {
				squaredErr += pedList.get(pedID).getSquaredError(groupList, param);
			}
			
			double numPeople = pedList.size();
			
			return -numPeople/2.0*(1.0 + Math.log(2.0*Math.PI/numPeople*squaredErr) );
		}
		
		else if (param.getCalibMode().equals("aggregatedtraveltimes")) {
		
			class CalibGroup {
	
				public final String routeName;
				public final int aggTimeInterval; //aggregation time interval (based on departure time)
				public int groupSize;
				public double totTravelTimeObs;
				public double totTravelTimeSim;
	
				// constructor
				public CalibGroup(String rName, int aggTimeInt, double travelTimeObs, double travelTimeSim) {
					routeName = rName;
					aggTimeInterval = aggTimeInt;
					groupSize = 1;
					totTravelTimeObs = travelTimeObs;
					totTravelTimeSim = travelTimeSim;
				}
				
				public void addPedestrian(double travelTimeObs, double travelTimeSim) {
					totTravelTimeObs += travelTimeObs;
					totTravelTimeSim += travelTimeSim;
					groupSize += 1;
				}
				
				public double getMeanTravelTimeObs() {
					return totTravelTimeObs/ (double) groupSize;
				}
				
				public double getMeanTravelTimeSim() {
					return totTravelTimeSim/ (double) groupSize;
				}
			}
			
			//generate empty group list
			Hashtable<Integer, CalibGroup> calibGroupList = new Hashtable<Integer, CalibGroup>();
	
			// current group
			CalibGroup curCalibGroup;
			
			//current pedestrian
			Pedestrian curPed;
			int pedAggTimeInt; //aggregation based on departure time
			String pedRouteName;
			double pedTravelTimeObs;
			double pedTravelTimeSim;
			boolean groupFound; //true if corresponding group already exists
	
			//iterate through disaggregate demand table
			for(int i=0; i<pedList.size(); i++)
			{
				//current pedestrian
				curPed = pedList.get(i);
				pedAggTimeInt = curPed.getCalibAggTimeInt(param);
				pedRouteName = curPed.getRouteName();
				pedTravelTimeObs = curPed.getTravelTime();
				pedTravelTimeSim = curPed.getMeanTravelTimeSim(groupList, param);
	
				groupFound = false;
				
				//iterate through list of existing groups
				for(int j=0; j<calibGroupList.size(); j++)
				{
					curCalibGroup = calibGroupList.get(j);
					
					//check if current pedestrian is associated with current group
					if( curCalibGroup.aggTimeInterval == pedAggTimeInt &&
							curCalibGroup.routeName.equals( pedRouteName ))
					{
						//If group exists, increment its size					
						curCalibGroup.addPedestrian(pedTravelTimeObs, pedTravelTimeSim);
						groupFound = true;
						break;
					}
				}
	
				// Otherwise, create new group with 1 pedestrian
				if(groupFound == false){
					curCalibGroup = new CalibGroup(pedRouteName,
						pedAggTimeInt, pedTravelTimeObs, pedTravelTimeSim);
					calibGroupList.put(calibGroupList.size(), curCalibGroup);
				}
			}
	
			double squaredErr = 0.0;
			
			for (CalibGroup calibGroup : calibGroupList.values()) {
				squaredErr += calibGroup.groupSize *
						Math.pow(calibGroup.getMeanTravelTimeObs() - calibGroup.getMeanTravelTimeSim(),2);
			}
			
			double numPeople = pedList.size();
			
			return -numPeople/2.0*(1.0 + Math.log(2.0*Math.PI/numPeople*squaredErr) );
		}
		
		else if (param.getCalibMode().equals("traveltimedistribution")) {
			
			double logLikelihood = 0.0;
			
			for (int pedID = 0; pedID < pedList.size(); pedID++) {
				logLikelihood += Math.log( pedList.get(pedID).getTravTimeObsProb(groupList, param) );
			}
			
			//System.out.println("logLikelihood: " + logLikelihood);
			
			return logLikelihood;
			
		}
		
		else {
			throw new IllegalArgumentException("Invalid calibration mode");
		}		
		
	}


	//return total number of pedestrians
	public int getNumPeople(){
		return pedList.size();
	}
	
	public Parameter getParam() {
		return param;
	}
	
	public Hashtable<Integer, Pedestrian> getPedList() {
		return pedList;
	}
	
	public Hashtable<Integer, Group> getGroupList() {
		return groupList;
	}
	
	public Hashtable<String, Route> getRouteList() {
		return routeList;
	}
	
	public Input getInput() {
		return input;
	}
	
	public Output getOutput() {
		return output;
	}
	
	public double[] getPedTravelTimeObs() {
		double[] travelTimeObs = new double[getNumPeople()];
		
		for (int i=0; i<getNumPeople(); i++){
			travelTimeObs[i] = pedList.get(i).getTravelTime();
		}
		
		return travelTimeObs;
	}
	
	//simulated travel time mean
	public double[] getMeanPedTravelTimeSim() {
		double[] travelTimeMeanSim = new double[getNumPeople()];
		
		for (int i=0; i<getNumPeople(); i++){
			travelTimeMeanSim[i] = pedList.get(i).getMeanTravelTimeSim(groupList, param);
		}
		
		return travelTimeMeanSim;
	}
	
	
	//simulated travel time standard deviation
	public double[] getStdDevPedTravelTimeSim() {
		double[] travelTimeStdDevSim = new double[getNumPeople()];
		
		for (int i=0; i<getNumPeople(); i++){
			travelTimeStdDevSim[i] = pedList.get(i).getStdDevTravelTimeSim(groupList, param);
		}
		
		return travelTimeStdDevSim;
	}
	
	public void calibrate(int numRuns) {
		
		calib = new Calibration(this, numRuns);
		
		calib.calibMultInit();
				
		calib.generateCalibStatistics(param.fileNameCalibStat);
		
		calib.generateTravelTimeStat(param.fileNameTravelTimeStat);
	}

	public void calibrateDefaultParam() {
		
		calib = new Calibration(this, 1);
		
		calib.calibFromDefault();
		
		calib.generateCalibStatistics(param.fileNameCalibFromDefaultStat);
		
		calib.generateTravelTimeStat(param.fileNameTravelTimeStat);
	}
	
	public void getTravTimeStat() {
		
		//initialize calibration framework
		calib = new Calibration(this, 0);
		
		//run experiment using default parameters
		calib.runDefault();
		
		//generate travel time statistics
		calib.generateTravelTimeStat(param.fileNameTravelTimeStat);
	}

	
	public void crossValidate(ArrayList<String> disAggDemTables, 
			int numIter) {
		
		calib = new Calibration(this, numIter);
		
		calib.crossValidateMulti(disAggDemTables);		
	}
	
	
	
	
	
}
