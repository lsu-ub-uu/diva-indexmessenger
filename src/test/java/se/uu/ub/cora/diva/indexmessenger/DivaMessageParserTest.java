/*
 * Copyright 2019, 2021 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.uu.ub.cora.diva.indexmessenger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.indexmessenger.log.LoggerFactorySpy;
import se.uu.ub.cora.indexmessenger.parser.MessageParser;
import se.uu.ub.cora.logger.LoggerProvider;

public class DivaMessageParserTest {
	private Map<String, String> headers;
	private String message;
	private LoggerFactorySpy loggerFactory;
	private String testedClassname = "DivaMessageParser";
	private MessageParser messageParser;
	private final static String TEST_RESOURCES_FILE_PATH = "./src/test/resources/";
	private final static String JMS_MESSAGE_WHICH_DOES_TRIGGER_INDEXING = "JmsMessageWhichDoesTriggerIndexing.xml";
	private final static String JMS_MESSAGE_WHEN_DELETE = "JmsMessageWhenDelete.xml";

	@BeforeMethod
	public void setUp() throws RuntimeException {
		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);

		headers = new HashMap<>();
		headers.put("methodName", "modifyDatastreamByReference");
		headers.put("pid", "authority-person:666498");

		tryToReadExampleMessageFromDivaClassic();

		messageParser = new DivaMessageParser();
	}

	private void tryToReadExampleMessageFromDivaClassic() {
		try {
			message = Files.readString(
					Path.of(TEST_RESOURCES_FILE_PATH + JMS_MESSAGE_WHICH_DOES_TRIGGER_INDEXING),
					StandardCharsets.UTF_8);
		} catch (IOException ioExecption) {
			throw new RuntimeException("File could not be closed", ioExecption);
		}
	}

	@Test
	public void testInit() throws Exception {
		messageParser = new DivaMessageParser();
	}

	@Test
	public void testNoMethodNameWorkOrderShouldNotBeCreated() throws Exception {
		headers.remove("methodName");

		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testWrongMethodNameWorkOrderShouldNotBeCreated() throws Exception {
		headers.put("methodName", "NOTmodifyDatastreamByReference");

		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMethodNameModifyDatastreamByReferenceWorkOrderShouldBeCreated()
			throws Exception {
		headers.put("methodName", "modifyDatastreamByReference");

		messageParser.parseHeadersAndMessage(headers, message);
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMethodNameModifyObjectANDDeleteMessageWorkOrderShouldBeCreated()
			throws Exception {
		String messageWhenDelete = Files
				.readString(Path.of(TEST_RESOURCES_FILE_PATH + JMS_MESSAGE_WHEN_DELETE));
		headers.put("methodName", "modifyObject");

		messageParser.parseHeadersAndMessage(headers, messageWhenDelete);
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMethodNameModifyObjectNOTDeleteMessageWorkOrderShouldNotBeCreated()
			throws Exception {
		headers.put("methodName", "modifyObject");

		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMethodNamePurgeObjectWorkOrderShouldBeCreated() throws Exception {
		headers.put("methodName", "purgeObject");

		messageParser.parseHeadersAndMessage(headers, message);
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMethodNameAddDatastreamWorkOrderShouldBeCreated() throws Exception {
		headers.put("methodName", "addDatastream");
		messageParser.parseHeadersAndMessage(headers, message);
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testTypeNotHandledWorkOrderShouldNotBeCreated() throws Exception {
		headers.put("pid", "diva2:45677");

		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserPidNullWorkOrderShouldNotBeCreated() throws Exception {
		headers.replace("pid", null);

		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserNoPidWorkOrderShouldNotBeCreated() throws Exception {
		headers.remove("pid");

		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserLogsWhenNoPidWorkOrderShouldNotBeCreated() throws Exception {
		headers.remove("pid");

		assertEquals(loggerFactory.getNoOfErrorLogMessagesUsingClassName(testedClassname), 0);

		messageParser.parseHeadersAndMessage(headers, message);

		assertEquals(loggerFactory.getNoOfErrorLogMessagesUsingClassName(testedClassname), 1);
		assertEquals(loggerFactory.getErrorLogMessageUsingClassNameAndNo(testedClassname, 0),
				"No pid found in header");
	}

	@Test
	public void testNoMethodNameNoValuesShouldBeSet() throws Exception {
		headers.remove("methodName");

		messageParser.parseHeadersAndMessage(headers, message);
		assertNull(messageParser.getRecordId());
		assertNull(messageParser.getRecordType());
		assertNull(messageParser.getModificationType());
	}

	@Test
	public void testMessageParserReturnsCorrectId() throws Exception {
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getRecordId(), headers.get("pid"));
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserReturnsCorrectType() throws Exception {
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getRecordType(), "person");
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testGetModificationTypeWhenUpdate() {
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getModificationType(), "update");
	}

	@Test
	public void testGetModificationTypeWhenCreate() {
		headers.put("methodName", "addDatastream");
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getModificationType(), "update");
	}

	@Test
	public void testGetModificationTypeWhenDelete() throws IOException {
		String messageWhenDelete = Files
				.readString(Path.of(TEST_RESOURCES_FILE_PATH + JMS_MESSAGE_WHEN_DELETE));
		headers.put("methodName", "modifyObject");
		messageParser.parseHeadersAndMessage(headers, messageWhenDelete);

		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
		assertEquals(messageParser.getModificationType(), "delete");
	}

	@Test
	public void testGetModificationTypeWhenPurge() throws IOException {
		headers.put("methodName", "purgeObject");
		messageParser.parseHeadersAndMessage(headers, message);

		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
		assertEquals(messageParser.getModificationType(), "delete");
	}

}
