def call(String name = 'User') {
		hello(${name})"
}

def hello(name){
	echo ${name}
}
