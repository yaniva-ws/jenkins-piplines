def call(version){
    def versionParts = version.tokenize('.')
    major = versionParts[0].toInteger()
    minor = versionParts[1].toInteger()
    patch = versionParts[2].toInteger()
    return "${major}.${minor}.${patch+1}-SNAPSHOT"
}
