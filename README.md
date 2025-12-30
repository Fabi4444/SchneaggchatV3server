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

# API Endpoints

## Authentication
| Method | Endpoint | Description | Parameters |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Register a new user | **Multipart/Form-Data**:<br>`username`: String<br>`password`: String (8+ chars, 1 digit, 1 upper, 1 lower)<br>`email`: String<br>`birthDate`: String<br>`profilepic`: File |
| `POST` | `/auth/login` | Login user | **Body**:<br>`username`: String<br>`password`: String |
| `POST` | `/auth/refresh` | Refresh access token | **Body**:<br>`refreshToken`: String |
| `GET` | `/auth/verify_email` | Verify email address | **Query**:<br>`token`: String |
| `POST` | `/auth/send_delete_email` | Send account deletion email | **Query**:<br>`email`: String |
| `GET` | `/auth/delete_account` | Delete account via token | **Query**:<br>`token`: String |

## Groups
| Method | Endpoint | Description | Parameters |
| :--- | :--- | :--- | :--- |
| `POST` | `/groups/create` | Create a new group | **Multipart/Form-Data**:<br>`name`: String<br>`memberlist[]`: List<String><br>`description`: String<br>`profilepic`: File |
| `POST` | `/groups/sync` | Sync groups for caching | **Body**:<br>List of `{ id: String, timeStamp: String }` |
| `GET` | `/groups/profilepic/{id}` | Get group profile picture | **Path**:<br>`id`: String |

## Messages
| Method | Endpoint | Description | Parameters |
| :--- | :--- | :--- | :--- |
| `POST` | `/messages/send/text` | Send a text message | **Body**:<br>`receiverId`: String<br>`groupMessage`: Boolean<br>`msgType`: String (TEXT, IMAGE)<br>`content`: String<br>`answerId`: String (Optional) |
| `POST` | `/messages/sync` | Sync messages | **Query**:<br>`page`: Int (default 0)<br>`page_size`: Int (default 400)<br>**Body**:<br>List of `{ id: String, timeStamp: String }` |
| `POST` | `/messages/setread` | Mark messages as read | **Query**:<br>`userid`: String<br>`group`: Boolean<br>`timestamp`: Long |
| `POST` | `/messages/edit` | Edit a message | **Body**:<br>`messageId`: String<br>`newContent`: String |
| `DELETE` | `/messages/delete` | Delete a message | **Query**:<br>`messageid`: String |

## Users
| Method | Endpoint | Description | Parameters |
| :--- | :--- | :--- | :--- |
| `POST` | `/users/verificationemail` | Send verification email | *(Auth Token Required)* |
| `POST` | `/users/setfirebasetoken` | Set Firebase token | **Query**:<br>`token`: String |
| `POST` | `/users/changeusername` | Change username | **Body**:<br>New Username (String) |
| `POST` | `/users/changepassword` | Change password | **Body**:<br>`oldPassword`: String<br>`newPassword`: String |
| `POST` | `/users/sync` | Sync users contact data | **Body**:<br>List of `{ id: String, timeStamp: String }` |
| `GET` | `/users/profilepic/{id}` | Get user profile picture | **Path**:<br>`id`: String |
| `POST` | `/users/setprofilepic` | Set user profile picture | **MultiPart**:<br>`file` (implicit in body) |
| `POST` | `/users/changeprofile` | Change user profile details | **Body**:<br>`userId`: String<br>`newDescription`: String (Optional)<br>`newStatus`: String (Optional) |
| `GET` | `/users/availableusers` | Search or list available users | **Query**:<br>`searchterm`: String (Optional) |
| `GET` | `/users/addfriend/{id}` | Send friend request | **Path**:<br>`id`: String |
| `GET` | `/users/denyfriend/{id}` | Deny friend request | **Path**:<br>`id`: String |

## General
| Method | Endpoint | Description | Parameters |
| :--- | :--- | :--- | :--- |
| `GET` | `/public/test` | Health check | - |
