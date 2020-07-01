def additionalVariablesBranched7(def branched7RepositoryListPath = "./script/branched-7-repository-list.txt") {
    def currentBranch = env.BRANCH_NAME ?: env.GIT_BRANCH
    def additionalVariables = [:]
    if ('master' == currentBranch) {
        def branched7RepositoryListFile = readFile branched7RepositoryListPath
        branched7RepositoryListFile.readLines().each { additionalVariables["${it}-scmRevision"] = '7.x' }
    }
    return additionalVariables
}
