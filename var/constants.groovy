final mavenLabel = 'kie-maven-3.5.4'
final jdk8 = 'kie-jdk1.8'

final numberOfBuildsToKeep = 10

def getMavenLabel() {
    return mavenLabel
}

def getJDK() {
    return getJDK8()
}

def getJDK8() {
    return jdk8
}

def getNumberOfBuildsToKeep() {
    return numberOfBuildsToKeep
}