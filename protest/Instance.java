package ch.rax.pviewer;

import java.io.Serializable;
import java.util.ArrayList;

import gnu.trove.TIntIntHashMap;

public class Instance implements Serializable {
	private String ngram_;
	private String target_;
	private ArrayList<String> antecedents_ = new ArrayList<String>();
	private int ana_start_, ana_end_;
	private int first_src_ = Integer.MAX_VALUE, last_src_ = Integer.MIN_VALUE;
	private int first_tgt_ = Integer.MAX_VALUE, last_tgt_ = Integer.MIN_VALUE;
	private TIntIntHashMap srcLabels_ = new TIntIntHashMap();
	private TIntIntHashMap tgtLabels_ = new TIntIntHashMap();

	private double[] predOutput_;
	private double[] predAnaphora_;
	private double[] solution_;
	private int correctClass_;

	private String docid_;
	private int docline_;

	public Instance(String csv) {
		// CSV fields:
		//  0 docid
		//  1 line (in doc)
		//  2 srcpos
		//  3 srcpronoun
		//  4 srccorpus
		//  5 tgtcorpus
		//  6 example_no
		//  7 line (global)
		//  8 srcpos (repeated)
		//  9 tgtpos_from
		// 10 tgtpos_to
		// 11 tgtpronoun
		// 12 srcproindex
		// 13 category
		// 14 approved_no
		// 15 selected

		String[] f = csv.split("\\|");

		docid_ = f[0];

		int gline = Integer.parseInt(f[7]);
	}



	public void setAnaphoraPosition(AlignedCorpus c, int start, int end) {
		ana_start_ = start;
		ana_end_ = end;
		if(start < first_src_)
			first_src_ = start;
		if(end > last_src_)
			last_src_ = end;
		for(int i = start; i <= end; i++) {
			srcLabels_.put(i, -1);
			int[] ids = c.getSource().getAlignedIDs(i);
			for(int j = 0; j < ids.length; j++) {
				if(ids[j] < first_tgt_)
					first_tgt_ = ids[j];
				if(ids[j] > last_tgt_)
					last_tgt_ = ids[j];
				tgtLabels_.put(ids[j], -1);
			}
		}
	}

	public void setContextNgram(String ng) {
		ngram_ = ng;
	}

	public String getContextNgram() {
		return ngram_;
	}

	public void setTarget(String t) {
		target_ = t;
	}

	public String getTarget() {
		return target_;
	}

	public void addAntecedent(AlignedCorpus c, String ant, int start, int end) {
		int antid = antecedents_.size() + 1;
		antecedents_.add(ant);
		if(start < first_src_)
			first_src_ = start;
		if(end > last_src_)
			last_src_ = end;
		for(int i = start; i <= end; i++) {
			srcLabels_.put(i, antid);
			int[] ids = c.getSource().getAlignedIDs(i);
			for(int j = 0; j < ids.length; j++) {
				if(ids[j] < first_tgt_)
					first_tgt_ = ids[j];
				if(ids[j] > last_tgt_)
					last_tgt_ = ids[j];
				tgtLabels_.put(ids[j], antid);
			}
		}
	}

	public String[] getAntecedents() {
		return antecedents_.toArray(new String[0]);
	}

	public int getFirstSourceIndex() {
		return first_src_;
	}

	public int getLastSourceIndex() {
		return last_src_;
	}

	public int getFirstTargetIndex() {
		return first_tgt_;
	}

	public int getLastTargetIndex() {
		return last_tgt_;
	}

	public int getSourceWordLabel(int position) {
		return srcLabels_.get(position);
	}

	public int getTargetWordLabel(int position) {
		return tgtLabels_.get(position);
	}

	public void setSolution(double[] prob) {
		solution_ = prob;
		double maxprob = Double.NEGATIVE_INFINITY;
		correctClass_ = -1;
		for(int i = 0; i < prob.length; i++)
			if(prob[i] > maxprob) {
				maxprob = prob[i];
				correctClass_ = i;
			}
	}

	public double[] getSolution() {
		return solution_;
	}

	public int getCorrectClass() {
		return correctClass_;
	}

	public void setPredictedOutput(double[] prob) {
		predOutput_ = prob;
	}

	public double[] getPredictedOutput() {
		return predOutput_;
	}

	public void setPredictedAnaphoraProbs(double[] prob) {
		predAnaphora_ = prob;
	}

	public double[] getPredictedAnaphoraProbs() {
		return predAnaphora_;
	}
}
