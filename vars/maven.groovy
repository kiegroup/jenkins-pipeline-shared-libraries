import java.util.Properties
import groovy.xml.*
import groovy.xml.dom.*

def runMavenWithSettings(String settingsXmlId, String goals, Properties properties, String logFileName = null) {
    configFileProvider([configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
        def propertiesString = ''

        properties.each { key, value ->
            propertiesString += " -D$key=$value"
        }

        def teeCommand = logFileName ? ' | tee $WORKSPACE/'+ logFileName + ' ; test ${PIPESTATUS[0]} -eq 0' : ''
        def mvnCommand = "mvn -B -s $MAVEN_SETTINGS_XML -fae ${goals}${propertiesString}${teeCommand}"
        sh mvnCommand
    }
}

def runMavenWithSettings(String settingsXmlId, String goals, boolean skipTests, String logFileName = null) {
    Properties properties = new Properties()
    properties.put('skipTests', skipTests)
    runMavenWithSettings(settingsXmlId, goals, properties, logFileName)
}

def runMavenWithSubmarineSettings(String goals, boolean skipTests, String logFileName = null) {
    runMavenWithSettings('9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', goals, skipTests, logFileName)
}

def runMavenWithSubmarineSettings(String goals, Properties properties, String logFileName = null) {
    runMavenWithSettings('9239af2e-46e3-4ba3-8dd6-1a814fc8a56d', goals, properties, logFileName)
}

/**
 *
 * @param settingsXmlId settings.xml file
 * @param goals maven gals
 * @param sonarCloudId Jenkins token for SonarCloud*
 */
def runMavenWithSettingsSonar(String settingsXmlId, String goals, String sonarCloudId, String logFileName = null) {
    configFileProvider([configFile(fileId: settingsXmlId, variable: 'MAVEN_SETTINGS_XML')]) {
        withCredentials([string(credentialsId: sonarCloudId, variable: 'TOKEN')]) {
            def teeCommand = logFileName ? ' | tee $WORKSPACE/'+ logFileName + ' ; test ${PIPESTATUS[0]} -eq 0' : ''
            sh "mvn -B -s $MAVEN_SETTINGS_XML -Dsonar.login=${TOKEN} ${goals}${teeCommand}"
        }
    }
}

/**
 *
 * @param newVersion New value for the maven version
 */
def updateMavenVersion(newVersion){
    def xml = readFile file: "pom.xml"
    def pom = updatePomElement(xml, "version.maven", newVersion)
    writeFile file: "pom.xml", text: pom
}

/**
 *
 * @param xml pom.xml file
 * @param elementName pom property that will be updated
 * @param newValue Value used to update elementName
 */
def updatePomElement(xml, elementName, newValue) {
  def index = xml.indexOf('<project')
  def header = xml.take(index)
  def xmlDom = DOMBuilder.newInstance().parseText(xml)
  def root = xmlDom.documentElement
  use(DOMCategory) {
    def versions = xmlDom.getElementsByTagName(elementName)
    if (versions.length == 0) {
      println "[INFO] No element found called ${elementName}"
    } else {
        def version = versions.item(0)
        version.textContent = newValue
        def newXml = XmlUtil.serialize(root)
        return header + newXml.minus('<?xml version="1.0" encoding="UTF-8"?>')
    }
  }
}
