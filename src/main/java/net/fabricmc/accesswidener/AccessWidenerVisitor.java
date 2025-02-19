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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class AccessWidenerVisitor extends ClassVisitor {
	private final AccessWidener accessWidener;
	private String className;
	private int classAccess;

	AccessWidenerVisitor(int api, ClassVisitor classVisitor, AccessWidener accessWidener) {
		super(api, classVisitor);
		this.accessWidener = accessWidener;
	}

	public static ClassVisitor createClassVisitor(int api, ClassVisitor visitor, AccessWidener accessWidener) {
		return new AccessWidenerVisitor(api, visitor, accessWidener);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		className = name;
		classAccess = access;
		super.visit(
				version,
				accessWidener.applyClassAccess(name, access, access),
				name,
				signature,
				superName,
				interfaces
		);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		super.visitInnerClass(
				name,
				outerName,
				innerName,
				accessWidener.applyClassAccess(name, access, classAccess)
		);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return super.visitField(
				accessWidener.applyFieldAccess(className, name, descriptor, classAccess, access),
				name,
				descriptor,
				signature,
				value
		);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new AccessWidenerMethodVisitor(super.visitMethod(
				accessWidener.applyMethodAccess(className, name, descriptor, classAccess, access),
				name,
				descriptor,
				signature,
				exceptions
		));
	}

	private class AccessWidenerMethodVisitor extends MethodVisitor {
		AccessWidenerMethodVisitor(MethodVisitor methodVisitor) {
			super(AccessWidenerVisitor.this.api, methodVisitor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (opcode == Opcodes.INVOKESPECIAL && owner.equals(className) && !name.equals("<init>")) {
				List<AccessWidener.AccessOperator> ops = accessWidener.methodAccess.get(new EntryTriple(owner, name, descriptor));

				if (ops != null && !ops.isEmpty()) {
					opcode = Opcodes.INVOKEVIRTUAL;
				}
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}
