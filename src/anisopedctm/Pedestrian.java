package anisopedctm;

import java.util.Hashtable;

/**
 * Represents OD and travel time information of a 'real' pedestrian.
 * Used for calibration only.
 *
 * @author Flurin Haenseler
 */

public class Pedestrian {
	private final String routeName; //observed route
	
	private final double depTime; //observed departure time
	
	private final double travelTimeObs; //observed travel time
	
	// constructor
	public Pedestrian(String rName, double depT, double travT) {
		routeName = rName;
		
		depTime = depT;
		
		travelTimeObs = travT;
	}

	public String getRouteName() {
		return routeName;
	}

	public double getDepTime() {
		return depTime;
	}

	public int getDepTimeInt(Parameter param) {
		return (int) Math.floor(depTime / param.getDeltaT() );
	}
	
	public int getCalibAggTimeInt(Parameter param) {
		return (int) Math.floor(depTime / param.getAggPeriodCalib() );
	}

	public double getTravelTime() {
		return travelTimeObs;
	}

	//get mean travel time sim
	public double getMeanTravelTimeSim(Hashtable<Integer, Group> groupList,
			Parameter param) {
		
		Group correspGroup = getCorrespondingGroup(groupList, param);
		
		return correspGroup.getMeanTTSimulated();
	}
	
	//get standard deviation of travel time of corresponding group
	public double getStdDevTravelTimeSim(Hashtable<Integer, Group> groupList,
			Parameter param) {
		
		Group correspGroup = getCorrespondingGroup(groupList, param);
		
		return correspGroup.getStdDevTTSimulated();
	}
	
	//get probability of observing the actual travel time given the model
	public double getTravTimeObsProb(Hashtable<Integer, Group> groupList,
			Parameter param) {
		
		Group correspGroup = getCorrespondingGroup(groupList, param);
		
		return correspGroup.getTravTimeProb(travelTimeObs, param);
	}

	//get simulation group corresponding to pedestrian
	private Group getCorrespondingGroup(Hashtable<Integer, Group> groupList,
			Parameter param) {
		
		int depTimeInt = getDepTimeInt(param);
		Group curGroup;
		
		for (Integer groupID : groupList.keySet()) {
			
			curGroup = groupList.get(groupID);
			
			if ( routeName.equals( curGroup.getRouteName() ) &&
					( depTimeInt == curGroup.getDepTime() ) ) {
				return curGroup;
			}
		}
		
		//if no corresponding group found, throw exception
		throw new IllegalArgumentException("No corresponding group found " +
				"for current pedestrian (depTime = " + depTime + 
				", routeName = " + routeName + ")" );
	}
	
	
	// return squared error between observed and simulated travel time
	public double getSquaredError(Hashtable<Integer, Group> groupList, Parameter param) {
		return Math.pow(travelTimeObs - getMeanTravelTimeSim(groupList, param), 2);
	}
	
	// returns entry for disaggregate table
	public String disAggTableEntry(Hashtable<Integer, Group> groupList, Parameter param)
	{
		String disAggEntry;
		disAggEntry = routeName + ", " + String.valueOf(depTime) + ", " +
				String.valueOf(travelTimeObs) + ", " + getMeanTravelTimeSim(groupList, param) + "\n";

		return disAggEntry;
	}

}
