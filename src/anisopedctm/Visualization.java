package anisopedctm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import javax.imageio.ImageIO;

/**
 * Visualization class.
 *
 * In this class, the method to print the pictures of the cell accumulation, the cell density,
 * the copy accumulation, the copy flow and the copy speed are implements.
 *
 * P.S.: A copy is a copy of flow. The link flows are aggregated by destination node.
 *
 * @author Flurin Haenseler, Gael Lederrey
 */


public class Visualization {

	private BufferedImage image;
	private int imageWidth;
	private int imageHeight;

	// Hashtables for the position of the cells
	private Hashtable<String, Integer> cellXPosition;
	private Hashtable<String, Integer> cellYPosition;
	private Hashtable<String, Integer> cellWidth;
	private Hashtable<String, Integer> cellHeight;

	// Length of the colorbar
	int lengthColorBar;

	// Parameter to make the image bigger
	private static int factor = 100;

	// Output directory
	private String outputDir;

	// Maximum density
	private double maxDensity;

	// Maximum accumulation
	private double maxAcc;

	// Critical density and velocity
	Hashtable<String, Double> critValues;

	// Tolerance
	private static double tol;

	// Correspondences between N, S, E, W and UP, DOWN, RIGHT, LEFT
	private Hashtable<String, String> corresps;

	// Constructor
	// It will search the positions of all the cells, search the minimum cell area
	// and the total number of pedestrians in order to set the maximum density
	public Visualization(Hashtable<String, Cell> cellList,
			Hashtable<Integer, Group> groupList, Parameter param, Input input) {

		// We set the output directory
		outputDir = param.getOutputDir().concat("/Pictures/");

		// Set the tolerance
		tol = 0.01;

		// Initialize the hashtables
		cellXPosition = new Hashtable<String, Integer>();
		cellYPosition = new Hashtable<String, Integer>();
		cellWidth = new Hashtable<String, Integer>();
		cellHeight = new Hashtable<String, Integer>();
		corresps = new Hashtable<String, String>();

		Enumeration<String> cellKeys = cellList.keys(); //enumeration of all cells

		String curCell = "";

		// Maximum and Minimum coordinates (Vertical and Horizontal)
		double maxVert = -10;
		double minVert = 10;

		double maxHori = -10;
		double minHori = 10;

		double coord1, coord2;

		double minArea = 1000;

		double maxArea = 0;

		// We enumerate on all the cells so that we can get the positions of the cells
		// We will also get the size of the image

		while(cellKeys.hasMoreElements()) {
			curCell = cellKeys.nextElement();

			// Store the minimum area

			if(cellList.get(curCell).areaSize < minArea)
			{
				minArea = cellList.get(curCell).areaSize;
			}

			// Store the maximum area that is not infinity
			if(cellList.get(curCell).areaSize > maxArea && cellList.get(curCell).areaSize != Double.POSITIVE_INFINITY)
			{
				maxArea = cellList.get(curCell).areaSize;
			}

			float[] coords = cellList.get(curCell).coordinates;

			coord1 = coords[0];
			coord2 = 0;

			// Horizontal coordinates
			for(int i=0; i<coords.length; i=i+2){
				if(coords[i] > maxHori)
				{
					maxHori = coords[i];
				}
				if(coords[i] < minHori)
				{
					minHori = coords[i];
				}

				if(coords[i] != coord1)
				{
					coord2 = coords[i];
				}
			}

			// We fill the information about the horizontal coordinates of the rectangle
			if(coord1 > coord2)
			{
				cellXPosition.put(curCell, (int)Math.round(factor*coord2));
				cellWidth.put(curCell, (int)Math.round(factor*(coord1-coord2)));
			}
			else
			{
				cellXPosition.put(curCell, (int)Math.round(factor*coord1));
				cellWidth.put(curCell, (int)Math.round(factor*(coord2-coord1)));
			}

			coord1 = coords[1];
			coord2 = 0;

			// Vertical coordinates
			for(int i=1; i<coords.length; i=i+2){
				if(coords[i] > maxVert)
				{
					maxVert = coords[i];
				}
				if(coords[i] < minVert)
				{
					minVert = coords[i];
				}

				if(coords[i] != coord1)
				{
					coord2 = coords[i];
				}
			}

			// We fill the information about the vertical coordinates of the rectangle
			if(coord1 > coord2)
			{
				cellYPosition.put(curCell, (int)Math.round(factor*coord2));
				cellHeight.put(curCell, (int)Math.round(factor*(coord1-coord2)));
			}
			else
			{
				cellYPosition.put(curCell, (int)Math.round(factor*coord1));
				cellHeight.put(curCell, (int)Math.round(factor*(coord2-coord1)));
			}
		}

		// The size of the image is a little bit bigger than the size of the cells
	    this.imageHeight = (int)Math.round(factor*(maxVert-minVert)+100);
		this.imageWidth = (int)Math.round(factor*(maxHori-minHori)+100+200);

	    this.image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

	    // Length of the colorbar
	    if(this.imageHeight <=100)
	    {
	    	lengthColorBar = 0;
	    }
	    else if(this.imageHeight <= 400)
	    {
	    	lengthColorBar = this.imageHeight-100;
	    }
	    else
	    {
	    	lengthColorBar = 300;
	    }

	    // Now, we need to to an offset on cellXPosition and cellYPosition to center the diagram on the picture
	    // The offset is 50 pixels + the minimum values for the horizontal and vertical
	    // coordinates.

		cellKeys = cellList.keys(); //enumeration of all cells

		while(cellKeys.hasMoreElements()) {
			curCell = cellKeys.nextElement();

			cellXPosition.put(curCell, cellXPosition.get(curCell) + 50 - (int)Math.round(factor*minHori));
			cellYPosition.put(curCell, cellYPosition.get(curCell) + 50 - (int)Math.round(factor*minVert));
		}

		// Now, we will get the total number of pedestrians
		maxAcc = 0.0;

		Enumeration<Integer> groupKeys = groupList.keys();

		int curGroup;

		while(groupKeys.hasMoreElements()) {
			curGroup = groupKeys.nextElement();

			maxAcc = maxAcc + groupList.get(curGroup).getNumPeople();
		}

		corresps = input.loadCorrespondences(param);

		// Create the different directories for the pictures
		String outputDir = param.getOutputDir();
		String lastChar = outputDir.substring(outputDir.length() - 1);
		String addSlash = "";
		if(!lastChar.equals("/")) {
			addSlash = "/";
		}
		createOutputDir(outputDir + addSlash + "Pictures/Accumulation");
		createOutputDir(outputDir + addSlash + "Pictures/Accumulation/Cell");
		createOutputDir(outputDir + addSlash + "Pictures/Accumulation/Cell/scaled");
		createOutputDir(outputDir + addSlash + "Pictures/Accumulation/Cell/unscaled");
		createOutputDir(outputDir + addSlash + "Pictures/Accumulation/Copy");
		createOutputDir(outputDir + addSlash + "Pictures/Accumulation/Copy/scaled");
		createOutputDir(outputDir + addSlash + "Pictures/Accumulation/Copy/unscaled");

		createOutputDir(outputDir + addSlash + "Pictures/Density");
		createOutputDir(outputDir + addSlash + "Pictures/Density/Cell");
		createOutputDir(outputDir + addSlash + "Pictures/Density/Cell/scaled");
		createOutputDir(outputDir + addSlash + "Pictures/Density/Cell/unscaled");
		createOutputDir(outputDir + addSlash + "Pictures/Density/Copy");
		createOutputDir(outputDir + addSlash + "Pictures/Density/Copy/scaled");
		createOutputDir(outputDir + addSlash + "Pictures/Density/Copy/unscaled");

		createOutputDir(outputDir + addSlash + "Pictures/Speed");
		createOutputDir(outputDir + addSlash + "Pictures/Speed/Copy");
		createOutputDir(outputDir + addSlash + "Pictures/Speed/Copy/scaled_wrt_vCrit");
		createOutputDir(outputDir + addSlash + "Pictures/Speed/Copy/scaled_wrt_vf");

		createOutputDir(outputDir + addSlash + "Pictures/Flow");
		createOutputDir(outputDir + addSlash + "Pictures/Flow/Copy");
		createOutputDir(outputDir + addSlash + "Pictures/Flow/Copy/scaled");
		createOutputDir(outputDir + addSlash + "Pictures/Flow/Copy/unscaled");

		// Compute the critical values

		critValues = cellList.get(curCell).funDiag.critValues();
		critValues.put("critAcc",critValues.get("critDens")*minArea);

		maxAcc = 2*critValues.get("critDens")*maxArea;

		maxDensity = maxAcc/minArea;
	}

	// Function to fix the color of the background
	public void background(Color color) {
	    Graphics g = image.getGraphics();
	    Graphics2D g2D = (Graphics2D) g;
	    //g2D.translate(0, this.imageHeight);
	    g2D.setColor(color);
	    g2D.fillRect(0, 0, this.imageWidth, this.imageHeight);
	    g2D.dispose();
	}

	// Write the value in the middle of the cell
	public void writeCellValue(Graphics2D g2D, String cellname, double value){

		g2D.setFont(new Font(g2D.getFont().getFontName(), Font.PLAIN, 20));

		FontMetrics fm = g2D.getFontMetrics();

		/** Use this if you want the full precision for double numbers */
		//String str = Double.toString(value);

		/** Use this if you want less precision */
		String str = String.format("%.2f", value);

		int totalWidth = (fm.stringWidth(str)) + 4;

		int xPos = (int)Math.round(cellXPosition.get(cellname) + (cellWidth.get(cellname) - totalWidth)/2.0);
		int yPos = (int)Math.round(cellYPosition.get(cellname) + (cellHeight.get(cellname) - fm.getHeight())/2.0 + fm.getAscent()/4.0);

		// Before writing the value of the arrow, we draw a white rectangle. It helps to read more easily the values
		g2D.setColor(Color.WHITE);
		g2D.fillRect(xPos - 2,  this.imageHeight - yPos - fm.getHeight() + 5, totalWidth,fm.getHeight());

		// Now, we write the value
		g2D.setColor(Color.BLACK);
		g2D.drawString(str, xPos , this.imageHeight - yPos);

	}

	// Write the name of the cells on the drawing
	public void writeCellNames(Hashtable<String, Cell> cellList){
		Enumeration<String> cellKeys = cellList.keys(); //enumeration of all cells

		String curCell;

		Graphics g = image.getGraphics();

	    Graphics2D g2D = (Graphics2D) g;

	    // We set a bigger line width
	    g2D.setStroke(new BasicStroke(2));

	    // We set some better rendering for the pictures
	    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// We draw the rectangles
		while(cellKeys.hasMoreElements()) {
			curCell = cellKeys.nextElement();

			// We set the color for the name
			g2D.setColor(Color.BLACK);
			g2D.drawString(curCell,cellXPosition.get(curCell) + 5, this.imageHeight - (cellYPosition.get(curCell) + cellHeight.get(curCell) - 15)  );
		}

		g2D.dispose();
	}

	// Function to draw the lines for the cells
	public void drawCells(Color color, Hashtable<String, Cell> cellList) {
		Enumeration<String> cellKeys = cellList.keys(); //enumeration of all cells

		String curCell;

		Graphics g = image.getGraphics();

	    Graphics2D g2D = (Graphics2D) g;

	    // We set a bigger line width
	    g2D.setStroke(new BasicStroke(2));

	    // We set some better rendering for the pictures
	    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// We draw the rectangles
		while(cellKeys.hasMoreElements()) {
			curCell = cellKeys.nextElement();

			// We set the color for the lines
			g2D.setColor(color);
			g2D.drawRect(cellXPosition.get(curCell), this.imageHeight - (cellYPosition.get(curCell) + cellHeight.get(curCell)), cellWidth.get(curCell), cellHeight.get(curCell));
		}

		g2D.dispose();
	}

	// Fill the cells with the colors of the cells with their densities
	public void fillCells(Hashtable<String, Cell> cellList, String type, Parameter param) {
		Enumeration<String> cellKeys = cellList.keys(); //enumeration of all cells

		String curCell;

		Graphics g = image.getGraphics();

		Graphics2D g2D = (Graphics2D) g;

	  	// We set some better rendering for the pictures
	  	g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		double value = 0.0;

		Hashtable<String, Double> tableValues = new Hashtable<String, Double>();

		// Maximum values for the color

		double maxValue = 0.0;

		if(type == "ACCUMULATION_SCALED")
		{
			maxValue = critValues.get("critAcc");
		}
		else if(type == "ACCUMULATION_UNSCALED")
		{
			maxValue = maxAcc;
		}
		else if(type == "DENSITY_SCALED")
		{
			maxValue = critValues.get("critDens");
		}
		else if(type == "DENSITY_UNSCALED")
		{
			maxValue = maxDensity;
		}

		// We draw the rectangles
		while(cellKeys.hasMoreElements()) {
			curCell = cellKeys.nextElement();

			// Get the value for the current cell
			if(type == "ACCUMULATION_UNSCALED")
			{
				value = cellList.get(curCell).getTotAcc();
			}
			else if(type == "ACCUMULATION_SCALED")
			{
				value = cellList.get(curCell).getTotAcc()/critValues.get("critAcc");
			}
			else if(type == "DENSITY_SCALED")
			{
				value = (cellList.get(curCell).getTotAcc()/(cellList.get(curCell).areaSize))/(critValues.get("critDens"));
			}
			else if(type == "DENSITY_UNSCALED")
			{
				value = cellList.get(curCell).getTotAcc()/(cellList.get(curCell).areaSize);
			}

			// We round the value and then get its corresponding color
			if(roundToSecondDecimal(value) >= tol)
			{
				if(type == "ACCUMULATION_SCALED" || type == "DENSITY_SCALED")
				{
					g2D.setColor(getColorRedBlue(value,maxValue));
				}
				else if(type == "ACCUMULATION_UNSCALED" || type == "DENSITY_UNSCALED")
				{
					g2D.setColor(getColorGreyScale(value,maxValue));
				}

				// Fill the rectangle with the color
				g2D.fillRect(cellXPosition.get(curCell), this.imageHeight - (cellYPosition.get(curCell) + cellHeight.get(curCell)), cellWidth.get(curCell), cellHeight.get(curCell));
			}

			tableValues.put(curCell, value);
		}

		drawCells(Color.BLACK, cellList);

		// Display the values
		if(param.displayNumbers == true)
		{
			cellKeys = cellList.keys();

			while(cellKeys.hasMoreElements())
			{
				curCell = cellKeys.nextElement();

				if(roundToSecondDecimal(tableValues.get(curCell)) >= tol)
				{
					writeCellValue(g2D, curCell, tableValues.get(curCell));
				}
			}
		}


		g2D.dispose();
	}

	// Draw the arrows for the copys speed, the accumulation and the flow.
	public void drawArrows(Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, String type, Parameter param) {
		Enumeration<String> cellKeys = cellList.keys(); //enumeration of all cells
		Enumeration<Integer> linkKeys; // enumeration of all links

		String curCell, curAdjCell;

		Graphics g = image.getGraphics();

	    Graphics2D g2D = (Graphics2D) g;

	    // We set a bigger line width
	    g2D.setStroke(new BasicStroke(5));

	    // We set some better rendering for the pictures
	    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    // Initilization of the color
	    Color color;

	    // Parameter for the arrow's head size
	    int dd = 20;

	    Hashtable<String, String> adjCells = new Hashtable<String, String>();

	    int ddx, ddy, xCenter, yCenter;

	    int curNode, curLink;

	    // We create the Hashtable for the values for each direction
	    Hashtable<String, Double> directionValues;

	    // We create the temporary value
	    double tmpValue = 0.0;

	    // We get the maximum value for the color
	    double maxValue = 0.0;

	    if(type == "ACCUMULATION_SCALED")
	    {
	    	maxValue = critValues.get("critAcc");
	    }
	    else if(type == "ACCUMULATION_UNSCALED")
	    {
	    	maxValue = maxAcc;
	    }
		else if(type == "DENSITY_SCALED")
		{
			maxValue = critValues.get("critDens");
		}
		else if(type == "DENSITY_UNSCALED")
		{
			maxValue = maxDensity;
		}
	    else if(type == "FLOW_SCALED")
	    {
	    	maxValue = critValues.get("critDens");
	    }
	    else if(type == "FLOW_UNSCALED")
	    {
	    	maxValue = maxDensity;
	    }
	    else if(type == "SPEED_vf")
	    {
	    	maxValue = 1.0;
	    }
	    else if(type == "SPEED_vcrit")
	    {
	    	maxValue = param.getFreeSpeed()/critValues.get("critVel");
	    }

		// We loop on all the cells
		while(cellKeys.hasMoreElements()) {
			curCell = cellKeys.nextElement();

			// Prepare the different values for the position of the arrows
			ddx = (int)Math.round(0.5*cellWidth.get(curCell));
			ddy = (int)Math.round(0.5*cellHeight.get(curCell));

			xCenter = cellXPosition.get(curCell) + ddx;
			yCenter = cellYPosition.get(curCell) + ddy;

			adjCells = cellList.get(curCell).adjCellPos;

			Enumeration<String> adjcellKeys = adjCells.keys();

			directionValues = new Hashtable<String, Double>();

			while(adjcellKeys.hasMoreElements())
			{
				curAdjCell = adjcellKeys.nextElement();

				curNode = cellList.get(curCell).adjCellNodes.get(curAdjCell);

				// We now store the temporary value for the adjacent cell in tmpValue

				// Value for the copy accumulationn
				if(type == "ACCUMULATION_SCALED" || type == "ACCUMULATION_UNSCALED")
				{
					linkKeys = linkList.keys();
					while(linkKeys.hasMoreElements())
					{
						curLink = linkKeys.nextElement();
						if(linkList.get(curLink).getDestNode() == curNode && linkList.get(curLink).cellName.equals(curCell))
						{
							tmpValue = tmpValue + linkList.get(curLink).getTotAcc();
						}
					}

					if(type == "ACCUMULATION_SCALED")
					{
						tmpValue = tmpValue/critValues.get("critAcc");
					}
				}

				// Value for the copy density
				else if(type == "DENSITY_SCALED" || type == "DENSITY_UNSCALED")
				{
					linkKeys = linkList.keys();
					while(linkKeys.hasMoreElements())
					{
						curLink = linkKeys.nextElement();
						if(linkList.get(curLink).getDestNode() == curNode && linkList.get(curLink).cellName.equals(curCell))
						{
							tmpValue = tmpValue + linkList.get(curLink).getTotAcc();
						}
					}

					tmpValue = tmpValue/(cellList.get(curCell).areaSize);

					if(type == "DENSITY_SCALED")
					{
						tmpValue = tmpValue/critValues.get("critDens");
					}
				}

				// Value for the copy flow
				if(type == "FLOW_SCALED" || type == "FLOW_UNSCALED")
				{
					linkKeys = linkList.keys();
					while(linkKeys.hasMoreElements())
					{
						curLink = linkKeys.nextElement();
						if(linkList.get(curLink).getDestNode() == curNode && linkList.get(curLink).cellName.equals(curCell))
						{
							tmpValue = tmpValue + linkList.get(curLink).getTotOutFlow();
						}
					}

					if(type == "FLOW_SCALED")
					{
						tmpValue = tmpValue/critValues.get("critDens");
					}
				}

				// Value for the copy speed
				else if(type == "SPEED_vf")
				{
					tmpValue = cellList.get(curCell).getStreamVel(adjCells.get(curAdjCell),linkList);
				}

				// Value for the copy speed
				else if(type == "SPEED_vcrit")
				{
					tmpValue = cellList.get(curCell).getStreamVel(adjCells.get(curAdjCell),linkList)*param.getFreeSpeed()/critValues.get("critVel");
				}


				// We will, now, fill the Hashtable directionValues

				// If the direction of the current adjacent cell doesn't exist, we just need
				// to put the tmpValue.
				if(directionValues.containsKey(curAdjCell) == false)
				{
					directionValues.put(adjCells.get(curAdjCell), tmpValue);
				}
				else // We have to update the value with tmpValue
				{
					double value = directionValues.get(curAdjCell);
					value = value + tmpValue;
					directionValues.put(adjCells.get(curAdjCell), value);
				}

				tmpValue = 0.0;
			}

			// Now, we will fill the colors
			Enumeration<String> directions = directionValues.keys();

			String curDir;

			while(directions.hasMoreElements())
			{
				curDir = directions.nextElement();

				// We get the color
				if(type == "SPEED_vcrit")
				{
					color = getColorBlueRed(directionValues.get(curDir),maxValue);
				}
				else if(type == "DENSITY_SCALED" || type == "ACCUMULATION_SCALED" || type == "FLOW_SCALED")
				{
					color = getColorRedBlue(directionValues.get(curDir),maxValue);
				}
				else
				{
					color = getColorGreyScale(directionValues.get(curDir),maxValue);
				}

				// We round it and draw one of the arrow
				if(roundToSecondDecimal(directionValues.get(curDir)) >= tol && !curDir.equals(""))
				{

					if(corresps.get(curDir).equals("UP"))
					{
						drawArrowUP(g2D, color, xCenter, yCenter, ddx, ddy, dd);
					}
					else if(corresps.get(curDir).equals("DOWN"))
					{
						drawArrowDOWN(g2D, color, xCenter, yCenter, ddx, ddy, dd);
					}
					else if(corresps.get(curDir).equals("RIGHT"))
					{
						drawArrowRIGHT(g2D, color, xCenter, yCenter, ddx, ddy, dd);
					}
					else if(corresps.get(curDir).equals("LEFT"))
					{
						drawArrowLEFT(g2D, color, xCenter, yCenter, ddx, ddy, dd);
					}

					if(param.displayNumbers == true)
					{
						// Write the value
						writeArrowValue(g2D,curCell,directionValues.get(curDir),maxValue,corresps.get(curDir),dd);
					}
				}
			}

			directionValues.clear();

		}

		g2D.dispose();
	}

	// Draw an arrow with the direction UP
	public void drawArrowUP(Graphics2D g2D, Color color, int xCenter, int yCenter, int ddx, int ddy, int dd)
	{
	    int[] x_vertex = new int[3];
	    int[] y_vertex = new int[3];

		int dy = ddy - 10;

		g2D.setColor(color);

		// Arrow pointing to the top
		g2D.drawLine(xCenter, this.imageHeight - (yCenter + 5), xCenter , this.imageHeight - (yCenter + dy));

		// Head of the arrow
		x_vertex[0] = xCenter;		 			y_vertex[0] = this.imageHeight - (yCenter + ddy);
		x_vertex[1] = xCenter - dd; 			y_vertex[1] = this.imageHeight - (yCenter + dy - dd);
		x_vertex[2] = xCenter + dd; 			y_vertex[2] = this.imageHeight - (yCenter + dy - dd);

		g2D.fillPolygon(x_vertex, y_vertex, 3);
	}

	// Draw an arrow with the direction DOWN
	public void drawArrowDOWN(Graphics2D g2D, Color color, int xCenter, int yCenter, int ddx, int ddy, int dd)
	{
	    int[] x_vertex = new int[3];
	    int[] y_vertex = new int[3];

		int dy = ddy - 10;

		g2D.setColor(color);

		// Arrow pointing to the bottom
		g2D.drawLine(xCenter, this.imageHeight - (yCenter - 5), xCenter , this.imageHeight - (yCenter - dy));


		// Head of the arrow
		x_vertex[0] = xCenter;		 			y_vertex[0] = this.imageHeight - (yCenter - ddy);
		x_vertex[1] = xCenter - dd; 			y_vertex[1] = this.imageHeight - (yCenter - dy + dd);
		x_vertex[2] = xCenter + dd; 			y_vertex[2] = this.imageHeight - (yCenter - dy + dd);

		g2D.fillPolygon(x_vertex, y_vertex, 3);
	}

	// Draw an arrow with the direction RIGHT
	public void drawArrowRIGHT(Graphics2D g2D, Color color, int xCenter, int yCenter, int ddx, int ddy, int dd)
	{
	    int[] x_vertex = new int[3];
	    int[] y_vertex = new int[3];

		int dx = ddx - 10;

		g2D.setColor(color);

		// Arrow pointing to the right
		g2D.drawLine(xCenter + 5, this.imageHeight - yCenter, xCenter + dx, this.imageHeight - yCenter);

		// Head of the arrow
		x_vertex[0] = xCenter + ddx; 			y_vertex[0] = this.imageHeight - yCenter;
		x_vertex[1] = xCenter + dx - dd; 		y_vertex[1] = this.imageHeight - (yCenter + dd);
		x_vertex[2] = xCenter + dx - dd; 		y_vertex[2] = this.imageHeight - (yCenter - dd);

		g2D.fillPolygon(x_vertex, y_vertex, 3);
	}

	// Draw an arrow with the direction LEFT
	public void drawArrowLEFT(Graphics2D g2D, Color color, int xCenter, int yCenter, int ddx, int ddy, int dd)
	{
	    int[] x_vertex = new int[3];
	    int[] y_vertex = new int[3];

		int dx = ddx - 10;

		g2D.setColor(color);

		// Arrow point to the left
		g2D.drawLine(xCenter - 5, this.imageHeight - yCenter, xCenter - dx, this.imageHeight - yCenter);

		// Head of the arrow
		x_vertex[0] = xCenter - ddx; 			y_vertex[0] = this.imageHeight - yCenter;
		x_vertex[1] = xCenter - dx + dd; 		y_vertex[1] = this.imageHeight - (yCenter + dd);
		x_vertex[2] = xCenter - dx + dd; 		y_vertex[2] = this.imageHeight - (yCenter - dd);

		g2D.fillPolygon(x_vertex, y_vertex, 3);
	}

	// Write the value for an arrow
	public void writeArrowValue(Graphics2D g2D, String cellname, double value, double maxValue, String arrowDirection, int arrow_head){

		g2D.setFont(new Font(g2D.getFont().getFontName(), Font.PLAIN, 20));

		g2D.setColor(Color.BLACK);

		FontMetrics fm = g2D.getFontMetrics();

		/** Use this if you want the full precision for double numbers */
		//String str = Double.toString(value);

		/** Use this if you want less precision */
		String str = String.format("%.2f", value);

		int totalWidth = (fm.stringWidth(str)) + 4;

		int xPos = 0;
		int yPos = 0;

		if(roundToSecondDecimal(value) >= tol)
		{

			// If the arrow points in the UP, we will place the text on the left
			if(arrowDirection.equals("UP"))
			{
				int xShift = 10;
				if((double)1.0/4.0*cellHeight.get(cellname) < (double)2.0*arrow_head)
				{
					xShift = (int)Math.round(2.5*xShift);
				}
				xPos = (int)Math.round(cellXPosition.get(cellname) + 0.5*cellWidth.get(cellname) + xShift);
				yPos = (int)Math.round(cellYPosition.get(cellname) + 3.0/4.0*cellHeight.get(cellname) - fm.getHeight()/2.0 + fm.getAscent()/4.0);
			}
			// If the arrow points in the DOWN direction, we will place the text on the right
			else if(arrowDirection.equals("DOWN"))
			{
				int xShift = 10;
				if((double)1.0/4.0*cellHeight.get(cellname) < (double)2.0*arrow_head)
				{
					xShift = (int)Math.round(2.5*xShift);
				}
				xPos = (int)Math.round(cellXPosition.get(cellname) + 0.5*cellWidth.get(cellname) - xShift - totalWidth);
				yPos = (int)Math.round(cellYPosition.get(cellname) + 1.0/4.0*cellHeight.get(cellname) - fm.getHeight()/2.0 + fm.getAscent()/4.0);
			}
			// If the arrow points in the RIGHT direction, we will place the text below the arrow
			else if(arrowDirection.equals("RIGHT"))
			{
				int yShift = 15;
				int xShift = 5;
				if((double)cellWidth.get(cellname)/2.1-totalWidth < (double)2*arrow_head)
				{
					yShift = (int)Math.round(2.5*yShift);
					xShift = 0;
				}
				xPos = (int)Math.round(cellXPosition.get(cellname) + 3.0/4.0*cellWidth.get(cellname) - totalWidth/2.0 - xShift);
				yPos = (int)Math.round(cellYPosition.get(cellname) + (cellHeight.get(cellname) - fm.getHeight())/2.0 + fm.getAscent()/4.0 - yShift);
			}
			// If the arrow points in the LEFT direction, we will place the text above the arrow
			else // arrowDirection.equals("LEFT")
			{
				int yShift = 15;
				int xShift = 10;
				if((double)cellWidth.get(cellname)/2.1-totalWidth < (double)2*arrow_head)
				{
					yShift = (int)Math.round(2.5*yShift);
					xShift = 0;
				}
				xPos = (int)Math.round(cellXPosition.get(cellname) + 1.0/4.0*cellWidth.get(cellname) - totalWidth/2.0 + xShift);
				yPos = (int)Math.round(cellYPosition.get(cellname) + (cellHeight.get(cellname) - fm.getHeight())/2.0 + fm.getAscent()/4.0 + yShift);
			}

			// Before writing the value of the arrow, we draw a white rectangle. It helps to read more easily the values
			g2D.setColor(Color.WHITE);
			g2D.fillRect(xPos - 2,  this.imageHeight - yPos - fm.getHeight() + 5, totalWidth,fm.getHeight());

			// Now, we write the value
			g2D.setColor(Color.BLACK);
			g2D.drawString(str, xPos , this.imageHeight - yPos);
		}
	}

	// Function to return a color with a grey scale
	public Color getColorGreyScale(double value, double maxValue) {

		double red = 0.0;
		double blue = 0.0;
		double green = 0.0;

		if(value < maxValue)
		{
			red = 1.0 - value/maxValue;
			green = 1.0 - value/maxValue;
			blue = 1.0 - value/maxValue;
		}
		else
		{
			red = 0.0;
			green = 0.0;
			blue = 0.0;
		}

		Color col = new Color((float)red,(float)green,(float)blue);

		return col;
	}

	// Function to draw the colorbar with the grey scale
	public void drawColorBarGrey(double maxValue)
	{
		Graphics g = image.getGraphics();

	    Graphics2D g2D = (Graphics2D) g;

	    // We set some better rendering for the pictures
	    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    // We already know that the color bar will be on the right side of the picture

	    double dv = maxValue/10.0;

	    int dh = lengthColorBar/10;

	    for(int i=0; i<10; i++)
	    {
	    	g2D.setColor(getColorGreyScale(i*dv, maxValue));
	    	// Draw like this in order to have the maximum value on top
			g2D.fillRect(this.imageWidth - 175, lengthColorBar + 50 - (i+1)*dh, 50, dh);
	    }

	    g2D.setColor(Color.GRAY);
	    g2D.drawRect(this.imageWidth - 175, 50, 50, lengthColorBar);


	    // Write some values on the colorbar

	    // First, the maximum value

	    double value = maxValue;
	    int yPos = 50;

	    // Then, we print some value between depending on the size of the colorbar

	    int nbrTicks = 11;

	    dv = maxValue/(double)(nbrTicks-1);

	    dh = lengthColorBar/(nbrTicks-1);

	    for(int i=0; i<nbrTicks; i++)
	    {
	    	value = i*dv;
	    	yPos = lengthColorBar + 50 - i*dh;
	    	printColorBarValue(value,yPos);
	    }

	}

	// Function to return a color between red and blue
	public Color getColorRedBlue(double value, double maxValue)
	{
		double red = 0.0;
		double blue = 0.0;
		double green = 0.0;

		if(value < 1.0)
		{
			red = 1.0-value;
			blue = 1.0;
			green = 1.0-value;
		}
		else if(value >= 1.0 && value < maxValue)
		{
			red = 1.0;
			blue = 0.8*(1.0-(value-1.0)/(maxValue-1.0));
			green = 0.8*(1.0-(value-1.0)/(maxValue-1.0));
		}
		else
		{
			red = 1.0;
		}

		Color col = new Color((float)red,(float)green,(float)blue);

		return col;
	}

	// Function to draw the colorbar with the red and blue scale
	public void drawColorBarRedBlue()
	{
		double maxValue = 2.0;

		Graphics g = image.getGraphics();

	    Graphics2D g2D = (Graphics2D) g;

	    // We set some better rendering for the pictures
	    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    // We already know that the color bar will be on the right side of the picture
	    double dvTop = (maxValue-1)/5.0;

	    double dvBottom = 1.0/5.0;

	    int dh = lengthColorBar/10;

	    for(int i=0; i<5; i++)
	    {
	    	g2D.setColor(getColorRedBlue(i*dvBottom, maxValue));
	    	// Draw like this in order to have the maximum value on top
	    	g2D.fillRect(this.imageWidth - 175, lengthColorBar + 50 - (i+1)*dh, 50, dh);
	    }

	    for(int i=5; i<10; i++)
	    {
	    	g2D.setColor(getColorRedBlue((i-5)*dvTop + 1, maxValue));
	    	// Draw like this in order to have the maximum value on top
	    	g2D.fillRect(this.imageWidth - 175, lengthColorBar + 50 - (i+1)*dh, 50, dh);
	    }

	    g2D.setColor(Color.GRAY);
	    g2D.drawRect(this.imageWidth - 175, 50, 50, lengthColorBar);


	    // Write some values on the colorbar

	    // First, the maximum value

	    double value = maxValue;
	    int yPos = 50;

	    // Then, we print some value between depending on the size of the colorbar

	    int nbrTicks = 11;

	    dvTop = (maxValue-1)/5.0;

	    dvBottom = 1.0/5.0;

	    dh = lengthColorBar/(nbrTicks-1);

	    for(int i=0; i<=5; i++)
	    {
	    	value = i*dvBottom;
	    	yPos = lengthColorBar + 50 - i*dh;
	    	printColorBarValue(value,yPos);
	    }

	    for(int i=6; i<nbrTicks; i++)
	    {
	    	value = (i-5)*dvTop + 1.0;
	    	yPos = lengthColorBar + 50 - i*dh;
	    	printColorBarValue(value,yPos);
	    }

	}

	public Color getColorBlueRed(double value, double maxValue)
	{
		double red = 0.0;
		double blue = 0.0;
		double green = 0.0;

		if(value == 0.0)
		{
			red = 1.0;
			blue = 1.0;
			green = 1.0;
		}
		else if(value < 1.0)
		{
			red = 1.0;
			blue = 0.8*value;
			green = 0.8*value;
		}
		else if(value >= 1.0 && value < maxValue)
		{
			red = 0.8*(1.0-(value-1.0)/(maxValue-1.0));
			blue = 1.0;
			green = 0.8*(1.0-(value-1.0)/(maxValue-1.0));
		}
		else
		{
			blue = 1.0;
		}

		Color col = new Color((float)red,(float)green,(float)blue);

		return col;
	}

	public void drawColorBarBlueRed()
	{
		double maxValue = 2.0;

		Graphics g = image.getGraphics();

	    Graphics2D g2D = (Graphics2D) g;

	    // We set some better rendering for the pictures
	    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    // We already know that the color bar will be on the right side of the picture

	    double dvTop = (maxValue-1)/5.0;

	    double dvBottom = 1.0/5.0;

	    int dh = lengthColorBar/10;

	    for(int i=0; i<5; i++)
	    {
	    	g2D.setColor(getColorBlueRed(i*dvBottom, maxValue));
	    	// Draw like this in order to have the maximum value on top
	    	g2D.fillRect(this.imageWidth - 175, lengthColorBar + 50 - (i+1)*dh, 50, dh);
	    }

	    for(int i=5; i<10; i++)
	    {
	    	g2D.setColor(getColorBlueRed((i-5)*dvTop + 1, maxValue));
	    	// Draw like this in order to have the maximum value on top
	    	g2D.fillRect(this.imageWidth - 175, lengthColorBar + 50 - (i+1)*dh, 50, dh);
	    }

	    g2D.setColor(Color.GRAY);
	    g2D.drawRect(this.imageWidth - 175, 50, 50, lengthColorBar);


	    // Write some values on the colorbar

	    // First, the maximum value

	    double value = maxValue;
	    int yPos = 50;

	    // Then, we print some value between depending on the size of the colorbar

	    int nbrTicks = 11;

	    dvTop = (maxValue-1)/5.0;

	    dvBottom = 1.0/5.0;

	    dh = lengthColorBar/(nbrTicks-1);

	    for(int i=0; i<=5; i++)
	    {
	    	value = i*dvBottom;
	    	yPos = lengthColorBar + 50 - i*dh;
	    	printColorBarValue(value,yPos);
	    }

	    for(int i=6; i<nbrTicks; i++)
	    {
	    	value = (i-5)*dvTop + 1.0;
	    	yPos = lengthColorBar + 50 - i*dh;
	    	printColorBarValue(value,yPos);
	    }

	}

	// Function that print the value in the right place in the ColorBar
	public void printColorBarValue(double value, int yPos)
	{
	    int start_line_x = this.imageWidth-125;
	    int finish_line_x = this.imageWidth-115;
	    int xPos_text = this.imageWidth-110;

		Graphics g = image.getGraphics();

	    Graphics2D g2D = (Graphics2D) g;

	    g2D.setColor(Color.BLACK);

		g2D.setFont(new Font(g2D.getFont().getFontName(), Font.PLAIN, 20));

		FontMetrics fm = g2D.getFontMetrics();

		// Use this if you want less precision
		String str = String.format("%.2f", value);
	    g2D.drawLine(start_line_x, yPos, finish_line_x, yPos);
	    g2D.drawString(str, xPos_text, yPos + (int)(fm.getHeight()/2.0 - fm.getAscent()/4.0));
	}

	// Function to save the picture
	public void savePng(String str, int timestep) {
		String Filename = str.concat(Integer.toString(timestep));
		Filename = Filename.concat(".png");
	    try {
	        ImageIO.write(image, "png", new File(outputDir.concat(Filename)));
	    } catch (IOException exception) {
	        exception.printStackTrace();
	    }
	}

	// Function to draw the cell accumulation
	public void drawCellAccumulationUnscaled(int timestep, Hashtable<String, Cell> cellList, Parameter param)
	{
		background(Color.WHITE);
		fillCells(cellList, "ACCUMULATION_UNSCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarGrey(maxAcc);
		savePng("Accumulation/Cell/unscaled/AccumulationCellUnscaled_",timestep);
	}

	// Function to draw the cell accumulation
	public void drawCellAccumulationScaled(int timestep, Hashtable<String, Cell> cellList, Parameter param)
	{
		background(Color.WHITE);
		fillCells(cellList, "ACCUMULATION_SCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarRedBlue();
		savePng("Accumulation/Cell/scaled/AccumulationCellScaled_",timestep);
	}

	// Function to draw the copy accumulation
	public void drawCopyAccumulationUnscaled(int timestep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		background(Color.WHITE);
		drawCells(Color.GRAY, cellList);
		drawArrows(cellList, linkList,"ACCUMULATION_UNSCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarGrey(maxAcc);
		savePng("Accumulation/Copy/unscaled/AccumulationCopyUnscaled_",timestep);
	}

	// Function to draw the copy accumulation
	public void drawCopyAccumulationScaled(int timestep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		background(Color.WHITE);
		drawCells(Color.GRAY, cellList);
		drawArrows(cellList, linkList,"ACCUMULATION_SCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarRedBlue();
		savePng("Accumulation/Copy/scaled/AccumulationCopyScaled_",timestep);
	}

	// Function to draw the cell density
	public void drawCellDensityUnscaled(int timestep, Hashtable<String, Cell> cellList, Parameter param)
	{
		background(Color.WHITE);
		fillCells(cellList, "DENSITY_UNSCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarGrey(maxDensity);
		savePng("Density/Cell/unscaled/DensityCellUnScaled_",timestep);
	}

	// Function to draw the cell density
	public void drawCellDensityScaled(int timestep, Hashtable<String, Cell> cellList, Parameter param)
	{
		background(Color.WHITE);
		fillCells(cellList, "DENSITY_SCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarRedBlue();
		savePng("Density/Cell/scaled/DensityCellScaled_",timestep);
	}

	// Function to draw the copy of density
	public void drawCopyDensityUnscaled(int timestep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		background(Color.WHITE);
		drawCells(Color.GRAY, cellList);
		drawArrows(cellList, linkList,"DENSITY_UNSCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarGrey(maxDensity);
		savePng("Density/Copy/unscaled/DensityCopyUnscaled_",timestep);
	}

	// Function to draw the copy of density
	public void drawCopyDensityScaled(int timestep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		background(Color.WHITE);
		drawCells(Color.GRAY, cellList);
		drawArrows(cellList, linkList,"DENSITY_SCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarRedBlue();
		savePng("Density/Copy/scaled/DensityCopyScaled_",timestep);
	}

	// Function to draw the copy speeds scaled wrt vf
	public void drawCopySpeedvf(int timestep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		background(Color.WHITE);
		drawCells(Color.GRAY, cellList);
		drawArrows(cellList, linkList,"SPEED_vf", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarGrey(1.0);
		savePng("Speed/Copy/scaled_wrt_vf/SpeedCopyScaledWrtVf_",timestep);
	}

	// Function to draw the copy speeds scaled wrt vcrit
	public void drawCopySpeedvCrit(int timestep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		background(Color.WHITE);
		drawCells(Color.GRAY, cellList);
		drawArrows(cellList, linkList,"SPEED_vcrit", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarBlueRed();
		savePng("Speed/Copy/scaled_wrt_vCrit/SpeedCopyScaledWrtVCrit_",timestep);
	}

	// Function to draw the copy flow
	public void drawCopyFlowScaled(int timestep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		background(Color.WHITE);
		drawCells(Color.GRAY, cellList);
		drawArrows(cellList, linkList,"FLOW_SCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarRedBlue();
		savePng("Flow/Copy/scaled/FlowCopyScaled_",timestep);
	}

	// Function to draw the copy flow
	public void drawCopyFlowUnscaled(int timestep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		background(Color.WHITE);
		drawCells(Color.GRAY, cellList);
		drawArrows(cellList, linkList,"FLOW_UNSCALED", param);
		if(param.displayCellNames == true)
		{
			writeCellNames(cellList);
		}
		drawColorBarGrey(maxDensity);
		savePng("Flow/Copy/unscaled/FlowCopyUnscaled_",timestep);
	}

	public void drawPictures(int timeStep, Hashtable<String, Cell> cellList, Hashtable<Integer, Link> linkList, Parameter param)
	{
		// Draw the cell and copy for the density
		drawCellDensityScaled(timeStep,cellList, param);
		drawCellDensityUnscaled(timeStep,cellList, param);
		drawCopyDensityScaled(timeStep, cellList, linkList, param);
		drawCopyDensityUnscaled(timeStep, cellList, linkList, param);

		// Draw the cell and copy for the accumulation
		drawCellAccumulationScaled(timeStep, cellList, param);
		drawCellAccumulationUnscaled(timeStep, cellList, param);
		drawCopyAccumulationScaled(timeStep, cellList, linkList, param);
		drawCopyAccumulationUnscaled(timeStep, cellList, linkList, param);

		// Draw the copy for the flow
		drawCopyFlowScaled(timeStep, cellList, linkList, param);
		drawCopyFlowUnscaled(timeStep, cellList, linkList, param);

		// Draw the copy for the speed
		drawCopySpeedvf(timeStep, cellList, linkList, param);
		drawCopySpeedvCrit(timeStep, cellList, linkList, param);


	}

	// Function to set the adjacent cells in the cellList + the corresponding node
	public void setAdjCells(Hashtable<String, Cell> cellList, Hashtable<Integer, Node> nodeList, Parameter param)
	{
		Enumeration<String> cellKeys1 = cellList.keys(); //enumeration of all cells

		String curCell1, curCell2;
		int curNode;

		Hashtable<String, Integer> adjacentCellsNodes = new Hashtable<String, Integer>();

		HashSet<String> adjCells;

		// First, we loop on all the cells
		while(cellKeys1.hasMoreElements()) {
			curCell1 = cellKeys1.nextElement();

			Enumeration<String> cellKeys2 = cellList.keys();
			// We loop again on all the cells
			while(cellKeys2.hasMoreElements()) {
				curCell2 = cellKeys2.nextElement();

				Enumeration<Integer> nodeKeys = nodeList.keys();
				// We loop on all the nodes
				while(nodeKeys.hasMoreElements()) {
					curNode = nodeKeys.nextElement();

					adjCells = nodeList.get(curNode).getadjacentCells();

					if(adjCells.contains(curCell1) && adjCells.contains(curCell2) && curCell1 != curCell2)
					{
						adjacentCellsNodes.put(curCell2, curNode);
					}
					else if(adjCells.contains(curCell1) && adjCells.contains("none"))
					{
						adjacentCellsNodes.put("none", curNode);
					}
				}
			}

			cellList.get(curCell1).setAdjCellNodes(adjacentCellsNodes);
			adjacentCellsNodes.clear();
		}
	}

	// Function to set the positions of the adjacent cells (UP, DOWN, RIGHT, LEFT)
	// In order to use this function, we already need to know the adjacent cells
	public void setPosAdjCells(Hashtable<String, Cell> cellList)
	{
		Enumeration<String> cellKeys = cellList.keys(); //enumeration of all cells

		String curCell, adjCell;

		Hashtable<String, Integer> adjcell_nodes;

		Hashtable<String, String> posAdjCell = new Hashtable<String, String>();

		Boolean none_cell = false;

		// We loop over all the cells
		while(cellKeys.hasMoreElements()) {
			curCell = cellKeys.nextElement();

			adjcell_nodes = cellList.get(curCell).adjCellNodes;

			Enumeration<String> adjcellKeys = adjcell_nodes.keys();
			// We loop over all the adjacent cells
			while(adjcellKeys.hasMoreElements()) {
				adjCell = adjcellKeys.nextElement();

				String positionAdjCell = "";

				// The case none is difficult, we will take care of it after
				if(adjCell != "none")
				{
					// We must try the four directions (UP, DOWN, RIGHT, LEFT)
					// Warning, since the position are already in Integer, they can have a small shift
					// => we test according to a small error

					// Test if the adjacent cell is at position UP
					if(Math.abs((cellYPosition.get(adjCell) - cellHeight.get(curCell)) - cellYPosition.get(curCell)) < 2)
					{
						positionAdjCell = "N";
					}
					// Test if the adjacent cell is at position DOWN
					if(Math.abs((cellYPosition.get(adjCell) + cellHeight.get(adjCell)) - cellYPosition.get(curCell)) < 2)
					{
						positionAdjCell = "S";
					}
					// Test if the adjacent cell is at position RIGHT
					if(Math.abs((cellXPosition.get(adjCell) - cellWidth.get(curCell)) - cellXPosition.get(curCell)) < 2)
					{
						positionAdjCell = "E";
					}
					// Test if the adjacent cell is at position LEFT
					if(Math.abs((cellXPosition.get(adjCell) + cellWidth.get(adjCell)) - cellXPosition.get(curCell)) < 2)
					{
						positionAdjCell = "W";
					}

					posAdjCell.put(adjCell, positionAdjCell);
				}
				else
				{
					none_cell = true;
				}
			}

			// If a cell has an adjacent as none, we need to indicate the out flow..
			if(none_cell == true)
			{
				// We only do it in the case that the cells which have an adjacent cell as "none" have
				// only 2 links.. (To change this, more if conditions should be added)
				if(posAdjCell.size() == 1)
				{
					if(posAdjCell.values().toString().equals("[E]"))
					{
						posAdjCell.put("none", "W");
					}
					else if(posAdjCell.values().toString().equals("[W]"))
					{
						posAdjCell.put("none", "E");
					}
					else if(posAdjCell.values().toString().equals("[S]"))
					{
						posAdjCell.put("none", "N");
					}
					else //posAdjCell.values().toString().equals("[N]")
					{
						posAdjCell.put("none", "S");
					}
				}

				none_cell = false;
			}
			cellList.get(curCell).setAdjCellPos(posAdjCell);
			posAdjCell.clear();
		}
	}

	public double roundToSecondDecimal(double value)
	{
		return (double) Math.round(value * 100)/100;
	}

	//creates output directory unless existing
	private void createOutputDir(String STRoutputDir) {
		//create output directory as specified in parameters
		try {
		File outputDir = new File(STRoutputDir);
		outputDir.mkdirs();
		}
		//raise exception if unable to create directory
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
