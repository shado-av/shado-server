# SHADO-server

To read more about the research behind the Simulator for Humans & Automation in Dispatch Operations (SHADO) by Nneji, Victoria Chibuogu (2019). A Workload Model for Designing & Staffing Future Transportation Network Operations (Doctoral dissertation, Duke University), click [here](https://dukespace.lib.duke.edu/dspace/bitstream/handle/10161/18694/Nneji_duke_0066D_14997.pdf).

## Shado backend service
SHADO-server web service is built on Java Spring Boot. 
Spring Boot Tutorial: https://spring.io/guides/gs/spring-boot/

### Build the project locally
To Run the Simulation locally, Go to **DataWrapper.java, GreetingController.java** and comment out all remote server paths, uncomment local paths and change the local path to your current directory. (The **DataWrapper.java** is under "shado-server/core/server/src/main/java/server/Output" folder. The **GreetingController.java** is under "shado-server/core/server/src/main/java/server" folder.)


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
./mvnw package
```
to get a .jar runnable and clone/pull the repo in to the server.

Check out the tutorial here: https://spring.io/blog/2014/03/07/deploying-spring-boot-applications to setup the systemd service.
Finally, use the following command to start the service.
```
sudo systemctl restart shado_server.service
```
### Test the SHADO-server

There is a website that can be used to interact with this server running backend. Here is the link: https://shado-av.github.io/shado-webdev/

If you prefer to use your own JSON file as input, please go to this website: http://apps.hal.pratt.duke.edu/shado-webdev/sim-test.html. In this website, you can use the "Choose File" button to upload your own JSON input, then hit the "Run Simulation" button to run the server. A few result download options will appear after the simulation success. 
If you want a more compact utilization report, please use this link to download it: http://apps.hal.pratt.duke.edu:8080/shado/validation

If you are more comfortable with command line:
Try:
```
curl -H POST http://localhost:8080/shado/runShado -d @shadovar.json \
--header "Content-Type: application/json"
```
(Remember to substitue the **"localhost** to your server's URL) 
To post and you can see the message and session number returned by the server.


### Project Structure

Here is a simple layout of SHADO server-side simulation
![alt text](https://github.com/shado-av/shado-server/blob/master/SHADO-server_structure.jpg)

