package anisopedctm;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Cell class
 *
 * @author Flurin Haenseler, Gael Lederrey
 *
 */

public class Cell{

	public final String zone; // zone
	public final double areaSize; // surface size

	public final float[] coordinates; // coordinates for drawing

	private ArrayList<Integer> localLinkIDs; // list of linkIDs associated with cell

	//fundamental diagram (stores actual and critical link accumulations and speeds)
	public FunDiag funDiag;

	//cell accumulation (for visualization and calibration only)
	private double totAcc;

	///////////////////////////////////////////////////////////////////////////////////////

	// Only for the visualization

	// Hashtable giving the corresponding nodes to the adjacent cells of this cell
	public Hashtable<String, Integer> adjCellNodes;

	// Hashtable giving the position of the adjacent cell ('UP','DOWN','RIGHT','LEFT)
	public Hashtable<String, String> adjCellPos;

	///////////////////////////////////////////////////////////////////////////////////////

	/**
	 * constructor of the cell
	 */
	public Cell(String zoneName, double aSize, float[] coo, Parameter param) {
		zone = zoneName;
		areaSize = aSize;

		coordinates = coo.clone();

		localLinkIDs = new ArrayList<Integer>();

		if(param.getFunDiagName().equals("Weidmann")) {
			funDiag = new FunDiagWeidmann(param, areaSize);
		}
		else if(param.getFunDiagName().equals("Drake")) {
			funDiag = new FunDiagDrake(param, areaSize);
		}
		else if(param.getFunDiagName().equals("SbFD")) {
			funDiag = new FunDiagSbFD(param, areaSize);
		}
		else if(param.getFunDiagName().equals("Zero")) {
			funDiag = new FunDiagZero(param, areaSize);
		} else {
			throw new IllegalStateException("No fundamental diagram has been created.");
		}

		adjCellPos = new Hashtable<String, String>();

		adjCellNodes = new Hashtable<String, Integer>();
	}

	public double getTotAcc() {
		return totAcc;
	}

	//add link to local index, add link direction to FD
	public void addLocalLink(int linkID, String lnkName) {
		localLinkIDs.add(linkID);
		funDiag.addLinkName(lnkName);
	}

	//compute accumulation, prevailing and critical speed on all links
	public void computeAccVelCritVel(Hashtable<Integer, Link> linkList) {
		//set link and cell accumulation
		setLinkCellAcc(linkList);

		//compute and update link velocities
		setLinkVel(linkList);

		//compute and update critical link accumulation and velocities
		setCritLinkAccVel(linkList);
	}

	// Compute the link and the cell accumulation
	public void setLinkCellAcc(Hashtable<Integer,Link> linkList) {
		//re-initialize accumulation vector
		funDiag.clearLinkAcc();

		//re-initialize cell accumulation
		totAcc = 0.0;

		//add links with non-zero accumulation to linkAcc
		for (int linkID : localLinkIDs) {

			//get orientation of current link
			String linkStreamOrient = linkList.get(linkID).linkOrient;

			//set total accumulation on current link
			linkList.get(linkID).setTotAcc();

			//get total accumulation on current link
			double linkAcc = linkList.get(linkID).getTotAcc();

			//add link accumulation to corresponding link
			funDiag.addToLink(linkStreamOrient,linkAcc);

			//add link accumulation to cell accumulation
			totAcc += linkAcc;
		}
	}

	//compute and set link velocities
	//NOTE: link accumulations need be up-to-date (use setLinkCellAcc())
	public void setLinkVel(Hashtable<Integer,Link> linkList) {

		//invoke fundamental diagram
		funDiag.setLinkVel();

		//TODO: needs to be rewritten

		String lnkName; //string of current link (orientation)
		double linkVel; //link velocity associated with current link

		//for each link, set current velocity
		for (int linkID : localLinkIDs) {

			//get orientation of current link
			lnkName = linkList.get(linkID).linkOrient;

			//infer link velocity
			linkVel = funDiag.getLinkVel(lnkName);

			//store link velocity in link
			linkList.get(linkID).setVelNd(linkVel);
		}
	}

	//compute and set critical link accumulations and critical link velocities
	//NOTE: link accumulations and velocities need be up-to-date (use computeLinkVel())
	public void setCritLinkAccVel(Hashtable<Integer,Link> linkList) {

		//invoke fundamental diagram
		funDiag.setCritLinkAccVel();

		//TODO: rewrite the following

		Link curLink; //current link

		String lnkName; //string of current link (orientation)
		double linkCritAcc; //critical link accumulation
		double linkCritVel; //critical link velocity

		//for each link, set critical accumulation and velocity
		for (int linkID : localLinkIDs) {
			//get current link
			curLink = linkList.get(linkID);

			//get link name of current link
			lnkName = curLink.linkOrient;

			//get critical link accumulation
			linkCritAcc = funDiag.getCritLinkAcc(lnkName);

			//get non-dimensional critial link velocity
			linkCritVel = funDiag.getCritLinkVel(lnkName);

			//assign critical link accumulation using a homogeneous distribution across links
			curLink.setCritAcc(linkCritAcc);

			//assign non-dimensional critical link velocity
			curLink.setCritVelNd(linkCritVel);
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////

	// Only for the visualization

	public void setAdjCellNodes(Hashtable<String, Integer> adjcellnodes)
	{
		Enumeration<String> tmp = adjcellnodes.keys();

		String key;
		int value;

		while(tmp.hasMoreElements())
		{
			key = tmp.nextElement();
			value = adjcellnodes.get(key);

			adjCellNodes.put(key, value);
		}
	}


	public void setAdjCellPos(Hashtable<String, String> adjcellpos)
	{
		Enumeration<String> tmp = adjcellpos.keys();

		String key;
		String value;

		while(tmp.hasMoreElements())
		{
			key = tmp.nextElement();
			value = adjcellpos.get(key);

			adjCellPos.put(key, value);
		}
	}

	// Compute the mean value for the speed of the different (ACTIVE) links that are in the same link
	public double getStreamVel(String linkDest, Hashtable<Integer,Link> linkList)
	{
		double streamVel = 0.0;
		int nbrLinkInStream = 0;

		Link curLink; //current link

		String lnkName; //Name of the current link (using the orientation)

		HashSet<String> activeLinks = funDiag.getActLinks();

		for(int linkID : localLinkIDs)
		{
			if(linkList.get(linkID).getLinkDest().equals(linkDest))
			{
				//get current link
				curLink = linkList.get(linkID);

				//get link name of current link
				lnkName = curLink.linkOrient;

				if(activeLinks.contains(lnkName))
				{
					streamVel = (streamVel*nbrLinkInStream + funDiag.getLinkVel(lnkName))/(nbrLinkInStream+1) ;

					nbrLinkInStream = nbrLinkInStream + 1;
				}
			}
		}

		return streamVel;
	}

}
