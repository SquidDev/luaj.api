import org.gradle.api.Project

public class Deploy {
	Project project

	boolean shouldDeploy() {
		return getUser() != null
	}

	String getUser() {
		return project.hasProperty("ftpUser") ? project.ftpUser : System.getenv("FTP_USER")
	}

	String getPass() {
		return project.hasProperty("ftpPass") ? project.ftpPass : System.getenv("FTP_PASSWORD")
	}

	String getHost() {
		return project.hasProperty("ftpHost") ? project.ftpHost : System.getenv("FTP_HOST")
	}

	/**
	 * If there is a tag then use that, else get the current branch
	 * @return The version to use
	 */
	String toString() {
		String version = System.getenv("TRAVIS_TAG")
		if (version != null && version != "") return version.replace("v", "")

		version = git(["tag", "-l", "--contains", "HEAD"])
		if(version != null && version != "") return version.replace("v", "")

		version = System.getenv("TRAVIS_BRANCH")
		if (version != null && version != "") return version + "-SNAPSHOT"

		version = git(["symbolic-ref", "--short", "-q", "HEAD"])
		if (version != null && version != "") return version + "-SNAPSHOT"

		return "unknown-SNAPSHOT"
	}

	/**
	 * Execute a git command
	 * @param gitArgs Arguments to run
	 * @return StdOut
	 */
	String git(List gitArgs) {
		def stdout = new ByteArrayOutputStream()

		project.exec {
			executable = 'git'
			args = gitArgs
			standardOutput = stdout
			ignoreExitValue = true
		}

		return stdout.toString().trim()
	}
}
