package com.j256.simpleschemareg;

import java.io.IOException;

import com.j256.simpleschemareg.entities.SchemaDetails;

/**
 * Schema persistence operations.
 */
public interface SchemaPersister {

	/**
	 * Initialize the schema persister. May be a no-op for some persisters.
	 */
	public void initialize() throws IOException;

	/**
	 * Return the subjects stored.
	 */
	public String[] lookupSubjects();

	/**
	 * See if the schema already exists in the registry.
	 * 
	 * @return Details about the schema or null if not found.
	 */
	public SchemaDetails lookupSchema(String schema);

	/**
	 * Lookup to see if schema has been saved in this subject.
	 * 
	 * @param subject
	 *            Subject name to associate with this schema.
	 * @param schema
	 *            Schema that we are looking up.
	 * @return Details about the schema or null if not found.
	 */
	public SchemaDetails lookupSchema(String subject, String schema) throws IOException;

	/**
	 * Lookup and return the schema details associated with the schema-id.
	 */
	public SchemaDetails lookupSchemaId(long id);

	/**
	 * Lookup and return the schema details associated with a subject and version.
	 */
	public SchemaDetails lookupSubjectVersion(String subject, long version) throws IOException;

	/**
	 * Lookup and return the versions for a subject.
	 * 
	 * @return An array of sorted versions or null if subject is not found.
	 */
	public long[] lookupSubjectVersions(String subject);

	/**
	 * Delete the schema associated with the schema-id.
	 */
	public void deleteSchemaId(long id);

	/**
	 * Delete all versions associated with a subject.
	 * 
	 * @return An array of sorted versions that were deleted or null if not found.
	 */
	public long[] deleteSubject(String subject);

	/**
	 * Delete the schema associated with a subject and version. This might leave the schema around if it is associated
	 * with another subject/version. Set permanent to true to also delete the associated schema-id.
	 */
	public SchemaDetails deleteSubjectVersion(String subject, long version, boolean permanent) throws IOException;

	/**
	 * Save schema to the registry. If it is new then it will be added to the various internal indexes. If it already
	 * exists then details about it will be returned.
	 * 
	 * @param subject
	 *            Subject name to associate with this schema.
	 * @param schema
	 *            Schema that we may be persisting.
	 */
	public SchemaDetails saveSchema(String subject, String schema) throws IOException;
}
