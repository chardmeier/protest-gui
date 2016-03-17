package protest.db;

import java.util.Arrays;

public class Position {
	private int line_;
	private int start_;
	private int end_;
	
	public Position(int line, int start, int end) {
		line_ = line;
		start_ = start;
		end_ = end;
	}

	public Position(int line, int pos) {
		line_ = line;
		start_ = pos;
		end_ = pos;
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

	public boolean equals(Object o) {
		if(!(o instanceof Position))
			return false;

		return o.line_ == line_ && o.start_ == start_ && o.end_ == end_;
	}

	public int hashCode() {
		return Arrays.asList(line, start, end).hashCode();
	}
}
