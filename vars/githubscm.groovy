def resolveRepository(String repository, String author, String branches, boolean ignoreErrors, String credentialID = 'kie-ci') {
    println "[INFO] Resolving Repository https://github.com/${author}/${repository}:${branches}. CredentialsID: ${credentialID}"
    return [$class                           : 'GitSCM',
            branches                         : [[name: branches]],
            doGenerateSubmoduleConfigurations: false,
            extensions                       : [[$class: 'CleanBeforeCheckout'],
                                                [$class             : 'SubmoduleOption',
                                                 disableSubmodules  : false,
                                                 parentCredentials  : true,
                                                 recursiveSubmodules: true,
                                                 reference          : '',
                                                 trackingSubmodules : false],
                                                [$class           : 'RelativeTargetDirectory',
                                                 relativeTargetDir: "./"]],
            submoduleCfg                     : [],
            userRemoteConfigs                : [[credentialsId: credentialID, url: "https://github.com/${author}/${repository}.git"]]
    ]
}

def checkoutIfExists(String repository, String author, String branches, String defaultAuthor, String defaultBranches, boolean mergeTarget = false, def credentials = ['token': 'kie-ci1-token', 'usernamePassword': 'kie-ci']) {
    assert credentials['token']
    assert credentials['usernamePassword']
    def sourceAuthor = author
    def sourceRepository = getForkedProjectName(defaultAuthor, repository, sourceAuthor, credentials['token']) ?: repository
    // Checks source group and branch (for cases where the branch has been created in the author's forked project)
    def repositoryScm = getRepositoryScm(sourceRepository, author, branches, credentials['usernamePassword'])
    if (repositoryScm == null) {
        // Checks target group and and source branch (for cases where the branch has been created in the target project itself
        repositoryScm = getRepositoryScm(repository, defaultAuthor, branches, credentials['usernamePassword'])
        sourceAuthor = repositoryScm ? defaultAuthor : author
    }
    if (repositoryScm != null && hasPullRequest(defaultAuthor, repository, author, branches, credentials['token'])) {
        if (mergeTarget) {
            mergeSourceIntoTarget(sourceRepository, sourceAuthor, branches, repository, defaultAuthor, defaultBranches, credentials['usernamePassword'])
        } else {
            checkout repositoryScm
        }
    } else {
        checkout(resolveRepository(repository, defaultAuthor, defaultBranches, false, credentials['usernamePassword']))
    }
}

def getRepositoryScm(String repository, String author, String branches, String credentialId = 'kie-ci') {
    def repositoryScm = resolveRepository(repository, author, branches, true, credentialId)
    def tempDir = sh(script: 'mktemp -d', returnStdout: true).trim()
    dir(tempDir) {
        try {
            checkout repositoryScm
        } catch (Exception ex) {
            println "[WARNING] Branches [${branches}] from repository ${repository} not found in ${author} organisation."
            repositoryScm = null
        }
    }
    return repositoryScm
}

def mergeSourceIntoTarget(String sourceRepository, String sourceAuthor, String sourceBranches, String targetRepository, String targetAuthor, String targetBranches, String credentialId = 'kie-ci') {
    println "[INFO] Merging source [${sourceAuthor}/${sourceRepository}:${sourceBranches}] into target [${targetAuthor}/${targetRepository}:${targetBranches}]..."
    checkout(resolveRepository(targetRepository, targetAuthor, targetBranches, false, credentialId))
    setUserConfigFromCreds(credentialId)
    def targetCommit = getCommit()

    try {
        withCredentials([usernameColonPassword(credentialsId: credentialId, variable: 'kieCiUserPassword')]) {
            sh "git pull https://${kieCiUserPassword}@github.com/${sourceAuthor}/${sourceRepository} ${sourceBranches}"
        }
    } catch (Exception e) {
        println """
        -------------------------------------------------------------
        [ERROR] Can't merge source into Target. Please rebase PR branch.
        -------------------------------------------------------------
        Source: git://github.com/${sourceAuthor}/${sourceRepository} ${sourceBranches}
        Target: ${targetCommit}
        -------------------------------------------------------------
        """
        throw e;
    }
    def mergedCommit = getCommit()

    println """
    -------------------------------------------------------------
    [INFO] Source merged into Target
    -------------------------------------------------------------
    Target: ${targetCommit}
    Produced: ${mergedCommit}
    -------------------------------------------------------------
    """
}

def createBranch(String branchName) {
    try {
        sh "git checkout -b ${branchName}"
    } catch (Exception e) {
        println "[ERROR] Can't create branch ${branchName} on repo."
        throw e;
    }
    println "[INFO] Created branch '${branchName}' on repo."
}

def commitChanges(String commitMessage, Closure preCommit) {
    preCommit()
    sh "git commit -m '${commitMessage}'"
}

def commitChanges(String commitMessage, String filesToAdd = '--all') {
    commitChanges(commitMessage, { sh "git add ${filesToAdd}" })
}

def forkRepo(String credentialID = 'kie-ci') {
    cleanHubAuth()
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
        setUserConfig("${GITHUB_USER}")
        sh 'git config hub.protocol https'
        sh "hub fork --remote-name=origin"
        sh 'git remote -v'
    }
}

def createPR(String pullRequestTitle, String pullRequestBody = '', String targetBranch = 'master', String credentialID = 'kie-ci') {
    cleanHubAuth()
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
        def pullRequestLink
        try {
            setUserConfig("${GITHUB_USER}")
            pullRequestLink = sh(returnStdout: true, script: "hub pull-request -m '${pullRequestTitle}' -m '${pullRequestBody}' -b '${targetBranch}'").trim()
        } catch (Exception e) {
            println "[ERROR] Unable to create PR. Please make sure the targetBranch ${targetBranch} is correct."
            throw e;
        }
        println "Please see the created PR at: ${pullRequestLink}"
        return pullRequestLink
    }
}

def mergePR(String pullRequestLink, String credentialID = 'kie-ci') {
    cleanHubAuth()
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
        try {
            setUserConfig("${GITHUB_USER}")
            sh "hub merge ${pullRequestLink}"
        } catch (Exception e) {
            println "[ERROR] Can't merge PR ${pullRequestLink} on repo."
            throw e;
        }
        println "[INFO] Merged PR '${pullRequestLink}' on repo."
    }
}

// Optional: Pass in env.BUILD_TAG as buildTag in pipeline script 
// to trace back the build from which this tag came from.
def tagRepository(String tagName, String buildTag = '') {
    def currentCommit = getCommit()
    def tagMessageEnding = buildTag ? " in build \"${buildTag}\"." : '.'
    def tagMessage = "Tagged by Jenkins${tagMessageEnding}"
    sh "git tag -a '${tagName}' -m '${tagMessage}'"
    println """
-------------------------------------------------------------
[INFO] Tagged current repository
-------------------------------------------------------------
Commit: ${currentCommit}
Tagger: ${env.GIT_COMMITTER_NAME} (${env.GIT_COMMITTER_EMAIL})
Tag: ${tagName}
Tag Message: ${tagMessage}
-------------------------------------------------------------
"""
}

def pushObject(String remote, String object, String credentialsId = 'kie-ci') {
    try {
        withCredentials([usernamePassword(credentialsId: "${credentialsId}", usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
            setUserConfig("${GITHUB_USER}")
            sh("git config --local credential.helper \"!f() { echo username=\\$GITHUB_USER; echo password=\\$GITHUB_TOKEN; }; f\"")
            sh("git push ${remote} ${object}")
        }
    } catch (Exception e) {
        println "[ERROR] Couldn't push object '${object}' to ${remote}."
        throw e;
    }
    println "[INFO] Pushed object '${object}' to ${remote}."
}

def setUserConfig(String username) {
    sh "git config user.email ${username}@jenkins.redhat"
    sh "git config user.name ${username}"
}

def setUserConfigFromCreds(String credentialsId = 'kie-ci') {
    withCredentials([usernamePassword(credentialsId: "${credentialsId}", usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
        setUserConfig("${GITHUB_USER}")
    }
}

def getCommit() {
    return sh(returnStdout: true, script: 'git log --oneline -1').trim()
}

def getBranch() {
    return sh(returnStdout: true, script: 'git branch --all --contains HEAD').trim()
}

def getRemoteInfo(String remoteName, String configName) {
    return sh(returnStdout: true, script: "git config --get remote.${remoteName}.${configName}").trim()
}

def hasPullRequest(String group, String repository, String author, String branch, String credentialsId = 'kie-ci1-token') {
    return hasForkPullRequest(group, repository, author, branch, credentialsId) || hasOriginPullRequest(group, repository, branch, credentialsId)
}

def hasOriginPullRequest(String group, String repository, String branch, String credentialsId = 'kie-ci1-token') {
    return hasForkPullRequest(group, repository, group, branch, credentialsId)
}

def hasForkPullRequest(String group, String repository, String author, String branch, String credentialsId = 'kie-ci1-token') {
    def result = false
    withCredentials([string(credentialsId: credentialsId, variable: 'OAUTHTOKEN')]) {
        def curlResult = sh(returnStdout: true, script: "curl -H \"Authorization: token ${OAUTHTOKEN}\" 'https://api.github.com/repos/${group}/${repository}/pulls?head=${author}:${branch}&state=open'")?.trim()
        if (curlResult) {
            def pullRequestJsonObject = readJSON text: curlResult
            result = pullRequestJsonObject.size() > 0
        }
    }
    println "[INFO] has pull request for ${group}/${repository}:${author}:${branch} -> ${result}"
    return result
}

def getForkedProjectName(String group, String repository, String owner, String credentialsId = 'kie-ci1-token', int page = 1, int perPage = 100, replays = 3) {
    if (group == owner) {
        return repository;
    }
    def result = null
    def forkedProjects = null
    withCredentials([string(credentialsId: credentialsId, variable: 'OAUTHTOKEN')]) {
        new PipelineHelper(this)
                .retry({
                    def curlResult = sh(returnStdout: true, script: "curl -H \"Authorization: token ${OAUTHTOKEN}\" 'https://api.github.com/repos/${group}/${repository}/forks?per_page=${perPage}&page=${page}'")?.trim()
                    if (curlResult) {
                        forkedProjects = readJSON text: curlResult
                    }
                },
                        replays,
                        30,
                        {
                            println "[ERROR] Error getting forked project name for ${group}/${repository}/forks?per_page=${perPage}&page=${page}"
                            throw new Exception("Error getting forked project name for ${group}/${repository}/forks?per_page=${perPage}&page=${page}", e)
                        },
                       MissingPropertyException.class
                )


        if (forkedProjects != null && forkedProjects.size() > 0) {
            def forkedProject = forkedProjects.find { it.owner.login == owner }
            result = forkedProject ? forkedProject.name : getForkedProjectName(group, repository, owner, credentialsId, ++page, perPage)
        }
    }
    return result
}

def cleanHubAuth() {
    sh "rm -rf ~/.config/hub"
}

/**
* Uses `find` command to stage all files matching the pattern and which are not in .gitignore 
*/
def findAndStageNotIgnoredFiles(String findNamePattern){
    // based on https://stackoverflow.com/a/59888964/8811872
    sh """
    find . -type f -name '${findNamePattern}' > found_files.txt
    files_to_add=""
    while IFS= read -r file; do
        if ! git check-ignore -q "\$file"; then
            files_to_add="\$files_to_add \$file"
        fi
    done < found_files.txt
    rm found_files.txt
    git add \$files_to_add
    git status
    """
}
