package net.xprova.piccolo;

public class TestHandler {

	@Command(description = "performs impressive arithmetic")
	public static void cal(String[] arr) {

		System.out.println("3 x 5 = 15");

	}

	@Command(description = "prints Hello!")
	public static void hello(String[] arr) {

		System.out.println("Hello!");

	}

	@Command(description = "counts from 1 to 10")
	public static void count(String[] arr) {

		for (int i = 1; i < 11; i++) {

			System.out.printf("%d out of %d ...\n", i, 10);

		}

	}

	@Command(description = "raises an Exception")
	public static void fail(String[] arr) {

		int a = 1;

		int b = 0;

		System.out.println("result = " + (a/b));

	}

}
