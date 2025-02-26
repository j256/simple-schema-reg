package com.j256.simpleschemareg.entities;

/**
 * Details about a schema with an optional version.
 */
public class SchemaDetails {

	private final String schema;
	private final byte[] digest;
	private final long id;
	// may not be persisted but can be returned as part of the response
	private final transient long version;

	public SchemaDetails(String schema, byte[] digest, long id) {
		this.schema = schema;
		this.digest = digest;
		this.id = id;
		this.version = 0;
	}

	public SchemaDetails(SchemaDetails details, long version) {
		this.schema = details.schema;
		this.digest = details.digest;
		this.id = details.id;
		this.version = version;
	}

	public String getSchema() {
		return schema;
	}

	public byte[] getDigest() {
		return digest;
	}

	public long getId() {
		return id;
	}

	public long getVersion() {
		return version;
	}
}
