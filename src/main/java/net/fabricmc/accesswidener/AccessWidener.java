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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public final class AccessWidener {
	String namespace;
	final Map<String, List<AccessOperator>> classAccess = new HashMap<>();
	final Map<EntryTriple, List<AccessOperator>> methodAccess = new HashMap<>();
	final Map<EntryTriple, List<AccessOperator>> fieldAccess = new HashMap<>();
	final Set<String> classes = new LinkedHashSet<>();

	public Set<String> getTargets() {
		return classes;
	}

	public String getNamespace() {
		return namespace;
	}

	void applyClass(String className, String access) {
		List<AccessOperator> operators = classAccess.get(className);

		if (operators == null) {
			operators = new ArrayList<>();
		}

		switch (access.toLowerCase(Locale.ROOT)) {
		case "accessible":
			operators.add(new Public());
			break;
		case "extendable":
			operators.add(new Extendable());
			break;
		case "mutable":
			operators.add(new Mutable());
			break;
		case "natural":
			operators.add(new Natural());
			break;
		case "denumerised":
			operators.add(new Denumerised());
			break;
		default:
			throw new UnsupportedOperationException("Unknown access type:" + access);
		}

		classAccess.put(className, operators);
	}

	void applyMethod(EntryTriple input, String access) {
		List<AccessOperator> operators = methodAccess.get(input);

		if (operators == null) {
			operators = new ArrayList<>();
		}

		switch (access.toLowerCase(Locale.ROOT)) {
		case "accessible":
			operators.add(new Public());
			break;
		case "extendable":
			operators.add(new Extendable());
			break;
		case "mutable":
			operators.add(new Mutable());
			break;
		case "natural":
			operators.add(new Natural());
			break;
		case "denumerised":
			throw new UnsupportedOperationException(access + " is not applicable for the method " + input);
		default:
			throw new UnsupportedOperationException("Unknown access type:" + access);
		}

		methodAccess.put(input, operators);
	}

	void applyField(EntryTriple input, String access) {
		List<AccessOperator> operators = fieldAccess.get(input);

		if (operators == null) {
			operators = new ArrayList<>();
		}

		switch (access.toLowerCase(Locale.ROOT)) {
		case "accessible":
			operators.add(new Public());
			break;
		case "extendable":
			operators.add(new Extendable());
			break;
		case "mutable":
			operators.add(new Mutable());
			break;
		case "natural":
			operators.add(new Natural());
			break;
		case "denumerised":
			throw new UnsupportedOperationException(access + " is not applicable for the field " + input);
		default:
			throw new UnsupportedOperationException("Unknown access type:" + access);
		}

		fieldAccess.put(input, operators);
	}

	private static int makePublic(int i) {
		return (i & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
	}

	private static int makeProtected(int i) {
		if ((i & Opcodes.ACC_PUBLIC) != 0) {
			//Return i if public
			return i;
		}

		return (i & ~(Opcodes.ACC_PRIVATE)) | Opcodes.ACC_PROTECTED;
	}

	private static int removeFinal(int i) {
		return i & ~Opcodes.ACC_FINAL;
	}

	private static int removeSynthetic(int i) {
		return i & ~Opcodes.ACC_SYNTHETIC;
	}

	private static int removeEnum(int i) {
		return i & ~Opcodes.ACC_ENUM;
	}

	int applyClassAccess(String className, int access, int outeraccess) {
		List<AccessOperator> ops = classAccess.get(className);

		if (ops == null) {
			return access;
		}

		for (AccessOperator op : ops) {
			access = op.apply(access, className, outeraccess);
		}

		return access;
	}

	int applyMethodAccess(String className, String methodName, String methodDescripion, int classAcces, int access) {
		List<AccessOperator> ops = methodAccess.get(new EntryTriple(className, methodName, methodDescripion));

		if (ops == null) {
			return access;
		}

		for (AccessOperator op : ops) {
			access = op.apply(access, methodName, classAcces);
		}

		return access;
	}

	int applyFieldAccess(String className, String fieldName, String fieldDescripion, int classAcces, int access) {
		List<AccessOperator> ops = fieldAccess.get(new EntryTriple(className, fieldName, fieldDescripion));

		if (ops == null) {
			return access;
		}

		for (AccessOperator op : ops) {
			access = op.apply(access, fieldName, classAcces);
		}

		return access;
	}

	@FunctionalInterface
	interface AccessOperator {
		/**
		 * Applies the operator.
		 *
		 * @param access The old access
		 * @param targetName The target instance
		 * @param ownerAccess The access of the owning type
		 * @return The new access
		 */
		int apply(int access, String targetName, int ownerAccess);
	}

	/**
	 * Removes the final status.
	 */
	class Mutable implements AccessOperator {
		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return removeFinal(access);
		}

		@Override
		public String toString() {
			return "mutable";
		}
	}

	/**
	 * Makes the component protected and removes the final status.
	 */
	class Extendable implements AccessOperator {
		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return makeProtected(removeFinal(access));
		}

		@Override
		public String toString() {
			return "extendable";
		}
	}

	/**
	 * Makes the component public.
	 */
	class Public implements AccessOperator {
		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return makePublic(access);
		}

		@Override
		public String toString() {
			return "accessible";
		}
	}

	/**
	 * Strips the synthetic flag.
	 */
	class Natural implements AccessOperator {
		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return removeSynthetic(access);
		}

		@Override
		public String toString() {
			return "natural";
		}
	}

	/**
	 * Strips the enum flag.
	 */
	class Denumerised implements AccessOperator {
		@Override
		public int apply(int access, String targetName, int ownerAccess) {
			return removeEnum(access);
		}

		@Override
		public String toString() {
			return "denumerised";
		}
	}
}
