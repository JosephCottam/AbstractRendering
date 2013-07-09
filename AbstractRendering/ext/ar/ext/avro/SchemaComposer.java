package ar.ext.avro;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Schema;

/**Fluent tool for loading up a set of avro schemas
 * and generating a composite from them based on alias resolution
 * 
 * Usage notes:  
 *  Schemas may not be mutually recursive
 *  Leaf schemas must be fully defined
 *  Schemas must be inserted from leaves up
 *  Alias references must be fully qualified
 *  
 * Based on http://www.infoq.com/articles/ApacheAvro.
 * **/
public class SchemaComposer {

	private final Map<String, Schema> schemas = new HashMap<String, Schema>();
	private Schema mostRecent=null;

	/**Return the most recently add item.**/
	public Schema resolved() {return mostRecent;}

	private String resolveSchema(String sc){
		String result = sc;
		for(Map.Entry<String, Schema> entry : schemas.entrySet())
			result = replace(result, entry.getKey(),
					entry.getValue().toString());
		return result;
	}

	private static String replace(String str, String pattern, String replace) {
		StringBuffer result = new StringBuffer();
		int e = str.indexOf(pattern, 0);
		if (e < 0) {return str;}
		
		result.append(str.substring(0, e-1));
		result.append(replace);
		result.append(str.substring(e+pattern.length()+1));
		return result.toString();
	}

	
	
	/**Register a new schema with this repository.**/
	public SchemaComposer add(Schema schema){
		for (String alias: schema.getAliases()) {
			schemas.put(alias, schema);
		}
		schemas.put(schema.getFullName(), schema);
		mostRecent = schema;
		return this;
	}


	/**Load a schema, directly from the string.**/
	public SchemaComposer add(String schemaString) {
		try {
			String completeSchema = resolveSchema(schemaString);
			Schema schema = new Schema.Parser().parse(completeSchema);
			this.add(schema);
		} catch (Exception e) {
			throw new RuntimeException("Error loading schema:" + schemaString, e);
		}
		return this;
	}

	/**Load a schema from an input stream.**/
	public SchemaComposer add(InputStream in)throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return add(out.toString());
	}

	/**Load a schema from a file.**/
	public SchemaComposer add(File file)throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			return add(fis);
		} catch (Exception e) {
			throw new RuntimeException("Error loading schema " + file.getName(), e.getCause());
		}
	}


	/**Load a schema from a file (specified as a string).**/
	public SchemaComposer addFile(String file)throws IOException {return add(new File(file));}

		
	/**Load a schema via the class-loader resource mechanism.**/
	public SchemaComposer addResource(String path) throws IOException {
		try {
			add(AggregateSerializer.class.getClassLoader().getResourceAsStream(path));
		} catch (Exception e) {
			throw new RuntimeException("Error loading schema " + path, e.getCause());
		}
		return this;
	}
}