package protest.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

public class AnnotationRecord {
	private int candidate_id_;
	private int annotator_id_;

	private String antecedentAnnotation_ = "unset";
	private String anaphorAnnotation_ = "unset";
	private String remarks_ = "";
	private Set<String> tags_ = new TreeSet<String>();
	private HashMap<Position,String> approvedTokens_ = new HashMap<Position,String>();

	private boolean dirty_ = false;

	public AnnotationRecord(int candidate_id, int annotator_id) {
		candidate_id_ = candidate_id;
		annotator_id_ = annotator_id;
	}

	public String getAntecedentAnnotation() {
		return antecedentAnnotation_;
	}

	public void setAntecedentAnnotation(String annotation) {
		if(!antecedentAnnotation_.equals(annotation)) {
			antecedentAnnotation_ = annotation;
			dirty_ = true;
		}
	}

	public String getAnaphorAnnotation() {
		return anaphorAnnotation_;
	}

	public void setAnaphorAnnotation(String annotation) {
		if(!anaphorAnnotation_.equals(annotation)) {
			anaphorAnnotation_ = annotation;
			dirty_ = true;
		}
	}

	public String getRemarks() {
		return remarks_;
	}

	public void setRemarks(String remarks) {
		if(!remarks_.equals(remarks)) {
			remarks_ = remarks;
			dirty_ = true;
		}
	}

	public String getTokenApproval(int line, int token) {
		String a = approvedTokens_.get(new Position(line, token));
		return a == null ? "" : a;
	}

	public Collection<Position> getApprovedPositions() {
		return Collections.immutableCollection(approvedTokens_.keySet());
	}

	public void setTokenApproval(int line, int token, String approved) {
		String a = getTokenApproval(line, token);
		if(a != null && !a.equals(approved)) {
			approvedTokens_.put(new Position(line, token), approved);
			dirty_ = true;
		}
	}
	
	public Set<String> getTags() {
		return Collections.immutableSet(tags_);
	}

	public void addTag(String tag) {
		tags_.add(tag);
	}

	public void removeTag(String tag) {
		tags_.remove(tag);
	}
}
