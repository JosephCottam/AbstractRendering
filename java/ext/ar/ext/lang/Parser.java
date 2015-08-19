package ar.ext.lang;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;

import static java.util.stream.Collectors.*;

public class Parser {
	public class TreeNode<A> implements Iterable<TreeNode<A>> {
		  private Optional<A> value;
		  private Set<TreeNode<A>> children;
		  
		  public TreeNode() {this(null, Collections.emptySet());}
		  public TreeNode(A value) {this(value, Collections.emptySet());}
		  public TreeNode(A value, Set<TreeNode<A>> children) {
			  this.value = Optional.fromNullable(value);
			  this.children = Collections.unmodifiableSet(children);
		  }
		  
		  public Iterator<TreeNode<A>> iterator() {return children.iterator();}
		  public Set<TreeNode<A>> children() {return children;}
		  public Optional<A> value() {return value;}
		  
		  public TreeNode<A> extend(TreeNode<A> child) {
			  Set<TreeNode<A>> children = new HashSet<>();
			  children.addAll(children);
			  children.add(child);
			  return new TreeNode<>(null, children);
		  }
	}
	
	public static List<String> tokens(String input) {
		 return Arrays.stream(input.trim().split("(?<=[\\s|(|)])|(?=[\\s|(|)])"))
				 .filter(s -> !s.trim().equals(""))
				 .collect(toList());
	}
	
	public TreeNode<String> parseTree(Iterator<String> tokens) {
		TreeNode<String> root = new TreeNode<>();
		while (tokens.hasNext()) {
			String token = tokens.next();
			switch (token) {
				case "(": root = root.extend(parseTree(tokens)); break;
				case ")": return root;
				default: root = root.extend(new TreeNode<>(token)); break;
			}
		}		
		return root;
	}
}
