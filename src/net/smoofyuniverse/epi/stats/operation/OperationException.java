package net.smoofyuniverse.epi.stats.operation;

public class OperationException extends Exception {
	private static final long serialVersionUID = 6121133825683609553L;
	
	public int line = -1;

	public OperationException() {
		super();
	}
	
	public OperationException(String message) {
		super(message);
	}
}
