package protest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TestSuiteExample {
	private Database db_;
	
	// id in the pro_candidates table, uniquely identifying a
	// (srccorpus, tgtcorpus, example_no) combination
	private int candidate_id_;

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
	private Set<String> tags_;
	private List<String[]> approvedTokens_;

	public TestSuiteExample(Database db, int srccorpus, int tgtcorpus, int example_no) {
		db_ = db;
		srccorpus_ = srccorpus;
		tgtcorpus_ = tgtcorpus;
		example_no_ = example_no;
		loaded_ = false;
		currline_ = -1;
	}

	public Database getDatabase() {
		return db_;
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
					new int[] { anaphorSource.getStart() });
			

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
		int line = -1;
		int srcpos = -1;

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = db_.getConnection();
			stmt = conn.prepareStatement(
				"select id, line, srcpos from pro_candidates " +
				"where srccorpus=? and tgtcorpus=? and example_no=?");
			stmt.setInt(1, srccorpus_);
			stmt.setInt(2, tgtcorpus_);
			stmt.setInt(3, example_no_);
			ResultSet res = stmt.executeQuery();
			res.next();
			candidate_id_ = res.getInt("id");
			line = res.getInt("line");
			srcpos = res.getInt("srcpos");
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}
		return new Position(line, srcpos, srcpos);
	}

	private List<Position> retrieveAntecedentSourcePositions() throws SQLException {
		ArrayList<Position> out = new ArrayList<Position>();

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = db_.getConnection();
			stmt = conn.prepareStatement(
				"select line, srcheadpos from pro_antecedents " +
				"where srccorpus=? and tgtcorpus=? and example_no=? " +
				"order by line, srcheadpos");
			stmt.setInt(1, srccorpus_);
			stmt.setInt(2, tgtcorpus_);
			stmt.setInt(3, example_no_);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				int line = res.getInt("line");
				int srcstartpos = res.getInt("srcheadpos");
				int srcendpos = res.getInt("srcheadpos");
				out.add(new Position(line, srcstartpos, srcendpos));
			}
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}
		return out;
	}

	private List<Position> retrieveAnaphorTargetPositions() throws SQLException {
		ArrayList<Position> out = new ArrayList<Position>();

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = db_.getConnection();
			stmt = conn.prepareStatement(
				"select line, tgtpos from translations " +
				"where tgtcorpus=? and example_no=? and ant_no is null " +
				"order by line, tgtpos");
			stmt.setInt(1, tgtcorpus_);
			stmt.setInt(2, example_no_);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				int line = res.getInt("line");
				int tgtpos = res.getInt("tgtpos");
				out.add(new Position(line, tgtpos, tgtpos));
			}
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

		return out;
	}

	private List<Position> retrieveAntecedentTargetPositions() throws SQLException {
		ArrayList<Position> out = new ArrayList<Position>();

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = db_.getConnection();
			stmt = conn.prepareStatement(
				"select line, tgtpos from translations " +
				"where tgtcorpus=? and example_no=? and ant_no is not null " +
				"order by line, tgtpos");
			stmt.setInt(1, tgtcorpus_);
			stmt.setInt(2, example_no_);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				int line = res.getInt("line");
				int tgtpos = res.getInt("tgtpos");
				out.add(new Position(line, tgtpos, tgtpos));
			}
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

		return out;
	}

	private List<String> retrieveSentences(int corpus, int minsnt, int maxsnt)
			throws SQLException {
		ArrayList<String> out = new ArrayList<String>();

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = db_.getConnection();
			stmt = conn.prepareStatement(
				"select sentence from sentences where corpus=? and line between ? and ? order by line");
			stmt.setInt(1, corpus);
			stmt.setInt(2, minsnt);
			stmt.setInt(3, maxsnt);
			ResultSet res = stmt.executeQuery();
			while(res.next())
				out.add(res.getString(1));
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

		return out;
	}

	private void loadAnnotations() throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = db_.getConnection();
			stmt = conn.prepareStatement(
				"select ant_annotation, anaph_annotation, remarks from annotations where candidate=?");
			stmt.setInt(1, candidate_id_);
			ResultSet res = stmt.executeQuery();
			if(res.next()) {
				antecedentAnnotation_ = res.getString(1);
				anaphorAnnotation_ = res.getString(2);
				remarks_ = res.getString(3);
				if(res.next())
					System.err.println("Warning: Multiple annotation records for candidate ID " + candidate_id_);
			} else {
				antecedentAnnotation_ = "unset";
				anaphorAnnotation_ = "unset";
				remarks_ = "";
			}

			stmt.close();
			stmt = conn.prepareStatement(
				"select tag from tag_annotations where candidate=? order by tag");
			stmt.setInt(1, candidate_id_);
			res = stmt.executeQuery();
			tags_ = new TreeSet<String>();
			while(res.next())
				tags_.add(res.getString("tag"));

			int nsent = lastLine_ - firstLine_ + 1;
			approvedTokens_ = new ArrayList<String[]>(nsent);
			for(int i = 0; i < nsent; i++) {
				String[] t = targetSentences_.get(i).split(" ");
				approvedTokens_.add(new String[t.length]);
			}

			stmt.close();
			stmt = conn.prepareStatement(
				"select line, token, annotation from token_annotations where candidate=?");
			stmt.setInt(1, candidate_id_);
			res = stmt.executeQuery();
			while(res.next()) {
				int line = res.getInt(1) - firstLine_;
				int token = res.getInt(2);
				approvedTokens_.get(line)[token] = res.getString(3);
			}
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

	}

	public void saveAnnotations(int annotator, String conflictStatus) {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = db_.getConnection();
			conn.setAutoCommit(false);

			stmt = conn.prepareStatement(
				"delete from annotations where candidate=? and annotator_id=?");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator);
			stmt.execute();
			
			stmt.close();
			stmt = conn.prepareStatement(
				"delete from token_annotations where candidate=? and annotator_id=?");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator);
			stmt.execute();
			
			stmt.close();
			stmt = conn.prepareStatement(
				"delete from tag_annotations where candidate=? and annotator_id=?");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator);
			stmt.execute();

			stmt.close();
			stmt = conn.prepareStatement(
				"insert into tag_annotations (candidate, annotator_id, tag) " +
				"values (?, ?, ?)");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator);
			for(String tag : tags_) {
				stmt.setString(3, tag);
				stmt.execute();
			}
			
			boolean hasTokenAnnotations = false;
			
			stmt.close();
			stmt = conn.prepareStatement(
				"insert into token_annotations (candidate, annotator_id, line, token, annotation) " +
				"values (?, ?, ?, ?, ?)");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator);
			for(int i = 0; i < approvedTokens_.size(); i++)
				for(int j = 0; j < approvedTokens_.get(i).length; j++) {
					String app = approvedTokens_.get(i)[j];
					if(app == null || app.isEmpty())
						continue;
					hasTokenAnnotations = true;
					stmt.setInt(3, firstLine_ + i);
					stmt.setInt(4, j);
					stmt.setString(5, approvedTokens_.get(i)[j]);
					stmt.execute();
				}

			if(antecedentAnnotation_.equals("unset") && anaphorAnnotation_.equals("unset") &&
				   tags_.isEmpty() && remarks_.isEmpty() && !hasTokenAnnotations) {
				conn.commit();
				return;
			}
			
			stmt.close();
			stmt = conn.prepareStatement(
				"insert into annotations (candidate, ant_annotation, anaph_annotation, remarks, annotator_id, conflict_status) " +
				"values (?, ?, ?, ?, ?, ?)");
			stmt.setInt(1, candidate_id_);
			stmt.setString(2, antecedentAnnotation_);
			stmt.setString(3, anaphorAnnotation_);
			stmt.setString(4, remarks_);
			stmt.setInt(5, annotator);
			stmt.setString(6, conflictStatus);
			stmt.execute();


			conn.commit();
		} catch(SQLException e) {
			if(conn != null) {
				try {
					conn.rollback();
				} catch(SQLException e2) {}
			}
			e.printStackTrace();
			System.exit(1);
		} finally {
			Database.close(stmt);
			Database.close(conn);
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

	public int getIndex() {
		return currline_;
	}

	public String getCandidateLocator() {
		return String.format("%d - %d - %d", srccorpus_, tgtcorpus_, example_no_);
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

	public String getTokenApproval(int line, int token) {
		String a = approvedTokens_.get(line)[token];
		return a == null ? "" : a;
	}

	public void setTokenApproval(int line, int token, String approved) {
		approvedTokens_.get(line)[token] = approved;
	}
	
	public Set<String> getTags() {
		return tags_;
	}

	public void addTag(String tag) {
		tags_.add(tag);
	}

	public void removeTag(String tag) {
		tags_.remove(tag);
	}

	//Return True if pronoun example category requires antecedent agreement
	public boolean getAntecedentAgreementRequired() {
		boolean agree = false;

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = db_.getConnection();
			stmt = conn.prepareStatement(
				"select c.antagreement from categories as c " +
				"left join pro_candidates as pe on pe.category_no = c.id " +
				"where pe.srccorpus=? and pe.tgtcorpus=? and pe.example_no=?");
			stmt.setInt(1, srccorpus_);
			stmt.setInt(2, tgtcorpus_);
			stmt.setInt(3, example_no_);
			ResultSet res = stmt.executeQuery();
			res.next();
			// Check if antecedent agreement is required
			if (res.getInt("antagreement") == 1){
				agree = true;
			}
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			Database.close(stmt);
			Database.close(conn);
		}

		return agree;
	}
	
	private List<Position> retrieveApprovedPositions() {
		ArrayList<Position> out = new ArrayList<Position>();
		int lineNo;
		int tokNo;
		for(int i = 0; i < approvedTokens_.size(); i++)
			for(int j = 0; j < approvedTokens_.get(i).length; j++) {
				String app = approvedTokens_.get(i)[j];
				if(app == null || app.isEmpty())
					continue;
				lineNo = firstLine_ + i;
				tokNo = j;
				out.add(new Position(lineNo, tokNo, tokNo));
			}
		return out;
	}
	
	private boolean checkIfTokensAnnotated(List<Position> all, List<Position> approved) {
		boolean result = false;
		for(Position p : all) {
			for(Position q : approved) {
				if ((p.getLine() == q.getLine()) && (p.getStart() == q.getStart()) && (p.getEnd() == q.getEnd())){
					result = true;
				}
			}
		}
		return result;
	}
	
	public ConflictStatus checkAnnotationConflict() {
		int anaphConflictType = ConflictStatus.NO_CONFLICT;
		int antConflictType = ConflictStatus.NO_CONFLICT;
		try {
			String anaph_annotation = getAnaphorAnnotation();
			String ant_annotation = getAntecedentAnnotation();
			// Get positions of anaphor and antecedent in target
			List<Position> anaphorTarget = retrieveAnaphorTargetPositions();
			List<Position> antecedentTarget = retrieveAntecedentTargetPositions();
			// Get positions of highlighted tokens from: approvedTokens_
			List<Position> approved = retrieveApprovedPositions();
			// Compare
			boolean antTokAnnotated = checkIfTokensAnnotated(antecedentTarget,approved);
			boolean anaphTokAnnotated = checkIfTokensAnnotated(anaphorTarget,approved);

			// Pronoun annotation conflicts
			if(anaph_annotation.equals("ok") && anaphTokAnnotated == false && !anaphorTarget.isEmpty())
				anaphConflictType = ConflictStatus.OK_BUT_NO_TOKENS;
			else if (anaph_annotation.equals("unset") && !tags_.isEmpty())
				anaphConflictType = ConflictStatus.TAGS_BUT_UNSET;
			else if (!anaph_annotation.equals("ok") && anaphTokAnnotated == true)
				anaphConflictType = ConflictStatus.TOKENS_BUT_NOT_OK;

			// Antecedent annotation conflicts
			if (ant_annotation.equals("ok") && antTokAnnotated == false && !antecedentTarget.isEmpty())
				antConflictType = ConflictStatus.OK_BUT_NO_TOKENS;
			else if (!ant_annotation.equals("ok") && antTokAnnotated == true)
				antConflictType = ConflictStatus.TOKENS_BUT_NOT_OK;
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return new ConflictStatus(anaphConflictType, antConflictType);
	}
}
