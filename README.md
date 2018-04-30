# SHADO-server
## Shado backend service
SHADO-server web service is built on Java Spring Boot. 
Spring Boot Tutorial: https://spring.io/guides/gs/spring-boot/

### Build the project locally
To Run the Simulation locally, Go to **DataWrapper.java, ProcRep.Java, GreetingController.java** and comment out all remote server path and uncommen local path.

```
   //String summary_file_name =   "/home/rapiduser/shado-server/core/server/out/Summary/" + "Workload_Summary.csv"; 
   String summary_file_name =   localSummary + "Workload_Summary.csv";
```
After switching to the local file path, use the following command to build the project.

```
 ./mvnw spring-boot:run
```
Make sure you are in this directory: **shado-server/core/server/ **
### Deploy the service

To Deploy the service in a server. You should use systemd to let the program run in the background.

Use:
```
mvn package
```
to get a .jar runnable and clone/pull the repo in to the server.

Check out the tutorial here: https://spring.io/blog/2014/03/07/deploying-spring-boot-applications to setup the systemd service.
Finally, use the following command to start the service.
```
sudo systemctl start shado_server.service
```
### Test the SHADO-server

There is simple webapp that can test basic posting function in the backend. http://apps.hal.pratt.duke.edu/shado-web-simple/index.html

If you are more comfortable with command line:
Try:
```
curl -H POST http://localhost:8080/shado/testpost -d @shadovar.json \
--header "Content-Type: application/json"
```
(Remember to substitue the **"localhost** to your server's URL) 
To post and you can see the message and session number returned by the server.

### Project Structure

Here is a simple layout of SHADO server-side simulation
![alt text](https://github.com/shado-av/shado-server/blob/master/SHADO-server_structure.jpg)

