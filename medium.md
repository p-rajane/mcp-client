# Building an MCP Client with Spring Boot, Spring AI and Ollama

## Introduction

Large Language Models (LLMs) are excellent at understanding natural-language requests, but an LLM by itself does not have direct access to enterprise applications, databases, REST APIs, or internal business capabilities.

This is where **Model Context Protocol (MCP)** becomes interesting.

MCP provides a standardized mechanism through which AI applications can discover and invoke external tools and capabilities.

In this article, we will walk through a simple **Spring Boot MCP Client** application built using:

* Java 17
* Spring Boot 4.0.7
* Spring AI 2.0.0
* Spring AI MCP Client
* Ollama
* Llama 3.2
* Streamable HTTP MCP transport

The project demonstrates an important architecture:

```text
                         User
                           |
                           | Natural language query
                           v
                +----------------------+
                |   Spring Boot App    |
                |                      |
                |    ChatClient        |
                +----------+-----------+
                           |
                           |
                           v
                    +-------------+
                    |    Ollama   |
                    |   Llama 3.2 |
                    +------+------+
                           |
                     Tool decision
                           |
                           v
                    +-------------+
                    |  MCP Client |
                    +------+------+
                           |
                    MCP / HTTP
                           |
                           v
                    +-------------+
                    | MCP Server  |
                    +------+------+
                           |
                           v
                 Business APIs / DB
```

The important point is that **MCP does not replace REST APIs**.

An MCP server can itself call REST APIs, databases, or other backend systems. MCP provides an AI-friendly standardized interface through which an AI application can access those capabilities.

---

# 1. Project Structure

The project is intentionally small:

```text
mcp-client/
│
├── pom.xml
│
├── README.md
│
├── mvnw
├── mvnw.cmd
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/demo/mcp_client/
    │   │       ├── McpClientApplication.java
    │   │       │
    │   │       └── controller/
    │   │           └── RestController.java
    │   │
    │   └── resources/
    │       └── application.yaml
    │
    └── test/
        └── java/
            └── com/demo/mcp_client/
                └── McpClientApplicationTests.java
```

There are three particularly important files:

```text
pom.xml
application.yaml
RestController.java
```

These three files essentially define the application's architecture.

---

# 2. Maven Configuration

Let's start with `pom.xml`.

The project uses Spring Boot:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.7</version>
</parent>
```

The Spring Boot parent provides dependency management and Maven defaults for the application.

The application is configured for Java 17:

```xml
<properties>
    <java.version>17</java.version>
    <spring-ai.version>2.0.0</spring-ai.version>
</properties>
```

Java 17 is important because Spring Boot 4 requires a modern Java runtime.

---

# 3. Spring WebMVC Dependency

The application contains:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

This provides the web layer required for exposing HTTP endpoints.

In this project, the REST endpoint is:

```text
GET /getInfo
```

The endpoint accepts a query parameter:

```text
/getInfo?query=...
```

The HTTP endpoint is therefore the entry point for the user/application calling the MCP-enabled AI application.

---

# 4. Spring AI MCP Client

The most important MCP dependency is:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

This dependency enables Spring AI's MCP Client capabilities.

The MCP client is responsible for connecting the Spring Boot application to an MCP server and making the server's capabilities available to the AI layer.

Conceptually:

```text
Spring Boot
     |
     | MCP Client
     |
     v
MCP Server
     |
     +-- Tool 1
     +-- Tool 2
     +-- Tool 3
```

The MCP client does not itself implement the business functionality.

Instead, it discovers the tools exposed by the MCP server and makes them available to the AI application.

---

# 5. Ollama Integration

The project also includes:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
```

This integrates Spring AI with Ollama.

Ollama allows LLMs to run locally.

In this project, the selected model is:

```text
llama3.2:latest
```

Therefore, the architecture can run without sending the LLM request to a cloud-based model provider.

The communication looks like:

```text
Spring AI
    |
    | HTTP
    v
Ollama
    |
    v
Llama 3.2
```

---

# 6. Spring AI BOM

The project imports the Spring AI BOM:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

BOM stands for **Bill of Materials**.

Instead of specifying versions for every Spring AI dependency individually, the BOM manages compatible versions.

For example:

```text
spring-ai.version = 2.0.0
```

The dependency versions are then aligned through the BOM.

This is particularly useful because Spring AI contains several related modules that need to work together.

---

# 7. Application Configuration

Now let's look at the most important configuration file:

```text
src/main/resources/application.yaml
```

The application name is configured as:

```yaml
spring:
  application:
    name: mcp-client
```

This gives the Spring Boot application the name:

```text
mcp-client
```

---

# 8. Configuring Ollama

The Ollama configuration is:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
```

By default, Ollama exposes its API on port `11434`.

Therefore:

```text
http://localhost:11434
```

is the address where Spring AI expects Ollama to be running.

The model is configured using:

```yaml
chat:
  options:
    model: llama3.2:latest
```

This means that the Spring AI `ChatClient` will use:

```text
Llama 3.2
```

for chat interactions.

The complete flow is:

```text
HTTP Request
     |
     v
Spring Boot
     |
     v
Spring AI ChatClient
     |
     v
Ollama
     |
     v
Llama 3.2
```

---

# 9. Configuring the MCP Client

The MCP client configuration starts with:

```yaml
spring:
  ai:
    mcp:
      client:
```

The client name is:

```yaml
name: mcp-client
```

and its version is:

```yaml
version: 1.0.0
```

These values identify the MCP client.

---

# 10. Tool Callback Configuration

One of the most important configuration properties is:

```yaml
toolcallback:
  enabled: true
```

This allows MCP tools discovered by the MCP client to be exposed as tool callbacks that Spring AI can provide to the LLM.

This is critical for tool calling.

Without tools, the LLM can generate text but cannot directly execute external operations.

With tools, the LLM can reason:

```text
User request
     |
     v
LLM
     |
     | "I need external information"
     |
     v
Available MCP tools
     |
     v
Select appropriate tool
     |
     v
MCP Server
```

---

# 11. Streamable HTTP MCP Connection

The project uses:

```yaml
streamable-http:
  connections:
    mcp-server:
      url: http://localhost:8080/mcp
```

This tells the MCP client where the MCP server is located.

The MCP server is expected to expose its MCP endpoint at:

```text
http://localhost:8080/mcp
```

The architecture therefore becomes:

```text
Spring Boot MCP Client
          |
          | Streamable HTTP
          |
          v
http://localhost:8080/mcp
          |
          v
      MCP Server
```

Notice that the MCP client itself runs on a different port:

```yaml
server:
  port: 8081
```

So we have:

```text
MCP Server
localhost:8080

MCP Client
localhost:8081

Ollama
localhost:11434
```

This gives us three separate runtime components.

---

# 12. Complete Runtime Architecture

Putting everything together:

```text
                     +----------------+
                     |      User      |
                     +-------+--------+
                             |
                             | HTTP
                             v
                  +----------------------+
                  | Spring Boot MCP      |
                  | Client :8081         |
                  |                      |
                  | REST Controller      |
                  | ChatClient           |
                  | MCP Client           |
                  +----------+-----------+
                             |
                 +-----------+-----------+
                 |                       |
                 |                       |
                 v                       v
        +----------------+       +----------------+
        |    Ollama      |       |   MCP Server   |
        |   :11434       |       |     :8080      |
        |                |       |                |
        | Llama 3.2      |       | MCP Tools      |
        +----------------+       +-------+--------+
                                         |
                                         v
                                Business System
                                REST API / DB
```

This separation is one of the key architectural concepts in MCP-based applications.

---

# 13. Spring Boot Application Class

The application starts with:

```java
@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }
}
```

`@SpringBootApplication` combines three important Spring annotations:

```text
@Configuration
@EnableAutoConfiguration
@ComponentScan
```

This allows Spring Boot to:

1. Configure the application
2. Apply auto-configuration
3. Discover application components

The `main()` method starts the Spring application context.

---

# 14. The REST Controller

The most interesting code is in:

```text
RestController.java
```

The class is declared as:

```java
@org.springframework.web.bind.annotation.RestController
public class RestController {
```

This makes the class a Spring MVC REST controller.

The endpoint exposed by the class is:

```java
@GetMapping("/getInfo")
public String getInfo(@RequestParam("query") String query)
```

Therefore, the application exposes:

```text
GET http://localhost:8081/getInfo?query=...
```

For example:

```text
http://localhost:8081/getInfo?query=What is the status of order 1001?
```

---

# 15. ChatClient

The controller contains:

```java
private final ChatClient chatClient;
```

`ChatClient` is Spring AI's high-level API for interacting with chat models.

Instead of manually constructing HTTP requests to Ollama, the application can use:

```java
chatClient.prompt()
```

This provides a fluent API for constructing an AI request.

---

# 16. Constructor Injection

The constructor is:

```java
public RestController(
        ChatClient.Builder builder,
        ToolCallbackProvider toolCallbackProvider) {
```

Two important Spring-managed objects are injected:

```text
ChatClient.Builder
ToolCallbackProvider
```

This is constructor-based dependency injection.

The advantage is that the controller does not have to manually create the objects.

Spring Boot/Spring AI creates and configures them based on the application configuration and available dependencies.

---

# 17. ToolCallbackProvider

This is arguably the most important part of the controller:

```java
Arrays.stream(toolCallbackProvider.getToolCallbacks())
    .forEach(toolCallback -> {
        System.out.println(
            "Tool Definition: " + toolCallback.getToolDefinition()
        );
    });
```

Let's break this down.

The MCP server exposes tools.

For example, imagine the MCP server exposes:

```text
getCustomer
getOrder
searchOrders
```

The MCP client discovers these tools.

Spring AI represents them through tool callbacks.

Conceptually:

```text
MCP Server
    |
    | Tool definitions
    v
MCP Client
    |
    v
ToolCallbackProvider
    |
    +-- ToolCallback
    +-- ToolCallback
    +-- ToolCallback
```

The code:

```java
toolCallbackProvider.getToolCallbacks()
```

retrieves the discovered tool callbacks.

The application then prints their definitions.

This is useful during development because it allows you to see which tools have been discovered from the MCP server.

---

# 18. Why Tool Definitions Matter

An LLM cannot simply guess how to call an arbitrary Java method.

It needs structured information about the available tool.

A tool definition conceptually describes:

```text
Tool name
Description
Input parameters
Input schema
```

For example:

```text
Tool:
getCustomer

Description:
Retrieve customer details.

Input:
customerId : integer
```

The LLM can use this information to decide whether the tool is appropriate for the user's request.

This is the bridge between:

```text
Natural language
```

and:

```text
Executable capability
```

---

# 19. Registering MCP Tools with ChatClient

The most important line in the constructor is:

```java
this.chatClient = builder
        .defaultTools(toolCallbackProvider.getToolCallbacks())
        .build();
```

This tells Spring AI:

> Make the MCP-discovered tools available to this ChatClient.

The sequence is:

```text
MCP Server
     |
     | exposes tools
     v
MCP Client
     |
     v
ToolCallbackProvider
     |
     | getToolCallbacks()
     v
ChatClient.Builder
     |
     | defaultTools(...)
     v
ChatClient
```

Now the LLM can potentially use those tools during a conversation.

---

# 20. The `/getInfo` Endpoint

The endpoint implementation is:

```java
@GetMapping("/getInfo")
public String getInfo(@RequestParam("query") String query) {
    return chatClient.prompt()
            .user(query)
            .call()
            .content();
}
```

This small piece of code hides quite a lot of functionality.

Let's break it down.

---

# 21. `chatClient.prompt()`

The first call:

```java
chatClient.prompt()
```

starts building a chat interaction.

Think of it as:

```text
Create an AI request
```

---

# 22. `.user(query)`

Next:

```java
.user(query)
```

adds the user's input.

For example:

```text
query = "Find information about customer 1001"
```

The resulting AI interaction contains the user's request.

---

# 23. `.call()`

The next call:

```java
.call()
```

executes the interaction.

This is where Spring AI orchestrates the communication with the configured model.

Because tools have been registered with the `ChatClient`, the model can potentially decide that it needs to call an MCP tool.

The conceptual flow is:

```text
User Query
    |
    v
ChatClient
    |
    v
LLM
    |
    | Does the request require a tool?
    |
    +---- No ----> Generate answer
    |
    +---- Yes ---> Select MCP tool
                         |
                         v
                     MCP Client
                         |
                         v
                     MCP Server
                         |
                         v
                     Tool result
                         |
                         v
                        LLM
                         |
                         v
                   Final response
```

---

# 24. `.content()`

Finally:

```java
.content();
```

extracts the generated textual response.

The controller returns that string directly to the HTTP client.

Therefore:

```text
GET /getInfo
        |
        v
ChatClient
        |
        v
LLM + MCP Tools
        |
        v
Generated response
        |
        v
HTTP Response
```

---

# 25. Where Does the LLM Fit?

This is an important concept when learning MCP.

The MCP Client is **not the LLM**.

They have different responsibilities.

```text
LLM
 |
 | Understands intent
 | Reasons about available tools
 | Chooses tools
 | Generates final response
 |
 v
MCP Client
 |
 | Communicates using MCP
 |
 v
MCP Server
 |
 | Executes/exposes capabilities
 |
 v
Backend System
```

For example, if the user asks:

```text
"What's the status of order 123?"
```

the LLM can determine:

```text
I need order information.
```

If the MCP server has:

```text
getOrder(orderId)
```

the LLM can choose that tool.

The MCP client then handles the MCP communication.

---

# 26. MCP Does Not Replace REST

It is important not to misunderstand the role of MCP.

Suppose the existing backend has:

```text
GET /orders/123
```

You don't necessarily replace that REST API with MCP.

A common architecture is:

```text
                 AI Application
                       |
                     MCP
                       |
                       v
                  MCP Server
                       |
                     REST
                       |
                       v
                  Order Service
                       |
                       v
                    Database
```

The MCP server can act as an AI-facing adapter over existing enterprise capabilities.

This allows existing REST services to continue working while AI applications gain standardized tool access.

---

# 27. End-to-End Example

Suppose the MCP server exposes:

```text
getWeather(city)
```

The user calls:

```text
GET /getInfo?query=What is the weather in Mumbai?
```

The request reaches the Spring Boot application.

The controller executes:

```java
chatClient.prompt()
        .user(query)
        .call()
        .content();
```

Spring AI sends the request to the configured LLM.

The LLM sees that a weather tool is available.

It determines:

```text
Tool = getWeather
Argument = Mumbai
```

The MCP client invokes the MCP server.

The MCP server executes its implementation.

The result might be:

```json
{
  "city": "Mumbai",
  "temperature": 29,
  "condition": "Cloudy"
}
```

That result is returned to the LLM.

The LLM can then produce:

```text
The current weather in Mumbai is 29°C and cloudy.
```

The Spring Boot controller returns that response to the caller.

---

# 28. What Happens During Application Startup?

When the application starts, Spring Boot creates the application context.

The MCP client configuration tells Spring AI to connect to:

```text
http://localhost:8080/mcp
```

The MCP client can establish communication with the configured MCP server and discover the server's capabilities.

The resulting tool information becomes available through:

```java
ToolCallbackProvider
```

The controller then registers those callbacks with:

```java
ChatClient
```

Therefore, by the time `/getInfo` is called, the ChatClient has access to the discovered tools.

---

# 29. Why Use ToolCallbackProvider?

One advantage of this approach is that the controller doesn't need to hard-code every MCP tool.

You don't need code like:

```java
getCustomerTool();
getOrderTool();
getPaymentTool();
getTicketTool();
```

Instead, the MCP infrastructure provides the discovered tools:

```java
toolCallbackProvider.getToolCallbacks()
```

This makes the application more flexible.

The MCP server can expose capabilities, while the client can make those capabilities available to the AI layer.

---

# 30. Application Port Configuration

The application is configured to run on:

```yaml
server:
  port: 8081
```

Therefore:

```text
Spring Boot MCP Client
http://localhost:8081
```

The API endpoint becomes:

```text
http://localhost:8081/getInfo
```

Meanwhile, the MCP server is configured separately:

```text
http://localhost:8080/mcp
```

and Ollama:

```text
http://localhost:11434
```

So a local setup looks like:

```text
Port 8081
    |
    +-- Spring Boot MCP Client

Port 8080
    |
    +-- MCP Server

Port 11434
    |
    +-- Ollama
        |
        +-- Llama 3.2
```

---

# 31. Test Class

The project also contains:

```java
@SpringBootTest
class McpClientApplicationTests {

    @Test
    void contextLoads() {
    }

}
```

The important annotation is:

```java
@SpringBootTest
```

It loads the Spring application context for testing.

The test:

```java
contextLoads()
```

essentially verifies that the application context can start successfully.

This is useful for catching configuration and dependency injection problems.

For example, if a required bean cannot be created, the test can fail before the application is deployed.

---

# 32. Maven Wrapper

The project contains:

```text
mvnw
mvnw.cmd
```

These are Maven Wrapper scripts.

On Windows:

```bash
mvnw.cmd clean package
```

On Linux/macOS:

```bash
./mvnw clean package
```

The wrapper helps ensure that the project can be built using the Maven version expected by the project without requiring Maven to already be installed globally.

---

# 33. Running the Application

Before starting the Spring Boot application, the required services need to be available.

### Start Ollama

Ollama should be available at:

```text
http://localhost:11434
```

and the configured model should exist:

```text
llama3.2:latest
```

### Start the MCP Server

The MCP server should be running at:

```text
http://localhost:8080/mcp
```

### Start the MCP Client

Run:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The Spring Boot application will run on:

```text
http://localhost:8081
```

---

# 34. Calling the Application

Once everything is running, call:

```text
GET http://localhost:8081/getInfo?query=...
```

For example:

```text
http://localhost:8081/getInfo?query=Get information about customer 101
```

The exact query depends on what tools your MCP server exposes.

The important thing is that the client doesn't have to know the implementation details of every tool.

The LLM can decide which available capability is relevant.

---

# 35. Complete Request Lifecycle

Let's summarize the entire flow.

```text
1. User sends HTTP request
             |
             v
2. Spring Boot Controller
             |
             v
3. ChatClient receives query
             |
             v
4. Spring AI sends request to LLM
             |
             v
5. LLM evaluates available tools
             |
             v
6. LLM decides whether a tool is required
             |
        +----+----+
        |         |
       No        Yes
        |         |
        |         v
        |    MCP Client
        |         |
        |         v
        |    MCP Server
        |         |
        |         v
        |    Tool execution
        |         |
        |         v
        |    Tool response
        |         |
        +----<----+
             |
             v
7. LLM generates final answer
             |
             v
8. ChatClient.content()
             |
             v
9. HTTP response
```

This is the central concept behind the project.

---

# 36. Why This Architecture Is Useful

Without MCP, an AI application might need custom integrations for every backend system:

```text
LLM Application
    |
    +---- Customer REST API
    |
    +---- Order REST API
    |
    +---- Payment REST API
    |
    +---- Ticket REST API
    |
    +---- Inventory REST API
```

As the number of systems increases, the AI application becomes tightly coupled to individual APIs.

With MCP:

```text
                 AI Application
                       |
                    MCP Client
                       |
         +-------------+-------------+
         |             |             |
         v             v             v
     MCP Server    MCP Server    MCP Server
         |             |             |
     Customer        Order        Payment
      System         System        System
```

MCP provides a standardized AI-facing protocol.

This can significantly simplify how AI agents interact with enterprise capabilities.

---

# 37. Important Architectural Distinction

It is useful to keep these responsibilities separate:

```text
+----------------+--------------------------------+
| Component      | Responsibility                 |
+----------------+--------------------------------+
| LLM            | Reasoning and language         |
| Spring AI      | AI application abstraction     |
| ChatClient     | Chat interaction orchestration |
| MCP Client     | MCP communication              |
| MCP Server     | Exposes tools/resources        |
| REST API       | Business integration           |
| Database       | Persistent data                |
+----------------+--------------------------------+
```

A common misconception is:

> "MCP retrieves data, so why do I need an LLM?"

The answer is that these components solve different problems.

The MCP server provides the capability.

The MCP client communicates with it.

The LLM decides **when and why that capability should be used**.

---

# 38. A Better Mental Model

Think about the system like this:

```text
             LLM
              |
       "What should I do?"
              |
              v
         MCP Client
              |
       "How do I invoke it?"
              |
              v
         MCP Server
              |
       "How do I execute it?"
              |
              v
     REST / DB / External API
```

Or even more simply:

```text
LLM       = Brain
MCP       = Standardized tool interface
MCP Server = Tool provider
REST/DB   = Actual backend systems
Spring AI = Glue between application and AI model
```

---

# 39. Conclusion

This Spring Boot project demonstrates a compact but important AI architecture.

The application combines:

```text
Spring Boot
     +
Spring AI
     +
Ollama
     +
Llama 3.2
     +
MCP Client
     +
MCP Server
```

The most important concept is that these technologies have different responsibilities.

**Spring Boot** provides the application framework and REST endpoint.

**Spring AI** provides the abstraction for interacting with the LLM and integrating AI capabilities into the Spring application.

**Ollama** provides local model execution.

**Llama 3.2** acts as the reasoning and language model.

**MCP Client** connects the AI application to MCP servers.

**MCP Server** exposes tools and resources.

And finally, the underlying **REST APIs, databases, or enterprise systems** perform the actual business operations.

The resulting architecture is:

```text
                 User
                   |
                   v
            Spring Boot API
                   |
                   v
              Spring AI
                   |
                   v
                 LLM
                   |
          "Which tool do I need?"
                   |
                   v
              MCP Client
                   |
                   | MCP
                   v
              MCP Server
                   |
             REST / DB / API
                   |
                   v
              Tool Result
                   |
                   v
                  LLM
                   |
                   v
            Natural Language
                Response
```

And that is the key difference between a traditional REST integration and an AI-enabled MCP architecture:

**REST tells an application how to access a capability. MCP standardizes how AI applications can discover and use capabilities, while the LLM provides the reasoning needed to decide which capability to invoke.**
