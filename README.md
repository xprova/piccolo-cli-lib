### Piccolo CLI Library

This is a lightweight and friendly library to add a command line interface to
a Java program. Just provide one or more "handler" classes with public methods
annotated with `@Command` and the library will allow users to call them from
the console.

#### Example Usage

Create a `DemoHandler` class as below:

```Java
public class DemoHandler {

	@Command
	public static void hello(String[] arr) {

		System.out.println("Hello World!");

	}

}
```

Pass an instance of `DemoHandler` to a `Console` object:

```Java

package net.xprova.piccolo;

public static void main(String[] args) {

	Console c = new Console().addHandler(new DemoHandler());

	c.run();

}
```

This will generate the following console:

```
______  _                     _
| ___ \(_)                   | |
| |_/ / _   ___   ___   ___  | |  ___
|  __/ | | / __| / __| / _ \ | | / _ \
| |    | || (__ | (__ | (_) || || (_) |
\_|    |_| \___| \___| \___/ |_| \___/


Type :l for a list of available commands or :q to quit

>> :l
There are 3 available commands:
:list (aliases: :l)
:quit (aliases: :q)
hello

>> hello
Hello World!

>>
```
