package net.xprova.piccolo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeSet;

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

				try {

					runCommand(line);

				} catch (Exception e) {

					e.printStackTrace();

				}

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
	 * @throws Exception
	 *             if the command is not found or fails during execution
	 */
	public void runCommand(String line) throws Exception {

		String[] parts = line.split(" ");

		String cmd = parts[0];

		String[] args = Arrays.copyOfRange(parts, 1, parts.length);

		runCommand(cmd, args);

	}

	/**
	 * Runs a console command
	 *
	 * @param methodAlias
	 *            command (method) name
	 * @param args
	 *            command arguments
	 * @throws Exception
	 *             if the command is not found or fails during execution
	 */
	public void runCommand(String methodAlias, String args[]) throws Exception {

		if (methodAlias.isEmpty() || methodAlias.startsWith("#"))
			return;

		MethodData methodData = methodAliases.get(methodAlias);

		if (methodData == null) {

			throw new Exception(String.format("Unknown command <%s>", methodAlias));

		} else {

			try {

				smartInvoke(methodAlias, methodData.method, methodData.object, args);

			} catch (InvocationTargetException e) {

				throw new Exception(e.getCause());

			}

		}

	}

	@Command(aliases = { ":type" }, description = "print type information for a given command")
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

	@Command(aliases = { ":shell", ":!" }, description = "run shell command")
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

	@Command(aliases = { ":source", ":s" }, description = "run script file")
	public void runScript(String scriptFile) throws Exception {

		BufferedReader br = new BufferedReader(new FileReader(scriptFile));

		try {

			exitFlag = 0;

			String line;

			while ((line = br.readLine()) != null && exitFlag == 0) {

				if (!line.isEmpty())
					this.runCommand(line);

			}

			exitFlag = 0;

		} finally {

			br.close();
		}

	}

	@Command(aliases = { ":quit", ":q" }, description = "exit program")
	public boolean exitConsole() {

		exitFlag = 1;

		return true;

	}

	@Command(aliases = { ":help", ":h" }, description = "print help text of a command", help = { "Uage:",
			"  :help <command>" })
	public void printHelp(String methodAlias) {

		MethodData md = methodAliases.get(methodAlias);

		if (md == null) {

			System.out.println("Unrecognized command");

		} else {

			Command anot = getCommandAnnotation(md.method);

			System.out.printf("%s : %s\n\n", methodAlias, anot.description());

			for (String s : anot.help())
				System.out.println(s);

		}

	}

	@Command(aliases = { ":time", ":t" }, description = "time the execution of a command")
	public void timeCommand(String args[]) throws Exception {

		long startTime = System.nanoTime();

		runCommand(String.join(" ", args));

		long endTime = System.nanoTime();

		double searchTime = (endTime - startTime) / 1e9;

		System.out.printf("Completed execution in %f seconds\n", searchTime);

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

			out.printf("command <%s> requires %d parameter(s) (%d supplied)", usedAlias, nMethodArgs, nArgs);

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

		TreeSet<String> methodList = new TreeSet<String>();

		for (MethodData md : availMethods) {

			Command anot = getCommandAnnotation(md.method);

			if (anot.visible()) {

				String[] aliases = anot.aliases();

				String cmd = aliases.length == 0 ? md.method.getName() : aliases[0];

				methodList.add(cmd);

			}

		}

		out.println("Available commands:");

		for (String cmd : methodList) {

			Method m = methodAliases.get(cmd).method;

			String desc = getCommandAnnotation(m).description();

			out.printf("%-20s : %s\n", cmd, desc);

		}

	}

	@Command(aliases = { ":aliases", ":a" }, description = "list command aliases")
	private void listAliases() {

		TreeSet<String> methodList = new TreeSet<String>();

		for (MethodData md : availMethods) {

			Command anot = getCommandAnnotation(md.method);

			if (anot.visible()) {

				String[] aliases = anot.aliases();

				if (aliases.length > 1) {

					String cmd = aliases.length == 0 ? md.method.getName() : aliases[0];

					methodList.add(cmd);

				}

			}

		}

		out.println("Available command aliases:");

		for (String cmd : methodList) {

			Method m = methodAliases.get(cmd).method;

			Command anot = getCommandAnnotation(m);

			String aliases = "";

			if (anot.aliases().length < 2) {

				aliases = "n/a";

			} else {

				aliases = anot.aliases()[1];

				for (int i = 2; i < anot.aliases().length; i++) {

					aliases += ", " + anot.aliases()[i];

				}

			}

			out.printf("%-20s : %s\n", cmd, aliases);

		}

	}

	private Command getCommandAnnotation(Method method) {

		Command[] anots = method.getDeclaredAnnotationsByType(Command.class);

		return anots[0];

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

	@Command(aliases = { ":print", ":p" }, description = "print a text to the console")
	private void print(String[] args) {

		out.println(String.join(" ", args));

	}

}
