package ar.ext.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Optional;
import java.awt.Color;

import static java.util.stream.Collectors.*;

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
			  String val = value.isPresent() ? value.get().toString() : "<null>";
			  String start = children.size() > 0 ? "(" : "";
			  String end = children.size() > 0 ? ")" : "";
			  return start +  val + " " + children.stream().map(e -> e.toString()).collect(Collectors.joining(" ")) + end;
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
	
	public static Object reify(TreeNode<?> tree) {return reify(tree, DEFAULT_FUNCTIONS);}
	public static Object reify(TreeNode<?> tree, Map<String, Function<List<Object>, Object>> lookup) {
		if (!tree.value().isPresent()) {
			List<Object> parts = tree.children().stream().map(e -> reify(e, lookup)).collect(toList());
			String name;
			
			if (parts.get(0) instanceof String) {name = (String) parts.get(0);}
			else {throw new IllegalArgumentException("Must have function-name in first position");}
			
			Function<List<Object>, Object> fn  = lookup.getOrDefault(name, null);
			if (fn == null) {throw new IllegalArgumentException("Function name not known: " + name);}
			
			List<Object> args = parts.subList(1, parts.size());
			return fn.apply(args);
		}
		
		String val = tree.value().get().toString();
		try {return Integer.parseInt(val);}
		catch (Exception e) {
			try {return Double.parseDouble(val);}
			catch (Exception ex) {return val;}
		}
		
	}
	
	public static TreeNode<String> parse(String input) {return parseTree(tokens(input));}
	
	public static final Map<String, Function<List<Object>, Object>> DEFAULT_FUNCTIONS = new HashMap<>();
	static {
		DEFAULT_FUNCTIONS.put("rgb", args -> new Color((int) args.get(0), (int) args.get(1), (int) args.get(2)));
		DEFAULT_FUNCTIONS.put("string", args -> args.stream().map(e -> e.toString()).collect(joining(" ")));
		
	}
}
