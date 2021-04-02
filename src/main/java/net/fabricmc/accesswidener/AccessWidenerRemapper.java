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

import java.util.List;
import java.util.Map;

import org.objectweb.asm.commons.Remapper;

public final class AccessWidenerRemapper {
	private final AccessWidener input;
	private final String to;
	private final Remapper remapper;

	public AccessWidenerRemapper(AccessWidener input, Remapper remapper, String to) {
		this.input = input;
		this.to = to;
		this.remapper = remapper;
	}

	public AccessWidener remap() {
		//Dont remap if we dont need to
		if (input.namespace.equals(to)) {
			return input;
		}

		AccessWidener remapped = new AccessWidener();
		remapped.namespace = to;

		remapKeysClass(remapped.classAccess, input.classAccess);
		remapKeysMethod(remapped.methodAccess, input.methodAccess);
		remapKeysField(remapped.fieldAccess, input.fieldAccess);
		return remapped;
	}

	private EntryTriple remapMethod(EntryTriple entryTriple) {
		return new EntryTriple(
				remapper.map(entryTriple.getOwner()),
				remapper.mapMethodName(entryTriple.getOwner(), entryTriple.getName(), entryTriple.getDesc()),
				remapper.mapDesc(entryTriple.getDesc())
		);
	}

	private EntryTriple remapField(EntryTriple entryTriple) {
		return new EntryTriple(
				remapper.map(entryTriple.getOwner()),
				remapper.mapFieldName(entryTriple.getOwner(), entryTriple.getName(), entryTriple.getDesc()),
				remapper.mapDesc(entryTriple.getDesc())
		);
	}

	private <T> void remapKeysClass(Map<String, List<T>> to, Map<String, List<T>> from) {
		for (Map.Entry<String, List<T>> entry : from.entrySet()) {
			to.put(remapper.map(entry.getKey()), entry.getValue());
		}
	}

	private <T> void remapKeysMethod(Map<EntryTriple, List<T>> to, Map<EntryTriple, List<T>> from) {
		for (Map.Entry<EntryTriple, List<T>> entry : from.entrySet()) {
			to.put(remapMethod(entry.getKey()), entry.getValue());
		}
	}

	private <T> void remapKeysField(Map<EntryTriple, List<T>> to, Map<EntryTriple, List<T>> from) {
		for (Map.Entry<EntryTriple, List<T>> entry : from.entrySet()) {
			to.put(remapField(entry.getKey()), entry.getValue());
		}
	}
}
