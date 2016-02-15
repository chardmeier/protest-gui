package protest;

public class TargetCorpus {
	private String label_;
	private int count_;

	public TargetCorpus(String label, int count) {
		label_ = label;
		count_ = count;
	}

	public String getLabel() {
		return label_;
	}

	public int getCount() {
		return count_;
	}
}
