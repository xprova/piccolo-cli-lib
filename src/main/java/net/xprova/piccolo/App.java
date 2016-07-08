package net.xprova.piccolo;

public class App {

	public static void main(String[] args) {

		Console c = new Console().addHandler(new TestHandler());

		c.run();

	}
}
