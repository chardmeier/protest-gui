package protest.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AnnotationRecord implements Comparable<AnnotationRecord> {
	private TestSuiteExample example_;

	private int candidate_id_;
	private int annotator_id_;

	private String antecedentAnnotation_ = "unset";
	private String anaphorAnnotation_ = "unset";
	private String remarks_ = "";
	private TreeSet<String> tags_ = new TreeSet<String>();
	private HashMap<Position,String> approvedTokens_ = new HashMap<Position,String>();

	private ConflictStatus conflictStatus_ = null;
	private boolean dirty_ = false;

	public AnnotationRecord(TestSuiteExample example, int candidate_id, int annotator_id) {
		example_ = example;
		candidate_id_ = candidate_id;
		annotator_id_ = annotator_id;
	}

	private void setDirty() {
		dirty_ = true;
		conflictStatus_ = null;
	}

	protected void resetDirty() {
		dirty_ = false;
		conflictStatus_ = null;
	}

	public boolean needsSaving() {
		return dirty_;
	}

	public TestSuiteExample getExample() {
		return example_;
	}

	public int getAnnotatorID() {
		return annotator_id_;
	}

	public String getAnnotatorName() {
		return example_.getDatabase().getAnnotatorName(annotator_id_);
	}

	public String getAntecedentAnnotation() {
		return antecedentAnnotation_;
	}

	public void setAntecedentAnnotation(String annotation) {
		if(!antecedentAnnotation_.equals(annotation)) {
			antecedentAnnotation_ = annotation;
			setDirty();
		}
	}

	public String getAnaphorAnnotation() {
		return anaphorAnnotation_;
	}

	public void setAnaphorAnnotation(String annotation) {
		if(!anaphorAnnotation_.equals(annotation)) {
			anaphorAnnotation_ = annotation;
			setDirty();
		}
	}

	public String getRemarks() {
		return remarks_;
	}

	public void setRemarks(String remarks) {
		if(!remarks_.equals(remarks)) {
			remarks_ = remarks;
			setDirty();
		}
	}

	public String getTokenApproval(int line, int token) {
		String a = approvedTokens_.get(new Position(line, token));
		return a == null ? "" : a;
	}

	public Collection<Position> getApprovedPositions() {
		return Collections.unmodifiableCollection(approvedTokens_.keySet());
	}

	public void setTokenApproval(int line, int token, String approved) {
		String a = getTokenApproval(line, token);
		if(a != null && !a.equals(approved)) {
			approvedTokens_.put(new Position(line, token), approved);
			setDirty();
		}
	}
	
	public Set<String> getTags() {
		return Collections.unmodifiableSet(tags_);
	}

	public void addTag(String tag) {
		tags_.add(tag);
	}

	public void removeTag(String tag) {
		tags_.remove(tag);
	}
	
	private boolean checkIfTokensAnnotated(Collection<Position> all, Collection<Position> approved) {
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
	
	public ConflictStatus getConflictStatus() {
		if(conflictStatus_ == null) {
			int anaphConflictType = ConflictStatus.NO_CONFLICT;
			int antConflictType = ConflictStatus.NO_CONFLICT;

			String anaph_annotation = getAnaphorAnnotation();
			String ant_annotation = getAntecedentAnnotation();

			Collection<Position> antecedentTarget = example_.getAntecedentTargetPositions();
			Collection<Position> anaphorTarget = example_.getAnaphorTargetPositions();
			Collection<Position> approved = getApprovedPositions();

			boolean antTokAnnotated = checkIfTokensAnnotated(antecedentTarget, approved);
			boolean anaphTokAnnotated = checkIfTokensAnnotated(anaphorTarget, approved);

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

			conflictStatus_ = new ConflictStatus(anaphConflictType, antConflictType);
		}

		return conflictStatus_;
	}

	public void saveAnnotations() {
		if(!needsSaving())
			return;

		String conflictStatus = getConflictStatus().encode();

		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = example_.getDatabase().getConnection();
			conn.setAutoCommit(false);

			stmt = conn.prepareStatement(
				"delete from annotations where candidate=? and annotator_id=?");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator_id_);
			stmt.execute();
			
			stmt.close();
			stmt = conn.prepareStatement(
				"delete from token_annotations where candidate=? and annotator_id=?");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator_id_);
			stmt.execute();
			
			stmt.close();
			stmt = conn.prepareStatement(
				"delete from tag_annotations where candidate=? and annotator_id=?");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator_id_);
			stmt.execute();

			stmt.close();
			stmt = conn.prepareStatement(
				"insert into tag_annotations (candidate, annotator_id, tag) " +
				"values (?, ?, ?)");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator_id_);
			for(String tag : tags_) {
				stmt.setString(3, tag);
				stmt.execute();
			}
			
			stmt.close();
			stmt = conn.prepareStatement(
				"insert into token_annotations (candidate, annotator_id, line, token, annotation) " +
				"values (?, ?, ?, ?, ?)");
			stmt.setInt(1, candidate_id_);
			stmt.setInt(2, annotator_id_);
			for(Map.Entry<Position,String> e : approvedTokens_.entrySet()) {
				Position pos = e.getKey();
				String annot = e.getValue();
				stmt.setInt(3, example_.getFirstLine() + pos.getLine());
				stmt.setInt(4, pos.getStart());
				stmt.setString(5, annot);
				stmt.execute();
			}

			if(antecedentAnnotation_.equals("unset") && anaphorAnnotation_.equals("unset") &&
				   tags_.isEmpty() && remarks_.isEmpty() && approvedTokens_.isEmpty()) {
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
			stmt.setInt(5, annotator_id_);
			stmt.setString(6, conflictStatus);
			stmt.execute();

			conn.commit();
			dirty_ = false;
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

		example_.getDatabase().fireDatabaseUpdate(this);
	}

	public int compareTo(AnnotationRecord o) {
		if(candidate_id_ != o.candidate_id_)
			return candidate_id_ < o.candidate_id_ ? -1 : 1;
		else if(annotator_id_ != o.annotator_id_)
			return annotator_id_ < o.annotator_id_ ? -1 : 1;
		else
			return 0;
	}
}
