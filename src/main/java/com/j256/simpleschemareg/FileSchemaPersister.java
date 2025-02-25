package com.j256.simpleschemareg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;

/**
 * Persists the schema to the file-system.
 */
public class FileSchemaPersister implements SchemaPersister {

	private static final String IDS_SUBDIR_NAME = "id";
	private static final String SUBJECTS_SUBDIR_NAME = "subject";

	private final Map<DigestInfo, SchemaDetails> digestSchemaMap = new ConcurrentHashMap<>();
	private final Map<Long, SchemaDetails> schemaIdMap = new ConcurrentHashMap<>();
	private final AtomicLong maxId = new AtomicLong();

	private final File rootDir;
	private final File subjectsDir;
	private final File idsDir;

	public FileSchemaPersister(File dirRoot) {
		this.rootDir = dirRoot;
		this.subjectsDir = new File(rootDir, SUBJECTS_SUBDIR_NAME);
		subjectsDir.mkdirs();
		this.idsDir = new File(rootDir, IDS_SUBDIR_NAME);
		idsDir.mkdirs();
	}

	@Override
	public void initialize() throws IOException {
		Gson gson = new Gson();
		for (File file : idsDir.listFiles()) {
			if (file.isDirectory() || file.getName().startsWith(".")) {
				// skip any files or dot directories
				continue;
			}
			long id;
			try {
				id = Long.parseLong(file.getName());
			} catch (NumberFormatException nfe) {
				// skip non numbers
				continue;
			}
			try (FileReader reader = new FileReader(file);) {
				SchemaDetails details = gson.fromJson(reader, SchemaDetails.class);
				if (details.getId() != id) {
					System.err.println("id file " + file + " contains wrong id " + details.getId());
					continue;
				}
				digestSchemaMap.put(new DigestInfo(details.getDigest()), details);
				schemaIdMap.put(details.getId(), details);
				if (id > maxId.get()) {
					maxId.set(id);
				}
			}
		}
	}

	@Override
	public String[] lookupSubjects() {

		List<String> subjects = new ArrayList<>();
		for (File subjectDir : subjectsDir.listFiles()) {
			subjects.add(subjectDir.getName());
		}

		Collections.sort(subjects);
		return subjects.toArray(new String[subjects.size()]);
	}

	@Override
	public SchemaDetails lookupSchema(String schema) {
		byte[] digest = digestFromSchema(schema);
		return digestSchemaMap.get(new DigestInfo(digest));
	}

	@Override
	public synchronized SchemaDetails saveSchema(String subject, String schema) throws IOException {

		byte[] digest = digestFromSchema(schema);
		File subjectDir = new File(subjectsDir, subject);

		SchemaDetails details = lookupSchema(subjectDir, digest);
		if (details != null && details.getVersion() != 0) {
			// return the details if we have a version otherwise we will need to make the symlink
			return details;
		}

		subjectDir.mkdirs();
		File idFile;

		if (details == null) {

			long id = maxId.incrementAndGet();
			details = new SchemaDetails(schema, digest, id);

			idFile = new File(idsDir, Long.toString(id));
			try (Writer writer = new FileWriter(idFile);) {
				Gson gson = new Gson();
				gson.toJson(details, writer);
			}

			digestSchemaMap.put(new DigestInfo(digest), details);
			schemaIdMap.put(id, details);
		} else {
			idFile = new File(idsDir, Long.toString(details.getId()));
		}

		long maxVersion = 0;
		for (File file : subjectDir.listFiles()) {
			Path path = file.toPath();
			if (!Files.isSymbolicLink(path)) {
				continue;
			}
			try {
				long version = Long.parseLong(file.getName());
				if (version > maxVersion) {
					maxVersion = version;
				}
			} catch (NumberFormatException nfe) {
				continue;
			}
		}

		// now link to the new subject
		maxVersion++;
		Path link = Paths.get(subjectDir.getPath(), Long.toString(maxVersion));
		if (Files.exists(link)) {
			Files.delete(link);
		}
		Files.createSymbolicLink(link, new File(Long.toString(details.getId())).toPath());
		return new SchemaDetails(details, maxVersion);
	}

	@Override
	public SchemaDetails lookupSchema(String subject, String schema) throws IOException {
		byte[] digest = digestFromSchema(schema);
		File subjectDir = new File(subjectsDir, subject);
		return lookupSchema(subjectDir, digest);
	}

	@Override
	public SchemaDetails lookupSchemaId(long id) {
		SchemaDetails details = schemaIdMap.get(id);
		return details;
	}

	@Override
	public SchemaDetails lookupSubjectVersion(String subject, long version) throws IOException {
		File subjectDir = new File(subjectsDir, subject);
		File subjectFile = new File(subjectDir, Long.toString(version));
		if (!Files.isSymbolicLink(subjectFile.toPath())) {
			return null;
		}

		Path idPath = Files.readSymbolicLink(subjectFile.toPath());

		try (FileReader reader = new FileReader(new File(idsDir, idPath.toString()));) {
			Gson gson = new Gson();
			SchemaDetails details = gson.fromJson(reader, SchemaDetails.class);
			return details;
		} catch (FileNotFoundException fnfe) {
			// might as well remove it
			subjectFile.delete();
			return null;
		}
	}

	@Override
	public long[] lookupSubjectVersions(String subject) {
		return listVersions(subject, false);
	}

	@Override
	public void deleteSchemaId(long id) {
		File idFile = new File(idsDir, Long.toString(id));
		idFile.delete();
	}

	@Override
	public long[] deleteSubject(String subject) {
		return listVersions(subject, true);
	}

	@Override
	public void deleteSubjectVersion(String subject, long version) {
		File subjectDir = new File(subjectsDir, subject);
		File idFile = new File(subjectDir, Long.toString(version));
		idFile.delete();
	}

	private byte[] digestFromSchema(String schema) {
		byte[] digest;
		try {
			MessageDigest digestInstance = MessageDigest.getInstance("md5");
			digest = digestInstance.digest(schema.getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return digest;
	}

	private long[] listVersions(String subject, boolean delete) {
		File subjectDir = new File(subjectsDir, subject);
		if (!subjectDir.exists()) {
			return null;
		}

		List<Long> results = new ArrayList<>();
		for (File file : subjectDir.listFiles()) {
			try {
				long version = Long.parseLong(file.getName());
				results.add(version);
				if (delete) {
					file.delete();
				}
			} catch (NumberFormatException nfe) {
				// ignore it
			}
		}
		if (delete) {
			subjectDir.delete();
		}

		Collections.sort(results);
		long[] versions = new long[results.size()];
		int i = 0;
		for (Long version : results) {
			versions[i++] = version;
		}
		return versions;
	}

	private SchemaDetails lookupSchema(File subjectDir, byte[] digest) throws IOException {

		SchemaDetails details = digestSchemaMap.get(new DigestInfo(digest));
		if (details == null) {
			return null;
		}

		if (!subjectDir.isDirectory()) {
			return details;
		}

		File idFile = new File(Long.toString(details.getId()));

		// find the version number in this subject
		for (File file : subjectDir.listFiles()) {
			Path path = file.toPath();
			if (!Files.isSymbolicLink(path)) {
				continue;
			}
			long version;
			try {
				version = Long.parseLong(file.getName());
			} catch (NumberFormatException nfe) {
				// ignore this
				continue;
			}
			Path idPath = Files.readSymbolicLink(path);
			if (idPath.equals(idFile.toPath())) {
				return new SchemaDetails(details, version);
			}
		}
		return details;
	}

	/**
	 * For hashing on the digest as a key.
	 */
	private static class DigestInfo {

		private final byte[] digest;

		public DigestInfo(byte[] digest) {
			this.digest = digest;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(digest);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			DigestInfo other = (DigestInfo) obj;
			return Arrays.equals(digest, other.digest);
		}
	}
}
