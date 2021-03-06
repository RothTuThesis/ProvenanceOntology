package service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.LogOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectingLogOutputStream extends LogOutputStream {

	private List<String> lines = new ArrayList<>();

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void processLine(String line, int logLevel) {
		logger.debug(line);
		lines.add(line);
	}

	public List<String> getLines() {
		return lines;
	}
}