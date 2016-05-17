package net.xprova.piccolo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import jline.TerminalFactory;
import jline.console.ConsoleReader;

public class Console {

	private String banner;
	private String prompt;
	private int exitFlag;

	private HashSet<MethodData> availMethods;
	private HashMap<String, MethodData> methodAliases;

	private class MethodData {

		public Method method;
		public Object object;

		public MethodData(Method method, Object object) {
			this.method = method;
			this.object = object;
		}

	};

	private HashSet<Object> handlers;

	public Console() {

		methodAliases = new HashMap<String, MethodData>();

		availMethods = new HashSet<Console.MethodData>();

		handlers = new HashSet<Object>();

		// load banner

		String bannerFileContent = "";

		Scanner s = null;

		try {

			final InputStream stream;

			stream = Console.class.getClassLoader().getResourceAsStream("piccolo_banner.txt");

			s = new Scanner(stream);

			bannerFileContent = s.useDelimiter("\\Z").next();

		} catch (Exception e) {

			bannerFileContent = "<could not load internal banner file>\n";

		} finally {

			if (s != null)
				s.close();

		}

		setBanner(bannerFileContent).setPrompt(">> ");

	}

	public Console addHandler(Object handler) {

		handlers.add(handler);

		scanForMethods();

		return this;
	}

	public Console removeHandler(@SuppressWarnings("rawtypes") Class handler) {

		handlers.remove(handler);

		scanForMethods();

		return this;
	}

	public Console setBanner(String newBanner) {

		banner = newBanner;

		return this;

	}

	public Console setPrompt(String newPrompt) {

		prompt = newPrompt;

		return this;

	}

	public void run() {

		try {

			ConsoleReader console = new ConsoleReader();

			System.out.println(banner);

			console.setPrompt(prompt);

			String cmd = null;

			exitFlag = 0;

			while ((cmd = console.readLine()) != null) {

				if (!runMethod(cmd)) {

					System.out.printf("%s: command not found\n", cmd);

				}

				if (exitFlag != 0)
					break;

				System.out.println(""); // new line after each command

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

	public boolean runMethod(String methodAlias) {

		MethodData methodData = methodAliases.get(methodAlias);

		if (methodData == null) {

			return false;

		} else {

			try {

				methodData.method.invoke(methodData.object, new Object[] { new String[] {} });

			} catch (Exception e) {

				System.err.printf("Error while invoking method <%s> ...\n", methodData.method.getName());

				e.printStackTrace();

			}

			return true;

		}

	}

	/*
	 * returns a list of command aliases for a method annotated with Command
	 *
	 * If any aliases are defined in the Command annotation then these are
	 * returned. Otherwise the method name is returned as the only alias.
	 *
	 */
	private ArrayList<String> getCommandNames(Method method) {

		Command[] anots = method.getDeclaredAnnotationsByType(Command.class);

		ArrayList<String> result = new ArrayList<String>();

		if (anots.length > 0) {

			if (anots[0].aliases().length > 0) {

				// this command has aliases

				for (String a : anots[0].aliases()) {

					result.add(a);

				}

			} else {

				// no defined aliases, use command line as only alias

				result.add(method.getName());

			}

		}

		return result;

	}

	private void scanForMethods() {

		HashSet<Object> allHandlers = new HashSet<Object>(handlers);

		allHandlers.add(this);

		for (Object handler : allHandlers) {

			Method[] methods = handler.getClass().getDeclaredMethods();

			for (Method method : methods) {

				Command[] anots = method.getDeclaredAnnotationsByType(Command.class);

				if (anots.length > 0) {

					MethodData md = new MethodData(method, handler);

					availMethods.add(md);

					ArrayList<String> aliases;

					aliases = getCommandNames(method);

					for (String a : aliases) {

						methodAliases.put(a, md);

					}

				}

			}

		}

	}

	@Command(aliases = { ":quit", ":q" })
	public void exitConsole(String[] args) {

		exitFlag = 1;

	}

	@Command(aliases = { ":list", ":l" }, description = "lists available commands")
	public void listMethods(String[] dummy) {

		List<String> resultList = new ArrayList<String>();

		for (MethodData md : availMethods) {

			ArrayList<String> methodNames = getCommandNames(md.method);

			StringBuilder sb = new StringBuilder(methodNames.get(0));

			if (methodNames.size() > 1) {

				sb.append(" (aliases: ");

				int j = methodNames.size() - 1;

				for (int i = 2; i < j - 1; i++) {

					sb.append(methodNames.get(i)).append(", ");

				}

				sb.append(methodNames.get(j)).append(")");

			}

			resultList.add(sb.toString());

		}

		Collections.sort(resultList);

		System.out.printf("There are %d available commands:\n", resultList.size());

		for (String methodName : resultList) {

			System.out.println(methodName);

		}

	}
}
