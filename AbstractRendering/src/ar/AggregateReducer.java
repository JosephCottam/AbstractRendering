package ar;

/**Combine two aggregate sets into a third composite aggregate.**/
public interface AggregateReducer<IN1,IN2,OUT> {
	public OUT combine(IN1 left, IN2 right);
}
