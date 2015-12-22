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
	private Connection connection_;
	
	// id in the pro_examples table, uniquely identifying a
	// (srccorpus, tgtcorpus, example_no) combination
	private int example_id_;

	private int srccorpus_;
	private int tgtcorpus_;
	private int example_no_;

	private int firstLine_;
	private int lastLine_;

	private boolean loaded_;

	private List<int[]> anaphorSourceHighlight_;
	private List<int[]> anaphorTargetHighlight_;
	private List<int[]> antecedentSourceHighlight_;
	private List<int[]> antecedentTargetHighlight_;

	private List<String> sourceSentences_;
	private List<String> targetSentences_;

	private int currline_;

	private String antecedentAnnotation_;
	private String anaphorAnnotation_;
	private String remarks_;
	private List<boolean[]> approvedTokens_;

	public TestSuiteExample(Connection conn, int srccorpus, int tgtcorpus, int example_no) {
		connection_ = conn;
		srccorpus_ = srccorpus;
		tgtcorpus_ = tgtcorpus;
		example_no_ = example_no;
		loaded_ = false;
		currline_ = -1;
	}

	private void load() {
		if(loaded_)
			return;

		try {
			Position anaphorSource = retrieveAnaphorSourcePosition();
			List<Position> anaphorTarget = retrieveAnaphorTargetPositions();
			List<Position> antecedentSource = retrieveAntecedentSourcePositions();
			List<Position> antecedentTarget = retrieveAntecedentTargetPositions();

			firstLine_ = anaphorSource.getLine();
			lastLine_ = anaphorSource.getLine();
			for(Position p : antecedentSource) {
				if(p.getLine() < firstLine_)
					firstLine_ = p.getLine();
				if(p.getLine() > lastLine_)
					lastLine_ = p.getLine();
			}

			if(firstLine_ > 0)
				firstLine_--;

			sourceSentences_ = retrieveSentences(srccorpus_, firstLine_, lastLine_);
			targetSentences_ = retrieveSentences(tgtcorpus_, firstLine_, lastLine_);

			int nsent = lastLine_ - firstLine_ + 1;
			anaphorSourceHighlight_ = new ArrayList<int[]>(Collections.nCopies(nsent, new int[0]));
			anaphorTargetHighlight_ = new ArrayList<int[]>(Collections.nCopies(nsent, new int[0]));
			antecedentSourceHighlight_ = new ArrayList<int[]>(Collections.nCopies(nsent, new int[0]));
			antecedentTargetHighlight_ = new ArrayList<int[]>(Collections.nCopies(nsent, new int[0]));

			anaphorSourceHighlight_.set(anaphorSource.getLine() - firstLine_,
					new int[] { anaphorSource.getHead() });

			int[] buf = new int[100];

			if(!anaphorTarget.isEmpty()) {
				int line = anaphorTarget.get(0).getLine();
				int i = 0;
				for(Position p : anaphorTarget) {
					if(p.getLine() != line) {
						anaphorTargetHighlight_.set(line - firstLine_,
								Arrays.copyOf(buf, i));
						i = 0;
					}
					for(int j = p.getStart(); j <= p.getEnd(); j++)
						buf[i++] = j;
				}
				anaphorTargetHighlight_.set(line - firstLine_, Arrays.copyOf(buf, i));
			}

			if(!antecedentSource.isEmpty()) {
				int line = antecedentSource.get(0).getLine();
				int i = 0;
				for(Position p : antecedentSource) {
					if(p.getLine() != line) {
						antecedentSourceHighlight_.set(line - firstLine_,
								Arrays.copyOf(buf, i));
						i = 0;
					}
					for(int j = p.getStart(); j <= p.getEnd(); j++)
						buf[i++] = j;
				}
				antecedentSourceHighlight_.set(line - firstLine_, Arrays.copyOf(buf, i));
			}

			if(!antecedentTarget.isEmpty()) {
				int line = antecedentTarget.get(0).getLine();
				int i = 0;
				for(Position p : antecedentTarget) {
					if(p.getLine() != line) {
						antecedentTargetHighlight_.set(line - firstLine_,
								Arrays.copyOf(buf, i));
						i = 0;
					}
					for(int j = p.getStart(); j <= p.getEnd(); j++)
						buf[i++] = j;
				}
				antecedentTargetHighlight_.set(line - firstLine_, Arrays.copyOf(buf, i));
			}

			loadAnnotations();
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}

		loaded_ = true;
	}

	private Position retrieveAnaphorSourcePosition() throws SQLException {
		PreparedStatement stmt = connection_.prepareStatement(
			"select id, line, srcpos from pro_examples " +
			"where srccorpus=? and tgtcorpus=? and example_no=?");
		stmt.setInt(1, srccorpus_);
		stmt.setInt(2, tgtcorpus_);
		stmt.setInt(3, example_no_);
		ResultSet res = stmt.executeQuery();
		res.next();
		example_id_ = res.getInt("id");
		int line = res.getInt("line");
		int srcpos = res.getInt("srcpos");
		return new Position(line, srcpos, srcpos, srcpos);
	}

	private List<Position> retrieveAntecedentSourcePositions() throws SQLException {
		PreparedStatement stmt = connection_.prepareStatement(
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

	private List<Position> retrieveAnaphorTargetPositions() throws SQLException {
		PreparedStatement stmt = connection_.prepareStatement(
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

	private List<Position> retrieveAntecedentTargetPositions() throws SQLException {
		PreparedStatement stmt = connection_.prepareStatement(
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

	private List<String> retrieveSentences(int corpus, int minsnt, int maxsnt)
			throws SQLException {
		PreparedStatement stmt = connection_.prepareStatement(
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

	private void loadAnnotations() throws SQLException {
		PreparedStatement stmt = connection_.prepareStatement(
			"select ant_annotation, anaph_annotation, remarks from annotations where example=?");
		stmt.setInt(1, example_id_);
		ResultSet res = stmt.executeQuery();
		res.next();
		antecedentAnnotation_ = res.getString(1);
		anaphorAnnotation_ = res.getString(2);
		remarks_ = res.getString(3);
		if(res.next())
			System.err.println("Warning: Multiple annotation records for example ID " + example_id_);

		int nsent = lastLine_ - firstLine_ + 1;
		approvedTokens_ = new ArrayList<boolean[]>(nsent);
		for(int i = 0; i < nsent; i++) {
			String[] t = targetSentences_.get(i).split(" ");
			approvedTokens_.add(new boolean[t.length]);
		}

		stmt = connection_.prepareStatement(
			"select line, token, annotation from token_annotations where example=?");
		stmt.setInt(1, example_id_);
		res = stmt.executeQuery();
		while(res.next()) {
			int line = res.getInt(1) - firstLine_;
			int token = res.getInt(2);
			String annotation = res.getString(3);
			approvedTokens_.get(line)[token] = annotation.equals("ok");
		}
	}

	public void saveAnnotations() {
		try {
			connection_.setAutoCommit(false);

			PreparedStatement stmt = connection_.prepareStatement(
				"delete from annotations where example=?");
			stmt.setInt(1, example_id_);
			stmt.execute();

			stmt = connection_.prepareStatement(
				"insert into annotations(example, ant_annotation, anaph_annotation, remarks) " +
				"values (?, ?, ?, ?)");
			stmt.setInt(1, example_id_);
			stmt.setString(2, antecedentAnnotation_);
			stmt.setString(3, antecedentAnnotation_);
			stmt.setString(4, remarks_);
			stmt.execute();

			stmt = connection_.prepareStatement(
				"delete from token_annotations where example=?");
			stmt.setInt(1, example_id_);
			stmt.execute();

			stmt = connection_.prepareStatement(
				"insert into token_annotations (example, line, token, annotation) " +
				"values (?, ?, ?, ?)");
			stmt.setInt(1, example_id_);
			for(int i = 0; i < approvedTokens_.size(); i++)
				for(int j = 0; j < approvedTokens_.get(i).length; j++) {
					stmt.setInt(2, firstLine_ + i);
					stmt.setInt(3, j);
					stmt.setString(4, approvedTokens_.get(i)[j] ? "ok" : "bad");
					stmt.execute();
				}

			connection_.commit();
		} catch(SQLException e) {
			try {
				connection_.rollback();
			} catch(SQLException e2) {}
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void reset() {
		currline_ = -1;
	}

	public void next() {
		load();
		currline_++;
	}

	public boolean hasNext() {
		load();
		return currline_ < sourceSentences_.size() - 1;
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

	public String getAntecedentAnnotation() {
		return antecedentAnnotation_;
	}

	public void setAntecedentAnnotation(String annotation) {
		antecedentAnnotation_ = annotation;
	}

	public String getAnaphorAnnotation() {
		return anaphorAnnotation_;
	}

	public void setAnaphorAnnotation(String annotation) {
		anaphorAnnotation_ = annotation;
	}

	public String getRemarks() {
		return remarks_;
	}

	public void setRemarks(String remarks) {
		remarks_ = remarks;
	}

	public boolean isTokenApproved(int line, int token) {
		return approvedTokens_.get(line)[token];
	}

	public void setTokenApproved(int line, int token, boolean approved) {
		approvedTokens_.get(line)[token] = approved;
	}
}
