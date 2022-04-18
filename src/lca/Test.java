package lca;

import java.io.File;

import org.openlca.eigen.NativeLibrary;
import org.openlca.julia.Julia;


public class Test {

	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		// TODO Auto-generated method stub
		double[] a = { 1, -4, 0, 9 };

		Julia.loadFromDir(new File("C:/Program Files (x86)/openLCA") );
		int c = Julia.invert(2, a);
		System.out.println(c);


	}

}
