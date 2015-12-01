package protest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

public class TestSuiteExample {
	private int srccorpus_;
	private int tgtcorpus_;
	private int example_no_;

	private List<int[]> anaphorSourceHighlight_;
	private List<int[]> anaphorTargetHighlight_;
	private List<int[]> antecedentSourceHighlight_;
	private List<int[]> antecedentTargetHighlight_;

	private List<String> sourceSentences_;
	private List<String> targetSentences_;

	private int currline_;

	public TestSuiteExample(Connection conn, int srccorpus, int tgtcorpus, int example_no) 
			throws SQLException {
		srccorpus_ = srccorpus;
		tgtcorpus_ = tgtcorpus;
		example_no_ = example_no;

		Position anaphorSource = retrieveAnaphorSourcePosition(conn);
		List<Position> anaphorTarget = retrieveAnaphorTargetPositions(conn);
		List<Position> antecedentSource = retrieveAntecedentSourcePositions(conn);
		List<Position> antecedentTarget = retrieveAntecedentTargetPositions(conn);

		int firstLine = anaphorSource.getLine();
		int lastLine = anaphorSource.getLine();
		for(Position p : antecedentSource) {
			if(p.getLine() < firstLine)
				firstLine = p.getLine();
			if(p.getLine() > lastLine)
				lastLine = p.getLine();
		}

		if(firstLine > 0)
			firstLine--;

		sourceSentences_ = retrieveSentences(conn, srccorpus_, firstLine, lastLine);
		targetSentences_ = retrieveSentences(conn, tgtcorpus_, firstLine, lastLine);

		int nsent = lastLine - firstLine + 1;
		anaphorSourceHighlight_ = new ArrayList<int[]>(Collections.nCopies(nsent, new int[0]));
		anaphorTargetHighlight_ = new ArrayList<int[]>(Collections.nCopies(nsent, new int[0]));
		antecedentSourceHighlight_ = new ArrayList<int[]>(Collections.nCopies(nsent, new int[0]));
		antecedentTargetHighlight_ = new ArrayList<int[]>(Collections.nCopies(nsent, new int[0]));

		anaphorSourceHighlight_.set(anaphorSource.getLine() - firstLine,
				new int[] { anaphorSource.getHead() });

		int[] buf = new int[100];

		if(!anaphorTarget.isEmpty()) {
			int line = anaphorTarget.get(0).getLine();
			int i = 0;
			for(Position p : anaphorTarget) {
				if(p.getLine() != line) {
					anaphorTargetHighlight_.set(line - firstLine,
							Arrays.copyOf(buf, i));
					i = 0;
				}
				for(int j = p.getStart(); j <= p.getEnd(); j++)
					buf[i++] = j;
			}
			anaphorTargetHighlight_.set(line - firstLine, Arrays.copyOf(buf, i));
		}

		if(!antecedentSource.isEmpty()) {
			int line = antecedentSource.get(0).getLine();
			int i = 0;
			for(Position p : antecedentSource) {
				if(p.getLine() != line) {
					antecedentSourceHighlight_.set(line - firstLine,
							Arrays.copyOf(buf, i));
					i = 0;
				}
				for(int j = p.getStart(); j <= p.getEnd(); j++)
					buf[i++] = j;
			}
			antecedentSourceHighlight_.set(line - firstLine, Arrays.copyOf(buf, i));
		}

		if(!antecedentTarget.isEmpty()) {
			int line = antecedentTarget.get(0).getLine();
			int i = 0;
			for(Position p : antecedentTarget) {
				if(p.getLine() != line) {
					antecedentTargetHighlight_.set(line - firstLine,
							Arrays.copyOf(buf, i));
					i = 0;
				}
				for(int j = p.getStart(); j <= p.getEnd(); j++)
					buf[i++] = j;
			}
			antecedentTargetHighlight_.set(line - firstLine, Arrays.copyOf(buf, i));
		}
	}

	private Position retrieveAnaphorSourcePosition(Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
			"select line, srcpos from pro_examples " +
			"where srccorpus=? and tgtcorpus=? and example_no=?");
		stmt.setInt(1, srccorpus_);
		stmt.setInt(2, tgtcorpus_);
		stmt.setInt(3, example_no_);
		ResultSet res = stmt.executeQuery();
		res.next();
		int line = res.getInt("line");
		int srcpos = res.getInt("srcpos");
		return new Position(line, srcpos, srcpos, srcpos);
	}

	private List<Position> retrieveAntecedentSourcePositions(Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
			"select line, srcstartpos, srcheadpos, srcendpos from pro_antecedents " +
			"where srccorpus=? and tgtcorpus=? and example_no=? " +
			"order by line, srcstartpos, srcheadpos, srcendpos");
		stmt.setInt(1, srccorpus_);
		stmt.setInt(2, tgtcorpus_);
		stmt.setInt(3, example_no_);
		ResultSet res = stmt.executeQuery();
		ArrayList<Position> out = new ArrayList<Position>();
		while(res.next()) {
			int line = res.getInt("line");
			int srcstartpos = res.getInt("srcstartpos");
			int srcheadpos = res.getInt("srcheadpos");
			int srcendpos = res.getInt("srcendpos");
			out.add(new Position(line, srcstartpos, srcheadpos, srcendpos));
		}
		return out;
	}

	private List<Position> retrieveAnaphorTargetPositions(Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
			"select line, tgtpos from translations " +
			"where tgtcorpus=? and example_no=? and ant_no is null " +
			"order by line, tgtpos");
		stmt.setInt(1, tgtcorpus_);
		stmt.setInt(2, example_no_);
		ResultSet res = stmt.executeQuery();
		ArrayList<Position> out = new ArrayList<Position>();
		while(res.next()) {
			int line = res.getInt("line");
			int tgtpos = res.getInt("tgtpos");
			out.add(new Position(line, tgtpos, tgtpos, tgtpos));
		}
		return out;
	}

	private List<Position> retrieveAntecedentTargetPositions(Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
			"select line, tgtpos from translations " +
			"where tgtcorpus=? and example_no=? and ant_no is not null " +
			"order by line, tgtpos");
		stmt.setInt(1, tgtcorpus_);
		stmt.setInt(2, example_no_);
		ResultSet res = stmt.executeQuery();
		ArrayList<Position> out = new ArrayList<Position>();
		while(res.next()) {
			int line = res.getInt("line");
			int tgtpos = res.getInt("tgtpos");
			out.add(new Position(line, tgtpos, tgtpos, tgtpos));
		}
		return out;
	}

	private List<String> retrieveSentences(Connection conn, int corpus, int minsnt, int maxsnt)
			throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
			"select sentence from sentences where corpus=? and line between ? and ? order by line");
		stmt.setInt(1, corpus);
		stmt.setInt(2, minsnt);
		stmt.setInt(3, maxsnt);
		ResultSet res = stmt.executeQuery();
		ArrayList<String> out = new ArrayList<String>();
		while(res.next())
			out.add(res.getString(1));
		return out;
	}

	public void reset() {
		currline_ = -1;
	}

	public void next() {
		currline_++;
	}

	public boolean hasNext() {
		return currline_ < sourceSentences_.size();
	}

	public Sentence getSourceSentence() {
		return new Sentence(sourceSentences_.get(currline_),
				anaphorSourceHighlight_.get(currline_),
				antecedentSourceHighlight_.get(currline_));
	}

	public Sentence getTargetSentence() {
		return new Sentence(targetSentences_.get(currline_),
				anaphorTargetHighlight_.get(currline_),
				antecedentTargetHighlight_.get(currline_));
	}
}
