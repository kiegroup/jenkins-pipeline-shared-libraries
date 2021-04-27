import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild
import groovy.util.slurpersupport.NodeChildren

class MavenSpec extends JenkinsPipelineSpecification {
    def mavenGroovy = null

    class VersionChildNode {
        String version

        VersionChildNode(String version) {
            this.version = version;
        }

        def text() {
            return this.version;
        }
    }

    def setup() {
        mavenGroovy = loadPipelineScriptForTest("vars/maven.groovy")
    }

    def "[maven.groovy] run Maven"() {
        when:
        mavenGroovy.runMaven("clean install")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B clean install', returnStdout: false])
    }

    def "[maven.groovy] run Maven with option"() {
        when:
        mavenGroovy.runMaven("clean install", ['-fae'])
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -fae clean install', returnStdout: false])
    }

    def "[maven.groovy] run Maven with log file"() {
        setup:
        Properties props = new Properties()
        props.setProperty("anykey", "anyvalue")
        when:
        mavenGroovy.runMaven("clean install", ['-fae'], props, "logFile.txt")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -fae clean install -Danykey=anyvalue | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[maven.groovy] run Maven with skip tests"() {
        when:
        mavenGroovy.runMaven("clean install", true, ['-fae'], "logFile.txt")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -fae clean install -DskipTests=true | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[maven.groovy] run Maven without skip tests"() {
        when:
        mavenGroovy.runMaven("clean install", false, ['-fae'], "logFile.txt")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -fae clean install -DskipTests=false | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[maven.groovy] run Maven without log file"() {
        setup:
        Properties props = new Properties()
        props.setProperty("anykey", "anyvalue")
        when:
        mavenGroovy.runMaven("clean install", ['-fae'], props)
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -fae clean install -Danykey=anyvalue', returnStdout: false])
    }

    def "[maven.groovy] run Maven with settings with log file"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        Properties properties = new Properties()
        properties.put('property1', 'value1')
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", properties, "logFile.txt")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId -fae clean install -Dproperty1=value1 | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[maven.groovy] run Maven with settings without log file"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        Properties properties = new Properties()
        properties.put('property1b', 'value1b')
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", properties)
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId -fae clean install -Dproperty1b=value1b', returnStdout: false])
    }

    def "[maven.groovy] run Maven with settings without properties"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        Properties properties = new Properties()
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", properties, "logFile.txt")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId -fae clean install | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[maven.groovy] run Maven sonar settings with log file"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        mavenGroovy.getBinding().setVariable("TOKEN", 'tokenId')
        when:
        mavenGroovy.runMavenWithSettingsSonar("settings.xml", "clean install", "sonarCloudId", "logFile.txt")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId clean install -Dsonar.login=tokenId | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[maven.groovy] run Maven sonar settings without log file"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        mavenGroovy.getBinding().setVariable("TOKEN", 'tokenId')
        when:
        mavenGroovy.runMavenWithSettingsSonar("settings.xml", "clean install", "sonarCloudId")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId clean install -Dsonar.login=tokenId', returnStdout: false])
    }

    def "[maven.groovy] run with Settings with log file"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        Properties properties = new Properties()
        properties.put('skipTests', true)
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", true, "logFile.txt")
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId -fae clean install -DskipTests=true | tee $WORKSPACE/logFile.txt ; test ${PIPESTATUS[0]} -eq 0', returnStdout: false])
    }

    def "[maven.groovy] run with Settings without log file"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        when:
        mavenGroovy.runMavenWithSettings("settings.xml", "clean install", false)
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId -fae clean install -DskipTests=false', returnStdout: false])
    }

    def "[maven.groovy] run with Submarine Settings without properties and without log file"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        when:
        mavenGroovy.runMavenWithSubmarineSettings("clean install", false)
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId -fae clean install -DskipTests=false', returnStdout: false])
    }

    def "[maven.groovy] run with Submarine Settings with properties and without log file"() {
        setup:
        mavenGroovy.getBinding().setVariable("env", ['MAVEN_SETTINGS_XML': 'settingsFileId'])
        Properties properties = new Properties()
        when:
        mavenGroovy.runMavenWithSubmarineSettings("clean install", properties)
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -s settingsFileId -fae clean install', returnStdout: false])
    }

    def "[maven.groovy] run mvn versions set"() {
        setup:
        def String newVersion = '3.6.2'
        when:
        mavenGroovy.mvnVersionsSet(newVersion)
        then:
        1 * getPipelineMock("sh")([script: "mvn -B -N -e versions:set -Dfull -DnewVersion=${newVersion} -DallowSnapshots=false -DgenerateBackupPoms=false", returnStdout: false])
    }

    def "[maven.groovy] run mvn versions set with allow snapshots"() {
        setup:
        def String newVersion = '3.6.2'
        when:
        mavenGroovy.mvnVersionsSet(newVersion, true)
        then:
        1 * getPipelineMock("sh")([script: "mvn -B -N -e versions:set -Dfull -DnewVersion=${newVersion} -DallowSnapshots=true -DgenerateBackupPoms=false", returnStdout: false])
    }

    def "[maven.groovy] run mvn versions update parent"() {
        setup:
        def String newVersion = '3.6.2'
        when:
        mavenGroovy.mvnVersionsUpdateParent(newVersion)
        then:
        1 * getPipelineMock("sh")([script: "mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=false -DgenerateBackupPoms=false", returnStdout: false])
    }

    def "[maven.groovy] run mvn versions update parent with allow snapshots"() {
        setup:
        def String newVersion = '3.6.2'
        when:
        mavenGroovy.mvnVersionsUpdateParent(newVersion, true)
        then:
        1 * getPipelineMock("sh")([script: "mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=true -DgenerateBackupPoms=false", returnStdout: false])
    }

    def "[maven.groovy] run mvn versions update child modules"() {
        when:
        mavenGroovy.mvnVersionsUpdateChildModules()
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=false -DgenerateBackupPoms=false', returnStdout: false])
    }

    def "[maven.groovy] run mvn versions update child modules with allow snapshots"() {
        when:
        mavenGroovy.mvnVersionsUpdateChildModules(true)
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=true -DgenerateBackupPoms=false', returnStdout: false])
    }

    def "[maven.groovy] run mvn versions update parent and child modules"() {
        setup:
        def String newVersion = '3.6.2'
        when:
        mavenGroovy.mvnVersionsUpdateParentAndChildModules(newVersion)
        then:
        1 * getPipelineMock("sh")([script: "mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=false -DgenerateBackupPoms=false", returnStdout: false])
        1 * getPipelineMock("sh")([script: 'mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=false -DgenerateBackupPoms=false', returnStdout: false])
    }

    def "[maven.groovy] run mvn versions update parent and child modules with allow snapshots"() {
        setup:
        def String newVersion = '3.6.2'
        when:
        mavenGroovy.mvnVersionsUpdateParentAndChildModules(newVersion, true)
        then:
        1 * getPipelineMock("sh")([script: "mvn -B -N -e versions:update-parent -Dfull -DparentVersion=[${newVersion}] -DallowSnapshots=true -DgenerateBackupPoms=false", returnStdout: false])
        1 * getPipelineMock("sh")([script: 'mvn -B -N -e versions:update-child-modules -Dfull -DallowSnapshots=true -DgenerateBackupPoms=false', returnStdout: false])
    }

    def "[maven.groovy] run mvn set version property"() {
        setup:
        String newVersion = '1.2.3'
        String propertyName = 'version.org.kie.kogito'
        when:
        mavenGroovy.mvnSetVersionProperty(propertyName, newVersion)
        then:
        1 * getPipelineMock("sh")([script: "mvn -B -e versions:set-property -Dproperty=$propertyName -DnewVersion=$newVersion -DallowSnapshots=true -DgenerateBackupPoms=false", returnStdout: false])
    }

    def "[maven.groovy] uploadLocalArtifacts"() {
        setup:
        String mvnUploadCredsId = 'mvnUploadCredsId'
        String mvnUploadCreds = 'user:password'
        mavenGroovy.getBinding().setVariable('kieUnpack', mvnUploadCreds)
        String artifactDir = '/tmp'
        String repoUrl = 'https://redhat.com'
        when:
        mavenGroovy.uploadLocalArtifacts(mvnUploadCredsId, artifactDir, repoUrl)
        then:
        1 * getPipelineMock('usernameColonPassword.call')([credentialsId: mvnUploadCredsId, variable: 'kieUnpack']) >> mvnUploadCreds
        1 * getPipelineMock('withCredentials')([mvnUploadCreds], _ as Closure)
        1 * getPipelineMock('dir')(artifactDir, _ as Closure)
        1 * getPipelineMock('sh')('zip -r artifacts .')
        1 * getPipelineMock('sh')("curl --silent --upload-file artifacts.zip -u ${mvnUploadCreds} -v ${repoUrl}")
    }

    def "[maven.groovy] getLatestArtifactFromRepository OK"() {
        setup:
        String expectedVersion = '7.11.0.redhat-210426'
        String repositoryUrl = 'http://repoUrl'
        String groupId = 'org.kie.rhba'
        String artifactId = 'rhdm'
        def xmlSlurper = GroovySpy(XmlSlurper.class, global: true)
        def gPathResult = Mock(GPathResult.class)
        when:
        def result = mavenGroovy.getLatestArtifactFromRepository(repositoryUrl, groupId, artifactId)
        then:
        1 * xmlSlurper.parse('http://repoUrl/org/kie/rhba/rhdm/maven-metadata.xml') >> gPathResult
        3 * gPathResult.getProperty('versioning') >> gPathResult
        2 * gPathResult.getProperty('latest') >> gPathResult
        1 * gPathResult.text() >> expectedVersion
        expectedVersion == result
    }

    def "[maven.groovy] getLatestArtifactFromRepository null"() {
        setup:
        String expectedVersion = '7.11.0.redhat-210426'
        String repositoryUrl = 'http://repoUrl'
        String groupId = 'org.kie.rhba'
        String artifactId = 'rhdm'
        def xmlSlurper = GroovySpy(XmlSlurper.class, global: true)
        def gPathResult = Mock(GPathResult.class)
        when:
        def result = mavenGroovy.getLatestArtifactFromRepository(repositoryUrl, groupId, artifactId)
        then:
        1 * xmlSlurper.parse('http://repoUrl/org/kie/rhba/rhdm/maven-metadata.xml') >> gPathResult
        1 * gPathResult.getProperty('versioning') >> null
        null == result
    }

    def "[maven.groovy] getLatestArtifactVersionPrefixFromRepository OK"() {
        setup:
        String expectedVersion = '7.52.0.Final-redhat-00004'
        String repositoryUrl = 'http://repoUrl'
        String groupId = 'org.kie'
        String artifactId = 'kie-api'
        def xmlSlurper = GroovySpy(XmlSlurper.class, global: true)
        def gPathResult = Mock(GPathResult.class)
        def versionIterator = [new VersionChildNode('7.52.0.Final'), new VersionChildNode('7.52.0.Final-redhat-00001'), new VersionChildNode('7.52.0.Final-redhat-00004'), new VersionChildNode('7.52.0.Final-redhat-00003'), new VersionChildNode('7.53.0.Final-redhat-00009')].iterator()
        when:
        def result = mavenGroovy.getLatestArtifactVersionPrefixFromRepository(repositoryUrl, groupId, artifactId, '7.52.0.Final-redhat')
        then:
        1 * xmlSlurper.parse('http://repoUrl/org/kie/kie-api/maven-metadata.xml') >> gPathResult
        1 * gPathResult.getProperty('versioning') >> gPathResult
        1 * gPathResult.getProperty('versions') >> gPathResult
        1 * gPathResult.childNodes() >> versionIterator
        expectedVersion == result
    }

    def "[maven.groovy] cleanRepository"() {
        when:
        mavenGroovy.cleanRepository()
        then:
        1 * getPipelineMock('sh')('rm -rf $HOME/.m2/repository')
    }
}
