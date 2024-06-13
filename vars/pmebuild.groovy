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
 * @return a map of "${projectGroup} + '_' + {projectName}": projectVersion
 */
def buildProjects(List<String> projectCollection, String settingsXmlId, String buildConfigPathFolder, String pmeCliPath, Map<String, String> projectVariableMap, Map<String, String> buildConfigAdditionalVariables, Map<String, String> variableVersionsMap = [:]) {
    env.DATE_TIME_SUFFIX = env.DATE_TIME_SUFFIX ?: "${new Date().format(env.DATE_TIME_SUFFIX_FORMAT ?: 'yyMMdd')}"
    env.PME_BUILD_VARIABLES = ''

    println "[INFO] Build projects ${projectCollection}. Build path ${buildConfigPathFolder}. DATE_TIME_SUFFIX '${env.DATE_TIME_SUFFIX}'"
    def buildConfigContent = readFile "${buildConfigPathFolder}/build-config.yaml"
    Map<String, Object> buildConfigMap = getBuildConfiguration(buildConfigContent, buildConfigPathFolder, buildConfigAdditionalVariables)
  
    checkoutProjects(projectCollection, buildConfigMap, buildConfigAdditionalVariables)
    def result = projectCollection.collectEntries { [ (it) : buildProject(it, settingsXmlId, buildConfigMap, pmeCliPath, projectVariableMap, variableVersionsMap) ] }

    saveVariablesToEnvironment(variableVersionsMap)
    return result
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
    def result = null
    def projectGroupName = util.getProjectGroupName(project, defaultGroup)
    def group = projectGroupName[0]
    def name = projectGroupName[1]
    def finalProjectName = "${group}/${name}"
    dir("${env.WORKSPACE}/${group}_${name}") {
        def projectConfig = getProjectConfiguration(finalProjectName, buildConfig)

        executePME(finalProjectName, projectConfig, pmeCliPath, settingsXmlId, variableVersionsMap)
        executeBuildScript(finalProjectName, buildConfig, settingsXmlId, "-DaltDeploymentRepository=local::default::file://${env.WORKSPACE}/deployDirectory")

        def pom = readMavenPom file: 'pom.xml'
        result = pom?.version
        def groupName = group + '_' + name
        if (projectVariableMap.containsKey(groupName)) {
            def key = projectVariableMap[groupName]
            variableVersionsMap << ["${key}": result]
        }
        
        def cleanScript = buildConfig['defaultBuildParameters']['cleanScript'] ? buildConfig['defaultBuildParameters']['cleanScript'].minus('mvn ') : 'clean'
        maven.runMavenWithSettings(settingsXmlId, cleanScript, Boolean.valueOf(SKIP_TESTS))
    }
    saveBuildProjectOk(project)
    return result
}


/**
 * Checks out the project collection
 *
 * @param projectCollection the list of projects to be checked out
 * @return
 */
def checkoutProjects(List<String> projectCollection, Map<String, Object> buildConfig, Map<String, String> buildConfigAdditionalVariables) {
    println "[INFO] Checking out projects ${projectCollection}"

    projectCollection.each { project ->
        def projectGroupName = util.getProjectGroupName(project)
        def group = projectGroupName[0]
        def name = projectGroupName[1]
        dir("${env.WORKSPACE}/${group}_${name}") {
            if(fileExists(".git")) {
                println "[WARNING] '.git' directory exists, cleaning Git working tree"
                githubscm.cleanWorkingTree()
            } else {
                checkoutProject(name, group, getProjectConfiguration("${group}/${name}", buildConfig), buildConfigAdditionalVariables)
            }
        }
    }
}

/**
 * Checkouts the git project for group/name arguments
 *
 * @param name project name
 * @param group project group
 * @param projectConfig the buildConfig map for the project
 * @param buildConfigAdditionalVariables build config additional variables to get revision, it considers all the %projectName%-scmRevision variables
 */
def checkoutProject(String name, String group, Map<String, Object> projectConfig, Map<String, String> buildConfigAdditionalVariables) {
    def author = env.CHANGE_AUTHOR ?: group
    def branch = buildConfigAdditionalVariables["${name}-scmRevision"] ?: env.CHANGE_BRANCH ?: BRANCH_NAME
    def defaultAuthor = group
    def defaultBranch = getDefaultBranch(projectConfig, branch)

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
def getBuildConfiguration(String buildConfigContent, String buildConfigPathFolder, Map<String, String> buildConfigAdditionalVariables) {
    def additionalVariables = [datetimeSuffix: env.DATE_TIME_SUFFIX, groovyScriptsPath: "file://${buildConfigPathFolder}", productVersion: env.PRODUCT_VERSION]
    Map<String, Object> variables = getFileVariables(buildConfigContent) << additionalVariables << buildConfigAdditionalVariables
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
            def pmeParameters = customPmeParameters ? customPmeParameters.join(' ') : ''
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
def executeBuildScript(String project, Map<String, Object> buildConfig, String settingsXmlId, String additionalFlags = '') {
    Map<String, Object> projectConfig = getProjectConfiguration(project, buildConfig)
    def buildScript = (projectConfig != null && projectConfig['buildScript'] != null ? projectConfig['buildScript'] : buildConfig['defaultBuildParameters']['buildScript'])

    buildScript.split(";").each {
        if (it.trim().startsWith("mvn")) {
            maven.runMavenWithSettings(settingsXmlId, "${it.minus('mvn ')} ${additionalFlags}", Boolean.valueOf(SKIP_TESTS), "${project.replaceAll('/', '_') + '.maven.log'}")
        } else {
            sh it
        }
    }
}

/**
 * Gets the project default branch
 *
 * @param projectConfig the buildConfig map for the project
 * @param currentBranch the branch in case there's no smcRevision for the project
 * @return the branch name
 */
def getDefaultBranch(Map<String, Object> projectConfig, String currentBranch) {
    return projectConfig != null && projectConfig['scmRevision'] ? projectConfig['scmRevision'] : currentBranch
}

/**
 * Saves build project ok in ALREADY_BUILT_PROJECTS env var
 *
 * @param project the project name (this should match with the builds.project from the file)
 */
def saveBuildProjectOk(String project){
    env.ALREADY_BUILT_PROJECTS = "${env.ALREADY_BUILT_PROJECTS ?: ''}${project};"
}

/**
 * Parse Bacon build configuration extracting PME alignment parameters and build scripts.
 * Extracted values are exported using the following format:
 *   - build script PME_BUILD_SCRIPT_${sanitizedProjectName}
 *   - pme params PME_ALIGNMENT_PARAMS_${sanitizedProjectName}
 * 
 * @param buildConfigPathFolder the build config folder where groovy and yaml files are contained
 * @param buildConfigAdditionalVariables additional variables
 * @return a Map<project, Map<key, value>> where the inner map is composed by buildScript and pmeParameters
 */
def parseBuildConfig(String buildConfigPathFolder, Map<String, String> buildConfigAdditionalVariables) {
    env.DATE_TIME_SUFFIX = env.DATE_TIME_SUFFIX ?: "${new Date().format(env.DATE_TIME_SUFFIX_FORMAT ?: 'yyMMdd')}"
    env.PME_BUILD_VARIABLES = ''

    println "[INFO] Parsing build configs at ${buildConfigPathFolder}. DATE_TIME_SUFFIX '${env.DATE_TIME_SUFFIX}'"
    def buildConfigContent = readFile "${buildConfigPathFolder}/build-config.yaml"
    Map<String, Object> buildConfigMap = getBuildConfiguration(buildConfigContent, buildConfigPathFolder, buildConfigAdditionalVariables)
    
    def projects = buildConfigMap['builds'].collect { it['project'] }
    println "[INFO] Extracting build configuration for the following projects: ${projects}"

    projects.each { proj ->
        def projectConfig = getProjectConfiguration(proj, buildConfigMap)
        def sanitizedProjectName = proj.replaceAll('/', '_').replaceAll('-', '_')

        // export build script
        def buildScript = "${projectConfig['buildScript'] ?: buildConfigMap['defaultBuildParameters']['buildScript']} -DaltDeploymentRepository=local::default::file://${env.WORKSPACE}/deployDirectory"
        env["PME_BUILD_SCRIPT_${sanitizedProjectName}"] = buildScript

        // export PME alignment parameters
        def pmeParams = (projectConfig['alignmentParameters'] ?: projectConfig['customPmeParameters']).join(' ')
        env["PME_ALIGNMENT_PARAMS_${sanitizedProjectName}"] = pmeParams
    }
}

return this;
