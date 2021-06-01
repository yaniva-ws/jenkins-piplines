def call(version) {
	def bumpVersion = load "bumpVersion.groovy"
	echo bumpVersion.call(${version})
}

