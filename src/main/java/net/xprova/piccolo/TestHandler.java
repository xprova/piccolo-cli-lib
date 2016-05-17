package net.xprova.piccolo;

public class TestHandler {

	@Command(description = "prints Hello!")
	public void hello() {

		System.out.println("Hello!");

	}

	@Command(description = "counts from 1 to n")
	public void count(int n) {

		for (int i = 1; i <= n; i++) {

			System.out.printf("%d out of %d ...\n", i, n);

		}

	}

	@Command(description = "raises an Exception")
	public void div(int a, int b) {

		System.out.printf("%d / %d = %d\n", a, b, a / b);

	}

	@Command
	public void countargs(String[] args) {

		System.out.println("I received " + args.length + " arguments");

	}

	@Command
	public void sum(int a, int b) {

		System.out.printf("%d + %d = %d\n", a, b, a + b);
	}

	@Command
	public void not(boolean x) {

		System.out.println(!x);
	}

}
