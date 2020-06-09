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
            mergeSourceIntoTarget(repository, branches, defaultAuthor, defaultBranches)
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

def mergeSourceIntoTarget(String repository, String sourceBranches, String targetAuthor, String targetBranches) {
    println "[INFO] Merging source [${ghprbAuthorRepoGitUrl}:${sourceBranches}] into target [${targetAuthor}/${repository}:${targetBranches}]..."
    checkout(resolveRepository(repository, targetAuthor, targetBranches, false))
    def targetCommit = getCommit()

    try {
        withCredentials([usernameColonPassword(credentialsId: 'kie-ci', variable: 'kieCiUserPassword')]) {
            def gitUrlWithoutProtocol = ghprbAuthorRepoGitUrl.replace('https://', '')
            sh "git pull https://$kieCiUserPassword@${gitUrlWithoutProtocol} ${sourceBranches}"
        }
    } catch (Exception e) {
        println """
-------------------------------------------------------------
[ERROR] Can't merge source into Target. Please rebase PR branch.
-------------------------------------------------------------
Source: ${ghprbAuthorRepoGitUrl} ${sourceBranches}
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

def getCommit() {
    return sh(returnStdout: true, script: 'git log --oneline -1').trim()
}

def getBranch() {
    return sh(returnStdout: true, script: 'git branch --all --contains HEAD').trim()
}

def getRemoteInfo(String remoteName, String configName) {
    return sh(returnStdout: true, script: "git config --get remote.${remoteName}.${configName}").trim()    
}
