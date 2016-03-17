package protest.db;

public class TargetCorpus {
	private int id_;
	private String label_;
	private int count_;

	public TargetCorpus(int id, String label, int count) {
		id_ = id;
		label_ = label;
		count_ = count;
	}

	public int getID() {
		return id_;
	}

	public String getLabel() {
		return label_;
	}

	public int getCount() {
		return count_;
	}
}
