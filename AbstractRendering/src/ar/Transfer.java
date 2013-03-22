package ar;

import java.awt.Color;

public interface Transfer<A> {
	public Color at(int x, int y, Aggregates<A> aggregates);
}
