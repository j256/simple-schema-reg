package com.j256.simpleschemareg.entities;

/**
 * Bunch of fields returned when looking up a subject/version.
 */
public class SubjectVersionResponse {

	private final String subject;
	private final long version;
	private final long id;
	private final String schema;

	public SubjectVersionResponse(String subject, long version, long id, String schema) {
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
