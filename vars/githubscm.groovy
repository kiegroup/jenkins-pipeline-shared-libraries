def resolveRepository(String repository, String author, String branches, boolean ignoreErrors, String credentialID = 'kie-ci') {
    return resolveScm(
            source: github(
                    credentialsId: credentialID,
                    repoOwner: author,
                    repository: repository,
                    traits: [[$class: 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', strategyId: 3],
                             [$class: 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', strategyId: 1],
                             [$class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', strategyId: 1, trust: [$class: 'TrustPermission']]]),
            ignoreErrors: ignoreErrors,
            targets: [branches])
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
    println "[INFO] Resolving repository ${repository} author ${author} branches ${branches}"
    def repositoryScm = null
    try {
        repositoryScm = resolveRepository(repository, author, branches, true, credentialId)
    } catch (Exception ex) {
        println "[WARNING] Branches [${branches}] from repository ${repository} not found in ${author} organisation."
    }
    return repositoryScm
}

def mergeSourceIntoTarget(String sourceRepository, String sourceAuthor, String sourceBranches, String targetRepository, String targetAuthor, String targetBranches, String credentialId = 'kie-ci') {
    println "[INFO] Merging source [${sourceAuthor}/${sourceRepository}:${sourceBranches}] into target [${targetAuthor}/${targetRepository}:${targetBranches}]..."
    checkout(resolveRepository(targetRepository, targetAuthor, targetBranches, false, credentialId))
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

def commitChanges(String commitMessage, String filesToAdd = '--all') {
    sh "git add ${filesToAdd}"
    sh "git commit -m '${commitMessage}'"
}

def forkRepo(String credentialID = 'kie-ci') {
    cleanHubAuth()
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD')]) {
        sh 'git config --global hub.protocol https'
        sh "hub fork --remote-name=origin"
        sh 'git remote -v'
    }
}

def createPR(String pullRequestTitle, String pullRequestBody = '', String targetBranch = 'master', String credentialID = 'kie-ci') {
    cleanHubAuth()
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD')]) {
        def pullRequestLink
        try {
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
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD')]) {
        try {
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
        withCredentials([usernamePassword(credentialsId: "${credentialsId}", usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
            sh("git config --local credential.helper \"!f() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; f\"")
            sh("git push ${remote} ${object}")
        }
    } catch (Exception e) {
        println "[ERROR] Couldn't push object '${object}' to ${remote}."
        throw e;
    }
    println "[INFO] Pushed object '${object}' to ${remote}."
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
    println "${group}/${repository}:${branch}. Author:${author}"
    withCredentials([string(credentialsId: credentialsId, variable: 'OAUTHTOKEN')]) {
        def curlResult = sh(returnStdout: true, script: "curl -H \"Authorization: token ${OAUTHTOKEN}\" 'https://api.github.com/repos/${group}/${repository}/pulls?head=${author}:${branch}&state=open'")?.trim()
        if (curlResult) {
            def pullRequestJsonObject = readJSON text: curlResult
            println "result ${pullRequestJsonObject.size() > 0}"
            println pullRequestJsonObject
            result = pullRequestJsonObject.size() > 0
        }
    }
    println "[INFO] has pull request for ${group}/${repository}:${author}:${branch} -> ${result}"
    return result
}

def getForkedProjectName(String group, String repository, String owner, String credentialsId = 'kie-ci1-token') {
    def result = null
    withCredentials([string(credentialsId: credentialsId, variable: 'OAUTHTOKEN')]) {
        def curlResult = sh(returnStdout: true, script: "curl -H \"Authorization: token ${OAUTHTOKEN}\" 'https://api.github.com/repos/${group}/${repository}/forks'")?.trim()
        if (curlResult) {
            def forkedProjects = readJSON text: curlResult
            def forkedProject = forkedProjects.find { it.owner.login == owner }
            result = forkedProject ? forkedProject.name : null
        }
    }
    return result
}

def cleanHubAuth() {
    sh "rm -rf ~/.config/hub"
}
