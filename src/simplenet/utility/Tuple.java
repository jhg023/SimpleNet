package simplenet.utility;

public final class Tuple<T, U> {

	private T t;

	private U u;

	public Tuple(T t, U u) {
		this.t = t;
		this.u = u;
	}

	public T getLeft() {
		return t;
	}

	public U getRight() {
		return u;
	}

}
