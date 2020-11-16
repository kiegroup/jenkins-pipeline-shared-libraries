import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kie.jenkins.MavenStagingHelper

class MavenStagingHelperSpec extends JenkinsPipelineSpecification {
    def steps

    def setup() {
        steps = new Step() {
            @Override
            StepExecution start(StepContext stepContext) throws Exception {
                return null
            }
        }
    }

    def "[MavenStagingHelper.groovy] stageLocalArtifacts"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.stageLocalArtifacts('ID', 'FOLDER')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -q help:evaluate -Dexpression=project.artifactId -DforceStdout', returnStdout: true]) >> { return 'NAME' }
        1 * getPipelineMock("sh")([script: 'mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout', returnStdout: true]) >> { return 'VS' }
        1 * getPipelineMock("sh")([script: "mvn -B --projects :NAME org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:deploy-staged-repository -DnexusUrl=https://repository.jboss.org/nexus -DserverId=jboss-releases-repository -DstagingDescription='NAME VS' -DkeepStagingRepositoryOnCloseRuleFailure=true -DstagingProgressTimeoutMinutes=10 -DrepositoryDirectory=FOLDER -DstagingProfileId=ID", returnStdout: false])
        1 * getPipelineMock("sh")([script: 'find FOLDER -name *.properties', returnStdout: true]) >> { return 'file.properties' }
        1 * getPipelineMock("readProperties")([file: 'file.properties']) >> { return ['stagingRepository.id':'STAGING_ID'] }
        'STAGING_ID' == helper.stagingRepositoryId
    }

    def "[MavenStagingHelper.groovy] stageLocalArtifacts empty stagingProfileId"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.stageLocalArtifacts('', 'FOLDER')
        then:
        thrown(AssertionError)
    }

    def "[MavenStagingHelper.groovy] stageLocalArtifacts empty artifacts folder"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.stageLocalArtifacts('ID', '')
        then:
        thrown(AssertionError)
    }

    def "[MavenStagingHelper.groovy] promoteStagingRepository"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.withStagingRepositoryId('STAGING_ID')
            .promoteStagingRepository('ID')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -q help:evaluate -Dexpression=project.artifactId -DforceStdout', returnStdout: true]) >> { return 'NAME' }
        1 * getPipelineMock("sh")([script: 'mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout', returnStdout: true]) >> { return 'VS' }
        1 * getPipelineMock("sh")([script: "mvn -B --projects :NAME org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:promote -DnexusUrl=https://repository.jboss.org/nexus -DserverId=jboss-releases-repository -DstagingDescription='NAME VS' -DbuildPromotionProfileId=ID -DstagingRepositoryId=STAGING_ID", returnStdout: false])
    }

    def "[MavenStagingHelper.groovy] promoteStagingRepository empty buildPromotionProfileId"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.withStagingRepositoryId('STAGING_ID')
            .promoteStagingRepository('')
        then:
        thrown(AssertionError)
    }

    def "[MavenStagingHelper.groovy] promoteStagingRepository no stagingRepositoryId"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.promoteStagingRepository('ID')
        then:
        thrown(AssertionError)
    }

    def "[MavenStagingHelper.groovy] full process"() {
        setup:
        def helper = new MavenStagingHelper(steps)
                        .withNexusReleaseUrl('NEXUS_URL')
                        .withNexusReleaseRepositoryId('NEXUS_REPOSITORY_ID')
                        .withStagingDescription('DESCRIPTION')
        getPipelineMock("sh")([script: 'mvn -B -q help:evaluate -Dexpression=project.artifactId -DforceStdout', returnStdout: true]) >> { return 'NAME' }
        getPipelineMock("sh")([script: 'mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout', returnStdout: true]) >> { return 'VS' }
        getPipelineMock("sh")([script: 'find FOLDER -name *.properties', returnStdout: true]) >> { return 'file.properties' }
        getPipelineMock("readProperties")([file: 'file.properties']) >> { return ['stagingRepository.id':'STAGING_ID'] }
        helper.stageLocalArtifacts('STAGE_PROFILE_ID', 'FOLDER')
        when:
        helper.promoteStagingRepository('BUILD_PROMOTE_ID')
        then:
        1 * getPipelineMock("sh")([script: "mvn -B --projects :NAME org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:promote -DnexusUrl=NEXUS_URL -DserverId=NEXUS_REPOSITORY_ID -DstagingDescription='DESCRIPTION' -DbuildPromotionProfileId=BUILD_PROMOTE_ID -DstagingRepositoryId=STAGING_ID", returnStdout: false])
    }
}
