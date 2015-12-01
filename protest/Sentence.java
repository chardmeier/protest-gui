package protest;

public class Sentence {
	private String[] tokens_;
	private int[] anaphorHighlight_;
	private int[] antecedentHighlight_;

	private int currtoken_;
	private int i_ana_;
	private int i_ant_;

	public Sentence(String line, int[] anaphorHighlight, int[] antecedentHighlight) {
		tokens_ = line.split(" ");
		anaphorHighlight_ = anaphorHighlight;
		antecedentHighlight_ = anaphorHighlight;
		currtoken_ = -1;
		i_ana_ = 0;
		i_ant_ = 0;
	}

	public void next() {
		currtoken_++;
	}

	public boolean hasNext() {
		return currtoken_ < tokens_.length - 1;
	}

	public String getToken() {
		return tokens_[currtoken_];
	}

	public boolean highlightAsAnaphor() {
		while(i_ana_ < anaphorHighlight_.length && anaphorHighlight_[i_ana_] < currtoken_)
			i_ana_++;

		if(i_ana_ < anaphorHighlight_.length)
			return anaphorHighlight_[i_ana_] == currtoken_;
		else
			return false;
	}

	public boolean highlightAsAntecedent() {
		while(i_ant_ < antecedentHighlight_.length && antecedentHighlight_[i_ant_] < currtoken_)
			i_ant_++;

		if(i_ant_ < antecedentHighlight_.length)
			return antecedentHighlight_[i_ant_] == currtoken_;
		else
			return false;
	}
}
