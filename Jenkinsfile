/*
* DISABLED
*/
//pipeline{
//    agent any
//    tools {
//        // 'maven' tool is pre-configured in Manage Jenkins -> Tools -> Maven installations
//        maven 'maven'
//    }
//    stages{
//        stage("Clean Up"){
//            steps{
//                deleteDir()
//            }
//        }
//        stage("Clone Repo"){
//            steps{
//                sh "git clone git@github.com:alexander-dieser/conntest.git"
//            }
//        }
//        stage("Build"){
//            steps{
//                dir("conntest"){
//                    sh "mvn clean install"
//                }
//
//                jacoco buildOverBuild: true, sourceInclusionPattern: '**/*.java'
//            }
//        }
//        stage("Test"){
//            steps{
//                dir("conntest"){
//                    sh "mvn test"
//                }
//            }
//        }
//
//    }
//}
