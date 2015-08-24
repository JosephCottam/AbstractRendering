package ar.ext.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Optional;

import static java.util.stream.Collectors.*;

/**Central utilities for AR language.**/
public class Parser {
	public static class TreeNode<A> implements Iterable<TreeNode<A>> {
		  private Optional<A> value;
		  private List<TreeNode<A>> children;
		  
		  public TreeNode(Optional<A> value, List<TreeNode<A>> children) {
			  this.value = value;
			  this.children = children;
		  }
		  
		  public Iterator<TreeNode<A>> iterator() {return children.iterator();}
		  public List<TreeNode<A>> children() {return children;}
		  public Optional<A> value() {return value;}
		  
		  public TreeNode<A> extend(TreeNode<A> child) {
			  List<TreeNode<A>> newChildren = new ArrayList<>();
			  newChildren.addAll(children);
			  newChildren.add(child);
			  return new TreeNode<A>(value, newChildren);
		  }
		  
		  public boolean equals(Object alter) {
			  if (!(alter instanceof TreeNode)) {return false;}
			  TreeNode<?> other = (TreeNode<?>) alter;
			  return this.value.equals(other.value)
					  && this.children.size() == other.children.size()
					  && IntStream.range(0,children.size())
					  		.allMatch(i -> children.get(i).equals(other.children.get(i)));
		  }
		  
		  public String toString() {
			  String val = value.isPresent() ? value.get().toString() + " " : "";
			  String start = children.size() > 0 ? "(" : "";
			  String end = children.size() > 0 ? ")" : "";
			  return start +  val + children.stream().map(e -> e.toString()).collect(Collectors.joining(" ")) + end;
		  }
		  
		  public static <A> TreeNode<A> empty() {return new TreeNode<A>(Optional.empty(), Collections.emptyList());}
		  public static <A> TreeNode<A> leaf(A value) {return new TreeNode<A>(Optional.ofNullable(value), Collections.emptyList());}

		  @SafeVarargs
		  public static <A> TreeNode<A> inner(TreeNode<A>... children) {return new TreeNode<A>(Optional.empty(), Arrays.asList(children));}

	}
	
	public static List<String> tokens(String input) {
		 return Arrays.stream(input.trim().split("(?<=[\\s|(|)|,])|(?=[\\s|(|)|,])"))	//Lookahead/behind to return separators as tokens
				 .filter(s -> !s.trim().equals(""))
				 .filter(s -> !s.trim().equals(","))
				 .collect(toList());
	}
	
	public static TreeNode<String> parseTree(Iterable<String> tokens) {
		TreeNode<String> result = parseTree(tokens.iterator(), TreeNode.empty());
		return result.children.get(0);
	}
	
	public static TreeNode<String> parseTree(Iterator<String> tokens, TreeNode<String> root) {
		while (tokens.hasNext()) {
			String token = tokens.next();
			switch (token) {
				case "(": 
					TreeNode<String> newRoot = TreeNode.empty();
					root = root.extend(parseTree(tokens, newRoot)); 
					break;
				case ")": 
					return root;
				default: 
					root = root.extend(TreeNode.leaf(token)); 
					break;
			}
		}		
		return root;
	}
	
	public static Object reify(TreeNode<?> tree, Map<String, ? extends Function<List<Object>,?>> lookup) {
		if (!tree.value().isPresent()) {
			List<Object> parts = tree.children().stream().map(e -> reify(e, lookup)).collect(toList());
			String name;
			
			if (parts.get(0) instanceof String) {name = (String) parts.get(0);}
			else {throw new IllegalArgumentException("Must have function-name in first position");}
			
			Function<List<Object>,?> fn  = lookup.getOrDefault(name, null);
			if (fn == null) {throw new IllegalArgumentException("Function name not known: " + name);}
			
			List<Object> args = parts.subList(1, parts.size());
			try {return fn.apply(args);}
			catch (Exception e) {throw new RuntimeException("Error reifying " + tree + "\n" + e.getMessage());}
		}
		
		String val = tree.value().get().toString();
		if (val.equals("NaN")) {return Double.NaN;}
		if (val.equals("fNaN")) {return Float.NaN;}
		try {return Integer.parseInt(val);}
		catch (Exception e) {
			try {return Double.parseDouble(val);}
			catch (Exception ex) {return val;}
		}
		
	}
	
	public static TreeNode<String> parse(String input) {return parseTree(tokens(input));}
	
	
	/**Create a help string for the language, including information on the function library passed in.
	 * Returns two lists of strings.  The first is a brief description of syntax/semantics.  
	 * The second is a list of the available functions.**/
	public static String basicHelp(String lineSep) {
		String[] basics = new String[]{
				"Basic syntax follows s-epxressions: (item (item item item))",
				"Looks like lisp, with all of the visual appeal and very little of the actual power!",
				"There are separators, lists, numeric literals, symbol liteals and function calls.",
				"There are no strings, variables, function definitions, comments or other nice things like that.",
				"There are first-class functions and externally defined higher-order functions, so you get some fun.",
				"There is one namespace.",
				"",
				"The first item in the expression determines a function to call, all other items are arguments.",
				"Since there are no higher-order functions, the first item in each list must be a symbol.",
				"All expression arguments are evaluated before the function call is made.",
				"For example (RGB 0 0 0) makes the color black. RGB is the function, 0 is a numeric literal.",
				"Whitespace and comma are the separators.  All seperators are equal, so (RGB,0,0,0) works just as well.",
		};
		return Arrays.stream(basics).collect(Collectors.joining(lineSep)); 
	}
	
	public static String functionHelp(Map<?, ?> funcs, String format, String sep) {
		return funcs.values().stream().map(e -> e.toString()).map(s -> String.format(format, s)).sorted().collect(Collectors.joining(sep));
	}
}
