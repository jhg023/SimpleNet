package simplenet.utility;

public final class Tuple<Left, Right> {

	private Left left;

	private Right right;

	public Tuple(Left left, Right right) {
		this.left = left;
		this.right = right;
	}

	public Left getLeft() {
		return left;
	}

	public Right getRight() {
		return right;
	}

}
