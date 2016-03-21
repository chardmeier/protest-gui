package protest.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
	private boolean dirty_;

	private Position anaphorSourcePosition_;
	private List<Position> anaphorTargetPositions_;
	private List<Position> antecedentSourcePositions_;
	private List<Position> antecedentTargetPositions_;

	private List<int[]> anaphorSourceHighlight_;
	private List<int[]> anaphorTargetHighlight_;
	private List<int[]> antecedentSourceHighlight_;
	private List<int[]> antecedentTargetHighlight_;

	private List<String> sourceSentences_;
	private List<String> targetSentences_;

	private HashMap<Integer,AnnotationRecord> annotationRecords_;

	private int currline_;

	public TestSuiteExample(Database db, int srccorpus, int tgtcorpus, int example_no) {
		db_ = db;
		srccorpus_ = srccorpus;
		tgtcorpus_ = tgtcorpus;
		example_no_ = example_no;
		loaded_ = false;
		dirty_ = false;
		currline_ = -1;
	}

	public Database getDatabase() {
		return db_;
	}

	private void load() {
		if(loaded_)
			return;

		// set loaded_ to true now to avoid infinite loop when called during the loading process
		loaded_ = true;

		try {
			anaphorSourcePosition_ = retrieveAnaphorSourcePosition();
			anaphorTargetPositions_ = retrieveAnaphorTargetPositions();
			antecedentSourcePositions_ = retrieveAntecedentSourcePositions();
			antecedentTargetPositions_ = retrieveAntecedentTargetPositions();

			firstLine_ = anaphorSourcePosition_.getLine();
			lastLine_ = anaphorSourcePosition_.getLine();
			for(Position p : antecedentSourcePositions_) {
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
			
			anaphorSourceHighlight_.set(anaphorSourcePosition_.getLine() - firstLine_,
					new int[] { anaphorSourcePosition_.getStart() });
			

			int[] buf = new int[100];

			if(!anaphorTargetPositions_.isEmpty()) {
				int line = anaphorTargetPositions_.get(0).getLine();
				int i = 0;
				for(Position p : anaphorTargetPositions_) {
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

			if(!antecedentSourcePositions_.isEmpty()) {
				int line = antecedentSourcePositions_.get(0).getLine();
				int i = 0;
				for(Position p : antecedentSourcePositions_) {
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

			if(!antecedentTargetPositions_.isEmpty()) {
				int line = antecedentTargetPositions_.get(0).getLine();
				int i = 0;
				for(Position p : antecedentTargetPositions_) {
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

		dirty_ = false;
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
		return new Position(line, srcpos);
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
				out.add(new Position(line, tgtpos));
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
				out.add(new Position(line, tgtpos));
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

	public boolean needsSaving() {
		return dirty_;
	}

	private void loadAnnotations() throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;

		annotationRecords_ = new HashMap<Integer,AnnotationRecord>();

		try {
			conn = db_.getConnection();
			stmt = conn.prepareStatement(
				"select annotator_id, ant_annotation, anaph_annotation, remarks from annotations " +
				"where candidate=? order by annotator_id");
			stmt.setInt(1, candidate_id_);
			ResultSet res = stmt.executeQuery();
			while(res.next()) {
				int annotator_id = res.getInt("annotator_id");
				AnnotationRecord rec = getAnnotationRecord(annotator_id);
				rec.setAntecedentAnnotation(res.getString("ant_annotation"));
				rec.setAnaphorAnnotation(res.getString("anaph_annotation"));
				rec.setRemarks(res.getString("remarks"));
			}
			
			stmt.close();
			stmt = conn.prepareStatement(
				"select annotator_id, tag from tag_annotations where candidate=? order by annotator_id, tag");
			stmt.setInt(1, candidate_id_);
			res = stmt.executeQuery();
			while(res.next()) {
				int annotator_id = res.getInt("annotator_id");
				AnnotationRecord rec = getAnnotationRecord(annotator_id);
				rec.addTag(res.getString("tag"));
			}

			stmt.close();
			stmt = conn.prepareStatement(
				"select annotator_id, line, token, annotation from token_annotations where candidate=?");
			stmt.setInt(1, candidate_id_);
			res = stmt.executeQuery();
			while(res.next()) {
				int annotator_id = res.getInt("annotator_id");
				AnnotationRecord rec = getAnnotationRecord(annotator_id);
				int line = res.getInt("line") - firstLine_;
				int token = res.getInt("token");
				rec.setTokenApproval(line, token, res.getString("annotation"));
			}

			for(AnnotationRecord rec : annotationRecords_.values())
				rec.resetDirty();
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

	public int getFirstLine() {
		return firstLine_;
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

	public List<Position> getAnaphorTargetPositions() {
		return Collections.unmodifiableList(anaphorTargetPositions_);
	}

	public List<Position> getAntecedentTargetPositions() {
		return Collections.unmodifiableList(antecedentTargetPositions_);
	}

	public AnnotationRecord getAnnotationRecord(int annotator_id) {
		load();

		AnnotationRecord rec = annotationRecords_.get(Integer.valueOf(annotator_id));
		if(rec == null) {
			rec = new AnnotationRecord(this, candidate_id_, annotator_id);
			annotationRecords_.put(Integer.valueOf(annotator_id), rec);
		}
		return rec;
	}

	public List<AnnotationRecord> getAnnotationRecords() {
		load();

		ArrayList<AnnotationRecord> out = new ArrayList<AnnotationRecord>(annotationRecords_.values());
		Collections.sort(out);
		return out;
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
}
