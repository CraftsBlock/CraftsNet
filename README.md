# CraftsNet 
### Easy creation of HTTP routes and WebSocket endpoints in Java.

![Latest Release on Maven](https://repo.craftsblock.de/api/badge/latest/releases/de/craftsblock/craftsnet?color=40c14a&name=CraftsNet&prefix=v)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/CraftsBlock/CraftsNet)
![GitHub](https://img.shields.io/github/license/CraftsBlock/CraftsNet)
![GitHub all releases](https://img.shields.io/github/downloads/CraftsBlock/CraftsNet/total)
![GitHub issues](https://img.shields.io/github/issues-raw/CraftsBlock/CraftsNet)

---

CraftsNet is an easy-to-use Java library that enables developers to effortlessly create and manage HTTP routes and WebSocket endpoints in their server applications. This library greatly simplifies server development, allowing developers to focus on the actual functionality of their application without having to deal with complex server details.

## Functions

- Define and manage HTTP routes for different endpoints of the application.
- Support for dynamic parameters in HTTP routes for more flexible functionality.
- Effortless creation of WebSocket endpoints for real-time communication between server and client.
- SSL support for secure and encrypted communication with users.
- Addon based for easy expansion and maintenance

## Installation

### Maven
```xml
<repositories>
  ...
  <repository>
    <id>craftsblock-releases</id>
    <name>CraftsBlock Repositories</name>
    <url>https://repo.craftsblock.de/releases</url>
  </repository>
</repositories>
```
```xml
<dependencies>
  ...
  <dependency>
    <groupId>de.craftsblock</groupId>
    <artifactId>craftsnet</artifactId>
    <version>VERSION</version>
  </dependency>
</dependencies>
```

### Gradle
```gradle
repositories {
  ...
  maven { url "https://repo.craftsblock.de/releases" }
  mavenCentral()
}
```
```gradle
dependencies {
  ...
  implementation "de.craftsblock:craftsnet:VERSION"
}
```

## Quick Start

1. Create an `addon.json` in your project in which you specify where your main class is located and what your addon is called.
```json
{"name": "My Addon","main": "de.craftsblock.myaddon.MyAddon"}
```

2. HTTP endpoints can be created as shown in the following example.
```java
package de.craftsblock.myaddon;

import de.craftsblock.backend.api.http.Exchange;
import de.craftsblock.backend.api.http.RequestHandler;
import de.craftsblock.backend.api.http.Route;

public class MyRoute implements RequestHandler {
    
  @Route("/v1/route")
  public void handleRoute(Exchange exchange) {
    // Process the request
  }
    
}
```

3. The following source code shows you how to create a WebSocket endpoint.
```java
package de.craftsblock.myaddon;

import de.craftsblock.backend.api.websocket.MessageReceiver;
import de.craftsblock.backend.api.websocket.Socket;
import de.craftsblock.backend.api.websocket.SocketExchange;
import de.craftsblock.backend.api.websocket.SocketHandler;

public class MySocket implements SocketHandler {
    
  @Socket("/v1/socket")
  public void handleSocketMessage(SocketExchange exchange, String data) {
    // Process the socket message
  }
    
}
```

4. Last but not least, you need the heart of your application, the Addon class. This is where all routes and endpoints are registered.
```java
package de.craftsblock.myaddon;

import de.craftsblock.craftsnet.addon.Addon;

public class MyAddon extends Addon {

  @Override
  public void onEnable() {
    routeRegistry().register(new MyRoute());
    routeRegistry().register(new MySocket());
  }

  @Override
  public void onDisable() { }

}
```

Please visit our [Wiki](https://github.com/CraftsBlock/CraftsNet/wiki) for a more detailed description.

## Enable SSL
To enable SSL and enable a secure connection, you can simply add your SSL certificates. Currently only certificates issued by LetsEncrypt are accepted. You need the `fullchain.pem` and the `privkey.pem` which you have to put in the folder `certificates` so that CraftsNet recognizes them.

You can then enable SSL by appending `--ssl` to your startup command.

## Open Source Licenses
We are using some third party open source libraries. Below you find a list of all third party open source libraries used:

| Name                                                                   | Description                                                                                                                           | Licecnse                                                                                    |
|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| [CraftsCore](https://github.com/CrAfTsArMy/CraftsCore)                 | https://repo.craftsblock.de/#/releases/de/craftsblock/craftscore                                                                      | [Apache License 2.0](https://github.com/CrAfTsArMy/CraftsCore/blob/master/LICENSE)          |
| [GSON](https://github.com/google/gson)                                 | A Java serialization/deserialization library to convert Java Objects into JSON and back                                               | [Apache License 2.0](https://github.com/google/gson/blob/main/LICENSE)                      |
| [Apache Tika](https://github.com/apache/tika)                          | The Apache Tika toolkit detects and extracts metadata and text from over a thousand different file types (such as PPT, XLS, and PDF). | [Apache License 2.0](https://github.com/apache/tika/blob/main/LICENSE.txt)                  |
| [JetBrains Annotations](https://github.com/JetBrains/java-annotations) | Annotations for JVM-based languages.                                                                                                  | [Apache License 2.0](https://github.com/JetBrains/java-annotations/blob/master/LICENSE.txt) |
| [Apache Maven](https://github.com/apache/maven/tree/master)            | Apache Maven core                                                                                                                     | [Apache License 2.0](https://github.com/apache/maven/blob/master/LICENSE)                   |
| [Apache Maven-Resolver](https://github.com/apache/maven-resolver)      | Apache Maven Artifact Resolver                                                                                                        | [Apache License 2.0](https://github.com/apache/maven-resolver/blob/master/LICENSE)          |

## Support and contribution
If you have any questions or have found a bug, please feel free to let us know in our [issue tracker](https://github.com/CraftsBlock/CraftsNet/issues). We appreciate any help and welcome your contributions to improve the CraftsNet project.

---

Thank you for your interest in CraftsNet! We hope that you will benefit from this user-friendly Java library and use it to create powerful server applications. If you have any questions or suggestions, please feel free to contact us [here](https://dc.craftsblock.de)! 
