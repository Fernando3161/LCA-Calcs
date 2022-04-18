package lca;

import java.io.File;
import java.io.IOException;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.CalculationType;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.matrix.LinkingConfig;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.matrix.solvers.DenseSolver;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.SimpleResult;
import org.openlca.eigen.NativeLibrary;
import org.openlca.julia.Julia;
import org.openlca.julia.JuliaSolver;


public class Calculation {

	public static IDatabase connectDB(String db_name) {

		IDatabase db = null;
		try {
			//String dbpath = "C:/Users/fpenaherrera_vaca/openLCA-data-1.4/databases/" + db_name;
			String dbpath = "C:/Users/Besitzer/openLCA-data-1.4/databases/" + db_name;
			db = new DerbyDatabase(new File(dbpath));
			System.out.println(db.getName() + " ZOLCA Database Connected");
		} catch (Exception e) {
			System.out.println("Database could not be connected");
		}
		return db;

	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		IDatabase db = connectDB("20210526ppre_sustainability_db");
		
		//Path of installation of openLCA
		File lib_folder = new File("C:/Program Files (x86)/openLCA");
		Julia.loadFromDir(lib_folder);
	    var solver = new JuliaSolver();
		
		ProductSystemDao psDao = new ProductSystemDao(db);
		ProductSystem ps = psDao.getForName("Trans Filled Bottle").get(0);

		ImpactMethodDao methodDao = new ImpactMethodDao(db);
		System.out.println(methodDao);
		ImpactMethod methodCalc = methodDao.getForName("CML").get(0);
		FullResult result = new FullResult();

		System.out.println("Done2");

		MatrixCache matrixCache = MatrixCache.createLazy(db);
		SystemCalculator calculator = new SystemCalculator(matrixCache, solver);
		CalculationSetup setup = new CalculationSetup(CalculationType.CONTRIBUTION_ANALYSIS, ps);
		setup.impactMethod = methodDao.getDescriptorForRefId(methodCalc.refId);

		System.out.println("Done3");

		result = calculator.calculateFull(setup);
		System.out.println(result);
		System.out.println("Done4");

		try {
			db.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
