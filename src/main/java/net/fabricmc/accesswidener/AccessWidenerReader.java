/*
 * Copyright (c) 2020 FabricMC
 * Copyright (c) 2021 Geolykt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.accesswidener;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AccessWidenerReader {
	private final AccessWidener accessWidener;

	public AccessWidenerReader(AccessWidener accessWidener) {
		this.accessWidener = accessWidener;
	}

	public void read(BufferedReader reader) throws IOException {
		this.read(reader, null);
	}

	public void read(BufferedReader reader, String currentNamespace) throws IOException {
		String[] header = reader.readLine().split("\\s+");

		if (header.length != 3 || !header[0].equals("accessWidener")) {
			throw new UnsupportedOperationException("Invalid access access widener file");
		}

		if (!(header[1].equals("v1") || header[1].equals("v2"))) { // v2 is backwards-compatible with v1
			throw new RuntimeException(String.format("Unsupported access widener format (%s). Only v1 and v2 is supported!", header[1]));
		}

		if (currentNamespace != null && !header[2].equals(currentNamespace)) {
			throw new RuntimeException(String.format("Namespace (%s) does not match current runtime namespace (%s)", header[2], currentNamespace));
		}

		if (accessWidener.namespace != null) {
			if (!accessWidener.namespace.equals(header[2])) {
				throw new RuntimeException(String.format("Namespace mismatch, expected %s got %s", accessWidener.namespace, header[2]));
			}
		}

		accessWidener.namespace = header[2];

		String line;
		Set<String> targets = new LinkedHashSet<>();

		while ((line = reader.readLine()) != null) {
			//Comment handling
			int commentPos = line.indexOf('#');

			if (commentPos >= 0) {
				line = line.substring(0, commentPos).trim();
			}

			if (line.isEmpty()) continue;

			String[] split = line.split("\\s+");

			if (split.length != 3 && split.length != 5) {
				throw new RuntimeException(String.format("Invalid line (%s)", line));
			}

			String access = split[0];

			targets.add(split[2].replaceAll("/", "."));

			switch (split[1]) {
			case "class":
				if (split.length != 3) {
					throw new RuntimeException(String.format("Expected (<access>\tclass\t<className>) got (%s)", line));
				}

				accessWidener.applyClass(split[2], access);
				break;
			case "field":
				if (split.length != 5) {
					throw new RuntimeException(String.format("Expected (<access>\tfield\t<className>\t<fieldName>\t<fieldDesc>) got (%s)", line));
				}

				accessWidener.applyField(new EntryTriple(split[2], split[3], split[4]), access);
				break;
			case "method":
				if (split.length != 5) {
					throw new RuntimeException(String.format("Expected (<access>\tmethod\t<className>\t<methodName>\t<methodDesc>) got (%s)", line));
				}

				accessWidener.applyMethod(new EntryTriple(split[2], split[3], split[4]), access);
				break;
			default:
				throw new UnsupportedOperationException("Unsupported type " + split[1]);
			}
		}

		Set<String> parentClasses = new LinkedHashSet<>();

		//Also transform all parent classes
		for (String clazz : targets) {
			while (clazz.contains("$")) {
				clazz = clazz.substring(0, clazz.lastIndexOf("$"));
				parentClasses.add(clazz);
			}
		}

		accessWidener.classes.addAll(targets);
		accessWidener.classes.addAll(parentClasses);
	}
}
