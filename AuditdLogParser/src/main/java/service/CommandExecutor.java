package service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;

public class CommandExecutor {

	public List<String> executeReturnOutputAsList(String command) throws Exception {
		CollectingLogOutputStream outputStream = new CollectingLogOutputStream();
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		execute(command, outputStream, resultHandler);

		if (resultHandler.getExitValue() == 1)
		{
			List<String> test = Arrays.asList(command.substring(command.indexOf("-f") + 3));
			return test;
		}
		return outputStream.getLines();
	}


	public int executeReturnExitVal(String command) throws Exception {
		CollectingLogOutputStream outputStream = new CollectingLogOutputStream();
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		execute(command, outputStream, resultHandler);

		return resultHandler.getExitValue();
	}

	private void execute(String command, LogOutputStream outputStream, DefaultExecuteResultHandler resultHandler)
			throws Exception {

		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

		Executor executor = new DefaultExecutor();
		executor.setStreamHandler(streamHandler);

		CommandLine cmdLine = new CommandLine("/run/current-system/sw/bin/bash");

		cmdLine.addArguments(new String[] { "-c", command }, false);

		try {
			executor.execute(cmdLine, resultHandler);
			resultHandler.waitFor();
		} catch (IOException | InterruptedException e) {
			System.out.println(cmdLine);
			throw e;
		}
	}
}
