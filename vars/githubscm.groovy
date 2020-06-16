def resolveRepository(String repository, String author, String branches, boolean ignoreErrors) {
    return resolveScm(
            source: github(
                    credentialsId: 'kie-ci',
                    repoOwner: author,
                    repository: repository,
                    traits: [[$class: 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait', strategyId: 3],
                             [$class: 'org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait', strategyId: 1],
                             [$class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait', strategyId: 1, trust: [$class: 'TrustPermission']]]),
            ignoreErrors: ignoreErrors,
            targets: [branches])
}

def checkoutIfExists(String repository, String author, String branches, String defaultAuthor, String defaultBranches, boolean mergeTarget = false) {
    def sourceAuthor = author
    // Checks source group and branch (for cases where the branch has been created in the author's forked project)
    def repositoryScm = getRepositoryScm(repository, author, branches)
    if (repositoryScm == null) {
        // Checks target group and and source branch (for cases where the branch has been created in the target project itself
        repositoryScm = getRepositoryScm(repository, defaultAuthor, branches)
        sourceAuthor = repositoryScm ? defaultAuthor : author
    }
    if (repositoryScm != null) {
        if(mergeTarget) {
            mergeSourceIntoTarget(repository, sourceAuthor, branches, defaultAuthor, defaultBranches)
        } else {
            checkout repositoryScm
        }
    } else {
        checkout(resolveRepository(repository, defaultAuthor, defaultBranches, false))
    }
}

def getRepositoryScm(String repository, String author, String branches) {
    println "[INFO] Resolving repository ${repository} author ${author} branches ${branches}"
    def repositoryScm = null
    try {
        repositoryScm = resolveRepository(repository, author, branches, true)
    } catch (Exception ex) {
        println "[WARNING] Branches [${branches}] from repository ${repository} not found in ${author} organisation."
    }
    return repositoryScm
}

def mergeSourceIntoTarget(String repository, String sourceAuthor, String sourceBranches, String targetAuthor, String targetBranches) {
    println "[INFO] Merging source [${sourceAuthor}/${repository}:${sourceBranches}] into target [${targetAuthor}/${repository}:${targetBranches}]..."
    checkout(resolveRepository(repository, targetAuthor, targetBranches, false))
    def targetCommit = getCommit()

    try {
        withCredentials([usernameColonPassword(credentialsId: 'kie-ci', variable: 'kieCiUserPassword')]) {
            sh "git pull https://${kieCiUserPassword}@github.com/${sourceAuthor}/${repository} ${sourceBranches}"
        }
    } catch (Exception e) {
        println """
        -------------------------------------------------------------
        [ERROR] Can't merge source into Target. Please rebase PR branch.
        -------------------------------------------------------------
        Source: git://github.com/${sourceAuthor}/${repository} ${sourceBranches}
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

def commitChanges(String userName, String userEmail, String commitMessage, String filesToAdd = '--all') {
    sh "git config user.name '${userName}'"
    sh "git config user.email '${userEmail}' "
    sh "git add ${filesToAdd}"
    sh "git commit -m '${commitMessage}' "
}


def forkRepo(String credentialID='kie-ci') {
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD')]){
        sh 'git config --global hub.protocol https'
        sh "hub fork --remote-name=origin"
        sh 'git remote -v'
    }
}

def createPR(String pullRequestMessage, String targetBranch='master', String credentialID='kie-ci') {
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD')]){
        try{
            def pullRequestLink = sh(returnStdout: true, script: "hub pull-request -m '${pullRequestMessage}' -b '${targetBranch}' ").trim()
        } catch (Exception e) {
            println "[ERROR] Unable to create PR make sure the targetBranch ${targetBranch} is correct"
            throw e;
        }
        println "Please see the created PR at: ${pullRequestLink}"
        return pullRequestLink
    }
}

def mergePR(String pullRequestLink, String credentialID='kie-ci') {
    withCredentials([usernamePassword(credentialsId: credentialID, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASSWORD')]){
        try{
            sh "hub merge ${pullRequestLink}"
        } catch (Exception e) {
            println "[ERROR] Can't merge PR ${pullRequestLink} on repo."
            throw e;
        }
        println "[INFO] Merged PR '${pullRequestLink}' on repo."
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
