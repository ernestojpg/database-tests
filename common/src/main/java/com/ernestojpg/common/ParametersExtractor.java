package com.ernestojpg.common;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ParametersExtractor.java
 *
 * @author Ernesto J. Perez, 2016
 */
public class ParametersExtractor {

	private Map<String,String> map = new HashMap<>();
	private String action;

	public ParametersExtractor(String[] args) {
		final Pattern p = Pattern.compile("--(\\w+)\\s*=\\s*([\\w\\.]+)");

		for (int i=0 ; i<args.length ; i++) {
			final String arg = args[i].trim();
			final Matcher m = p.matcher(arg);
			if (m.matches()) {
				map.put(m.group(1), m.group(2));
			} else if (arg.startsWith("--")) {
				throw new IllegalArgumentException("Illegal argument '" + arg + "'");
			} else if (action==null) {
				action = arg;
			}
		}
	}

	public String getStringParameter(String parameter) {
		if (map.containsKey(parameter)) {
			return map.get(parameter);
		} else {
			throw new IllegalArgumentException("Missing '--" + parameter + "' parameter");
		}
	}

	public String getStringParameter(String parameter, String defaultValue) {
		if (map.containsKey(parameter)) {
			return map.get(parameter);
		} else {
			return defaultValue;
		}
	}

	public Integer getIntegerParameter(String parameter) {
		if (map.containsKey(parameter)) {
			return Integer.parseInt(map.get(parameter));
		} else {
			throw new IllegalArgumentException("Missing '--" + parameter + "' parameter");
		}
	}

	public Integer getIntegerParameter(String parameter, Integer defaultValue) {
		if (map.containsKey(parameter)) {
			return Integer.parseInt(map.get(parameter));
		} else {
			return defaultValue;
		}
	}

	public String getAction() {
		if (action==null) {
			throw new IllegalArgumentException("Missing action");
		}
		return action;
	}
}
