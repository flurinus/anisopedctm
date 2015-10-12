package anisopedctm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Arrays;

/**
 * Debug class
 *
 * @author Flurin Haenseler, Gael Lederrey
 *
 */

public class Debug {

	//Write the cells in the debug file
	public void writeCells(Hashtable<String, Cell> cellList, Parameter param) {

		int nbrCells = cellList.size();

		ArrayList<String> headerCells = new ArrayList<String>();
		headerCells.add("List of the " + nbrCells + " Cells : \n");
		writeArrayToFile(headerCells, param.getFileNameDebug(), param, false);

		Enumeration<String> cellKeys = cellList.keys();
		String cellName;

		ArrayList<String> cellArray = new ArrayList<String>();

		while(cellKeys.hasMoreElements())
		{
			cellName = cellKeys.nextElement();
			cellArray.add("Cell " + cellName + " : zone = " +
					cellList.get(cellName).zone + "; Area size [m2] = " +
					cellList.get(cellName).areaSize + "\n");
		}
		cellArray.add("\n");

		writeArrayToFile(cellArray, param.getFileNameDebug(), param, true);
	}

	// Write the links in the debug file
	public void writeLinks(Hashtable<Integer, Link> linkList, Parameter param) {
		int nbrLinks = linkList.size();

		ArrayList<String> headerLinks = new ArrayList<String>();
		headerLinks.add("List of the " + nbrLinks + " Links : \n");
		writeArrayToFile(headerLinks, param.getFileNameDebug(), param, true);

		Enumeration<Integer> linkKeys = linkList.keys();
		Integer linkName;

		ArrayList<String> linkArray = new ArrayList<String>();

		while(linkKeys.hasMoreElements())
		{
			linkName = linkKeys.nextElement();
			linkArray.add(0,"Link " + linkName + " : CurrentCell = " +
					linkList.get(linkName).cellName + "; origCell = " +
					linkList.get(linkName).origCellName + "; destCell = " +
					linkList.get(linkName).destCellName + "; length = " +
					linkList.get(linkName).getLength() + "\n");
		}
		linkArray.add("\n");

		writeArrayToFile(linkArray, param.getFileNameDebug(), param, true);
	}

	// Write the nodes in the debug file
	public void writeNodes(Hashtable<Integer, Node> nodeList, Parameter param) {
		int nbrNodes = nodeList.size();

		ArrayList<String> headerNodes = new ArrayList<String>();
		headerNodes.add("List of the " + nbrNodes + " Nodes : \n");
		writeArrayToFile(headerNodes, param.getFileNameDebug(), param, true);

		Enumeration<Integer> nodeKeys = nodeList.keys();
		Integer nodeName;

		ArrayList<String> nodeArray = new ArrayList<String>();

		while(nodeKeys.hasMoreElements())
		{
			nodeName = nodeKeys.nextElement();
			nodeArray.add(0,"Node " + nodeName + " : InLinks = " +
					nodeList.get(nodeName).getInLinks() + " : OutLinks = " +
					nodeList.get(nodeName).getOutLinks() + "\n");
		}
		nodeArray.add("\n");

		writeArrayToFile(nodeArray, param.getFileNameDebug(), param, true);
	}

	//Write the routes in the debug file
	public void writeRoutes(Hashtable<String, Route> routeList, Parameter param) {

		int nbrRoutes = routeList.size();

		ArrayList<String> headerRoutes = new ArrayList<String>();
		headerRoutes.add("List of the " + nbrRoutes + " Routes : \n");
		writeArrayToFile(headerRoutes, param.getFileNameDebug(), param, true);

		Enumeration<String> routeKeys = routeList.keys();
		String routeName;

		ArrayList<String> routeArray = new ArrayList<String>();

		while(routeKeys.hasMoreElements())
		{
			routeName = routeKeys.nextElement();
			routeArray.add("Route " + routeName + " : zoneSeq = " +
					Arrays.toString(routeList.get(routeName).zoneSeq) + "; RouteNodes = " +
					routeList.get(routeName).getRouteNodes() + "\n");
		}
		routeArray.add("\n");

		writeArrayToFile(routeArray, param.getFileNameDebug(), param, true);
	}

	// Write the groups in the debug file
	public void writeGroups(Hashtable<Integer, Group> groupList, Parameter param) {

		int nbrGroups = groupList.size();

		ArrayList<String> headerGroups = new ArrayList<String>();
		headerGroups.add("List of the " + nbrGroups + " Groups : \n");
		writeArrayToFile(headerGroups, param.getFileNameDebug(), param, true);

		Enumeration<Integer> groupKeys = groupList.keys();
		Integer groupName;

		ArrayList<String> groupArray = new ArrayList<String>();

		while(groupKeys.hasMoreElements())
		{
			groupName = groupKeys.nextElement();
			groupArray.add(0,"Group " + groupName + ": depTime = " + groupList.get(groupName).getDepTime() +
					"; numPeople = " + groupList.get(groupName).getNumPeople() + "; routeName = " +
					groupList.get(groupName).getRouteName() + "\n");
		}
		groupArray.add("\n");

		writeArrayToFile(groupArray, param.getFileNameDebug(), param, true);
	}

	// Write parameters of route choice and fundamental diagram
	public void writeParameters(Parameter param) {

		ArrayList<String> headerParameters = new ArrayList<String>();
		headerParameters.add("List of parameters for route choice and fundamental diagram: \n");
		writeArrayToFile(headerParameters, param.getFileNameDebug(), param, true);

		ArrayList<String> paramArray = new ArrayList<String>();
		paramArray.add("vf: " + param.getFreeSpeed() + "\n");

		int numShapeParam = param.getShapeParam().length;

		for (int i=0; i<numShapeParam; i++)
		{
			paramArray.add("FD shape param #" + i + ": " + param.getShapeParam()[i] + "\n");
		}

		paramArray.add("mu: " + param.getMu() + "\n");
		paramArray.add("CFL: " + param.getCFL() + "\n");
		paramArray.add("deltaT: " + param.getDeltaT() + "\n");
		paramArray.add("minLinkLength: " + param.getMinLinkLength() + "\n");

		paramArray.add("\n");

		writeArrayToFile(paramArray, param.getFileNameDebug(), param, true);
	}
	
	public void printDisAggTable(Hashtable<Integer, Pedestrian> pedList) {
		Pedestrian ped;
		for (int i=0; i<pedList.size(); i++) {
			ped = pedList.get(i);
			System.out.println(ped.getRouteName() + ", " + 
					ped.getDepTime() + ", " + ped.getTravelTime());	
		}
	}


	// Function created to compress all the debug output functions
	public void writeDebug(Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList,
			Hashtable<Integer, Node> nodeList, Hashtable<String, Route> routeList,
			Hashtable<Integer, Group> groupList, Parameter param){

		// write cells
		writeCells(cellList, param);

		// write links
		writeLinks(linkList, param);

		// write nodes
		writeNodes(nodeList, param);

		// write routes
		writeRoutes(routeList, param);

		// write groups
		writeGroups(groupList, param);

		//write parameters
		writeParameters(param);
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
