package com.j256.simpleschemareg;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.easymock.EasyMock;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.j256.simpleschemareg.entities.ErrorResponse;
import com.j256.simpleschemareg.entities.SchemaDetails;
import com.j256.simpleschemareg.entities.SchemaInfo;

public class SchemaRegHandlerTest {

	private final Gson gson = new Gson();
	private SchemaPersister persister;
	private SchemaRegHandler handler;
	private Request baseRequest;
	private Request request;
	private Response response;
	private StringWriter stringOutput;

	@Before
	public void before() throws IOException {
		persister = EasyMock.createMock(SchemaPersister.class);
		handler = new SchemaRegHandler(persister, null, true, false);
		baseRequest = EasyMock.createMock(Request.class);
		request = EasyMock.createMock(Request.class);
		response = EasyMock.createMock(Response.class);
		stringOutput = new StringWriter();
		expect(response.getWriter()).andReturn(new PrintWriter(stringOutput));
	}

	@Test
	public void testUnknownMethod() throws IOException {
		expect(baseRequest.getMethod()).andReturn("UNKNOWN");
		response.setStatus(HttpStatus.BAD_REQUEST_400);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.BAD_REQUEST_400, errorResponse.getErrorCode());
	}

	@Test
	public void testUnhandledMethod() throws IOException {
		expect(baseRequest.getMethod()).andReturn("UPDATE");
		response.setStatus(HttpStatus.BAD_REQUEST_400);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.BAD_REQUEST_400, errorResponse.getErrorCode());
	}

	@Test
	public void testUnknownGet() throws IOException {
		expect(baseRequest.getMethod()).andReturn("GET");
		expect(request.getPathInfo()).andReturn("/unknown");
		response.setStatus(HttpStatus.BAD_REQUEST_400);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.BAD_REQUEST_400, errorResponse.getErrorCode());
	}

	@Test(timeout = 1000)
	public void testGetShutdown() throws IOException, InterruptedException {
		final SchemaRegHandler handler = this.handler;
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				handler.waitForShutdown();
			}
		});
		thread.start();

		expect(baseRequest.getMethod()).andReturn("GET");
		expect(request.getPathInfo()).andReturn("/shutdown");

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.OK_200, errorResponse.getErrorCode());

		thread.join();
	}

	@Test
	public void testGetSubjects() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		expect(request.getPathInfo()).andReturn("/subjects");
		String[] subjects = new String[] { "foo", "bar" };
		expect(persister.lookupSubjects()).andReturn(subjects);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		String[] results = gson.fromJson(stringOutput.toString(), String[].class);
		assertArrayEquals(subjects, results);
	}

	@Test
	public void testGetSchemaIds() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		int schemaId = 100;
		expect(request.getPathInfo()).andReturn("/schemas/ids/" + schemaId);
		String schema = "pjofjwfewfewf";
		SchemaDetails details = new SchemaDetails(schema, new byte[0], schemaId);
		expect(persister.lookupSchemaId(schemaId)).andReturn(details);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		SchemaInfo result = gson.fromJson(stringOutput.toString(), SchemaInfo.class);
		assertEquals(schema, result.getSchema());
	}

	@Test
	public void testGetSchemaIdInvalid() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		expect(request.getPathInfo()).andReturn("/schemas/ids/not-number");
		response.setStatus(HttpStatus.BAD_REQUEST_400);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.BAD_REQUEST_400, errorResponse.getErrorCode());
	}

	@Test
	public void testGetSchemaIdsUnknown() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		int schemaId = 100;
		expect(request.getPathInfo()).andReturn("/schemas/ids/" + schemaId);
		expect(persister.lookupSchemaId(schemaId)).andReturn(null);
		response.setStatus(HttpStatus.NOT_FOUND_404);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.NOT_FOUND_404, errorResponse.getErrorCode());
	}

	@Test
	public void testGetSchemaIdsBadNumber() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		expect(request.getPathInfo()).andReturn("/schemas/ids/not-number");
		response.setStatus(HttpStatus.BAD_REQUEST_400);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.BAD_REQUEST_400, errorResponse.getErrorCode());
	}

	@Test
	public void testGetSchemaIdsSchema() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		int schemaId = 100;
		expect(request.getPathInfo()).andReturn("/schemas/ids/" + schemaId + "/schema");
		String schema = "pjofjwfewfewf";
		SchemaDetails details = new SchemaDetails(schema, new byte[0], schemaId);
		expect(persister.lookupSchemaId(schemaId)).andReturn(details);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		String result = gson.fromJson(stringOutput.toString(), String.class);
		assertEquals(schema, result);
	}

	@Test
	public void testGetSchemaIdsSchemaUnknown() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		int schemaId = 100;
		expect(request.getPathInfo()).andReturn("/schemas/ids/" + schemaId + "/schema");
		expect(persister.lookupSchemaId(schemaId)).andReturn(null);
		response.setStatus(HttpStatus.NOT_FOUND_404);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.NOT_FOUND_404, errorResponse.getErrorCode());
	}

	@Test
	public void testGetSubjectVersion() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		String subject = "foo";
		int version = 101;
		expect(request.getPathInfo()).andReturn("/subjects/" + subject + "/versions/" + version);
		String schema = "fpoewjfewfewfew";
		long schemaId = 31414;
		SchemaDetails details = new SchemaDetails(schema, new byte[0], schemaId);
		expect(persister.lookupSubjectVersion(subject, version)).andReturn(details);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		SchemaDetails result = gson.fromJson(stringOutput.toString(), SchemaDetails.class);
		assertEquals(schemaId, result.getId());
	}

	@Test
	public void testGetSubjectVersionUnknownSubject() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		String subject = "foo";
		int version = 101;
		expect(request.getPathInfo()).andReturn("/subjects/" + subject + "/versions/" + version);
		expect(persister.lookupSubjectVersion(subject, version)).andReturn(null);
		response.setStatus(HttpStatus.NOT_FOUND_404);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.NOT_FOUND_404, errorResponse.getErrorCode());
	}

	@Test
	public void testGetSubjectVersions() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		String subject = "foo";
		expect(request.getPathInfo()).andReturn("/subjects/" + subject + "/versions");
		long[] versions = new long[] { 1, 4, 5, 8 };
		expect(persister.lookupSubjectVersions(subject)).andReturn(versions);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		long[] results = gson.fromJson(stringOutput.toString(), long[].class);
		assertArrayEquals(versions, results);
	}

	@Test
	public void testGetSubjectVersionsUnknownSubject() throws IOException {

		expect(baseRequest.getMethod()).andReturn("GET");
		String subject = "foo";
		expect(request.getPathInfo()).andReturn("/subjects/" + subject + "/versions");
		expect(persister.lookupSubjectVersions(subject)).andReturn(null);
		response.setStatus(HttpStatus.NOT_FOUND_404);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.NOT_FOUND_404, errorResponse.getErrorCode());
	}

	@Test
	public void testUnknownPost() throws IOException {
		SchemaPersister persister = EasyMock.createMock(SchemaPersister.class);
		SchemaRegHandler handler = new SchemaRegHandler(persister, null, true, false);

		expect(baseRequest.getMethod()).andReturn("POST");
		expect(request.getPathInfo()).andReturn("/unknown");
		response.setStatus(HttpStatus.BAD_REQUEST_400);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.BAD_REQUEST_400, errorResponse.getErrorCode());
	}

	@Test
	public void testUnknownDelete() throws IOException {
		SchemaPersister persister = EasyMock.createMock(SchemaPersister.class);
		SchemaRegHandler handler = new SchemaRegHandler(persister, null, true, false);

		expect(baseRequest.getMethod()).andReturn("DELETE");
		expect(request.getPathInfo()).andReturn("/unknown");
		response.setStatus(HttpStatus.BAD_REQUEST_400);

		replay(persister, baseRequest, request, response);
		handler.handle("target", baseRequest, request, response);
		verify(persister, baseRequest, request, response);

		ErrorResponse errorResponse = gson.fromJson(stringOutput.toString(), ErrorResponse.class);
		assertEquals(HttpStatus.BAD_REQUEST_400, errorResponse.getErrorCode());
	}
}
