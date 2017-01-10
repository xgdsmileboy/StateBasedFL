package localization.common.config;

/**
 * This class is used to record the current project information
 * 
 * @author Jiajun
 *
 */
public class DynamicRuntimeInfo {

	/**
	 * project name
	 */
	private String _projectName = "";
	/**
	 * project id
	 */
	private int _projectId;

	/**
	 * 
	 * @param projectName
	 * @param id
	 */
	public DynamicRuntimeInfo(String projectName, int id) {
		_projectName = projectName;
		_projectId = id;
	}

	public String getProjectName() {
		return _projectName;
	}

	public int getProjectId() {
		return _projectId;
	}

}
