package ar.test.ext;

import static org.junit.Assert.*;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import ar.ext.lang.BasicLibrary;
import static org.hamcrest.Matchers.*;
import static ar.ext.lang.Parser.*;
import static ar.ext.lang.Parser.TreeNode.*;

public class LangTest {
	public static final Map<String, Function<List<Object>, Object>> LIBRARY = new HashMap<>();
	static {
		LIBRARY.putAll(BasicLibrary.COLOR);
		LIBRARY.putAll(BasicLibrary.COMMON);
	}
	
	@Test
	public void tokenize() {
		assertThat(tokens("test"), contains("test"));
		assertThat(tokens("(test)"), contains("(", "test", ")"));
		assertThat(tokens("(test of some more)"), contains("(", "test", "of", "some", "more", ")"));
		assertThat(tokens("(test, comas, too)"), contains("(", "test", "comas", "too", ")"));
		assertThat(tokens("((nested))"), contains("(", "(", "nested", ")", ")"));
		assertThat(tokens("((nested (deeply)) shallowly)"), contains("(", "(", "nested", "(", "deeply", ")", ")", "shallowly", ")"));
		assertThat(tokens("x+c"), contains("x+c"));
		assertThat(tokens("(x+c)"), contains("(", "x+c", ")"));
	}
	
	@Test
	public void tree() {
		assertThat(parse("test"), is(equalTo(leaf("test"))));
		assertThat(parse("(test)"), is(equalTo(inner(leaf("test")))));
		assertThat(parse("(test of some more)"), is(equalTo(inner(leaf("test"), leaf("of"), leaf("some"), leaf("more")))));
		assertThat(parse("(nested)"), is(equalTo(inner(leaf("nested")))));
		assertThat(parse("((nested (deeply)) shallowly)"), is(equalTo(inner(inner(leaf("nested"), inner(leaf("deeply"))), leaf("shallowly")))));
	}
	
	@Test
	public void reifyAtom() {
		assertThat(reify(parse("1"), LIBRARY), is(Integer.valueOf(1)));
		assertThat(reify(parse("1.3"), LIBRARY), is(Double.valueOf(1.3)));
		assertThat(reify(parse("1.3b"), LIBRARY), is("1.3b"));
		assertThat(reify(parse("hello"), LIBRARY), is("hello"));
	}
	
	@Test
	public void reifyFunctions() {
		assertThat(reify(parse("(rgb 0 0 0)"), LIBRARY), is(new Color(0,0,0)));
		assertThat(reify(parse("(rgb 255 255 255)"), LIBRARY), is(new Color(255,255,255)));
		assertThat(reify(parse("(string (space) 255 255 255)"), LIBRARY), is("255 255 255"));
		assertThat(reify(parse("(color,RED)"), LIBRARY), is(Color.RED));
		assertThat(reify(parse("(color,red)"), LIBRARY), is(Color.RED));
		assertThat(reify(parse("(color,Red)"), LIBRARY), is(Color.RED));
		assertThat(reify(parse("(color,rEd)"), LIBRARY), is(Color.RED));
	}
	
	
	@Test 
	public void refiyFunctionsWithDefaults() {
		List<String> exclude = Arrays.asList("fn");	
		for (String key: BasicLibrary.ALL.keySet()) {
			if (exclude.contains(key)) {continue;}
			
			Object reified = reify(parse("("+key+")"), BasicLibrary.ALL);
			assertThat("Reifying " + key, reified, is(notNullValue()));
			assertThat(reified, instanceOf(BasicLibrary.ALL.get(key).apply(Collections.emptyList()).getClass()));
		}
	}
	
	@Test
	public void urlSafe() {
		for(String key: BasicLibrary.ALL.keySet()) {
			assertFalse(key.contains(" "));
			assertFalse(key.contains("+"));
			assertFalse(key.contains("&"));
			assertFalse(key.contains("#"));
		}
	}
}
