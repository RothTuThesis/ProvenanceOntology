package logParser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.ignore.FastIgnoreRule;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import com.TuThesis.App;

import gitCore.GitUtils;
import ontology.ProvOntologyMaintainer;
import service.CommandExecutor;

public class AuditctlParser extends Thread {

	volatile boolean keepRunning = true;

	private static final int SLEEP = 10;
	private File logFile;
	private ProvOntologyMaintainer ontM;
	private List<FastIgnoreRule> ignoreProcList = new ArrayList<FastIgnoreRule>();
	private List<FastIgnoreRule> ignoreDirList = new ArrayList<FastIgnoreRule>();
	private List<String> symlinkResolveList = new ArrayList<String>();
	private GitUtils gitUtil;
	private Git gitHook;

	private String workingDir = "/home/testResearcher1/";
	private final Path workingDirPath = Paths.get(workingDir);

	public void stopKeepRunning() {
		keepRunning = false;
	}

	public AuditctlParser(ProvOntologyMaintainer ontM, File logFile, String ignoreProcListString,
			String ignoreDirListString, String symlinkResolveListString) {
		StringTokenizer stIgnoreProcList = new StringTokenizer(ignoreProcListString, "\n", false);
		StringTokenizer stSymlinkResolveList = new StringTokenizer(symlinkResolveListString, "\n", false);
		StringTokenizer stIgnoreDirList = new StringTokenizer(ignoreDirListString, "\n", false);

		this.ontM = ontM;
		this.logFile = logFile;

		while (stIgnoreProcList.hasMoreTokens()) {
			ignoreProcList.add(new FastIgnoreRule(stIgnoreProcList.nextToken().trim()));
		}
		ignoreProcList = Collections.unmodifiableList(ignoreProcList);

		while (stIgnoreDirList.hasMoreTokens()) {
			ignoreDirList.add(new FastIgnoreRule(stIgnoreDirList.nextToken().trim()));
		}
		ignoreDirList = Collections.unmodifiableList(ignoreDirList);

		while (stSymlinkResolveList.hasMoreTokens()) {
			symlinkResolveList.add(new String(stSymlinkResolveList.nextToken().trim()));
		}
		symlinkResolveList = Collections.unmodifiableList(symlinkResolveList);

		gitUtil = new GitUtils(workingDir);
		gitHook = gitUtil.getGitHook();

		try {
			CommandExecutor cExec = new CommandExecutor();
			gitHook.add().addFilepattern(".").call();
			gitHook.add().setUpdate(true).addFilepattern(".").call();
			Map<String, String> diffData = new HashMap<String, String>();
			diffData.put("OBJNAME", ".");
			String command = StrSubstitutor.replace(gitUtil.getGitDiffCmd(), diffData);

			List<String> outList = cExec.executeReturnOutputAsList(command);
			if (!outList.isEmpty()) {
				gitHook.commit().setAuthor("Researcher", "1").setMessage("AuditdLogParser: Modification").call();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void run() {
		MyListener listener = new MyListener();
		@SuppressWarnings("unused")
		Tailer tailer = Tailer.create(logFile, listener, SLEEP);
		System.out.println("run");
		while (keepRunning) {
			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class MyListener extends TailerListenerAdapter {
		private boolean makeLibDB, makeCreatedDB, makeUsedDB, skipProc = false;
		private ParsedObject parsedObj = new ParsedObject();
		private final Pattern hiddenFileRegexPat = Pattern.compile(".*/\\..*");
		private final Pattern nixStoreRegexPat = Pattern.compile("(/nix/store/[^/]+)(.*)");
		private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

		private HashMap<String, String> processObjectHash = new HashMap<String, String>();
		private HashMap<String, List<String>> libraryObjectHash = new HashMap<String, List<String>>();
		private HashMap<Integer, String[]> createdFileHash = new HashMap<Integer, String[]>();
		private HashMap<String, List<String>> usedFileHash = new HashMap<String, List<String>>();
		private HashMap<String, String> resolvedSymlinkHash = new HashMap<String, String>();
		private HashMap<String, List<String>> openAtDirHash = new HashMap<String, List<String>>();

		private final String readlinkCommandTemplate = "readlink -f" + " " + "${OBJ_INFO}";
		private final String userNameFromUIDCommandTemplate = "awk -v val=${UID_VAL} -F \":\" '$3==${UID_VAL}{print $1}' /etc/passwd";

		private final String librariesDB = "_lib_collection";
		private final String createdDB = "_created_collection";
		private final String usedDB = "_used_collection";

		CommandExecutor cExec = new CommandExecutor();

		@Override
		public void endOfFileReached() {
			App.endTime = Instant.now();
			System.out.println("Process Time: " + Duration.between(App.startTime, App.endTime));
		}

		@Override
		public void handle(String line) {

			final String callType;
			String typeString = "type=";
			String cwdPath = "cwd=\"";

			callType = line.substring(typeString.length(), line.indexOf(" "));
			
			makeLibDB = makeCreatedDB = makeUsedDB = true;

			switch (callType) {
			case "SYSCALL":
				parsedObj.setLineToProcess(line);
				syscallEntryInit(line);
				if ((parsedObj.getProcName().matches("[0-9]+") && parsedObj.getProcName().length() > 2)) {
					skipProc = true;
				} else if (!ignoreProcList.stream().anyMatch(str -> str.isMatch(parsedObj.getProcName(), false))) {
					skipProc = false;
					addProcOntEntry(parsedObj);

					if (parsedObj.getCallKey().equals("CLOSE_CMD")) {
						processCreatedFile(parsedObj);
					}
				} else {
					skipProc = true;
				}
				break;
			case "PATH":
				if (skipProc == false && parsedObj.getProcName() != null) {
					parsedObj.setLineToProcess(line);
					pathEntryInit(parsedObj);
				}
				break;
			case "CWD":
				if (skipProc == false && parsedObj.getProcName() != null) {
					String objPath = line.substring(line.indexOf(cwdPath) + cwdPath.length(), line.length() - 1) + "/";

					if (parsedObj.getSyscall().equals("257")) {
						ArrayList<String> openAtDir = new ArrayList<String>();
						openAtDir.add(objPath);
						openAtDirHash.putIfAbsent(parsedObj.getPid(), openAtDir);
					}

					parsedObj.setObjPath(objPath);
				}
				break;
			case "EXECVE":
				if (skipProc == false && parsedObj.getProcName() != null) {
					parseProcArgs(line);
				}
			default:
				break;
			}
		}

		private void parseProcArgs(String line) {
			final String pidProcName = processObjectHash.get(parsedObj.getPid());
			StringBuilder procArgs = new StringBuilder();
			final int ARG_START_POS = 3;
			final int ARG_NUM_POS = 2;
			String[] tokens = line.split(" ");
			String argStringTemplate = "argc=";

			int numArgs = Integer.parseInt(tokens[ARG_NUM_POS].substring(argStringTemplate.length()));
			int walkDist = numArgs + ARG_START_POS;

			if (tokens[ARG_START_POS]
					.substring(tokens[ARG_START_POS].lastIndexOf("/") + 1, tokens[ARG_START_POS].length() - 1)
					.equals("bash")) {
				return;
			}

			for (int i = ARG_START_POS; i < walkDist; i++) {
				if (i > ARG_START_POS)
					procArgs.append(" ");
				procArgs.append(tokens[i].substring(3).replace("\"", ""));
			}
			ontM.addOntologyObjectPropertyAssertionAxiom(pidProcName, procArgs.toString().replace(" ", "%20"),
					ontM.getWasStartedByObjectProperty());
		}

		private void syscallEntryInit(String line) {
			String[] tokens = line.split(" ");
			int length = tokens.length - 1;
			String key = tokens[length].substring(tokens[length].indexOf("=") + 2, tokens[length].length() - 1);
			String syscall = tokens[3].substring(tokens[3].indexOf("=") + 1);
			String sysCallTimeString, pid, uid, procName, exit, stringToHash, a0;

			if (key.equals("PROC_END")) {
				final String pidProcName;

				sysCallTimeString = tokens[1].substring(tokens[1].indexOf("(") + 1, tokens[1].indexOf(":"));
				a0 = tokens[4].substring(tokens[6].indexOf("=") + 1);
				pid = tokens[10].substring(tokens[12].indexOf("=") + 1);
				uid = tokens[12].substring(tokens[12].indexOf("=") + 1);
				procName = tokens[22].substring(tokens[22].indexOf("=") + 2, tokens[22].length() - 1);

				if ((pidProcName = processObjectHash.get(parsedObj.getPid())) != null) {
					ontM.addOntologyDataPropertyAssertionAxiomLiteral(pidProcName, ontM.getEndedAtTimeDataProperty(),
							fmt.format(parsedObj.getSyscallDate()), XSDVocabulary.DATE_TIME.getIRI());
				}
			} else {
				sysCallTimeString = tokens[1].substring(tokens[1].indexOf("(") + 1, tokens[1].indexOf(":"));
				exit = tokens[5].substring(tokens[5].indexOf("=") + 1);
				a0 = tokens[6].substring(tokens[6].indexOf("=") + 1);
				pid = tokens[12].substring(tokens[12].indexOf("=") + 1);
				uid = tokens[14].substring(tokens[14].indexOf("=") + 1);
				procName = tokens[24].substring(tokens[24].indexOf("=") + 2, tokens[24].length() - 1);
				parsedObj.setExitVal(Integer.parseInt(exit));
			}

			if (key.equals("CLOSE_CMD")) {
				parsedObj.setA0(Integer.parseUnsignedInt(a0, 16));
			}

			parsedObj.setPid(pid);
			parsedObj.setProcName(procName);
			parsedObj.setCallKey(key);
			parsedObj.setUid(uid);
			parsedObj.setSyscall(syscall);

			final double syscallTimeVal = Double.parseDouble(sysCallTimeString);

			int seconds = (int) syscallTimeVal;
			int milli = (int) Math.ceil(((syscallTimeVal - seconds) * 1000));
			long milliFromEpoch = TimeUnit.SECONDS.toMillis(seconds) + milli;
			parsedObj.setSyscallDate(Date.from(Instant.ofEpochMilli(milliFromEpoch)));

			stringToHash = procName + pid + sysCallTimeString;
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				String uniqueHex = (new HexBinaryAdapter()).marshal(md.digest(stringToHash.getBytes()));
				parsedObj.setProcActName(procName + "_" + uniqueHex);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}

		}

		private void pathEntryInit(ParsedObject parsedObj) {
			final String callKey;
			processPathGenericEntry(parsedObj);
			processPathFileEntry(parsedObj);

			callKey = parsedObj.getCallKey();

			if (callKey.equals("WRITE_CMD") && !parsedObj.getNameType().equals("PARENT")) {
				addCreatedFile(parsedObj);
			} else {
				if (parsedObj.isLib() == true) {
					addLibrary(parsedObj);
				} else {
					addUsedObject(parsedObj);
				}
			}
		}

		private void processCreatedFile(ParsedObject parsedObj) {
			int fdVal = parsedObj.getA0();
			final String pidProcName = processObjectHash.get(parsedObj.getPid());
			final String[] fileInfo;

			if ((fileInfo = createdFileHash.get(fdVal)) != null) {

				if (fileInfo[0].contains("/.") || !fileInfo[0].contains(workingDir)) {
					return;
				}

				Path filePath = Paths.get(fileInfo[0] + fileInfo[1]);
				Path relFilePath = workingDirPath.relativize(filePath);

				try {
					String longSHA, uniqueSHA, command;
					Map<String, String> diffData = new HashMap<String, String>();
					diffData.put("OBJNAME", filePath.toString());
					command = StrSubstitutor.replace(gitUtil.getGitDiffCmd(), diffData);

					gitHook.add().addFilepattern(relFilePath.toString()).call();
					List<String> outList = cExec.executeReturnOutputAsList(command);
					if (outList.stream().anyMatch(relFilePath.toString()::contains)) {
						longSHA = gitHook.commit().setAuthor("Researcher", "1")
								.setMessage("AuditdLogParser: " + relFilePath.toString()).call().getName();
						Map<String, String> cmdData = new HashMap<String, String>();
						cmdData.put("OBJNAME", longSHA);
						command = StrSubstitutor.replace(gitUtil.getUniqueSHAFromLongCmd(), cmdData);
					} else {
						Map<String, String> cmdData = new HashMap<String, String>();
						cmdData.put("OBJNAME", filePath.toString());
						command = StrSubstitutor.replace(gitUtil.getUniqueSHAFromOutputCmd(), cmdData);
					}

					uniqueSHA = cExec.executeReturnOutputAsList(command).get(0);
					ontM.addOntologyObjectPropertyAssertionAxiom(pidProcName + createdDB, fileInfo[1] + "_" + uniqueSHA,
							ontM.getHadMemberObjectProperty());
					ontM.addOntologyDataPropertyAssertionAxiom(fileInfo[1] + "_" + uniqueSHA, fileInfo[0],
							ontM.getLocationDataProperty());
					ontM.addOntologyDataPropertyAssertionAxiomLiteral(fileInfo[1] + "_" + uniqueSHA,
							ontM.getGeneratedAtTimeDataProperty(), fmt.format(parsedObj.getSyscallDate()),
							XSDVocabulary.DATE_TIME.getIRI());
					ontM.addOntologyClassAssertionAxiom(fileInfo[1] + "_" + uniqueSHA, ontM.getOntologyClass("Entity"));
				} catch (Exception e) {
					e.printStackTrace();
				}

				createdFileHash.remove(fdVal);
			}

		}

		private void processPathGenericEntry(ParsedObject parsedObj) {
			final Pattern pathRegexPat = Pattern.compile("(\\bname=)(\\\".*\\\")(.*)(nametype=)(.*)?");
			final int NAME_POS = 2, NAMETYPE_POS = 5;
			Matcher regexMatcherPath;
			Matcher regexMatcherNix;
			String objName, objPath;
			final String line = parsedObj.getLineToProcess();
			String[] tokens = line.split(" ");
			String nameTemplate = "name=\"";
			String nameType;

			String nameField = tokens[3].substring(nameTemplate.length(), tokens[3].length() - 1);

			regexMatcherPath = pathRegexPat.matcher(line);
			if (regexMatcherPath.find()) {
				
				objPath = parsedObj.getObjPath();
				nameType = regexMatcherPath.group(NAMETYPE_POS);
				parsedObj.setNameType(nameType);
				parsedObj.setObjName("");
				nameField = regexMatcherPath.group(NAME_POS).replaceAll("\"", "");

				parsedObj.setLib(false);
				if (nameField.contains(".so.") || nameField.contains("/lib/")) {
					parsedObj.setLib(true);
				}

				regexMatcherNix = nixStoreRegexPat.matcher(nameField);
				if (regexMatcherNix.find()) {
					parsedObj.setObjPath(regexMatcherNix.group(1));
					parsedObj.setObjName("");
					return;
				}

				objPath = parsedObj.getObjPath();
				nameType = regexMatcherPath.group(NAMETYPE_POS);
				parsedObj.setNameType(nameType);
				parsedObj.setObjName("");
				if (nameType.equals("UNKNOWN")) {
					return;
				}
				/* Needed for openat syscall? 
				if (parsedObj.getSyscall().equals("257")) {
					if (nameType.equals("NORMAL")) {
						List<String> openAtDir = openAtDirHash.get(parsedObj.getPid());
						if (!Character.isLetter(nameField.charAt(0))) {
							openAtDir.clear();
						}
						openAtDir.add(nameField);
					} else if (nameType.equals("PARENT")) {
						List<String> pathList = openAtDirHash.get(parsedObj.getPid());
						String buildPath = "";
								
						for (String path: pathList)
						{
							buildPath += path;
						}
						buildPath += Character.isLetter(buildPath.charAt(buildPath.length()-1)) ? "/": "";
						nameField = buildPath + nameField;
						parsedObj.setObjPath(nameField);
					} else {
						parsedObj.setObjName(nameField.substring(nameField.lastIndexOf('/') + 1));
					}

					return;
				}
				*/

				if (nameType.equals("PARENT")) {
					if (nameField.contains("./")) {
						parsedObj.setObjPath(parsedObj.getObjPath() + nameField.substring(2));
					} else if (Character.isLetter(nameField.charAt(0))) {
						parsedObj.setObjPath(parsedObj.getObjPath() + nameField);
					} else {
						parsedObj.setObjPath(nameField);
					}
				} else if (nameType.equals("CREATE")) {
					parsedObj.setObjName(nameField.substring(nameField.lastIndexOf('/') + 1));
				} else {
					if (nameField.length() >= 2) {
						if (nameField.substring(0, 2).equals("./")) {
							nameField = nameField.substring(2);
						}
					}
					if (Character.isLetter(nameField.charAt(0))) {
						if (nameField.contains("/")) {
							objPath = parsedObj.getObjPath() + nameField.substring(0, nameField.lastIndexOf('/')) + "/";
						} else {
							objName = nameField;
						}
					} else {
						if (nameField.contains("/")) {
							objPath = nameField.substring(0, nameField.lastIndexOf('/')) + "/";
							objPath = objPath.replaceAll("/+", "/");
						}
					}

					objName = nameField.substring(nameField.lastIndexOf('/') + 1);
					parsedObj.setObjPath(objPath);
					parsedObj.setObjName(objName);
				}
			} else {
				parsedObj.setObjName("ERROR: Failed to parse name field");
				nameField = ("ERROR: Failed to parse name field");
			}
		}

		private void processPathFileEntry(ParsedObject parsedObj) {
			String objName = parsedObj.getObjName();
			String objPath = parsedObj.getObjPath();
			String resolvedSymlinkPath;
			Matcher regexMatcher;

			List<String> matchKey = symlinkResolveList.stream()
					.filter(str -> parsedObj.getObjPath().contains(str.toString())).collect(Collectors.toList());

			if (matchKey.size() > 0) {
				String symlinkToResolve = matchKey.get(0);
				if ((resolvedSymlinkPath = resolvedSymlinkHash.get(symlinkToResolve)) == null) {
					try {
						Map<String, String> templateData = new HashMap<String, String>();
						templateData.put("OBJ_INFO", symlinkToResolve);
						final String readlinkCommand = StrSubstitutor.replace(readlinkCommandTemplate, templateData);
						String test = cExec.executeReturnOutputAsList(readlinkCommand).get(0);
						regexMatcher = nixStoreRegexPat.matcher(test);
						if (regexMatcher.find()) {
							objPath = regexMatcher.group(1);
							objName = "";
							resolvedSymlinkHash.put(symlinkToResolve, objPath);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					objPath = resolvedSymlinkPath;
					objName = "";
				}
			}

			if (!objPath.substring(objPath.length() - 1).equals("/"))
				objPath += "/";
			if (objName.length() > 0) {
				if (objName.substring(0, 1).equals("/"))
					objName.substring(1);
			}

			parsedObj.setObjName(objName);
			parsedObj.setObjPath(objPath);
		}

		private void addProcOntEntry(ParsedObject parsedObj) {
			if (processObjectHash.putIfAbsent(parsedObj.getPid(), parsedObj.getProcActName()) == null) {
				try {
					Map<String, String> templateData = new HashMap<String, String>();
					templateData.put("UID_VAL", parsedObj.getUid());
					final String userNameFromUIDCommand = StrSubstitutor.replace(userNameFromUIDCommandTemplate,
							templateData);
					String userName = cExec.executeReturnOutputAsList(userNameFromUIDCommand).get(0);

					ontM.addOntologyObjectPropertyAssertionAxiom(parsedObj.getProcActName(),
							parsedObj.getUid() + "(" + userName + ")", ontM.getWasAssociatedWithObjectProperty());
					ontM.addOntologyClassAssertionAxiom(parsedObj.getUid() + "(" + userName + ")",
							ontM.getOntologyClass("Agent"));
				} catch (Exception e) {
					e.printStackTrace();
				}

				ontM.addOntologyDataPropertyAssertionAxiomLiteral(parsedObj.getProcActName(),
						ontM.getStartedAtTimeDataProperty(), fmt.format(parsedObj.getSyscallDate()),
						XSDVocabulary.DATE_TIME.getIRI());
				ontM.addOntologyClassAssertionAxiom(parsedObj.getProcActName(), ontM.getOntologyClass("Activity"));
				ontM.addOntologyClassAssertionAxiom(parsedObj.getProcName(), ontM.getOntologyClass("Entity"));
			}
		}

		private void addUsedObject(ParsedObject parsedObj) {
			Matcher regexMatcher;
			final String procActName = processObjectHash.get(parsedObj.getPid());
			String objPath = parsedObj.getObjPath();
			String objName = parsedObj.getObjName();

			if (objName.equals(""))
				return;
			
			regexMatcher = hiddenFileRegexPat.matcher(objPath + objName);
			if (regexMatcher.find()) {
				return;
			}

			if (ignoreDirList.stream().anyMatch(str -> str.isMatch(objPath + objName, false))) {
				return;
			}

			if (makeUsedDB) {
				ontM.addOntologyClassAssertionAxiom(procActName + usedDB, ontM.getOntologyClass("Collection"));
				ontM.addOntologyObjectPropertyAssertionAxiom(procActName, procActName + usedDB,
						ontM.getUsedObjectProperty());
			}
			usedFileHash.putIfAbsent(procActName, new ArrayList<String>());
			List<String> usedList = usedFileHash.get(procActName);
			if (!(usedList.contains(objPath + objName))) {
				usedList.add(objPath + objName);

				if (objPath.contains(workingDir)) {
					try {
						Map<String, String> cmdData = new HashMap<String, String>();
						cmdData.put("OBJNAME", objPath + objName);
						String command = StrSubstitutor.replace(gitUtil.getUniqueSHAFromOutputCmd(), cmdData);
						String uniqueSHA = cExec.executeReturnOutputAsList(command).get(0);
						ontM.addOntologyDataPropertyAssertionAxiom(objName + "_" + uniqueSHA, objPath,
								ontM.getLocationDataProperty());
						ontM.addAtTimeObjectPropertyAnnotation(procActName + usedDB, objName + "_" + uniqueSHA,
								ontM.getHadMemberObjectProperty(), fmt.format(parsedObj.getSyscallDate()),
								XSDVocabulary.DATE_TIME.getIRI());
						ontM.addOntologyClassAssertionAxiom(objName + "_" + uniqueSHA, ontM.getOntologyClass("Entity"));
					} catch (Exception e) {
						e.printStackTrace();
					}

				} else {
					ontM.addAtTimeObjectPropertyAnnotation(procActName + usedDB, objPath + objName,
							ontM.getHadMemberObjectProperty(), fmt.format(parsedObj.getSyscallDate()),
							XSDVocabulary.DATE_TIME.getIRI());
					ontM.addOntologyClassAssertionAxiom(objPath + objName, ontM.getOntologyClass("Entity"));
				}

			}
			makeUsedDB = false;
		}

		private void addCreatedFile(ParsedObject parsedObj) {
			String[] fileInfo = { parsedObj.getObjPath(), parsedObj.getObjName() };
			final String pidProcActName = processObjectHash.get(parsedObj.getPid());

			if (!fileInfo[0].contains(workingDir) || fileInfo[0].contains("/.")) {
				return;
			}

			if (makeCreatedDB) {
				ontM.addOntologyClassAssertionAxiom(pidProcActName + createdDB, ontM.getOntologyClass("Collection"));
				ontM.addOntologyObjectPropertyAssertionAxiom(pidProcActName, pidProcActName + createdDB,
						ontM.getGeneratedObjectProperty());
			}
			createdFileHash.put(parsedObj.getExitVal(), fileInfo);
			makeCreatedDB = false;
		}

		private void addLibrary(ParsedObject parsedObj) {
			String objName = parsedObj.getObjName();
			String objPath = parsedObj.getObjPath();
			final String procActName = processObjectHash.get(parsedObj.getPid());

			if (makeLibDB) {
				ontM.addOntologyClassAssertionAxiom(procActName + librariesDB, ontM.getOntologyClass("Collection"));
				ontM.addOntologyObjectPropertyAssertionAxiom(procActName, procActName + librariesDB,
						ontM.getUsedObjectProperty());
			}
			libraryObjectHash.putIfAbsent(procActName, new ArrayList<String>());
			List<String> libList = libraryObjectHash.get(procActName);
			if (!(libList.contains(objPath + objName))) {
				libList.add(objPath + objName);
				ontM.addOntologyClassAssertionAxiom(objPath + objName, ontM.getOntologyClass("Entity"));
				ontM.addOntologyObjectPropertyAssertionAxiom(procActName + librariesDB, objPath + objName,
						ontM.getHadMemberObjectProperty());
			}
			makeLibDB = false;
		}
	}
}
