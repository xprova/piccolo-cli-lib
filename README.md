# Piccolo CLI Library

This is a lightweight and friendly library to add a command line interface to
a Java program. Just provide one or more "handler" classes with public methods
annotated with `@Command` and the library will allow users to call them from
the console.

## Basic Usage

Create a `DemoHandler` class as below:

```Java
public class DemoHandler {

	@Command
	public static void hello() {

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

(:l is an internal command used to list available commands.)

## Command Arguments

Haskell-style arguments can be added after the command name, for example:

```Java
@Command
public void add(int a, int b) {

	System.out.printf("%d + %d = %d\n", a, b, a + b);

}
```

```
>> add 3 5
3 + 5 = 8
```

All primitive data types are supported.

Alternatively, the method can receive an `ArrayList<String>` object containing
the parameters as in:

```Java
@Command
public void sum(String[] args) {

	int total = 0;

	for (String s : args)
		total = total + Integer.valueOf(s);

	System.out.println(total);

}
```

```
>> sum 3 7 18
28
```

## @Command Annotations

The `@Command` annotation can be used to

Methods can be renamed using aliases:

```Java
@Command(aliases = { "perf", "compute" })
public void performComplexComputations(String[] args) {

	// do stuff

}
```

When one or more aliases are defined the method cannot be called by its Java
name. For the above code:

```
>> :l
There are 3 available commands:
:list (aliases: :l)
:quit (aliases: :q)
perf (aliases: compute)
```

## Built-in Commands

Piccolo introduces few built-in console commands:

* `:quit` (or `:q`) to exit the console
* `:list` (or `:l`) to list available commands

Built-in commands are also created using `@Command` annotations.

