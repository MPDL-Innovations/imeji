node {
	def JAVA_HOME = tool name: 'Java 8', type: 'jdk'
	def  mvnHome = tool name: 'Maven35', type: 'maven'

  	stage ('Checkout'){
	   // Checkout code from repository
	   checkout scm
	}

	stage ('Build'){	
		// Build with maven
		sh("${mvnHome}/bin/mvn clean install")	  
	}
	
   	stage ('Deploy'){
	   	echo "We are currently working on branch: ${env.BRANCH_NAME}"
	   
	    switch (env.BRANCH_NAME){
	    	case 'dev':
	    		echo "Deploy to dev";
		   		sshagent(credentials: ['59cb9a3a-7463-44b4-befe-457eac3bd014']) {
		   		   sh 'echo SSH_AUTH_SOCK=$SSH_AUTH_SOCK'
       			   sh 'ls -al $SSH_AUTH_SOCK || true'
				   sh "scp -vvv -o StrictHostKeyChecking=no target/imeji.war saquet@dev-imeji.mpdl.mpg.de:/var/lib/tomcat8/webapps"
				}
	    		break;
	    	case 'qa':
	    		echo "deploy to qa";
	    		break;
	    	default:
	    		echo "no deployment";
	    }
	}

}