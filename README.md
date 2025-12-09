# SchneaggchatV3server
Schneaggchat server for v3

# How to build
## Server
There are two docker - setups included: The one in the main structure for Localhost execution, and one in the server_docker folder for executing on a remote server (this one pulls the github repository during build). Just clone the project and Use the Dockerfile and docker-compose.yaml to start the server. 

## Localhost
* Install docker desktop (Windows), Install docker (linux) and sudo systemctl start docker
* Open project in Intellij Idea Ultimate (Basic version does not support Docker execution)
* Add run configuration
    * Top Right Center -> Current file dropdown -> Edit Configurations
    * Add new run configuration -> Docker compose
    * Name: Localhost(Title where the "Current File" text is)
    * Select compose file (./docker-compose.yml)
    * Modify dropdown -> Build -> select Always (Always rebuild for the changes to take effect)
    * Press ok
* Ready to build!

## Fast build without docker (Just for compile errors)
* On the right side click on Gradle -> Tasks -> application -> bootRun
* Main Project is now shown in the run config and can be used

## Port
The server will run on port 8080

# Features
