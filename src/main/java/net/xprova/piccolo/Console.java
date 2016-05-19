package net.xprova.piccolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
	private PrintStream out;

	private HashSet<MethodData> availMethods;
	private HashMap<String, MethodData> methodAliases;

	private HashSet<Object> handlers;

	private class MethodData {

		public Method method;
		public Object object;

		public MethodData(Method method, Object object) {
			this.method = method;
			this.object = object;
		}

	};

	// public functions

	/**
	 * Constructors
	 */
	public Console() {

		methodAliases = new HashMap<String, MethodData>();

		availMethods = new HashSet<Console.MethodData>();

		handlers = new HashSet<Object>();

		out = System.out;

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

	/**
	 * Add a handler class to the console
	 *
	 * @param handler
	 * @return same Console object for chaining
	 */
	public Console addHandler(Object handler) {

		handlers.add(handler);

		scanForMethods();

		return this;
	}

	/**
	 * Remove handler class from the console
	 *
	 * @param handler
	 * @return same Console object for chaining
	 */
	public Console removeHandler(@SuppressWarnings("rawtypes") Class handler) {

		handlers.remove(handler);

		scanForMethods();

		return this;
	}

	/**
	 * Set console banner
	 *
	 * @param newBanner
	 * @return same Console object for chaining
	 */
	public Console setBanner(String newBanner) {

		banner = newBanner;

		return this;

	}

	/**
	 * Set console prompt
	 *
	 * @param newPrompt
	 * @return same Console object for chaining
	 */
	public Console setPrompt(String newPrompt) {

		prompt = newPrompt;

		return this;

	}

	/**
	 * Start the console
	 */
	public void run() {

		try {

			ConsoleReader console = new ConsoleReader();

			out.println(banner);

			console.setPrompt(prompt);

			String line = null;

			exitFlag = 0;

			while ((line = console.readLine()) != null) {

				runCommand(line);

				if (exitFlag != 0)
					break;

				out.println(""); // new line after each command

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

	/**
	 * Run a console command
	 *
	 * @param line
	 *            string containing both command name and arguments, separated
	 *            by spaces
	 * @return true if the command runs successfully and false otherwise
	 */
	public boolean runCommand(String line) {

		String[] parts = line.split(" ");

		String cmd = parts[0];

		String[] args = Arrays.copyOfRange(parts, 1, parts.length);

		return runCommand(cmd, args);

	}

	/**
	 * Runs a console command
	 *
	 * @param methodAlias
	 *            command (method) name
	 * @param args
	 *            command arguments
	 * @return true if the command runs successfully and false otherwise
	 */
	public boolean runCommand(String methodAlias, String args[]) {

		MethodData methodData = methodAliases.get(methodAlias);

		if (methodData == null) {

			return false;

		} else {

			try {

				smartInvoke(methodAlias, methodData.method, methodData.object, args);

				return true;

			} catch (Exception e) {

				System.err.printf("Error while invoking method <%s> ...\n", methodData.method.getName());

				e.printStackTrace();

			}

			return true;

		}

	}

	@Command(aliases = { ":type", ":t" })
	public boolean getType(String methodAlias) {

		MethodData md = methodAliases.get(methodAlias);

		if (md == null) {

			out.printf("command <%s> does not exist", methodAlias);

			return false;

		} else {

			printParameters(methodAlias, md.method);

			return true;
		}

	}

	@Command(aliases = { "!" })
	public boolean runShellCmd(String args[]) {

		String cmd = String.join(" ", args);

		final Runtime rt = Runtime.getRuntime();

		try {

			Process proc = rt.exec(cmd);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

			String s = null;

			while ((s = stdInput.readLine()) != null)
				out.println(s);

			while ((s = stdError.readLine()) != null)
				out.println(s);

			return true;

		} catch (IOException e) {

			return false;

		}

	}

	@Command(aliases = { ":quit", ":q" })
	public boolean exitConsole() {

		exitFlag = 1;

		return true;

	}

	// internal (private) functions

	/*
	 * this function returns true when `method` has 1 parameter and of the type
	 * String[]
	 */
	private boolean isMethodGeneric(Method method) {

		@SuppressWarnings("rawtypes")
		Class[] parameterTypes = method.getParameterTypes();

		if (parameterTypes.length == 1) {

			return isStringArray(parameterTypes[0]);

		} else {

			return false;
		}

	}

	private boolean isStringArray(@SuppressWarnings("rawtypes") Class clazz) {

		boolean isArr = clazz.isArray();

		boolean isCompString = clazz.getComponentType() == String.class;

		return isArr && isCompString;

	}

	/*
	 * return true if command executes successfully and false otherwise
	 */
	private boolean smartInvoke(String usedAlias, Method method, Object object, String[] args) throws Exception {

		Object[] noargs = new Object[] { new String[] {} };

		int nArgs = args.length;

		Class<?>[] paramTypes = method.getParameterTypes();

		int nMethodArgs = paramTypes.length;

		// determine type of invocation

		if (nMethodArgs == 0) {

			if (nArgs == 0) {

				// simple case, invoke with no parameters

				method.invoke(object);

				return true;

			} else {

				printParameters(usedAlias, method);

				return false;

			}

		} else if (nMethodArgs == 1 && isMethodGeneric(method)) {

			// this method takes one parameter of type String[] that
			// contains all user parameters

			if (nArgs == 0) {

				// user supplied no parameters

				method.invoke(object, noargs);

				return true;

			} else {

				// user supplied 1+ parameters

				method.invoke(object, new Object[] { args });

				return true;

			}

		} else if (nMethodArgs == nArgs) {

			// the method accepts several parameters, attempt to convert
			// parameters to the correct types and pass them to the method

			ArrayList<Object> objs = new ArrayList<Object>();

			try {

				for (int i = 0; i < paramTypes.length; i++) {

					objs.add(toObject(paramTypes[i], args[i]));

				}

			} catch (Exception e) {

				out.println("Unable to parse parameters");

				printParameters(usedAlias, method);

				return false;

			}

			Object[] objsArr = objs.toArray();

			method.invoke(object, objsArr);

			return true;

		} else {

			out.printf("command <%s> requires %d parameter(s) (%d supplied)", method.getName(), nMethodArgs, nArgs);

			return false;

		}

	}

	private static Object toObject(@SuppressWarnings("rawtypes") Class clazz, String value) throws Exception {

		if (Boolean.class == clazz || Boolean.TYPE == clazz)
			return Boolean.parseBoolean(value);

		if (Byte.class == clazz || Byte.TYPE == clazz)
			return Byte.parseByte(value);

		if (Short.class == clazz || Short.TYPE == clazz)
			return Short.parseShort(value);

		if (Integer.class == clazz || Integer.TYPE == clazz)
			return Integer.parseInt(value);

		if (Long.class == clazz || Long.TYPE == clazz)
			return Long.parseLong(value);

		if (Float.class == clazz || Float.TYPE == clazz)
			return Float.parseFloat(value);

		if (Double.class == clazz || Double.TYPE == clazz)
			return Double.parseDouble(value);

		if (String.class == clazz)
			return value;

		throw new Exception("Attempted to parse non-primitive type");
	}

	private void printParameters(String usedAlias, Method method) {

		Class<?>[] paramTypes = method.getParameterTypes();

		if (paramTypes.length == 0) {

			out.printf("command <%s> takes no parameters\n", usedAlias);

		} else if (paramTypes.length == 1) {

			if (isStringArray(paramTypes[0])) {

				out.printf("command <%s> takes arbitrary parameters\n", usedAlias);

			} else {

				out.printf("command <%s> takes <%s> parameter\n", usedAlias, paramTypes[0].getName());

			}

		} else {

			StringBuilder sb = new StringBuilder();

			sb.append("command <" + method.getName() + "> takes <");

			sb.append(paramTypes[0].getName());

			int j = paramTypes.length;

			for (int i = 1; i < j - 1; i++) {

				sb.append(", " + paramTypes[i].getName());

			}

			sb.append(", " + paramTypes[j - 1].getName() + "> parameters");

			out.println(sb.toString());

		}
	}

	@Command(aliases = { ":list", ":l" }, description = "lists available commands")
	private void listMethods() {

		List<String> resultList = new ArrayList<String>();

		for (MethodData md : availMethods) {

			ArrayList<String> methodNames = getCommandNames(md.method);

			StringBuilder sb = new StringBuilder(methodNames.get(0));

			if (methodNames.size() > 1) {

				sb.append(" (aliases: ");

				int j = methodNames.size() - 1;

				for (int i = 1; i < j; i++) {

					sb.append(methodNames.get(i)).append(", ");

				}

				sb.append(methodNames.get(j)).append(")");

			}

			resultList.add(sb.toString());

		}

		Collections.sort(resultList);

		out.printf("There are %d available commands:\n", resultList.size());

		for (String methodName : resultList) {

			out.println(methodName);

		}

	}

	/**
	 * Return a list of command aliases for a method annotated with Command
	 *
	 * <p>
	 * If any aliases are defined in the Command annotation then these are
	 * returned. Otherwise the method name is returned as the only alias.
	 *
	 * @param method
	 * @return
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

}
