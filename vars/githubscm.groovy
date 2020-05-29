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

def checkoutIfExists(String repository, String author, String branches, String defaultAuthor, String defaultBranches, boolean mergeTarget = false) {
    def repositoryScm = null
    try {
        repositoryScm = resolveRepository(repository, author, branches, true)
    } catch (Exception ex) {
        echo 'Branches [' + branches + '] from repository ' + repository + ' not found in ' + author + ' organisation.'
        echo 'Checking branches ' + defaultBranches + ' from organisation ' + defaultAuthor + ' instead.'
    }
    if (repositoryScm != null) {
        if(mergeTarget) {
            mergeSourceIntoTarget(repository, author, branches, defaultAuthor, defaultBranches)
        } else {
            checkout repositoryScm
        }
    } else {
        checkout(resolveRepository(repository, defaultAuthor, defaultBranches, false))
    }
}

def mergeSourceIntoTarget(String repository, String sourceAuthor, String sourceBranches, String targetAuthor, String targetBranches) {
    println "Merging source [${repository}/${sourceAuthor}:${sourceBranches}] into target [${repository}/${targetAuthor}:${targetBranches}]..."
    checkout(resolveRepository(repository, targetAuthor, targetBranches, false))
    def targetCommit = getCommit()

    sh "git remote add source https://github.com/${sourceAuthor}/${repository}"
    sh 'git fetch source'
    sh "git checkout source/${sourceBranches}"
    sh "git pull source ${sourceBranches}"
    def sourceCommit = getCommit()
    sh "git checkout origin/${targetBranches}"

    try {
        sh "git merge source/${sourceBranches} -m 'merge $sourceCommit to $targetCommit'"
    } catch (Exception e) {
        println """
        -------------------------------------------------------------
        [ERROR] Can't merge source into Target
        -------------------------------------------------------------
        Source: ${sourceCommit}
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
    Source: ${sourceCommit}
    Target: ${targetCommit}
    Produced: ${mergedCommit}
    -------------------------------------------------------------
    """
}

def getCommit() {
    return sh(returnStdout: true, script: 'git log --oneline -1').trim()
}