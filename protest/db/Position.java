package protest;

public class Position {
	private int line_;
	private int start_;
	private int end_;
	
	Position(int line, int start, int end) {
		line_ = line;
		start_ = start;
		end_ = end;
	}

	public int getLine() {
		return line_;
	}

	public int getStart() {
		return start_;
	}

	public int getEnd() {
		return end_;
	}
}
