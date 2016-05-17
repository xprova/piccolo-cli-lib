package net.xprova.simplecli;

import java.lang.reflect.InvocationTargetException;

/**
 * Hello world!
 *
 */
public class App {

	public static void main(String[] args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		(new Console(TestHandler.class)).run();

	}
}
