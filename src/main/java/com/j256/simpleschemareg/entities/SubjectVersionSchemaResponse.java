package com.j256.simpleschemareg.entities;

/**
 * Response with just an id.
 */
public class SubjectVersionSchemaResponse {

	private final String subject;
	private final long version;
	private final long id;
	private final String schema;

	public SubjectVersionSchemaResponse(String subject, long version, long id, String schema) {
		this.subject = subject;
		this.version = version;
		this.id = id;
		this.schema = schema;
	}

	public String getSubject() {
		return subject;
	}

	public long getVersion() {
		return version;
	}
	
	public long getId() {
		return id;
	}

	public String getSchema() {
		return schema;
	}
}
