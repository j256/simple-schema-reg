package com.j256.simpleschemareg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.j256.simpleschemareg.entities.SchemaDetails;

public class FileSchemaPersisterTest {

	private static final String SCHEMAS_ROOT_DIR = "target/" + FileSchemaPersisterTest.class.getSimpleName();
	private File schemaRoot = new File(SCHEMAS_ROOT_DIR);

	@Before
	public void before() {
		if (schemaRoot.isDirectory()) {
			deleteDir(schemaRoot);
		} else {
			schemaRoot.delete();
		}
	}

	@Test
	public void testStuff() throws IOException {
		FileSchemaPersister persister = new FileSchemaPersister(schemaRoot);
		persister.initialize();

		assertArrayEquals(new String[] {}, persister.lookupSubjects());
		assertNull(persister.lookupSchemaId(1));

		String subject1 = "foo";
		String schema1 = "weopjpjwepfowerf";
		assertNull(persister.lookupSchema(schema1));

		assertNull(persister.lookupSchema(subject1, schema1));
		assertNull(persister.lookupSubjectVersions(subject1));

		SchemaDetails details = persister.saveSchema(subject1, schema1);
		assertNotNull(details);
		assertEquals(schema1, details.getSchema());
		assertEquals(1, details.getId());
		assertEquals(1, details.getVersion());

		assertArrayEquals(new long[] { 1 }, persister.lookupSubjectVersions(subject1));

		details = persister.lookupSchemaId(details.getId());
		assertEquals(1, details.getId());

		details = persister.lookupSchema(schema1);
		assertEquals(1, details.getId());
		// no version info
		assertEquals(0, details.getVersion());

		details = persister.lookupSchema(subject1, schema1);
		assertNotNull(details);
		assertEquals(1, details.getId());
		assertEquals(1, details.getVersion());

		assertArrayEquals(new String[] { subject1 }, persister.lookupSubjects());

		// save again
		details = persister.saveSchema(subject1, schema1);
		assertNotNull(details);

		assertEquals(schema1, details.getSchema());
		assertEquals(1, details.getId());

		// save different schema to different subject
		String subject2 = "bar";
		String schema2 = "rgergergergerg";
		details = persister.saveSchema(subject2, schema2);
		assertNotNull(details);
		assertEquals(schema2, details.getSchema());
		assertEquals(2, details.getId());
		assertEquals(1, details.getVersion());

		assertArrayEquals(new String[] { subject2, subject1 }, persister.lookupSubjects());

		// save different schema to different subject
		SchemaDetails details2 = persister.saveSchema(subject2, schema1);
		assertNotNull(details);
		assertEquals(1, details2.getId());
		assertEquals(2, details2.getVersion());

		/*
		 * Now start a new one reading in the files from disk.
		 */

		persister = new FileSchemaPersister(schemaRoot);
		persister.initialize();

		SchemaDetails details3 = persister.saveSchema(subject2, schema1);
		assertNotNull(details);
		assertEquals(1, details3.getId());
		assertEquals(2, details3.getVersion());

		assertNotNull(persister.lookupSubjectVersion(subject2, 1));
		SchemaDetails results = persister.deleteSubjectVersion(subject2, 1, false);
		assertEquals(details.getId(), results.getId());
		assertEquals(details.getVersion(), results.getVersion());

		assertNull(persister.lookupSubjectVersion(subject2, 1));
		assertNotNull(persister.lookupSubjectVersion(subject2, 2));

		assertArrayEquals(new long[] { 2 }, persister.deleteSubject(subject2));
		assertNull(persister.lookupSubjectVersion(subject2, 2));

		assertArrayEquals(new String[] { subject2, subject1 }, persister.lookupSubjects());

		/*
		 * Now start a new one reading in the files from disk.
		 */

		persister = new FileSchemaPersister(schemaRoot);
		persister.initialize();

		// now really blow it away
		results = persister.deleteSubjectVersion(subject2, 1, true);
		assertNotNull(results);
		assertEquals(details.getId(), results.getId());
		assertEquals(details.getVersion(), results.getVersion());

		assertArrayEquals(new String[] { subject1 }, persister.lookupSubjects());
	}

	@Test
	public void testDeleteSchemaId() throws IOException {
		FileSchemaPersister persister = new FileSchemaPersister(schemaRoot);
		persister.initialize();

		String subject = "foo";
		String schema = "weopjpjwepfowerf";

		SchemaDetails details = persister.saveSchema(subject, schema);
		assertNotNull(details);
		assertEquals(schema, details.getSchema());
		assertEquals(1, details.getId());
		assertEquals(1, details.getVersion());

		assertArrayEquals(new long[] { 1 }, persister.lookupSubjectVersions(subject));

		persister.deleteSchemaId(1);

		// dangling
		assertArrayEquals(new long[] { 1 }, persister.lookupSubjectVersions(subject));

		assertNull(persister.lookupSubjectVersion(subject, 1));

		assertArrayEquals(new long[] {}, persister.lookupSubjectVersions(subject));
	}

	@Test
	public void testSubjectVersionPermanent() throws IOException {
		FileSchemaPersister persister = new FileSchemaPersister(schemaRoot);
		persister.initialize();

		String subject = "foo";
		String schema = "weopjpjwepfowerf";

		SchemaDetails details = persister.saveSchema(subject, schema);
		assertNotNull(details);
		assertEquals(schema, details.getSchema());
		assertEquals(1, details.getId());
		assertEquals(1, details.getVersion());

		SchemaDetails details2 = persister.deleteSubjectVersion(subject, details.getVersion(), false);
		assertEquals(details.getId(), details2.getId());

		assertNull(persister.deleteSubjectVersion(subject, details.getVersion(), false));
		assertNotNull(persister.lookupSchemaId(details.getId()));

		details2 = persister.deleteSubjectVersion(subject, details.getVersion(), true);
		assertNotNull(details2);
		assertEquals(details.getId(), details2.getId());

		assertNull(persister.lookupSchemaId(details.getId()));
	}

	private void deleteDir(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				deleteDir(file);
			} else {
				file.delete();
			}
		}
		dir.delete();
	}
}
