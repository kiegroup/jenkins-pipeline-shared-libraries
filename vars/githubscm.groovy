def resolveRepository(String repository, String author, String branches, boolean ignoreErrors) {
    return resolveScm(
            source: github(
                    credentialsId: 'kie-ci',
                    repoOwner: author,
                    repository: repository,
                    traits: [[$class: 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', strategyId: 1],
                             [$class: 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', strategyId: 1],
                             [$class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', strategyId: 1, trust: [$class: 'TrustPermission']]]),
            ignoreErrors: ignoreErrors,
            targets: [branches])
}

def checkoutIfExists(String repository, String author, String branches, String defaultAuthor, String defaultBranches) {
    def repositoryScm = null
    try {
        repositoryScm = resolveRepository(repository, author, branches, true)
    } catch (Exception ex) {
        echo 'Branches [' + branches + '] from repository ' + repository + ' not found in ' + author + ' organisation.'
        echo 'Checking branches ' + defaultBranches + ' from organisation ' + defaultAuthor + ' instead.'
    }
    if (repositoryScm != null) {
        checkout repositoryScm
    } else {
        checkout(resolveRepository(repository, defaultAuthor, defaultBranches, false))
    }
}

def runMavenWithSettings(String settingsXmlId, String goals, boolean skipTests) {
    configFileProvider([configFile(fileId: '9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', variable: 'MAVEN_SETTINGS_XML')]) {
        def mvnCommand = 'mvn -s $MAVEN_SETTINGS_XML -fae ' + goals
        if (skipTests) {
            mvnCommand = mvnCommand + ' -DskipTests'
        }
        sh mvnCommand
    }
}

def runMavenWithSubmarineSettins(String goals, boolean skipTests) {
    runMavenWithSettings('9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', goals, skipTests)
}