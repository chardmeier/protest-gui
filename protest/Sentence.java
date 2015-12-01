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

	private void next() {
		currtoken_++;
	}

	private boolean hasNext() {
		return currtoken_ < tokens_.length;
	}

	private String getToken() {
		return tokens_[currtoken_];
	}

	private boolean highlightAsAnaphor() {
		while(i_ana_ < anaphorHighlight_.length && anaphorHighlight_[i_ana_] < currtoken_)
			i_ana_++;

		if(i_ana_ < anaphorHighlight_.length)
			return anaphorHighlight_[i_ana_] == currtoken_;
		else
			return false;
	}

	private boolean highlightAsAntecedent() {
		while(i_ant_ < antecedentHighlight_.length && antecedentHighlight_[i_ant_] < currtoken_)
			i_ant_++;

		if(i_ant_ < antecedentHighlight_.length)
			return antecedentHighlight_[i_ant_] == currtoken_;
		else
			return false;
	}
}
