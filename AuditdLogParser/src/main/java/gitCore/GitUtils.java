package gitCore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.ignore.FastIgnoreRule;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitUtils {

	private String gitShaCommand, gitUniqueSHAFromOutputCmd, gitUniqueSHAFromLongCmd, gitDiffCmd, gitDirAdditions;

	private final String gitDiffCmdTemplate = "git" + " " + " " + "${GITDIRADDITIONS}" + " " + "update-index --refresh;"
			+ " " + "git" + " " + "${GITDIRADDITIONS}" + " " + "diff-index --name-only HEAD --" + " " + "${OBJNAME}";

	private final String gitShaCommandTemplate = "git" + " " + "${GITDIRADDITIONS}" + " "
			+ "log -n 1 --pretty=format:%h";

	private final String gitDirAdditionsTemplate = "--git-dir=${WORKINGDIR}.git" + " " + "--work-tree=${WORKINGDIR}";

	private String getUniqueSHAFromOutputCmdTemplate = "git ${GITDIRADDITIONS}" + " " + "rev-parse --short" + " "
			+ "$(${GITSHACOMMAND}" + " " + "${OBJNAME})";

	private final String gitShortShaCommandTemplate = "git ${GITDIRADDITIONS}" + " " + "rev-parse --short" + " "
			+ "${OBJNAME}";

	private List<FastIgnoreRule> gitIgnoreList = new ArrayList<FastIgnoreRule>();
	private Git gitHook;

	public GitUtils(String workingDir, String gitIgnoreListString) throws IOException {
		this(workingDir);
		StringTokenizer st = new StringTokenizer(gitIgnoreListString, " ", false);
		while (st.hasMoreTokens()) {
			gitIgnoreList.add(new FastIgnoreRule(st.nextToken().trim()));
		}
		gitIgnoreList = Collections.unmodifiableList(this.gitIgnoreList);
	}

	public GitUtils(String workingDir) {
		try {
			File testDir = new File(workingDir + "/.git");
			File dir = new File(workingDir);
			Git git = Git.init().setDirectory(dir).call();
			FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
			repoBuilder.setMustExist(false);
			repoBuilder.setGitDir(testDir);
			Repository repo = repoBuilder.build();
			if (repo.resolve(Constants.HEAD) == null) {
				git.commit().setAuthor("testResearcher1", "testResearcher1@email.com")
						.setMessage("AuditdLogParser: Init Repo").call();
			}

			gitHook = git;

			Map<String, String> templateData = new HashMap<String, String>();
			templateData.put("WORKINGDIR", workingDir);
			gitDirAdditions = StrSubstitutor.replace(gitDirAdditionsTemplate, templateData);

			templateData.put("GITDIRADDITIONS", gitDirAdditions);
			gitUniqueSHAFromLongCmd = StrSubstitutor.replace(gitShortShaCommandTemplate, templateData);
			gitDiffCmd = StrSubstitutor.replace(gitDiffCmdTemplate, templateData);
			gitShaCommand = StrSubstitutor.replace(gitShaCommandTemplate, templateData);

			templateData.put("GITSHACOMMAND", gitShaCommand);
			gitUniqueSHAFromOutputCmd = StrSubstitutor.replace(getUniqueSHAFromOutputCmdTemplate, templateData);

		} catch (GitAPIException | IOException e) {
			e.printStackTrace();
		}
	}

	public Git getGitHook() {
		return gitHook;
	}

	public List<FastIgnoreRule> getGitIgnoreList() {
		return gitIgnoreList;
	}

	public String getSHACmd() {
		return gitShaCommand;
	}

	public String getUniqueSHAFromOutputCmd() {
		return gitUniqueSHAFromOutputCmd;
	}

	public String getUniqueSHAFromLongCmd() {
		return gitUniqueSHAFromLongCmd;
	}

	public String getGitDiffCmd() {
		return gitDiffCmd;
	}

}
