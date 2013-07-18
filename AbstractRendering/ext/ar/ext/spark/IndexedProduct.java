package ar.ext.spark;

import scala.Product;
import spark.api.java.function.Function;
import ar.glyphsets.implicitgeometry.Indexed;

/**Bridges the implicit geometry's "indexed" type with scala's "product" type.**/
public class IndexedProduct implements Indexed {
	private final Product p;
	public IndexedProduct(Product p) {this.p =p;}
	public Object get(int i) {return p.productElement(i);} 
	
	public static final class Wrapper extends Function<Product, IndexedProduct> {
		private static final long serialVersionUID = 5890076181745969708L;

		public IndexedProduct call(Product arg) throws Exception {return new IndexedProduct(arg);}
		
	}

}
