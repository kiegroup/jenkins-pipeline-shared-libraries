import org.yaml.snakeyaml.Yaml

/**
 * Builds the project collection
 *
 * @param projectCollection the project list to build
 * @param settingsXmlId the maven settings id from jenkins
 * @param buildConfigPathFolder the build config folder where groovy and yaml files are contained
 * @param pmeCliPath the pme cli path
 * @param projectVariableMap the project variable map
 * @param variableVersionsMap already defined versions map for the PME execution
 */
def buildProjects(List<String> projectCollection, String settingsXmlId, String buildConfigPathFolder, String pmeCliPath, Map<String, String> projectVariableMap, Map<String, String> variableVersionsMap = [:]) {
    env.DATE_TIME_SUFFIX = env.DATE_TIME_SUFFIX ?: "${new Date().format('yyyyMMdd')}"
    env.PME_BUILD_VARIABLES = ''

    println "[INFO] Build projects ${projectCollection}. Build path ${buildConfigPathFolder}. DATE_TIME_SUFFIX '${env.DATE_TIME_SUFFIX}'"
    def buildConfigContent = readFile "${buildConfigPathFolder}/build-config.yaml"
    Map<String, Object> buildConfigMap = getBuildConfiguration(buildConfigContent, buildConfigPathFolder)
  
    checkoutProjects(projectCollection, buildConfigMap)
    projectCollection.each { project -> buildProject(project, settingsXmlId, buildConfigMap, pmeCliPath, projectVariableMap, variableVersionsMap) }

    saveVariablesToEnvironment(variableVersionsMap)
}

/**
 * Builds the project
 *
 * @param project the project name (this should match with the builds.project from the file)
 * @param settingsXmlId the maven settings id from jenkins
 * @param buildConfig the build config map
 * @param pmeCliPath the pme cli path
 * @param defaultGroup the default group in case the project is not defined as group/name
 */
def buildProject(String project, String settingsXmlId, Map<String, Object> buildConfig, String pmeCliPath, Map<String, String> projectVariableMap, Map<String, String> variableVersionsMap, String defaultGroup = "kiegroup") {
    println "[INFO] Building project ${project}"
    def projectGroupName = util.getProjectGroupName(project, defaultGroup)
    def group = projectGroupName[0]
    def name = projectGroupName[1]
    def finalProjectName = "${group}/${name}"
    dir("${env.WORKSPACE}/${group}_${name}") {
        def projectConfig = getProjectConfiguration(finalProjectName, buildConfig)

        executePME(finalProjectName, projectConfig, pmeCliPath, settingsXmlId, variableVersionsMap)
        executeBuildScript(finalProjectName, buildConfig, settingsXmlId)

        if (projectVariableMap.containsKey(group + '_' + name)) {
            def key = projectVariableMap[group + '_' + name]
            def pom = readMavenPom file: 'pom.xml'
            variableVersionsMap << ["${key}": pom.version]
        }
        maven.runMavenWithSettings(settingsXmlId, 'clean', Boolean.valueOf(SKIP_TESTS))
    }
}


/**
 * Checks out the project collection
 *
 * @param projectCollection the list of projects to be checked out
 * @return
 */
def checkoutProjects(List<String> projectCollection, Map<String, Object> buildConfig) {
    println "[INFO] Checking out projects ${projectCollection}"

    projectCollection.each { project ->
        def projectGroupName = util.getProjectGroupName(project)
        def group = projectGroupName[0]
        def name = projectGroupName[1]
        if(!fileExists("${env.WORKSPACE}/${group}_${name}")) {
            dir("${env.WORKSPACE}/${group}_${name}") {
                checkoutProject(name, group, getProjectConfiguration("${group}/${name}", buildConfig))
            }
        } else {
            println "[WARNING] the project won't be checked out for '${group}/${name}'"
        }
    }
}

/**
 * Checkouts the git project for group/name arguments
 *
 * @param name project name
 * @param group project group
 * @param projectConfig the buildConfig map for the project
 */
def checkoutProject(String name, String group, Map<String, Object> projectConfig) {
    def author = env.CHANGE_AUTHOR ?: group
    def branch = env.CHANGE_BRANCH ?: BRANCH_NAME
    def defaultAuthor = group
    def defaultBranch = getDefaultBranch(projectConfig)

    println "[INFO] Checking out ${name}... Using author [${author}] and branch [${branch}]. Using default author [${defaultAuthor}] and default branch [${defaultBranch}]."
    githubscm.checkoutIfExists(name, author, branch, defaultAuthor, defaultBranch)
    util.storeGitInformation("${group}/${name}")
}

/**
 * Parses the build config yaml file to a map
 *
 * @param buildConfigContent the yaml file content
 * @return the yaml map
 */
def getBuildConfiguration(String buildConfigContent, String buildConfigPathFolder) {
    def additionalVariables = [datetimeSuffix: env.DATE_TIME_SUFFIX, groovyScriptsPath: "file://${buildConfigPathFolder}", productVersion: env.PRODUCT_VERSION]
    Map<String, Object> variables = getFileVariables(buildConfigContent) << additionalVariables
    saveVariablesToEnvironment(variables)
    def buildConfigContentTreated = treatVariables(buildConfigContent, variables)

    Yaml parser = new Yaml()
    return parser.load(buildConfigContentTreated)
}

/**
 * Saves a map of variables to job env variables
 *
 * @param variables the variables to save
 */
def saveVariablesToEnvironment(Map<String, Object> variables) {
    println "[INFO] Save variables to env ${variables}..."
    env.PME_BUILD_VARIABLES = env.PME_BUILD_VARIABLES == null ? "" : env.PME_BUILD_VARIABLES
    variables
            .each { key, value ->
                if (value != null && (value instanceof String && !value.contains(" ")) || !(value instanceof String)) {
                    env.PME_BUILD_VARIABLES = env.PME_BUILD_VARIABLES + "${key}=${value};"
                }
            }
    sh 'env'
}

/**
 * Gets the project configuration from the buildConfig map
 *
 * @param project the project name (this should match with the builds.project from the file)
 * @param buildConfig the buildConfig map
 * @return the project configuration
 */
def getProjectConfiguration(String project, Map<String, Object> buildConfig) {
    return buildConfig['builds'].find { (project == it['project']) }
}

/**
 * Treats the build-config variables to replace their values
 *
 * @param buildConfigContent
 * @param variables you can pass throw something like [productVersion: "1.0", milestone: "CRX"]
 * @return the map of treated variables
 */
def treatVariables(String buildConfigContent, Map<String, Object> variables) {
    def content = buildConfigContent
    variables.each { key, value ->
        content = content.replaceAll('\\{\\{' + key + '}}', value)
    }
    def matcher = content =~ /\{\{[a-zA-Z_0-9]*\}\}/

    return matcher.find() ? treatVariables(content, variables) : content
}

/**
 * Gets the variables #! and adds them to a map
 *
 * @param buildConfigContent the build config file content
 * @return a key:value map with #! variables from the  buildConfigFile
 */
def getFileVariables(String buildConfigContent) {
    def variables = [:]
    def matcher = buildConfigContent =~ /(#!)([a-zA-Z0-9_-]*)(=)(.*)/

    matcher.each { value ->
        variables.put(value[2], value[4])
    }
    return variables
}

/**
 * Executes the pme for the project
 *
 * @param project the project name (this should match with the builds.project from the file)
 * @param projectConfig the buildConfig map for the project
 * @param pmeCliPath the pme cli path
 * @param settingsXmlId the settings file id for PME execution
 */
def executePME(String project, Map<String, Object> projectConfig, String pmeCliPath, String settingsXmlId, Map<String, String> variableVersionsMap) {
    if (projectConfig != null) {
        configFileProvider([configFile(fileId: settingsXmlId, variable: 'PME_MAVEN_SETTINGS_XML')]) {
            List<String> customPmeParameters = projectConfig['customPmeParameters']
            def pmeParameters = customPmeParameters.join(' ')
            variableVersionsMap.each { k, v -> pmeParameters += " -D${k}=${v}" }
            println "[INFO] PME parameters for ${project}: ${pmeParameters}"
            sh "java -jar ${pmeCliPath} -s $PME_MAVEN_SETTINGS_XML -DallowConfigFilePrecedence=true -DprojectSrcSkip=false ${pmeParameters}"
        }
    }

}

/**
 * Executes the script for the project
 *
 * @param project the project id
 * @param buildConfig the whole build config
 * @param settingsXmlId the maven settings file id
 */
def executeBuildScript(String project, Map<String, Object> buildConfig, String settingsXmlId) {
    Map<String, Object> projectConfig = getProjectConfiguration(project, buildConfig)
    def buildScript = (projectConfig != null && projectConfig['buildScript'] != null ? projectConfig['buildScript'] : buildConfig['defaultBuildParameters']['buildScript'])

    buildScript.split(";").each {
        if (it.trim().startsWith("mvn")) {
            maven.runMavenWithSettings(settingsXmlId, "${it.minus('mvn ')} -DaltDeploymentRepository=local::default::file://${env.WORKSPACE}/deployDirectory", Boolean.valueOf(SKIP_TESTS), "${project.replaceAll('/', '_') + '.maven.log'}")
        } else {
            sh it
        }
    }
}

/**
 * Gets the project default branch
 *
 * @param projectConfig the buildConfig map for the project
 * @return the branch name
 */
def getDefaultBranch(Map<String, Object> projectConfig) {
    return projectConfig != null && projectConfig['scmRevision'] ? projectConfig['scmRevision'] : 'master'
}

return this;
