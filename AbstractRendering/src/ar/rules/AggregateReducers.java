package ar.rules;

import java.util.HashSet;

import ar.AggregateReducer;
import ar.rules.Aggregators.RLE;


public class AggregateReducers {

	public static class Count implements AggregateReducer<Integer,Integer,Integer> {
		public Integer combine(Integer left, Integer right) {return left+right;}
		public String toString() {return "Count (int x int -> int)";}
	}

	public static class MergeCOC implements AggregateReducer<RLE,RLE,RLE> {
		public RLE combine(RLE left, RLE right) {
			if (left == null || left.size()==0) {return right;}
			if (right == null || left.size()==0) {return left;}
			
			HashSet<Object> categories = new HashSet<Object>();
			categories.addAll(left.keys);
			categories.addAll(right.keys);
			
			RLE total = new RLE();
			
			for (Object category: categories) {
				int v1 = left.val(category);
				int v2 = right.val(category);
				total.add(category, v1+v2);
			}
			return total;
		}
		public String toString() {return "CoC (RLE x RLE -> RLE)";}
	}

	
}
