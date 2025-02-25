package com.j256.simpleschemareg.entities;

/**
 * Response with just an id.
 */
public class SchemaInfo {

	private final String schema;

	public SchemaInfo(String schema) {
		this.schema = schema;
	}

	public String getSchema() {
		return schema;
	}
}
