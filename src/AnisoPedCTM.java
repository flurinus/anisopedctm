import java.util.ArrayList;

import anisopedctm.Board;


/*

 * @author Flurin Haenseler, Gael Lederrey
 * 
 */

public class AnisoPedCTM {
	
	public static void main(String[] args) {

		/*
		 * Experiments
		 */
		ArrayList<String> expList = new ArrayList<String>();
		
		expList.add("examples/scenarios/HKU/HKU78-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU78-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU78-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU79-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU79-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU79-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU80-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU80-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU80-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU81-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU81-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU81-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU82-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU82-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU82-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU83-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU83-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU83-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU84-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU84-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU84-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU84-zero_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU85-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU85-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU85-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU85-zero_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU86-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU86-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU86-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU86-zero_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU87-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU87-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU87-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU88-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU88-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU88-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU88-zero_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU89-drake_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU89-sbfd_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU89-weidmann_scenario.txt");
		expList.add("examples/scenarios/HKU/HKU89-zero_scenario.txt");
		
		expList.add("examples/scenarios/BER-drake_scenario.txt");
		expList.add("examples/scenarios/BER-sbfd_scenario.txt");
		expList.add("examples/scenarios/BER-weidmann_scenario.txt");
		expList.add("examples/scenarios/BER-zero_scenario.txt");
		
		expList.add("examples/scenarios/BER-sbfd_scenario-visualized-notext.txt");
		//demonstrates visualization
		
		expList.parallelStream().forEach((exp) -> {
			
			//initialize simulation by generating board
			Board board = new Board(exp);
			
			//simulate
			board.simulate();
			
			//run board and generate travel time statistics
//			board.getTravTimeStat();
	
			System.out.println("Exp " + exp.split("_")[0] +
					"-- log-likelihood: " + board.getLogLikelihood() );
		});
		
		
		/*
		 * Calibration
		 */
		
		int numIter = 256;
		//WARNING: May take several hours to complete.
		
		ArrayList<String> calibList = new ArrayList<String>();
		
		calibList.add("examples/scenarios/BER-drake_scenario.txt");
		calibList.add("examples/scenarios/BER-sbfd_scenario.txt");
		calibList.add("examples/scenarios/BER-weidmann_scenario.txt");
		calibList.add("examples/scenarios/BER-zero_scenario.txt");
		
		calibList.add("examples/scenarios/HKU-drake_85_87.txt");
		calibList.add("examples/scenarios/HKU-sbfd_85_87.txt");
		calibList.add("examples/scenarios/HKU-weidmann_85_87.txt");
		calibList.add("examples/scenarios/HKU-zero_85_87.txt");
		
		calibList.parallelStream().forEach((exp) -> {
			System.out.println("Calibration run started.");
			
			//initialize simulation by generating board
			Board board = new Board(exp);
			
			//calibrate using random initial conditions sampled from parameter search space
			board.calibrate(numIter);
			
			//calibrate using default parameters as initial point
			//board.calibrateDefaultParam();
		});
	}

}
