package anisopedctm;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Group class
 *
 * @author Flurin Haenseler, Gael Lederrey
 *
 */

public class Group {

	private final String routeName;
	private final int depTime; //departure time interval
	private double numPeople;
	private Hashtable<Integer, Double> travelTimes;  // (numTINT, numPeople)
	private double meanTTSimulated; // Simulated travel time (Travel time of each group at the end of the simulation)
	private double stdDevTTSimulated; // Simulated travel time (Travel time of each group at the end of the simulation)
	private double relLoss; // relative loss (Number of pedestrians that reached the end over numPeople)

	// constructor
	public Group(String rName, int depT, double numPeople) {
		this.routeName = rName;
		this.depTime = depT;
		this.numPeople = numPeople;
		this.travelTimes = new Hashtable<Integer, Double>();
		this.relLoss = 0.0;
	}

	public Hashtable<Integer, Double> getTravelTimes() {
		return travelTimes;
	}


	public void addTravelTime(int arrivalTime, double numP) {
		int travTime = arrivalTime - depTime - 2; //corrected travel time
		//subtract 2 time intervals for the gate cells.

		if (!(travTime >= 0 && numP > 0)) {
			System.err.println("depTime = " + depTime);
			System.err.println("TT = " + travTime + "; numP = " + numP);
			System.err.println("ERROR: Either travel time or group fraction is invalid");
		}

		//in principle, this case should never occur as there is only one destination per group
		else if (travelTimes.contains(travTime)) {
			this.travelTimes.put(travTime,this.travelTimes.get(travTime) + numP);
		}

		else {
			this.travelTimes.put(travTime, numP);
		}

	}

	// increment group size by one
	public void increment()
	{
		numPeople += 1.0;
	}

	// Function that will return a String corresponding to the demand (In order to write a new demand file in Output)
	public String demandGroup()
	{
		String demand;
		demand = routeName + ", " + String.valueOf(depTime) + ", " + String.valueOf(numPeople) + "\n";

		return demand;
	}

	// returns a string corresponding to the aggregated table (to write aggregated table in output)
	public String aggregatedTableGroup()
	{
		String aggTT;
		aggTT = routeName + ", " + String.valueOf(depTime) + ", " + String.valueOf(numPeople) + ", "
			//+ String.valueOf(meanTTObserved) + ", "
				+ String.valueOf(meanTTSimulated) + "\n";

		return aggTT;
	}

	// Return the squared error between meanTTObserved and meanTTSimulated times numPeople
//	public double getWeightedSquaredError()
//	{
//		return numPeople*Math.pow(meanTTObserved-meanTTSimulated, 2);
//	}

	// Getter and Setter functions

	public String getRouteName()
	{
		return this.routeName;
	}

	public int getDepTime()
	{
		return this.depTime;
	}

	public double getNumPeople()
	{
		return this.numPeople;
	}

	public double getMeanTTSimulated()
	{
		return this.meanTTSimulated;
	}
	
	public double getStdDevTTSimulated()
	{
		return this.stdDevTTSimulated;
	}
	
	public void computeTravelTimeStats(Parameter param) {
		
		double DeltaT = param.getDeltaT();

		//parameters of travel time distribution
		int travelTimeInt; //arrival time - travel time interval
		double travelTime; //double version of travelTimeINT
		double fragSize; //fragment size (fraction of group)

		//number of people having arrived at destination (for validation of numerics)
		double groupSizeSurvived = 0.0;
		double cumTravelTime = 0.0; //cumulative weighted travel time
		double cumTravelTimeSquared = 0.0; //cumulative weighted error of travel times

	     //enumerate travel times
	     Enumeration<Integer> travelTimeKeys = travelTimes.keys();

	     while (travelTimeKeys.hasMoreElements()) {
		    	 travelTimeInt = travelTimeKeys.nextElement();
		    	 fragSize = travelTimes.get(travelTimeInt);

		    	 travelTime = travelTimeInt*DeltaT;

		    	 //update parameters of mean travel time
		    	 cumTravelTime += fragSize*travelTime;
		    	 cumTravelTimeSquared += fragSize*Math.pow(travelTime, 2);
		    	 groupSizeSurvived += fragSize;
	     }

	     //compute mean travel time, corresponding standard deviation and relative loss
	     meanTTSimulated = cumTravelTime/groupSizeSurvived;
	     stdDevTTSimulated = Math.sqrt( cumTravelTimeSquared/groupSizeSurvived - Math.pow(meanTTSimulated, 2) );
	     relLoss = groupSizeSurvived/numPeople;
	     
	}
	
	//get probability of observing a travel time given the simulated distribution
	public double getTravTimeProb(double travTime, Parameter param) {
		int travTimeInt = (int) Math.floor(travTime/param.getDeltaT());
		
		if ( travelTimes.containsKey(travTimeInt) ) {
			
			return travelTimes.get(travTimeInt)/numPeople;
		} else {			
			//return 0.0;
			return Double.MIN_VALUE; //to avoid numerical troubles
		}
		
		
	}

	public void setRelLoss(double relLoss)
	{
		this.relLoss = relLoss;
	}

	public double getRelLoss()
	{
		return this.relLoss;
	}
}
