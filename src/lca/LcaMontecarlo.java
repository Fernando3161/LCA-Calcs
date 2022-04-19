package lca;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.CalculationType;
import org.openlca.core.math.Simulator;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.SimulationResult;
import org.openlca.julia.Julia;
import org.openlca.julia.JuliaSolver;

import com.opencsv.CSVWriter;

class ModelMonteCarlo {
	int nThrows;
	FullResult fullResult;
	Simulator simulatorMC;
	double resultsList; // this shall be a list of values or whatever it is
	String scenario; // scenario for quick change of databases
	boolean terminated;

	class MonteCarlo implements Runnable {
		@Override
		public void run() {
			simulatorMC.nextRun();
		}
	}

	public ModelMonteCarlo(int i) {
		this.nThrows = i;
		this.scenario = "EVAL";
		this.terminated=false;

	}

	public void setUpModel() {
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.FATAL);
		String db_name = "test_methods";
		String ps_name = "mainboard balanced";
		String method_name = "CML 2001";

		if (this.scenario.equals("EVAL")) {
			db_name = "20210526ppre_sustainability_db";
			ps_name = "Trans Filled Bottle";
			method_name = "CML";
		}

		IDatabase db = Calculation.connectDB(db_name);
		// Path of installation of openLCA
		File lib_folder = new File("C:/Program Files (x86)/openLCA");
		Julia.loadFromDir(lib_folder);
		var solver = new JuliaSolver();

		ProductSystemDao psDao = new ProductSystemDao(db);
		ProductSystem ps = psDao.getForName(ps_name).get(0);
		ImpactMethodDao methodDao = new ImpactMethodDao(db);

		ImpactMethod methodCalc = methodDao.getForName(method_name).get(0);
		MatrixCache matrixCache = MatrixCache.createLazy(db);
		SystemCalculator calculator = new SystemCalculator(matrixCache, solver);

		CalculationSetup setup = new CalculationSetup(CalculationType.CONTRIBUTION_ANALYSIS, ps);
		setup.impactMethod = methodDao.getDescriptorForRefId(methodCalc.refId);
		FullResult fullResult = calculator.calculateFull(setup);
		this.fullResult = fullResult;
		Set<ImpactCategoryDescriptor> ic_descr = fullResult.getImpacts();

		// Now setup the montecarlo simulator
		CalculationSetup setupMC = new CalculationSetup(CalculationType.MONTE_CARLO_SIMULATION, ps);
		setupMC.impactMethod = methodDao.getDescriptorForRefId(methodCalc.refId);
		Simulator simulatorMC = Simulator.create(setupMC, matrixCache, solver);
		this.simulatorMC = simulatorMC;
	}

	public void runSimulations() { // Here Replace for get Results
		int nProcessors = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newWorkStealingPool(nProcessors);
		for (int i = 1; i <= nThrows; i++) {
			Runnable worker = new MonteCarlo();
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
			this.terminated=false;
		}
		
		this.terminated=true;
		


	}


	public void getImpacts() throws IOException, InterruptedException {
		if (!this.terminated) {
			Thread.sleep(1000);
		}
		List<String[]> table = new ArrayList<String[]>();
		Set<ImpactCategoryDescriptor> ic_descrs = this.fullResult.getImpacts();
		SimulationResult result_sim = this.simulatorMC.getResult();
		int len = ic_descrs.size();
		String[] header = new String[len];
		int j = 0;
		for (ImpactCategoryDescriptor ic_descr : ic_descrs) {
			header[j]=ic_descr.name;
			j=j+1;
		}
		table.add(header);
		double[] res_cat_list = null;
		Map<String, double[]> res_map = new HashMap<String, double[]>();
		for (ImpactCategoryDescriptor ic_descr : ic_descrs) {
			res_cat_list = result_sim.getAll(ic_descr);
			res_map.put(ic_descr.name, res_cat_list);
			
		}
		for (int i = 1; i < res_cat_list.length; i++) {
			String[] row = new String[len];
			int k =0;
			for (String head : header) {
				double[] list_val = res_map.get(head);
				row[k]=Double.toString(list_val[i-1]);
				k+=1;
			}
			table.add(row);
		}

		File file = new File("D:/DissGit/test.csv");
		FileWriter outputfile = null;
		try {
			outputfile = new FileWriter(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CSVWriter writer = new CSVWriter(outputfile, ',', CSVWriter.NO_QUOTE_CHARACTER,
				CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

		writer.writeAll(table);

		// closing writer connection
		writer.close();
	}
}

public class LcaMontecarlo {
	public static void main(String[] args) throws IOException, InterruptedException {

			long startTime = System.currentTimeMillis();
			ModelMonteCarlo mcVal = new ModelMonteCarlo(10000);
		mcVal.setUpModel();
		mcVal.runSimulations();
		mcVal.getImpacts();
		

		// mcVal.runSimulations();
		long stopTime = System.currentTimeMillis();
		System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
		System.out.println("Time Duration: " + (stopTime - startTime) + "ms");
	
	}
}
