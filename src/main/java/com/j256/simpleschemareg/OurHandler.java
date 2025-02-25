package com.j256.simpleschemareg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.gson.Gson;
import com.j256.simpleschemareg.SchemaPersister.SchemaDetails;
import com.j256.simpleschemareg.entities.ErrorResponse;
import com.j256.simpleschemareg.entities.IdResponse;
import com.j256.simpleschemareg.entities.SchemaInfo;
import com.j256.simpleschemareg.entities.SubjectVersionSchemaResponse;

/**
 * Little Jetty handler to process the redirect after the jetty request is confirmed that records the generated
 * verification code.
 */
public class OurHandler extends AbstractHandler {

	// GET /subjects/(string: subject)/versions/(versionId: version)/schema
	private static final String GET_SHUTDOWN = "/shutdown";
	private static final String GET_SUBJECTS = "/subjects";
	private static final Pattern GET_SCHEMCA_ID = Pattern.compile("/schemas/ids/(\\d+)");
	private static final Pattern GET_SCHEMCA_ID_SCHEMA = Pattern.compile("/schemas/ids/(\\d+)/schema");
	private static final Pattern GET_SUBJECT_VERSION = Pattern.compile("/subjects/([^/]+)/versions/(\\d+)");
	private static final Pattern GET_SUBJECT_VERSIONS = Pattern.compile("/subjects/([^/]+)/versions");
	private static final Pattern GET_SUBJECT_VERSION_SCHEMA =
			Pattern.compile("/subjects/([^/]+)/versions/(\\d+)/schema");
	private static final Pattern POST_SUBJECT_PATTERN = Pattern.compile("/subjects/([^/]+)/versions");
	private static final Pattern POST_SUBJECT_CHECK_PATTERN = Pattern.compile("/subjects/([^/]+)");
	private static final Pattern DELETE_SUBJECT_PATTERN = Pattern.compile("/subjects/([^/]+)");

	private final SchemaPersister persister;
	private final boolean handleShutdown;
	private final boolean verbose;

	private volatile boolean shuttingDown;

	public OurHandler(SchemaPersister persister, boolean handleShutdown, boolean verbose) {
		this.persister = persister;
		this.handleShutdown = handleShutdown;
		this.verbose = verbose;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		HttpMethod method = HttpMethod.fromString(baseRequest.getMethod());
		if (method == null) {
			writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400,
					"unknown request method: " + baseRequest.getMethod()));
			return;
		}

		if (method == HttpMethod.GET) {
			handleGet(target, baseRequest, request, response);
		} else if (method == HttpMethod.POST) {
			handlePost(target, baseRequest, request, response);
		} else if (method == HttpMethod.DELETE) {
			handleDelete(target, baseRequest, request, response);
		} else {
			writeResponse(response,
					new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled request method: " + method));
			return;
		}
	}

	/**
	 * Wait until the handler says we should shutdown.
	 */
	public void waitForShutdown() {
		synchronized (this) {
			while (!shuttingDown) {
				try {
					System.out.println("waiting for shutdown");
					this.wait();
					System.out.println("done waiting for shutdown");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private void handleGet(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		String pathInfo = request.getPathInfo();
		if (handleShutdown && GET_SHUTDOWN.equals(pathInfo)) {
			if (verbose) {
				System.out.println("Shutting down");
				System.out.flush();
			}
			synchronized (this) {
				shuttingDown = true;
				this.notify();
			}
			response.getWriter().close();
			return;
		}

		if (GET_SUBJECTS.equals(pathInfo)) {
			String[] subjects = persister.lookupSubjects();
			if (verbose) {
				System.out.println("Listing subjects: " + Arrays.toString(subjects));
				System.out.flush();
			}
			try (Writer writer = response.getWriter();) {
				Gson gson = new Gson();
				gson.toJson(subjects, writer);
			}
			return;
		}

		Matcher matcher = GET_SCHEMCA_ID.matcher(pathInfo);
		if (matcher.matches()) {
			long schemaId;
			try {
				schemaId = Long.parseLong(matcher.group(1));
			} catch (NumberFormatException nfe) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.BAD_REQUEST_400, "bad request schemaId: " + matcher.group(1)));
				return;
			}

			SchemaDetails details = persister.lookupSchemaId(schemaId);
			if (details == null) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "schema-id '" + schemaId + " not found"));
				return;
			}

			if (verbose) {
				System.out.println("Looking up schmea-id '" + schemaId + " got id " + details.getId());
				System.out.flush();
			}

			try (Writer writer = response.getWriter();) {
				Gson gson = new Gson();
				gson.toJson(new SchemaInfo(details.getSchema()), writer);
			}
			return;
		}

		matcher = GET_SCHEMCA_ID_SCHEMA.matcher(pathInfo);
		if (matcher.matches()) {
			long schemaId;
			try {
				schemaId = Long.parseLong(matcher.group(1));
			} catch (NumberFormatException nfe) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.BAD_REQUEST_400, "bad request schema-id: " + matcher.group(1)));
				return;
			}

			SchemaDetails details = persister.lookupSchemaId(schemaId);
			if (details == null) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "schema-id '" + schemaId + " not found"));
				return;
			}

			if (verbose) {
				System.out.println("Looking up schmea-id '" + schemaId + " got id " + details.getId());
				System.out.flush();
			}

			try (Writer writer = response.getWriter();) {
				writer.write(details.getSchema());
			}
			return;
		}

		matcher = GET_SUBJECT_VERSION.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long version;
			try {
				version = Long.parseLong(matcher.group(2));
			} catch (NumberFormatException nfe) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.BAD_REQUEST_400, "bad request version: " + matcher.group(1)));
				return;
			}

			SchemaDetails details = persister.lookupSubjectVersion(subject, version);
			if (details == null) {
				writeResponse(response, new ErrorResponse(HttpStatus.NOT_FOUND_404,
						"subject '" + subject + "' and version " + version + " not found"));
				return;
			}

			if (verbose) {
				System.out.println(
						"Looking up subject '" + subject + "' version " + version + " got id " + details.getId());
				System.out.flush();
			}

			try (Writer writer = response.getWriter();) {
				Gson gson = new Gson();
				gson.toJson(new SubjectVersionSchemaResponse(subject, version, details.getId(), details.getSchema()),
						writer);
			}
			return;
		}

		matcher = GET_SUBJECT_VERSIONS.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			long[] versions = persister.lookupSubjectVersions(subject);
			if (versions == null) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "subject '" + subject + "' not found"));
				return;
			}

			if (verbose) {
				System.out.println("Looking up subject '" + subject + "' versions: " + Arrays.toString(versions));
				System.out.flush();
			}

			try (Writer writer = response.getWriter();) {
				Gson gson = new Gson();
				gson.toJson(versions, writer);
			}
			return;
		}

		matcher = GET_SUBJECT_VERSION_SCHEMA.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long version;
			try {
				version = Long.parseLong(matcher.group(2));
			} catch (NumberFormatException nfe) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.BAD_REQUEST_400, "bad request version: " + matcher.group(1)));
				return;
			}

			SchemaDetails details = persister.lookupSubjectVersion(subject, version);
			if (details == null) {
				writeResponse(response, new ErrorResponse(HttpStatus.NOT_FOUND_404,
						"subject '" + subject + "' and version " + version + " not found"));
				return;
			}

			if (verbose) {
				System.out.println("Looking up subject '" + subject + "' version " + version + " schema got id "
						+ details.getId());
				System.out.flush();
			}

			try (Writer writer = response.getWriter();) {
				writer.append(details.getSchema());
			}
			return;
		}

		writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled GET request: " + pathInfo));
	}

	/**
	 * Handle post events.
	 */
	private void handlePost(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String pathInfo = request.getPathInfo();
		// POST /subjects/(string: subject)/versions
		Matcher matcher = POST_SUBJECT_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			Gson gson = new Gson();
			SchemaInfo saveSchema;
			// read in the schema
			try (BufferedReader reader = request.getReader();) {
				saveSchema = gson.fromJson(reader, SchemaInfo.class);
			}
			SchemaDetails details = persister.saveSchema(subject, saveSchema.getSchema());
			if (details == null) {
				writeResponse(response, new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
						" saving subject '" + subject + "' failed"));
				return;
			}

			if (verbose) {
				System.out.println("Saved schema for subject '" + subject + "' got version " + details.getVersion()
						+ ", id " + details.getId());
				System.out.flush();
			}

			try (Writer writer = response.getWriter();) {
				gson.toJson(new IdResponse(details.getId()), writer);
			}
			return;
		}

		matcher = POST_SUBJECT_CHECK_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);

			Gson gson = new Gson();
			SchemaInfo saveSchema;
			// read in the schema
			try (BufferedReader reader = request.getReader();) {
				saveSchema = gson.fromJson(reader, SchemaInfo.class);
			}
			SchemaDetails details = persister.lookupSchema(subject, saveSchema.getSchema());
			if (details == null) {
				writeResponse(response,
						new ErrorResponse(HttpStatus.NOT_FOUND_404, "subject '" + subject + "' schema not found"));
				return;
			}

			if (verbose) {
				System.out.println("Checking schema for subject '" + subject + "' got version " + details.getVersion()
						+ ", id " + details.getId());
				System.out.flush();
			}

			try (Writer writer = response.getWriter();) {
				gson.toJson(new IdResponse(details.getId()), writer);
			}
			return;
		}

		writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled POST request: " + pathInfo));
	}

	/**
	 * Handle delete events.
	 */
	private void handleDelete(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String pathInfo = request.getPathInfo();
		// POST /subjects/(string: subject)/versions
		Matcher matcher = DELETE_SUBJECT_PATTERN.matcher(pathInfo);
		if (matcher.matches()) {
			String subject = matcher.group(1);
			long[] versions = persister.deleteSubject(subject);
			if (verbose) {
				System.out.println("Deleting subject '" + subject + "' got versions: " + Arrays.toString(versions));
				System.out.flush();
			}
			try (Writer writer = response.getWriter();) {
				Gson gson = new Gson();
				gson.toJson(versions, writer);
			}
			return;
		}

		writeResponse(response, new ErrorResponse(HttpStatus.BAD_REQUEST_400, "unhandled DELETE request: " + pathInfo));
	}

	private void writeResponse(HttpServletResponse response, Object obj) throws IOException {
		if (verbose && obj instanceof ErrorResponse) {
			System.out.println("Writing error response: " + obj);
			System.out.flush();
		}
		try (Writer writer = response.getWriter();) {
			Gson gson = new Gson();
			gson.toJson(obj, writer);
		}
	}
}
