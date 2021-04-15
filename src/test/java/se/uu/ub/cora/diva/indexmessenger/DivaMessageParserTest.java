/*
 * Copyright 2019 Uppsala University Library
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
	private final static String JMS_MESSAGE_WHICH_DOES_NOT_TRIGGER_INDEXING = "JmsMessageWhichDoesNotTriggerIndexing.xml";

	@BeforeMethod
	public void setUp() throws RuntimeException {
		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);

		headers = new HashMap<>();
		headers.put("methodName", "modifyDatastreamByValue");
		headers.put("pid", "diva2:666498");

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
	public void testMessageParserReturnsCorrectId() throws Exception {
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getParsedId(), headers.get("pid"));
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	// TODO: NOT REALLY SURE WHERE TO EXTRACT THE TYPE FROM
	// diva2: -> publication
	// authority-person: -> person

	@Test
	public void testMessageParserReturnsCorrectType() throws Exception {
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getParsedType(), "publication");
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserReturnsTypeIdForPerson() throws Exception {
		headers.put("pid", "authority-person:666498");
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getParsedType(), "person");
	}

	@Test
	public void testMessageParserNotConsolidatedMessageWorkOrderShouldNotBeCreated()
			throws Exception {
		String messageNotTriggeringIndexing = Files.readString(
				Path.of(TEST_RESOURCES_FILE_PATH + JMS_MESSAGE_WHICH_DOES_NOT_TRIGGER_INDEXING));
		messageParser.parseHeadersAndMessage(headers, messageNotTriggeringIndexing);
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

	//
	@Test
	public void testMessageParserLogsWhenNoPidWorkOrderShouldNotBeCreated() throws Exception {
		headers.remove("pid");

		assertEquals(loggerFactory.getNoOfErrorLogMessagesUsingClassName(testedClassname), 0);
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(loggerFactory.getNoOfErrorLogMessagesUsingClassName(testedClassname), 1);
		assertEquals(loggerFactory.getErrorLogMessageUsingClassNameAndNo(testedClassname, 0),
				"No pid found in header");
	}

}
