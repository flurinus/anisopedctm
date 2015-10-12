package anisopedctm;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Class representing a fragment of a group
 * 
 * @author Flurin Haenseler, Gael Lederrey
 * 
 */

public class Fragment {
	
	private double numPeople; //number of people associated with fragment
	private Hashtable<Integer,Double> sendCap; //sending capacity per neighbor link (linkID,numPeople)
	
	public Fragment(double numPeople) {
		this.numPeople = numPeople;
		
		this.sendCap = new Hashtable<Integer,Double>();
		//this.candTransFlow = new Hashtable<Integer,Double>();		
	}

	public double getNumPeople() {
		return numPeople;
	}
	
	public void setNumPeople(double d) {
		numPeople = d;
	}
	
	public double getSendCap(Integer linkID) {
		return sendCap.get(linkID);
	}
	
	public void setSendCap(Integer linkID, double d) {
		sendCap.put(linkID,d);
	}
	
	public void resetSendCap() {
		sendCap.clear();
	}
	
	public Enumeration<Integer> getTargetLinks() {
		return sendCap.keys();
	}
	
}
