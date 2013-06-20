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
public class SchemaResolver {

	private final Map<String, Schema> schemas = new HashMap<String, Schema>();
	private Schema mostRecent=null;

	public Schema resolve() {return mostRecent;}
	
	/**Register a new schema with this repository.**/
	public SchemaResolver addSchema(Schema schema){
		for (String alias: schema.getAliases()) {
			schemas.put(alias, schema);
		}
		schemas.put(schema.getFullName(), schema);
		mostRecent = schema;
		return this;
	}

	public String resolveSchema(String sc){
		String result = sc;
		for(Map.Entry<String, Schema> entry : schemas.entrySet())
			result = replace(result, entry.getKey(),
					entry.getValue().toString());
		return result;
	}

	static String replace(String str, String pattern, String replace) {

		int s = 0;
		int e = 0;
		StringBuffer result = new StringBuffer();
		while ((e = str.indexOf(pattern, s)) >= 0) {
			result.append(str.substring(s, e-1));
			result.append(replace);
			s = e+pattern.length()+1;

		}
		result.append(str.substring(s));
		return result.toString();

	}

	public SchemaResolver addSchema(String schemaString) {
		try {
			String completeSchema = resolveSchema(schemaString);
			Schema schema = new Schema.Parser().parse(completeSchema);
			this.addSchema(schema);
		} catch (Exception e) {
			throw new RuntimeException("Error loading schema:" + schemaString, e);
		}
		return this;
	}

	public SchemaResolver loadSchema(InputStream in)throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return addSchema(out.toString());
	}

	public SchemaResolver loadSchema(File file)throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			return loadSchema(fis);
		} catch (Exception e) {
			throw new RuntimeException("Error loading schema " + file.getName(), e.getCause());
		}
	}
	
	public SchemaResolver loadSchema(String path) throws IOException {
		try {
			loadSchema(AggregateSerailizer.class.getClassLoader().getResourceAsStream(path));
		} catch (Exception e) {
			throw new RuntimeException("Error loading schema " + path, e.getCause());
		}

		return this;
	}
}