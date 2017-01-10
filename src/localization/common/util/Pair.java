package localization.common.util;

/**
 * User defined struct, which saves the information appear together
 * 
 * @author Jiajun
 *
 * @param <A>
 * @param <B>
 */
public class Pair<A, B> {

	private A _first;
	private B _second;

	public Pair() {
	}

	public Pair(A first, B second) {
		_first = first;
		_second = second;
	}

	public void setFirst(A first) {
		_first = first;
	}

	public void setSecond(B second) {
		_second = second;
	}

	public A getFirst() {
		return _first;
	}

	public B getSecond() {
		return _second;
	}

}
