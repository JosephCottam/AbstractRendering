package ar.test.ext;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import ar.ext.lang.Parser;

public class LangTest {
	@Test
	public void tokenize() {
		assertThat(Parser.tokens("test"), contains("test"));
		assertThat(Parser.tokens("(test)"), contains("(", "test", ")"));
		assertThat(Parser.tokens("(test of some more)"), contains("(", "test", "of", "some", "more", ")"));
		assertThat(Parser.tokens("((nested))"), contains("(", "(", "nested", ")", ")"));
		assertThat(Parser.tokens("((nested (deeply)) shallowly)"), contains("(", "(", "nested", "(", "deeply", ")", ")", "shallowly", ")"));
	}
}
