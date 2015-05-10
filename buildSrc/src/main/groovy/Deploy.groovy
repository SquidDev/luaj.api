import org.gradle.api.Project

public class Deploy {
	Project project

	boolean shouldDeploy() {
		return (
			getUser() != null && git(["symbolic-ref", "--short", "-q", "HEAD"]) == "master"
		);
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
	 * @return The
	 */
	String toString() {
		def version = git(["tag", "-l", "--contains", "HEAD"])
		return version != "" ? version.replace('v', '') : git(["symbolic-ref", "--short", "-q", "HEAD"]) + "-SNAPSHOT"
	}

	def git(List gitArgs) {
		def stdout = new ByteArrayOutputStream()

		project.exec {
			executable = 'git'
			args = gitArgs
			standardOutput = stdout
		}

		return stdout.toString().trim()
	}
}
