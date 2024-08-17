# conntest
Connection test tool for testing local router, isp router and internet

### API Swagger documentation
1. Run the application locally
2. Access Swagger UI: http://localhost:8080/swagger-ui/index.html#/Ping

### Create Windows executable
Run `mvn clean package` command and use the shaded version to create the executable. 

NOTE: Before executing the following command, make sure the jar used as input is located in a different directory other than the working directory. [+Info](https://stackoverflow.com/questions/64741275/jpackage-type-app-image-creates-infinite-recursive-directories)

Ex:
`jpackage --input C:\contest --name ConTest --main-jar conntest.jar --type app-image`

### CI - Jenkins vs GitHub Actions
Both technologies have been implemented. But only GitHub Actions is being use, since Jenkins requires a dedicated server

### Ping simulator
A ping simulato has been implemented, it can be enabled by setting the following property:
`conntest.simulator = disabled`

The simulator only works upon ip address 8.8.8.8. It simulates lost packages based on a random number.
Tha simulator class is **PingSimulator.java**