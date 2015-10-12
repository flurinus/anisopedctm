package anisopedctm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Input class
 *
 * @author Flurin Haenseler, Gael Lederrey
 *
 */

public class Input {
	private final String EOF = "ENDOFFILE";

	private String fdName = "";

	//returns an array of strings representing file lines
	//white spaces are removed
	private String[] getFileLines(File file) {
		//lines of file
		String[] fileLines = new String[Parameter.LimitLinesNumber];

		//read file lines into array of string
		try {
			//open input stream
			BufferedReader bufReader = new BufferedReader(new FileReader(file));
			try{
				String curLine; //current line
				int lineNumber = 0; //line number
	
				//read file
				while ((curLine = bufReader.readLine()) != null) {
					fileLines[lineNumber] = curLine.replaceAll("\\s","");
					lineNumber++;
				}
	
				//mark end of file
				fileLines[lineNumber] = EOF;
			} finally {
				bufReader.close();
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}

		return fileLines;
	}

	//load scenario configuration from file
	public Parameter loadScenario(File scenarioFile){
		
		//lines of file
		String[] fileLines = getFileLines(scenarioFile);

		//parameters
		String inDir = "", outDir = "", paramFile = "", linkFile = "";
		String cellFile = "", routeFile = "", funDiagName = "";
		double cflFactor = 1.0;
		boolean textOutput = false, textDebug = false, visualOut = false, showNumbers = false, showCellNames = false;
		String correspFile = "";
		String demandFormat = "";
		String demandFile = "";
		boolean writeAggTable = false;
		String paramRangeFile = "";
		String calibMode = "";
		double aggPeriodCalib = 0.0;

		//extract information from each line
		try {
			//variable containing elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			lineElements = fileLines[1].split(":");
			if (lineElements[0].equals("inputdirectory")) {
				inDir = lineElements[1];
			} else {
				throw new IllegalArgumentException("Illegal value on line 2. It should be 'input directory: "
						+ "{path/to/input/directory}'");
			}

			lineElements = fileLines[2].split(":");
			if (lineElements[0].equals("outputdirectory")) {
				outDir = lineElements[1];
			} else {
				throw new IllegalArgumentException("Illegal value on line 3. It should be 'output directory: "
						+ "{path/to/output/directory}'");
			}

			lineElements = fileLines[3].split(":");
			if (lineElements[0].equals("parameterfilename")) {
				paramFile = lineElements[1];
			} else {
				throw new IllegalArgumentException("Illegal value on line 4. It should be 'parameter file name: "
						+ "{path/to/parameter/file.txt}'");
			}

			lineElements = fileLines[4].split(":");
			if (lineElements[0].equals("linkconfigurationfilename")) {
				linkFile = lineElements[1];
			} else {
				throw new IllegalArgumentException("Illegal value on line 5. It should be 'link configuration file name: "
						+ "{path/to/link/configuration/file.txt}'");
			}

			lineElements = fileLines[5].split(":");
			if (lineElements[0].equals("cellconfigurationfilename")) {
				cellFile = lineElements[1];
			} else {
				throw new IllegalArgumentException("Illegal value on line 6. It should be 'cell configuration file name: "
						+ "{path/to/cell/configuration/file.txt}'");
			}

			lineElements = fileLines[6].split(":");
			if (lineElements[0].equals("routeconfigurationfilename")) {
				routeFile = lineElements[1];
			} else {
				throw new IllegalArgumentException("Illegal value on line 7. It should be 'route configuration file name: "
						+ "{path/to/route/configuration/file.txt}'");
			}

			lineElements = fileLines[7].split(":");
			if(lineElements[0].equals("fundamentaldiagram")) {
				funDiagName = lineElements[1];
				// Test that the name is right
				if(!funDiagName.equals("Weidmann") && !funDiagName.equals("Drake") &&
						!funDiagName.equals("SbFD") && !funDiagName.equals("Zero"))
				{
					throw new Error("The name of the FunDiag is wrong. You have the choice" +
							" between 'Weidnamm', 'Drake', 'SbFD' or 'Zero'. ");
				}

				fdName = funDiagName;
			} else {
				throw new IllegalArgumentException("Illegal value on line 8. It should be 'fundamental diagram: "
						+ "{Weidmann; Drake; SbFD; Zero}'");
			}
			
			lineElements = fileLines[8].split(":");
			if(lineElements[0].equals("CFLfactor")) {
				cflFactor = Double.valueOf(lineElements[1]);
				// Test that the name is right
				if(cflFactor <= 0.0 || cflFactor > 1.0)
				{
					throw new Error("Invalid cflFactor (" + cflFactor + "). Valid range between 0 and 1");
				}
			} else {
				throw new IllegalArgumentException("Illegal value on line 9. It should be 'CFL factor: "
						+ "{0-1.0}'");
			}

			lineElements = fileLines[11].split(":");
			if (lineElements[0].equals("textoutput")) {
				textOutput = Boolean.valueOf(lineElements[1]);
			} else {
				throw new IllegalArgumentException("Illegal value on line 12. It should be 'text output: "
						+ "{true; false}'");
			}
			
			lineElements = fileLines[12].split(":");
			if (lineElements[0].equals("debugoutput")) {
				textDebug = Boolean.valueOf(lineElements[1]);
			} else {
				throw new IllegalArgumentException("Illegal value on line 13. It should be 'debug output: "
						+ "{true; false}'");			
			}

			lineElements = fileLines[15].split(":");
			if (lineElements[0].equals("visualization")) {
				visualOut = Boolean.valueOf(lineElements[1]);
			} else {
				throw new IllegalArgumentException("Illegal value on line 16. It should be 'visualization: "
						+ "{true; false}'");			
			}

			if(visualOut == true) // Don't need to load the file if it's false
			{
				lineElements = fileLines[16].split(":");
				if (lineElements[0].equals("displaycellnames")) {
					showCellNames = Boolean.valueOf(lineElements[1]);
				} else {
					throw new IllegalArgumentException("Illegal value on line 17. It should be 'display cell names: "
							+ "{true; false}'");			
				}

				lineElements = fileLines[17].split(":");
				if (lineElements[0].equals("displaynumbers")) {
					showNumbers = Boolean.valueOf(lineElements[1]);
				} else {
					throw new IllegalArgumentException("Illegal value on line 18. It should be 'display numbers: "
							+ "{true; false}'");			
				}

				lineElements = fileLines[18].split(":");
				if (lineElements[0].equals("correspondences(visualization)filename")) {
					correspFile = lineElements[1];
				} else {
					throw new IllegalArgumentException("Illegal value on line 19. It should be 'correspondences (visualization) file name: "
							+ "path/to/correspondences/file.txt'");			
				}
			}
			
			lineElements = fileLines[21].split(":");
			if(lineElements[0].equals("demandformat")) {
				if(lineElements[1].equals("aggregate") || lineElements[1].equals("disaggregate")) {
					demandFormat = lineElements[1];
				} else {
					throw new IllegalArgumentException("The demand format is invalid. You can choose "
							+ "between aggregate and disaggregate.");
				}
			} else {
				throw new IllegalArgumentException("Illegal value on line 22. It should be 'demand format: "
							+ "{aggregate; disaggregate}'");
			}
			
			lineElements = fileLines[22].split(":");
			if(lineElements[0].equals("demandfilename")) {
				demandFile = lineElements[1];
			} else {
				throw new IllegalArgumentException("Illegal value on line 23. It should be 'demand file "
							+ "name: {path/to/demand/file.txt}'");
			}

			lineElements = fileLines[23].split(":");
			if(lineElements[0].equals("writeaggregatedtable")) {
				writeAggTable = Boolean.valueOf(lineElements[1]);
			} else {
				throw new IllegalArgumentException("Illegal value on line 24. It should be 'write aggregated "
							+ "table: {true; false}'");
			}			
			
			lineElements = fileLines[26].split(":");
			if (lineElements[0].equals("parametersearchrangefile")) {
				paramRangeFile = lineElements[1];
			} else {
				throw new IllegalArgumentException("Illegal value on line 27. It should be 'parameter search range file: "
							+ "path/to/parameter/search/range/file.txt'");
			}	
			
			lineElements = fileLines[27].split(":");
			if (lineElements[0].equals("calibrationmode")) {
				calibMode = lineElements[1];
				
				// Test that the name is right
				if(!calibMode.equals("meantraveltime")
						&& !calibMode.equals("aggregatedtraveltimes")
						&& !calibMode.equals("traveltimedistribution"))	{
					throw new IllegalArgumentException("Calibration mode is invalid. " + 
						"Choose between 'mean travel time', 'aggregated travel times', 'travel time distribution'");
				}
			} else {
				throw new IllegalArgumentException("Illegal value on line 28. It should be 'calibration mode: "
							+ "{mean travel time; aggregated travel times; travel time distribution}'");
			}	
			
			lineElements = fileLines[28].split(":");
			if (lineElements[0].equals("aggregationperiod(sec)")) {
				aggPeriodCalib = Double.valueOf(lineElements[1]);
			} else {
				throw new IllegalArgumentException("Illegal value on line 29. It should be 'aggregation period (sec): "
							+ "{0.0-Double.Infinty}'");
			}	


		}
		catch(Exception e){
			e.printStackTrace();
		}

		return new Parameter(inDir, outDir, paramFile, paramRangeFile, linkFile, cellFile, routeFile,
				funDiagName, cflFactor, textOutput, textDebug, visualOut, showNumbers, showCellNames, correspFile,
				demandFormat, demandFile, writeAggTable, calibMode, aggPeriodCalib);
	}


	//load parameters from file
	public void loadParam(Parameter param){
		//lines of file
		String[] fileLines = getFileLines(new File(param.paramFilePath));

		//parameters
		double vf = Double.NaN, mu = Double.NaN;
		Double[] shapeParam = new Double[0];

		/**
		 * Information : For every Fundamental Diagram, we will have the parameters vf (free-flow speed)
		 * and mu. But each FD have different internal parameters. They are stored in the Array called
		 * shapeParameters. Its size changes w.r.t. the FD parameters.
		 *
		 * Please change the switch conditions below if you add another FD.
		 */

		//extract information from each line
		try {
			//variable containing elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			//free flow speed
			lineElements = fileLines[1].split(",");
			if (lineElements[0].equals("vf[m/s]")) {
				vf = Double.parseDouble(lineElements[1]);
			} else {
				throw new IllegalArgumentException("Parameter vf invalid \nin file " +
						param.paramFilePath + ".");
			}

			if(fdName.equals("Weidmann"))
			{

				shapeParam = new Double[2];

				//gamma
				lineElements = fileLines[2].split(",");
				if (lineElements[0].equals("gamma[1/m^2]")) {
					shapeParam[0] = Double.parseDouble(lineElements[1]);
				} else {
					throw new IllegalArgumentException("Parameter gamma invalid");
				}

				//kj
				lineElements = fileLines[3].split(",");
				if (lineElements[0].equals("kj[1/m^2]")) {
					shapeParam[1] = Double.parseDouble(lineElements[1]);
				} else {
					throw new IllegalArgumentException("Parameter kj invalid");
				}
			}
			else if(fdName.equals("Drake")) {
				shapeParam = new Double[1];

				//thetaDrake
				lineElements = fileLines[2].split(",");
				if (lineElements[0].equals("thetaDrake[m^4]")) {
					shapeParam[0] = Double.parseDouble(lineElements[1]);
				} else {
					throw new IllegalArgumentException("Parameter thetaDrake invalid: " + lineElements[0]);
				}

			}
			else if(fdName.equals("SbFD")){

				shapeParam = new Double[2];

				//theta
				lineElements = fileLines[2].split(",");
				if (lineElements[0].equals("theta[m^4]")) {
					shapeParam[0] = Double.parseDouble(lineElements[1]);
				} else {
					throw new IllegalArgumentException("Parameter theta invalid");
				}

				//beta
				lineElements = fileLines[3].split(",");
				if (lineElements[0].equals("beta[m^2]")) {
					shapeParam[1] = Double.parseDouble(lineElements[1]);
				} else {
					throw new IllegalArgumentException("Parameter beta invalid");
				}
			}
			else if(fdName.equals("Zero")){
				shapeParam = new Double[0];
			}
			
			int index = 2 + shapeParam.length;

			//mu
			lineElements = fileLines[index].split(",");
			if (lineElements[0].equals("mu[-]")) {
				mu = Double.parseDouble(lineElements[1]);
			} else {
				throw new IllegalArgumentException("Parameter mu invalid");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		param.setFDRChParam(vf, shapeParam, mu);
	}
	
	
	//load parameter names
	public void loadParamNames(Parameter param){
		//lines of file
		String[] fileLines = getFileLines(new File(param.paramFilePath));

		//number of parameters
		int numParam = param.getNumParam();
		
		//parameter names
		String[] parNames = new String[numParam];

		//extract information from each line
		try {
			//variable containing elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			for (int i=0;i<numParam; i++) {
				lineElements = fileLines[i+1].split(","); //parameters start after header line
				parNames[i] = lineElements[0];
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		param.setParamNames(parNames);
	}
	
	//load parameters from file
	public void loadParamRange(Parameter param){
		//lines of file
		String[] fileLines = getFileLines(new File(param.paramRangeFilePath));

		//parameters
		double freeSpeedMin = Double.NaN, freeSpeedMax = Double.NaN;
		double muMin = Double.NaN, muMax = Double.NaN;
		Double[] shapeParamMin = new Double[0];
		Double[] shapeParamMax = new Double[0];

		/**
		 * Information : For every Fundamental Diagram, we will have the parameters vf (free-flow speed)
		 * and mu. But each FD have different internal parameters. They are stored in the Array called
		 * shapeParameters. Its size changes w.r.t. the FD parameters.
		 *
		 * Please change the switch conditions below if you add another FD.
		 */

		//extract information from each line
		try {
			//variable containing elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			//free flow speed
			lineElements = fileLines[1].split(",");
			if (lineElements[0].equals("vf[m/s]")) {
				freeSpeedMin = Double.parseDouble(lineElements[1]);
				freeSpeedMax = Double.parseDouble(lineElements[2]);
			} else {
				throw new IllegalArgumentException("Parameter vf invalid \nin file " +
						param.paramFilePath + ".");
			}

			if(fdName.equals("Weidmann"))
			{

				shapeParamMin = new Double[2];
				shapeParamMax = new Double[2];

				//gamma
				lineElements = fileLines[2].split(",");
				if (lineElements[0].equals("gamma[1/m^2]")) {
					shapeParamMin[0] = Double.parseDouble(lineElements[1]);
					shapeParamMax[0] = Double.parseDouble(lineElements[2]);
				} else {
					throw new IllegalArgumentException("Parameter gamma invalid");
				}

				//kj
				lineElements = fileLines[3].split(",");
				if (lineElements[0].equals("kj[1/m^2]")) {
					shapeParamMin[1] = Double.parseDouble(lineElements[1]);
					shapeParamMax[1] = Double.parseDouble(lineElements[2]);
				} else {
					throw new IllegalArgumentException("Parameter kj invalid");
				}
			}
			else if(fdName.equals("Drake")) {
				shapeParamMin = new Double[1];
				shapeParamMax = new Double[1];

				//thetaDrake
				lineElements = fileLines[2].split(",");
				if (lineElements[0].equals("thetaDrake[m^4]")) {
					shapeParamMin[0] = Double.parseDouble(lineElements[1]);
					shapeParamMax[0] = Double.parseDouble(lineElements[2]);
				} else {
					throw new IllegalArgumentException("Parameter thetaDrake invalid");
				}

			}
			else if(fdName.equals("SbFD")) {

				shapeParamMin = new Double[2];
				shapeParamMax = new Double[2];

				//theta
				lineElements = fileLines[2].split(",");
				if (lineElements[0].equals("theta[m^4]")) {
					shapeParamMin[0] = Double.parseDouble(lineElements[1]);
					shapeParamMax[0] = Double.parseDouble(lineElements[2]);
				} else {
					throw new IllegalArgumentException("Parameter theta invalid");
				}

				//beta
				lineElements = fileLines[3].split(",");
				if (lineElements[0].equals("beta[m^2]")) {
					shapeParamMin[1] = Double.parseDouble(lineElements[1]);
					shapeParamMax[1] = Double.parseDouble(lineElements[2]);
				} else {
					throw new IllegalArgumentException("Parameter beta invalid");
				}
			}
			else if(fdName.equals("Zero")) {
				shapeParamMin = new Double[0];
				shapeParamMax = new Double[0];
			}

			int index = 2 + shapeParamMin.length;

			//mu
			lineElements = fileLines[index].split(",");
			if (lineElements[0].equals("mu[-]")) {
				muMin = Double.parseDouble(lineElements[1]);
				muMax = Double.parseDouble(lineElements[2]);
			} else {
				throw new IllegalArgumentException("Parameter mu invalid");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		param.setCalibSearchRange(freeSpeedMin, freeSpeedMax,
				shapeParamMin, shapeParamMax, muMin, muMax);
	}


	//generate cell list from cell layout file
	public Hashtable<String, Cell> loadCells(HashSet<String> zoneList, Parameter param) {
		File cellFile = new File(param.cellFilePath);

		//list of cells
		Hashtable<String, Cell> cellList = new Hashtable<String, Cell>();

		//lines of file
		String[] fileLines = getFileLines(cellFile);

		Cell curCell; //current cell

		String cellName; //name of cell
		String zoneName; //name of zone
		double areaSize; //size of cell area
		float[] coord = new float[8]; //coordinates of cell corners

		int lineNr = 1; //line number, starting on second line

		//extract information from each line
		try {
			//elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			//array of coordinates
			String[] cornerCoord = new String[8];

			while (!fileLines[lineNr].equals(EOF)) {
				//retrieve elements of current line
				lineElements = fileLines[lineNr].split(",");

				//retrieve cell and zone name and remove spaces
				cellName = lineElements[0];
				zoneName = lineElements[1];

				//add zone to zoneList
				zoneList.add(zoneName);

				//retrieve cell area
				if (lineElements[2].equals("INF")) {
					areaSize = Double.POSITIVE_INFINITY;
				} else {
					areaSize = Double.parseDouble(lineElements[2]);
				}

				//retrieve string of corner coordinates (in format (0|0) (1|0) (1|1) (0|1) for unit square)
				cornerCoord = lineElements[3].replace(")(", ",").replace("|",",").replaceAll("[()]","").split(",");

				for (int i=0; i<8; i++){
					coord[i] = Float.parseFloat(cornerCoord[i]);
				}

				//generate new cell
				curCell = new Cell(zoneName, areaSize, coord, param);

				//add cell to list of cells with cellName as key
				cellList.put(cellName, curCell);

				//increment line number
				lineNr++;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		//return list of cells
		return cellList;
	}

	//generate list of links from link layout file and cell list
	//sets also length of time interval (Delta t = Delta l_min/v_f)
	public Hashtable<Integer, Link> loadLinks(Hashtable<String, Cell> cellList,
			HashSet<Integer> sinkLinks, Parameter param) {
		//link file
		File linkFile = new File(param.linkFilePath);

		//list of links
		Hashtable<Integer, Link> linkList = new Hashtable<Integer, Link>();

		//lines of file
		String[] fileLines = getFileLines(linkFile);

		//current link and link ID
		Link curLink;
		int curLinkID;

		String containingCell; //name of containing cell
		String origCell; //name of preceding cell
		String destCell; //name of next cell

		double length; //length of link

		char origDir; //direction of origin
		char destDir; //direction of destination

		boolean bidir; //boolean for bi-directionality

		double shortestLinkLength = Double.POSITIVE_INFINITY; //length of shortest link

		int lineNr = 1; //line number, starting on second line

		//extract information from each line
		try {
			//elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			while (!fileLines[lineNr].equals(EOF)) {
				//retrieve elements of current line
				lineElements = fileLines[lineNr].split(",");

				containingCell = lineElements[0];
				origCell = lineElements[1];
				destCell = lineElements[2];

				if (lineElements[3].equals("MIN")) {
					length = Double.POSITIVE_INFINITY;
				}
				else {
					length = Double.parseDouble(lineElements[3]);
				}

				origDir = lineElements[4].charAt(0);
				destDir = lineElements[5].charAt(0);

				bidir = lineElements[6].equals("true");

				//if necessary, update shortest link length
				if (length < shortestLinkLength) {
					shortestLinkLength = length;
				}

				//check feasibility of cell names
				if (!cellList.containsKey(containingCell)) {
					throw new IllegalArgumentException("Invalid containing cell '" +
							containingCell + "' on line " + String.valueOf(lineNr));
				}
				if (! (cellList.containsKey(origCell) || origCell.equals("none") ) ) {
					throw new IllegalArgumentException("Invalid origCell '" +
							origCell + "' on line " + String.valueOf(lineNr));
				}
				if (! (cellList.containsKey(destCell) || destCell.equals("none") ) ) {
					throw new IllegalArgumentException("Invalid destCell '" +
							destCell + "' on line " + String.valueOf(lineNr));
				}

				//generate new link and linkID
				curLinkID = 2*(lineNr-1);
				curLink = new Link(containingCell, origCell, destCell, length, origDir, destDir);

				//add link to global and cell-specific link list
				linkList.put(curLinkID, curLink);
				cellList.get(containingCell).addLocalLink(curLinkID, curLink.linkOrient);

				//add link to sink link list if necessary
				if (destCell.equals("none")) {
					sinkLinks.add(curLinkID);
				}

				// Set the parameter for the CFL condition on the current link
				linkList.get(curLinkID).setCFL(param.getCFL());

				//if bi-directional edge, add reverse link accordingly
				if (bidir){
					//generate reverse link and corresponding ID
					curLinkID = 2*(lineNr-1) + 1;
					// For the reverse link, we need to swap the origDir and the destDir
					curLink = new Link(containingCell, destCell, origCell, length, destDir, origDir);

					//add reverse link to global and cell-specific link list
					linkList.put(curLinkID, curLink);
					cellList.get(containingCell).addLocalLink(curLinkID, curLink.linkOrient);

					//add link to sink link list if necessary
					if (origCell.equals("none")) {
						sinkLinks.add(curLinkID);
					}

					// Set the parameter for the cfl condition on the current link
					linkList.get(curLinkID).setCFL(param.getCFL());
				}

				//increment line number
				lineNr++;

			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		//update minimal link length, and set length of time interval Delta t
		param.setMinLinkLength(shortestLinkLength);

		//compute relative link length, update links with minimum length
		for (Link currentLink : linkList.values()) {
			//if link length previously set to infty, set it to min length
			if (Double.isInfinite(currentLink.getLength())) {
				currentLink.setLength(shortestLinkLength);
			}

			//update relative length
			currentLink.setRelLength(param);
		}

		return linkList;
	}

	//generate list of nodes from link and cell list
	public Hashtable<Integer, Node> buildNodes(Hashtable<String, Cell> cellList,
			Hashtable<Integer, Link> linkList) {

		//initialize node list to be generated
		Hashtable<Integer, Node> nodeList = new Hashtable<Integer, Node>();

		//newly generated node and corresponding ID
		Node newNode;
		int newNodeID = 0;

		//enumerate links
		Enumeration<Integer> linkKeys = linkList.keys();
		int curLinkID; //ID of current link

		//origin, containing and destination cell of curLink
		String origCell, contCell, destCell;

		int origNodeID; //ID of origin node of curLink
		int destNodeID; //ID of destination node of curLink

		//associated zones
		String zoneA;
		String zoneB;

		//iterate through links
		while(linkKeys.hasMoreElements()) {
			curLinkID = linkKeys.nextElement();

			origCell = linkList.get(curLinkID).origCellName;
			contCell = linkList.get(curLinkID).cellName;
			destCell = linkList.get(curLinkID).destCellName;

			//get IDs of origin and destination nodes if they exist, "-1" otherwise
			origNodeID = getNodeID(nodeList, origCell, contCell);
			destNodeID = getNodeID(nodeList, contCell, destCell);

			//if origin node does not exist, create it
			if (origNodeID == -1){
				//generate new node
				newNode = new Node(origCell, contCell);

				//get associated zones and add them
				//(if the zones are identical, only one is added)
				if (!origCell.equals("none")) {
					zoneA = cellList.get(origCell).zone;
					newNode.addAssociatedZone(zoneA);
				}
				if (!contCell.equals("none")) { //this should always be true
					zoneB = cellList.get(contCell).zone;
					newNode.addAssociatedZone(zoneB);
				}

				//add node to node list
				nodeList.put(newNodeID, newNode);

				//update ID of origin node
				origNodeID = newNodeID;

				//increment node ID
				newNodeID++;
			}

			//analogously for destination node: create new node if necessary
			if (destNodeID == -1){
				//generate new node
				newNode = new Node(contCell, destCell);

				//get associated zones and add them
				//(if the zones are identical, only one is added)
				if (!contCell.equals("none")){ //should always be true
					zoneA = cellList.get(contCell).zone;
					newNode.addAssociatedZone(zoneA);
				}

				if (!destCell.equals("none")) {
					zoneB = cellList.get(destCell).zone;
					newNode.addAssociatedZone(zoneB);
				}

				//add node to node list
				nodeList.put(newNodeID, newNode);

				//update ID of destination node
				destNodeID = newNodeID;

				//increment node ID
				newNodeID++;
			}

			//update origin and destination node IDs in curLink
			linkList.get(curLinkID).setOrigNode(origNodeID);
			linkList.get(curLinkID).setDestNode(destNodeID);

			//add current link as outLink to origNode
			nodeList.get(origNodeID).addOutLink(curLinkID);

			//add current link as inLink to destNode
			nodeList.get(destNodeID).addInLink(curLinkID);
		}

		return nodeList;
	}

	//get ID of node from adjacent cells if it exists, -1 otherwise
	private int getNodeID(Hashtable<Integer, Node> nodeList, String cellA, String cellB) {

		Enumeration<Integer> nodeKeys = nodeList.keys(); //enumeration of nodes
		int curNodeID; //ID of current node

		//iterate through nodes
		while(nodeKeys.hasMoreElements()) {
			curNodeID = nodeKeys.nextElement();

			//if a node adjacent to cellA and cellB exists, return its ID
			if (nodeList.get(curNodeID).adjacentToCells(cellA, cellB)) {
				return curNodeID;
			}
		}

		//if no node adjacent to cellA, cellB is found, return -1
		return -1;
	}

	//load routes from route layout file and previously generated cell, link and node lists
	public Hashtable<String, Route> loadRoutes(Hashtable<String, Cell> cellList,
			HashSet<String> zoneList, Hashtable<Integer, Link> linkList,
			Hashtable<Integer, Node> nodeList, Parameter param) {

		//route file
		File routeFile = new File(param.routeFilePath);

		Hashtable<String, Route> routeList = new Hashtable<String, Route>();

		//lines of file (without white spaces)
		String[] fileLines = getFileLines(routeFile);

		//current route and route name
		Route curRoute;
		String curRouteName;

		//zone sequence
		String[] zoneSeq; //mathematically: origZone, innerZones, destZone

		int lineNr = 1; //line number, starting on second line

		//extract information from each line
		try {
			//elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			while (!fileLines[lineNr].equals(EOF)) {
				//retrieve elements of current line
				lineElements = fileLines[lineNr].split(",");

				curRouteName = lineElements[0];

				//retrieve zone sequence
				zoneSeq = lineElements[1].split("-");

				//generate route
				curRoute = new Route(zoneSeq);

				//for each zone, check feasibility and add route nodes
				for (String zoneName : zoneSeq){
					//check feasibility of zone
					if (!zoneList.contains(zoneName)) {
						throw new IllegalArgumentException("Invalid zone '" +
								zoneName + "' on route " + curRouteName);
					}

					//iterate over all nodes to find those associated with current zone
					//note: this approach is inefficient, but the resulting time loss irrelevant
					Enumeration<Integer> nodeKeys = nodeList.keys();
					int curNodeID;

					while(nodeKeys.hasMoreElements()) {
						curNodeID = nodeKeys.nextElement();
						//if current node is associated with zone, add it to route nodes
						if (nodeList.get(curNodeID).associatedWithZone(zoneName)) {
							curRoute.addRouteNode(curNodeID);
						}
					}
				}

				//iterate overs link to find origin and destination link and corresponding nodes
				//note: this approach is inefficient, but the resulting time loss irrelevant
				Enumeration<Integer> linkKeys = linkList.keys();
				int curLinkID;
				Link curLink;

				//set source and sink links, and corresponding origin and destination nodes
				while(linkKeys.hasMoreElements()) {
					curLinkID = linkKeys.nextElement();
					curLink = linkList.get(curLinkID);

					//retrieve source link and source node
					if (cellList.get(curLink.cellName).zone.equals(curRoute.origZone) & curLink.origCellName.equals("none")) {
						//if curLink is in origin zone, and origin of that link is "none", it is the source link
						curRoute.setSourceLinkID(curLinkID);

						//the origin node is retrieved from the link
						curRoute.setOrigNodeID(linkList.get(curLinkID).getOrigNode());
					}
					//retrieve sink link and destination node (analogous)
					else if (cellList.get(curLink.cellName).zone.equals(curRoute.destZone) & curLink.destCellName.equals("none")) {
						curRoute.setSinkLinkID(curLinkID);
						curRoute.setDestNodeID(linkList.get(curLinkID).getDestNode());
					}
				}

				//check if a route with the current name already exists (infeasible)
				if (routeList.containsKey(curRouteName)) {
					throw new IllegalArgumentException("Two or more routes with identical name '" +
							curRouteName + "' exist; conflicting specification on line " + String.valueOf(lineNr));
				}

				//add route
				routeList.put(curRouteName, curRoute);

				//increment line number
				lineNr++;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		return routeList;
	}

	
	//load disaggregate OD demand table and generate pedestrian list
	public Hashtable<Integer, Pedestrian> loadDisAggDemand(String fileName,
			Hashtable<String, Route> routeList) {

		Hashtable<Integer, Pedestrian> pedList = new Hashtable<Integer, Pedestrian>();
		
		//lines of file
		String[] fileLines = getFileLines(new File( fileName ));

		try {

			String[] lineElements = new String[Parameter.LimitLineLength];

			int lineNr = 1; //Line Number, starting on second line
			
			String routeName; //route name
			double depTime, travelTime; //observed departure time and travel time
			
			Pedestrian curPed;

			// First, we store all the information in arrays
			while (!fileLines[lineNr].equals(EOF)) {

				lineElements = fileLines[lineNr].split(",");
				
				routeName = lineElements[0];

				if (!routeList.containsKey(lineElements[0])) {
					throw new Error("Invalid route. Route " + routeName + " not contained in routeList.");
				}

				depTime = Double.parseDouble(lineElements[1]);
				
				travelTime = Double.parseDouble(lineElements[2]);
				
				//generate pedestrian
				curPed = new Pedestrian(routeName, depTime, travelTime);
				
				pedList.put(lineNr-1, curPed);

				lineNr++;

			}

		}
		catch(Exception e){
			e.printStackTrace();
		}

		return pedList;
	}
	
	
	//aggregate pedestrian groups and generate group list
	public Hashtable<Integer, Group> generateAggDemand(Hashtable<Integer, Pedestrian> pedList,
			Parameter param) {

		//generate empty group list
		Hashtable<Integer, Group> groupList = new Hashtable<Integer, Group>();

		// current group
		Group curGroup;
		
		//current pedestrian
		Pedestrian curPed;
		int pedDepTimeInt;
		String pedRouteName;
		boolean groupFound; //true if corresponding group already exists

		//iterate through disaggregate demand table
		for(int i=0; i<pedList.size(); i++)
		{
			//current pedestrian
			curPed = pedList.get(i);
			pedDepTimeInt = curPed.getDepTimeInt(param);
			pedRouteName = curPed.getRouteName();

			groupFound = false;
			
			//iterate through list of existing groups
			for(int j=0; j<groupList.size(); j++)
			{
				curGroup = groupList.get(j);
				
				//check if current pedestrian is associated with current group
				if( curGroup.getDepTime() == pedDepTimeInt &&
						curGroup.getRouteName().equals( pedRouteName ))
				{
					//If group exists, increment its size
					curGroup.increment();
					groupFound = true;
					break;
				}
			}

			// Otherwise, create new group with 1 pedestrian
			if(groupFound == false){
				curGroup = new Group(pedRouteName, pedDepTimeInt, 1);
				groupList.put(groupList.size(), curGroup);
			}
		}

		return groupList;
	}

	
	//load demand from file and generate group list
	public Hashtable<Integer, Group> loadAggDemand(Hashtable<String, Route> routeList,
			Parameter param) {

		File demandFile = new File(param.demandFilePath);

		//generate empty group list
		Hashtable<Integer, Group> groupList = new Hashtable<Integer, Group>();

		//lines of file (without white spaces)
		String[] fileLines = getFileLines(demandFile);

		//current route name, departure time interval, group size
		String routeName;
		int depTime;
		double numPeople;

		//current group
		Group curGroup;

		//line number, starting on second line
		//line number also used as group ID
		int lineNr = 1;

		//extract information from each line
		try {
			//elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			while (!fileLines[lineNr].equals(EOF)) {
				//retrieve elements of current line
				lineElements = fileLines[lineNr].split(",");

				routeName = lineElements[0];
				if (!routeList.containsKey(routeName)) {
					throw new Error("Invalid route. Route " + routeName + " not contained in routeList.");
				}

				depTime = Integer.parseInt(lineElements[1]);
				numPeople = Double.parseDouble(lineElements[2]);

				//check feasibility of route name
				if (!routeList.containsKey(routeName)){
					throw new IllegalArgumentException("Invalid route '" +
							routeName + "' of group " + String.valueOf(lineNr-1) +
							" on line " + String.valueOf(lineNr) );
				}

				//generate group
				curGroup = new Group(routeName, depTime, numPeople);

				//add current group to group list, using lineNr as groupID
				groupList.put(lineNr-1, curGroup);

				//increment line number
				lineNr++;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}



		return groupList;
	}

	public HashSet<Integer> setSourceSinkNodes(Hashtable<Integer, Link> linkList, HashSet<Integer> sinkLinks, Parameter param)
	{
		HashSet<Integer> sourceSinkNodes = new HashSet<Integer>();

		Enumeration<Integer> linkKeys = linkList.keys();
		int curLinkID;

		// Enumerate over all the links to find the sourceSinkNodes
		while(linkKeys.hasMoreElements()) {
			curLinkID = linkKeys.nextElement();

			if(sinkLinks.contains(curLinkID))
			{
				sourceSinkNodes.add(linkList.get(curLinkID).getDestNode());
			}
		}


		return sourceSinkNodes;

	}

	// Load the correspondences between N, S, E, W and UP, DOWN, RIGHT, LEFT (ONLY for visualization)
	public Hashtable<String, String> loadCorrespondences(Parameter param)
	{
		Hashtable<String,String> corresps = new Hashtable<String,String>();

		File correspFile = new File(param.correspFilePath);

		//lines of file (without white spaces)
		String[] fileLines = getFileLines(correspFile);

		// Tests for the different correspondences
		boolean UP=false, DOWN=false, RIGHT=false, LEFT=false;
		boolean N=false, S=false, E=false, W=false;

		//extract information from each line
		try {
			//variable containing elements of current line
			String[] lineElements = new String[Parameter.LimitLineLength];

			for(int i=1; i<=4; i++)
			{
				// correspondence
				lineElements = fileLines[i].split(",");
				if(!lineElements[0].equals("N") && !lineElements[0].equals("S")
						&& !lineElements[0].equals("E") && !lineElements[0].equals("W"))
				{
					throw new IllegalArgumentException("Problem with the line " + String.valueOf(i) +
							". The first string is equal to " + lineElements[0] + " but it should be "
							+ "equal to 'N', 'S', 'E' or 'W'.");
				}
				else
				{
					corresps.put(lineElements[0], lineElements[1]);
				}

				// Check that we have all the directions (N,S,E,W)
				if(lineElements[0].equals("N"))
				{
					N = true;
				}
				else if(lineElements[0].equals("S"))
				{
					S = true;
				}
				else if(lineElements[0].equals("E"))
				{
					E = true;
				}
				else if(lineElements[0].equals("W"))
				{
					W = true;
				}

				// Check that we have all the direction (UP, DOWN, RIGHT, LEFT)
				if(lineElements[1].equals("UP"))
				{
					UP = true;
				}
				else if(lineElements[1].equals("DOWN"))
				{
					DOWN = true;
				}
				else if(lineElements[1].equals("RIGHT"))
				{
					RIGHT = true;
				}
				else if(lineElements[1].equals("LEFT"))
				{
					LEFT = true;
				}


			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		if(corresps.size() != 4)
		{
			throw new Error("The size of the correspondences is equal to " + String.valueOf(corresps.size()) + ". It should be equal to 4.");
		}

		if(!N || !S || !E || !W)
		{
			throw new Error("One (or many) of the directions (N,S,E,W) is (are) wrong.");
		}

		if(!UP || !DOWN || !RIGHT || !LEFT)
		{
			throw new Error("One (or many) of the directions (UP,DOWN,RIGHT,LEFT) is (are) wrong.");
		}

		return corresps;

	}

}
