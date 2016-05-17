package net.xprova.simplecli;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jline.TerminalFactory;
import jline.console.ConsoleReader;

public class Console {

	@SuppressWarnings("rawtypes")
	private Class myhandler;

	public Console(@SuppressWarnings("rawtypes") Class handler) {
		myhandler = handler;
	}

	public void run() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		try {
			ConsoleReader console = new ConsoleReader();
			console.setPrompt("prompt> ");

			String cmd = null;

			while ((cmd = console.readLine()) != null) {

				if ("exit".equals(cmd)) {

					break;

				} else if ("list".equals(cmd)) {

					listMethods(myhandler);

				} else {

					if (runMethod(myhandler, cmd)) {

					} else {
						System.out.printf("error, command<%s> not found\n", cmd);
					}

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				TerminalFactory.get().restore();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean runMethod(@SuppressWarnings("rawtypes") Class handler, String methodName)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		Method[] methods = handler.getDeclaredMethods();

		for (Method method : methods) {

			if (method.getName().equals(methodName)) {

				method.invoke(null, new Object[] { new String[] {} });

				return true;
			}

		}

		return false;

	}

	public void listMethods(@SuppressWarnings("rawtypes") Class handler)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		Method[] methods = handler.getDeclaredMethods();

		for (Method method : methods) {

			Command[] anots = method.getDeclaredAnnotationsByType(Command.class);

			if (anots.length > 0) {

				System.out.println("found method: " + method.getName());

			}
		}

	}
}
