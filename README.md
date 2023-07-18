# CraftsNet 
### Einfach zu benutzende Java-Bibliothek für HTTP-Routen und WebSocket-Endpunkte

![Latest Release on Maven](https://repo.craftsblock.de/api/badge/latest/releases/de/craftsblock/craftsnet?color=40c14a&name=CraftsNet&prefix=v)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/CrAfTsArMy/CraftsNet)
![GitHub](https://img.shields.io/github/license/CrAfTsArMy/CraftsNet)
![GitHub all releases](https://img.shields.io/github/downloads/CrAfTsArMy/CraftsNet/total)
![GitHub issues](https://img.shields.io/github/issues-raw/CrAfTsArMy/CraftsNet)

---

CraftsNet ist eine benutzerfreundliche Java-Bibliothek, die es Entwicklern ermöglicht, mühelos HTTP-Routen und WebSocket-Endpunkte in ihren Serveranwendungen zu erstellen und zu verwalten. Diese Bibliothek vereinfacht die Serverentwicklung erheblich, sodass sich Entwickler auf die eigentliche Funktionalität ihrer Anwendung konzentrieren können, ohne sich mit komplexen Serverdetails herumschlagen zu müssen.

## Funktionen

- Definieren und Verwalten von HTTP-Routen für verschiedene Endpunkte der Anwendung.
- Unterstützung für dynamische Parameter in den HTTP-Routen für flexiblere Funktionalität.
- Mühelose Erstellung von WebSocket-Endpunkten für Echtzeit-Kommunikation zwischen Server und Client.
- SSL-Unterstützung für sichere und verschlüsselte Kommunikation mit den Benutzern.

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

1. Erstellen Sie in ihrem 
```json
{"name": "My Addon","main": "de.craftsarmy.myaddon.MyAddon"}
```

2. HTTP Endpoints können wie im folgenden Beispiel gezeigt erstellt werden.
```java
package de.craftsarmy.myaddon

import de.craftsblock.backend.api.http.Exchange;
import de.craftsblock.backend.api.http.RequestHandler;
import de.craftsblock.backend.api.http.Route;

public class MyRoute implements RequestHandler {
    
  @Route(path = "/v1/route")
  public void handleRoute(Exchange exchange) {
    // Den request verarbeiten
  }
    
}
```

3. Der nachfolgende Source Code zeigt Ihnen wie Sie einen WebSocket endpoint erstellen können.
```java
package de.craftsarmy.myaddon

import de.craftsblock.backend.api.websocket.MessageReceiver;
import de.craftsblock.backend.api.websocket.Socket;
import de.craftsblock.backend.api.websocket.SocketExchange;
import de.craftsblock.backend.api.websocket.SocketHandler;

@Socket(path = "/v1/socket")
public class MySocket implements SocketHandler {
    
  @MessageReceiver
  public void handleSocketMessage(SocketExchange exchange, String data) {
    // Die Socket message verarbeiten
  }
    
}
```

4. Zu guter letzt brauchen Sie noch das Herzstück Ihrer Anwendung, die Addon Klasse. Dort werden auch alle Routen und Endpoints registriert.
```java
package de.craftsarmy.myaddon

import de.craftsblock.craftsnet.addon.Addon

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

Bitte suchen Sie für eine detaliertere Beschreibung uns [Wiki]() auf.

## SSL aktivieren
Um SSL zu aktivieren und eine sichere Verbindung zu ermöglichen, können Sie einfach Ihre SSL-Zertifikate hinzufügen. Momentan werden nur von LetsEncrypt ausgestelle Zertifikate akzeptiert. Sie benötigen die `fullchain.pem` und die `privkey.pem` welche sie in dem Ordner `certificates` ablegen müssen, damit CraftsNet diese erkennt.

Anschließend können Sie SSL aktivieren, indem Sie an Ihren startup Befehl `--ssl CHOOSE_A_PASSWORD` anhängen, wobei Sie `CHOOSE_A_PASSWORD` durch eines von Ihnen gewähltes Passwort ersetzen.

## Unterstützung und Beitrag
Wenn Sie Fragen haben oder einen Fehler gefunden haben, können Sie uns gerne in unserem [Issue-Tracker](https://github.com/CrAfTsArMy/CraftsNet/issues) informieren. Wir schätzen jede Hilfe und begrüßen Ihre Beiträge zur Verbesserung des CraftsNet-Projekts.

---

Vielen Dank, dass Sie sich für CraftsNet interessieren! Wir hoffen, dass Sie von dieser benutzerfreundlichen Java-Bibliothek profitieren und damit leistungsfähige Serveranwendungen erstellen können. Bei Fragen oder Anregungen stehen wir Ihnen gerne [hier](https://dc.craftsblock.de) zur Verfügung! 
