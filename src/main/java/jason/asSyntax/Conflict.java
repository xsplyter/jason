package jason.asSyntax;

public class Conflict {
	
    public enum ConflictType {
    	PLAN_TRIGGER,
    	PLAN_NAME,
    	CONFLICT_IDENTIFIER,
    	ATOMIC
    }
	
	private String conflict;
	private ConflictType type;
	private int hash;
	
	public Conflict(String conflict, ConflictType type) {
		this.conflict = conflict;
		this.type = type;
		hash = conflict.hashCode() + type.hashCode();
	}

	public String getConflict() {
		return conflict;
	}

	public ConflictType getType() {
		return type;
	}
	
	public boolean equals(Object obj) {	
		return  ((Conflict) obj).conflict.equals(conflict) && ((Conflict) obj).type.equals(type);
	}
	
	public int hashCode() {
		return hash;
	}
	
	public Conflict clone() {
		return new Conflict(conflict, type);
	}
}
