package protest.db;

import java.util.Arrays;
import java.util.List;

public class ConflictStatus {
	public static final int NO_CONFLICT = 0;
	public static final int OK_BUT_NO_TOKENS = 1;
	public static final int TOKENS_BUT_NOT_OK = 2;
	public static final int TAGS_BUT_UNSET = 3;
	public static final int BAD_TRANSLATION_BUT_ANNOTATED = 4;
	public static final int INCORRECT_ALIGNMENT_BUT_ANNOTATED = 5;
	public static final int NONCOMPOSITIONAL_BUT_TOKENS_HIGHLIGHTED = 6;
	public static final int CONFLICTING_TAGS = 7;

	private static final List<String> ANAPH_TYPES =
		Arrays.asList("ana_ok", "ana_notokens", "ana_unset", "ana_tagsonly",
				"ana_bad_translation_annot",
				"ana_incorrect_alignment_annot",
				"ana_noncompositional_highlight",
				"ana_conflicting_tags");
	private static final List<String> ANT_TYPES =
		Arrays.asList("ant_ok", "ant_notokens", "ant_unset");

	private int anaphConflict_;
	private int antConflict_;

	public ConflictStatus(int anaph, int ant) {
		anaphConflict_ = anaph;
		antConflict_ = ant;
	}

	public ConflictStatus(String encoding) {
		String[] s = encoding.split(" ");
		if(s.length != 2)
			throw new IllegalArgumentException("Invalid conflict encoding: " + encoding);

		anaphConflict_ = ANAPH_TYPES.indexOf(s[0]);
		if(anaphConflict_ == -1)
			throw new IllegalArgumentException("Invalid anaphor conflict type: " + s[0]);

		antConflict_ = ANT_TYPES.indexOf(s[1]);
		if(antConflict_ == -1)
			throw new IllegalArgumentException("Invalid antecedent conflict type: " + s[1]);
	}

	public String explain() {
		String conflictMessage = "";
		if(anaphConflict_ == 0 && antConflict_ == 0)
			return conflictMessage;

		switch(anaphConflict_) {
			case NO_CONFLICT:
				break;
			case OK_BUT_NO_TOKENS:
				conflictMessage += "PRONOUN: Pronoun translation marked as OK, but no tokens selected.\n";
				break;
			case TOKENS_BUT_NOT_OK:
				conflictMessage += "PRONOUN: Tokens selected, but pronoun translation not marked as OK.\n";
				break;
			case TAGS_BUT_UNSET:
				conflictMessage += "PRONOUN: Tags assigned, but pronoun translation still unset.\n";
				break;
			case BAD_TRANSLATION_BUT_ANNOTATED:
				conflictMessage += "Assigned bad_translation tag, but annotated example.\n";
				break;
			case INCORRECT_ALIGNMENT_BUT_ANNOTATED:
				conflictMessage += "Assigned incorrect_word_alignment tag, but annotated example.\n";
				break;
			case NONCOMPOSITIONAL_BUT_TOKENS_HIGHLIGHTED:
				conflictMessage += "Assigned noncompositional_translation tag, but highlighted tokens.\n";
				break;
			case CONFLICTING_TAGS:
				conflictMessage += "Conflicting tags assigned.\n";
				break;
			default:
				conflictMessage += "PRONOUN: Conflicting annotations.\n";
				break;
		}
		switch(antConflict_) {
			case NO_CONFLICT:
				break;
			case OK_BUT_NO_TOKENS:
				conflictMessage += "ANTECEDENT: Antecedent head translation marked as OK, but no tokens selected.\n";
				break;
			case TOKENS_BUT_NOT_OK:
				conflictMessage += "ANTECEDENT: Tokens selected, but antecedent head translation not marked as OK.\n";
				break;
			default:
				conflictMessage += "ANTECEDENT: Conflicting annotations.\n";
				break;
		}

		return conflictMessage;
	}

	public String encode() {
		return String.format("%s %s", ANAPH_TYPES.get(anaphConflict_), ANT_TYPES.get(antConflict_));
	}

	public String toString() {
		return encode();
	}

	public boolean hasConflict() {
		return anaphConflict_ != NO_CONFLICT || antConflict_ != NO_CONFLICT;
	}
}
