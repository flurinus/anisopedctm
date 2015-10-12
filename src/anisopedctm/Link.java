package anisopedctm;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Link class
 *
 * @author Flurin Haenseler, Gael Lederrey
 */


public class Link {
	//name of affiliated cells, useful for building network
	public final String cellName; //name of containing cell
	public final String origCellName; //name of origin cell
	public final String destCellName; //name of destination cell

	private double length; //link length

	public final String linkOrient; //N->E, S->W, etc (link orientation)

	private int origNodeID; //ID of origin node
	private int destNodeID; //ID of destination node

	private double relLength; //relative link length (L/L_min)

	private Hashtable<Integer, Fragment> fragList; //(groupID, fragment)

	private double totAcc; //current total accumulation
	private double velNd; //prevailing speed (non-dimensional)

	private double critAcc; //accumulation at which hydrodynamic flow is maximal
	private double critVelNd; //velocity (non-dimensional) at which hydrodynamic flow is maximal

	private double candInFlow; //candidate inflow

	//for visualization
	private double totInFlow; //total inflow to link
	private double totOutFlow; //total outflow to link

	// cfl parameter
	private double cfl;

	///////////////////////////////////////////////////////////////////////////////////////

	// Only for the visualization

	private String linkOrig; // Origin of the link (N,S,E,W)
	private String linkDest; // Destination of the link (N,S,E,W)

	///////////////////////////////////////////////////////////////////////////////////////

	/**
	 * constructor of Link
	 */
	public Link(String containingCName, String origCName, String destCName, double len, char linkOrig, char linkDest) {

		cellName = containingCName;
		origCellName = origCName;
		destCellName = destCName;

		String strOrient = Character.toString(linkOrig) + "->" + Character.toString(linkDest);

		length = len;

		if (!Parameter.linkAngles.containsKey(strOrient)) {
			throw new IllegalArgumentException("Invalid orientation "
					+ strOrient);
		} else {
			linkOrient = strOrient;
		}

		this.fragList = new Hashtable<Integer,Fragment>();

		this.linkOrig = Character.toString(linkOrig);
		this.linkDest = Character.toString(linkDest);
	}

	public void setCFL(double cfl)
	{
		this.cfl = cfl;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double len) {
		length = len;
	}

	public int getOrigNode() {
		return origNodeID;
	}

	public void setOrigNode(int nodeID) {
		origNodeID = nodeID;
	}

	public int getDestNode() {
		return destNodeID;
	}

	public void setDestNode(int nodeID) {
		destNodeID = nodeID;
	}

	public double getRelLength() {
		return relLength;
	}

	public void setRelLength(Parameter param) {
		relLength = length/param.getMinLinkLength();
	}

	public Double getTotAcc() {
		return totAcc;
	}

	public Double getVelNd() {
		return velNd;
	}

	public void setVelNd(double d) {
		this.velNd = d;
	}

	public Double getRelTravTime() {
		return this.relLength/this.velNd;
	}

	public Double getCritAcc() {
		return critAcc;
	}

	public void setCritAcc(double d) {
		this.critAcc = d;
	}

	public Double getCritVelNd() {
		return critVelNd;
	}

	public void setCritVelNd(double d) {
		this.critVelNd = d;
	}

	public Double getCandInFlow() {
		return candInFlow;
	}

	public void resetCandInFlow() {
		this.candInFlow = 0;
	}

	public void addCandInFlow(double d) {
		this.candInFlow += d;
	}

	public double getTotInFlow() {
		return totInFlow;
	}

	public void addTotInFlow(double d){
		totInFlow += d;
	}

	public double getTotOutFlow() {
		return totOutFlow;
	}

	public void addTotOutFlow(double d){
		totOutFlow += d;
	}

	//total accumulation (sum of all fragments)
	public void setTotAcc() {
		double totAcc = 0.0; //total accumulation on link
		double fragSize; //size of current fragment
		int groupID; //groupID of current fragment

		//iterate over all fragments and increment total accumulation
		Enumeration<Integer> fragKeys = fragList.keys();

		while(fragKeys.hasMoreElements()) {
		    groupID = fragKeys.nextElement();
		    fragSize = fragList.get(groupID).getNumPeople();

		    totAcc += fragSize;
		}

		this.totAcc = totAcc;
	}

	//receiving capacity
	public double recCap() {
		return hydroInCap();
	}

	//hydrodynamic outflow capacity
	private double hydroOutCap() {
		if (totAcc <= critAcc) {
			return hydroFlow();
		} else {
			return critCap();
		}
	}

	//hydrodynamic inflow capacity
	private double hydroInCap() {
		if (totAcc <= critAcc) {
			return critCap();
		} else {
			return hydroFlow();
		}
	}

	//hydrodynamic flow
	private double hydroFlow() {
		return (cfl/relLength)*totAcc*velNd;
	}

	//maximal capacity (cumulative hydrodynamic flow)
	private double critCap() {
		return (cfl/relLength)*critAcc*critVelNd;
	}

	//get number of people associated with groupID
	public double getFragSize(int groupID) {
		return fragList.get(groupID).getNumPeople();
	}

	//set number of people associated with groupID; fragment required to exist
	private void setFragSize(int groupID, double numPeople) {
		fragList.get(groupID).setNumPeople(numPeople);
	}

	//add people to fragment; generate new fragment if necessary
	public void addFrag(int groupID, double numPeople) {
		if(numPeople > 0.0)
		{
			if (fragList.containsKey(groupID)) {
				double newFragSize = getFragSize(groupID) + numPeople;
				setFragSize(groupID,newFragSize);
			} else {
				fragList.put(groupID, new Fragment(numPeople));
			}
		}
	}

	//remove people from fragment
	public Boolean subFrag(int groupID, double numPeople) {

		if (!(groupID >= 0) | !(numPeople >= 0.0)) {
			throw new IllegalArgumentException("Invalid groupID (" + Integer.toString(groupID) +
					") or numPeople (" + Double.toString(numPeople) + ")");
		}

		Boolean fragmentRemoved = false;

		if (fragList.containsKey(groupID)) {
			double newFragSize = getFragSize(groupID) - numPeople;

			if (newFragSize > Parameter.absTol)
			{
				setFragSize(groupID, newFragSize);
			}
			else if (newFragSize > - Parameter.absTol)
			{
				fragList.remove(groupID);
				fragmentRemoved = true;
			}
			else
			{
				throw new IllegalArgumentException("Negative fragment size of groupID " + Integer.toString(groupID) + " on current link");
			}

		} else {
			//if groupID does not exist, throw exception
			throw new IllegalArgumentException("No fragment with groupID " + Integer.toString(groupID) + " on current link");
		}

		return fragmentRemoved;
	}

	//remove complete fragment (useful on sink links)
	public void removeFrag(int groupID) {
		fragList.remove(groupID);
	}

	//get fragment list (mainly for output)
	public Hashtable<Integer, Fragment> getFragList() {
		return fragList;
	}

	//compute candidate transition flow for given sending capacity,
	//and given candidate inflow and receiving capacity on target link
	private double candTransFlow(double curSendCap, double candInFlowTargLink, double recCapTargLink) {
		//check validity of arguments
		if ( !(curSendCap >= 0) | !(candInFlowTargLink >= 0) | !(recCapTargLink >= 0) ){
			throw new IllegalArgumentException("Invalid sendCap (" + Double.toString(curSendCap) +
					"), candInFlowTargLink (" + Double.toString(candInFlowTargLink) +
					"), or recCapTargLink ("+ Double.toString(recCapTargLink) +")");
		} else {
			//if no exception is thrown

			double curTransFlow;

			//if the candidate inflow is inferior to the receiving capacity on target link
			if (candInFlowTargLink <= recCapTargLink) {
				//allocate all sending capacity
				curTransFlow = curSendCap;

			} else {
				//otherwise apply demand-proportional supply distribution
				curTransFlow = curSendCap/candInFlowTargLink*recCapTargLink;
			}

			return curTransFlow;
		}
	}


	//propagate all fragments on current link
	//Note: sending capacities and candidate inflows need to be up-to-date
	public void propagate(Hashtable<Integer, Link> linkList){

		//iterate through fragments on current link
		Enumeration<Integer> enumGroups = fragList.keys();

		Boolean fragmentRemoved = false;

		while (enumGroups.hasMoreElements()) {

			//get groupID and corresponding fragment
			int groupID = enumGroups.nextElement();
			Fragment frag = fragList.get(groupID);

			//iterate through targetLinks of current fragment as given by sending capacities
			Enumeration<Integer> enumTargLinks = frag.getTargetLinks();

			while (enumTargLinks.hasMoreElements()){

				//get target link and corresponding sending capacity
				int targLinkID = enumTargLinks.nextElement();
				double sendCapToTargLink = frag.getSendCap(targLinkID);

				//get candidate inflow and receiving capacity of target link
				double candInFlowTargLink = linkList.get(targLinkID).getCandInFlow();
				double recCapTargLink = linkList.get(targLinkID).recCap();

				//compute actual flow (equal to candidate transition flow due to absence of cell capacity constraints)
				double actualFlow = candTransFlow(sendCapToTargLink, candInFlowTargLink, recCapTargLink);

				//subtract actual flow from current fragment and increment total outflow of current link
				fragmentRemoved = this.subFrag(groupID, actualFlow);
				this.addTotOutFlow(actualFlow);

				//add actual flow to target link and add to total inflow
				linkList.get(targLinkID).addFrag(groupID, actualFlow);
				linkList.get(targLinkID).addTotInFlow(actualFlow);

				// If the fragment has been removed, we stop the loop
				if(fragmentRemoved == true)
				{
					break;
				}
			}
		}


	}

	//reset total flows, candidate inflow and sending capacities
	public void resetFlows(){
		//reset total in and out flows
		totInFlow = 0.0;
		totOutFlow = 0.0;

		//reset candidate inflow
		candInFlow = 0.0;

		//iterate through fragments on current link
		for (Fragment frag : fragList.values() ) {
			//reset sending capacities
			frag.resetSendCap();
		}
	}

	//get route choice fraction from destination node of this link
	private double getRouteSplitFrac(String routeName, int targLinkID,
			Hashtable<Integer, Link> linkList, Hashtable<Integer, Node> nodeList, Parameter param) {
		Node node = nodeList.get(destNodeID);

		//if target link is a valid target for current route
		if (node.containsOutLink(linkList, nodeList, routeName, targLinkID))
		{
			//return corresponding split fraction
			return node.getRouteChoiceFrac(linkList, nodeList, routeName, targLinkID, param);
		} else
		{
			//otherwise return NaN
			return Double.NaN;
		}
	}

	//set sending capacities for fragment corresponding to groupID
	private void setSendCapFrag(int groupID, Hashtable<Integer, Link> linkList, Hashtable<Integer, Node> nodeList,
		Hashtable<Integer, Group> groupList, Parameter param){

		//get fragment corresponding to groupID
		Fragment frag = fragList.get(groupID);

		//route corresponding to group
		String routeName = groupList.get(groupID).getRouteName();

		//number of people in fragment
		double fragSize = frag.getNumPeople();

		//minimum of fragment size and hydrodynamic outflow capacity (with demand-proportional supply)
		double fragFlow = Math.min(fragSize, fragSize/totAcc*hydroOutCap());

		double routeSplitFrac; //container for route choice fraction
		double curSendCap; //container for current sending capacity


		//iterate over all out links
		for (int targLinkID : nodeList.get(destNodeID).getOutLinks()){
			//route choice fraction corresponding to route and target link (obtained from node)
			//if targLinkID infeasible on current route, NaN is returned
			routeSplitFrac = getRouteSplitFrac(routeName, targLinkID, linkList, nodeList, param);

			//if out link is part of current route
			if (Double.isNaN(routeSplitFrac) == false)
			{
				//compute sending capacity
				curSendCap = routeSplitFrac*fragFlow;

				//set sending capacity
				frag.setSendCap(targLinkID, curSendCap);

				//increment candidate inflow of target link
				linkList.get(targLinkID).addCandInFlow(curSendCap);
			}
		}
	}

	//set sending capacities for all fragments
	public void setSendCap(Hashtable<Integer, Link> linkList, Hashtable<Integer, Node> nodeList,
		Hashtable<Integer, Group> groupList, Parameter param){
		int groupID; //groupID of current fragment

		//iterate over fragments
		Enumeration<Integer> fragKeys = fragList.keys();
		while(fragKeys.hasMoreElements()) {
		    groupID = fragKeys.nextElement();

		    //set sending capacities corresponding to current fragment
		    setSendCapFrag(groupID, linkList, nodeList, groupList, param);
		}
	}

	//get IDs of groups currently on link
	public Enumeration<Integer> getActiveGroups() {
		return fragList.keys();
	}


	///////////////////////////////////////////////////////////////////////////////////////

	// Only for the visualization

	public String getLinkOrig()
	{
		return linkOrig;
	}

	public String getLinkDest()
	{
		return linkDest;
	}

	///////////////////////////////////////////////////////////////////////////////////////

}
