package net.xprova.piccolo;

public class TestHandler {

	@Command(enabled = false, description = "does stuff")
	public static void cmdFunc1(String[] arr) {

		System.out.println("func1");

	}

	@Command(enabled = false)
	public static void cmdDoThings(String[] arr) {

		System.out.println("create things here ...");

	}

	@Command
	public static void build(String[] arr) {

		for (int i = 1; i < 11; i++) {
			System.out.printf("building %d of %d ...\n", i, 10);
		}

	}

}
