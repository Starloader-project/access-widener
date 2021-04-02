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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class AccessWidenerWriter {
	private final AccessWidener accessWidener;

	public AccessWidenerWriter(AccessWidener accessWidener) {
		this.accessWidener = accessWidener;
	}

	public void write(StringWriter writer) {
		writer.write("accessWidener\tv2\t");
		writer.write(accessWidener.namespace);
		writer.write("\n");

		for (Map.Entry<String, List<AccessWidener.AccessOperator>> entry : accessWidener.classAccess.entrySet()) {
			for (String s : getAccesses(entry.getValue())) {
				writer.write(s);
				writer.write("\tclass\t");
				writer.write(entry.getKey());
				writer.write("\n");
			}
		}

		for (Entry<EntryTriple, List<AccessWidener.AccessOperator>> entry : accessWidener.methodAccess.entrySet()) {
			writeEntry(writer, "method", entry.getKey(), entry.getValue());
		}

		for (Map.Entry<EntryTriple, List<AccessWidener.AccessOperator>> entry : accessWidener.fieldAccess.entrySet()) {
			writeEntry(writer, "field", entry.getKey(), entry.getValue());
		}
	}

	private void writeEntry(StringWriter writer, String type, EntryTriple entryTriple, List<AccessWidener.AccessOperator> access) {
		for (String s : getAccesses(access)) {
			writer.write(s);
			writer.write("\t");
			writer.write(type);
			writer.write("\t");
			writer.write(entryTriple.getOwner());
			writer.write("\t");
			writer.write(entryTriple.getName());
			writer.write("\t");
			writer.write(entryTriple.getDesc());
			writer.write("\n");
		}
	}

	private List<String> getAccesses(List<AccessWidener.AccessOperator> access) {
		List<String> accesses = new ArrayList<>();

		for (AccessWidener.AccessOperator op : access) {
			accesses.add(op.toString());
		}

		return accesses;
	}
}
