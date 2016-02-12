package protest;

public class PrecheckReport {
	private boolean error_ = false;
	private String message_ = null;
	private int instanceDuplicates_;
	private int tokenDuplicates_;

	public void setError(String msg) {
		error_ = true;
		message_ = msg;
	}

	public void setInstanceDuplicates(int cnt) {
		instanceDuplicates_ = cnt;
	}

	public void setTokenDuplicates(int cnt) {
		tokenDuplicates_ = cnt;
	}

	public boolean canImport() {
		return !error_;
	}

	public boolean shouldWarn() {
		return instanceDuplicates_ > 0 || tokenDuplicates_ > 0;
	}

	public String getMessage() {
		if(message_ != null)
			return message_;
		else
			return String.format("There are %d instance-level duplicates and %d token-level duplicates.",
					instanceDuplicates_, tokenDuplicates_);
	}
}
