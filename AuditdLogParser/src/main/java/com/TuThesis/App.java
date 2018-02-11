package com.TuThesis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Scanner;
import logParser.AuditctlParser;
import org.apache.commons.cli.*;
import ontology.ProvOntologyMaintainer;

public class App {
	public static Instant startTime;
	public static Instant endTime;

	public static void main(String[] args) throws IOException {

		String ignoreProcList = "", ignoreDirList = "", symlinkResolveList = "";
		String inputOntologyName, outputOntologyName;
		String defaultOntologyName = "ontology";

		Options options = new Options();
		
		Option help = new Option("h", "help", false, "Show Auditd Log Parser Options");
		help.setRequired(false);
		options.addOption(help);
		
		Option inputOntology = new Option("O", "inputOntologyName", true, "Existing ontology to load");
		inputOntology.setRequired(false);
		options.addOption(inputOntology);

		Option ignoreProc = new Option("I", "ignoreProc", true, "File containing process ignore list - newline separated");
		ignoreProc.setRequired(false);
		options.addOption(ignoreProc);

		Option ignoreDir = new Option("i", "ignoreDir", true, " File containing directory ignore list - newline separated");
		ignoreDir.setRequired(false);
		options.addOption(ignoreDir);

		Option symlink = new Option("s", "symlink", true, "File containing (symlink) paths to resolve list- newline separted");
		symlink.setRequired(false);
		options.addOption(symlink);

		Option outputOntology = new Option("o", "outputOntologyName", true,
				"Output file name (no extension; default = \"" + defaultOntologyName + "\")");
		outputOntology.setRequired(false);
		options.addOption(outputOntology);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("Auditd Log Parser", options);
			System.exit(1);
			return;
		}

		if (cmd.hasOption("help")) {
			formatter.printHelp("Auditd Log Parser", options);
			System.exit(1);
		}
		
		if (cmd.hasOption("inputOntologyName")) {
			inputOntologyName = cmd.getOptionValue("inputOntologyName") + ".rdf";
			File tmpDir = new File(inputOntologyName);
			if (tmpDir.exists())
				System.out.println("Loading Ontology: \"" + inputOntologyName +"\"");
			else {
				System.out.println(inputOntologyName + " does not exist.\nCreating new ontology");
				inputOntologyName = null;
			}
		} else {
			inputOntologyName = null;
			System.out.println("Creating new ontology\n");
		}

		if (cmd.hasOption("outputOntologyName")) {
			outputOntologyName = cmd.getOptionValue("outputOntologyName") + ".rdf";
		} else {
			if (inputOntologyName == null) {
				outputOntologyName = defaultOntologyName + ".rdf";
			} else {
				outputOntologyName = inputOntologyName;
			}
		}

		System.out.println("export: export ontology and continue\nexit:   export ontology and exit");
		ProvOntologyMaintainer ontM = new ProvOntologyMaintainer("http://www.TuThesis_Example.com/#", inputOntologyName,
				outputOntologyName);

		if (cmd.hasOption("ignoreProc")) {
			File ignoreProcListFile = new File(cmd.getOptionValue("ignoreProc"));
			ignoreProcList = readFile(ignoreProcListFile.getAbsolutePath(), StandardCharsets.UTF_8);
		}

		if (cmd.hasOption("ignoreDir")) {
			File ignoreDirListFile = new File(cmd.getOptionValue("ignoreDir"));
			ignoreDirList = readFile(ignoreDirListFile.getAbsolutePath(), StandardCharsets.UTF_8);
		}
		if (cmd.hasOption("symlink")) {
			File symlinkResolveFile = new File(cmd.getOptionValue("symlink"));
			symlinkResolveList = readFile(symlinkResolveFile.getAbsolutePath(), StandardCharsets.UTF_8);
		}

		AuditctlParser actlParser = new AuditctlParser(ontM, new File("/var/log/audit/aud.log"), ignoreProcList,
				ignoreDirList, symlinkResolveList);
		Thread actlParserThread = new Thread(actlParser);
		startTime = Instant.now();
		actlParserThread.start();

		Scanner s = new Scanner(System.in);

		while (true) {
			switch (s.next()) {
			case "export":
				System.out.println("Exporting ontology to \"" + outputOntologyName +"\"");
				ontM.saveOntology();
				break;
			case "exit":
				System.out.println("Exporting ontology to \"" + outputOntologyName + " and exiting");
				actlParser.stopKeepRunning();
				ontM.saveOntology();
				s.close();
				System.exit(0);
				break;
			}
		}
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
}
