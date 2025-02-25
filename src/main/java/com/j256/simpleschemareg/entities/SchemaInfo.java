package com.j256.simpleschemareg.entities;

/**
 * Payload and response with just the schema.
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
