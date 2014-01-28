/*******************************************************************************
 * This file is part of Pebble.
 * 
 * Original work Copyright (c) 2009-2013 by the Twig Team
 * Modified work Copyright (c) 2013 by Mitchell Bösecke
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 ******************************************************************************/
package com.mitchellbosecke.pebble.node;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.compiler.Compiler;
import com.mitchellbosecke.pebble.template.PebbleTemplateImpl;
import com.mitchellbosecke.pebble.utils.Context;

public class NodeRoot extends AbstractNode {

	private final String filename;

	private final NodeBody body;

	private final String parentFileName;

	private final Map<String, NodeBlock> blocks;

	private final Map<String, List<NodeMacro>> macros;

	public NodeRoot(NodeBody body, String parentFileName, Map<String, NodeBlock> blocks,
			Map<String, List<NodeMacro>> macros, String filename) {
		super(0);
		this.body = body;
		this.parentFileName = parentFileName;
		this.blocks = blocks;
		this.macros = macros;
		this.filename = filename;
	}

	public String getFilename() {
		return filename;
	}

	@Override
	public void compile(Compiler compiler) {
		String className = compiler.getEngine().getTemplateClassName(filename);
		
		compileMetaInformationInComments(compiler);
		compileClassHeader(compiler, className);
		compileConstructor(compiler, className);
		compileBuildContentFunction(compiler);
		compileBlocks(compiler);
		compileMacros(compiler);
		compileClassFooter(compiler);
	}
	
	private void compileMetaInformationInComments(Compiler compiler){
		compiler.write("/*").newline();
		compiler.write(" * Filename: ").raw(filename).newline();
		compiler.write(" * Parent filename: ").raw(parentFileName).newline();
		compiler.write(" * Compiled on: ").raw(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date())).newline();
		compiler.write(" */").newline();
	}

	private void compileClassHeader(Compiler compiler, String className) {
		String parentClass = compiler.getEngine().getTemplateParentClass().getName();

		compiler.write(String.format("package %s;", PebbleTemplateImpl.COMPILED_PACKAGE_NAME)).newline(2)
				.write("import java.util.Map;").newline().write("import java.util.HashMap;").newline().write("import ")
				.raw(Context.class.getName()).raw(";").newline(2)
				.write(String.format("public class %s extends %s {", className, parentClass)).indent();
	}

	private void compileConstructor(Compiler compiler, String className) {
		compiler.newline(2).write("public ").raw(className).raw(" (String javaCode, ")
				.raw(PebbleEngine.class.getName()).raw(" engine, ").raw(PebbleTemplateImpl.class.getName()).raw(" parent) {").newline();

		compiler.indent().write("super(javaCode, engine, parent);").newline();

		compiler.outdent().write("}").newline(2);
	}

	private void compileBuildContentFunction(Compiler compiler) {
		compiler.newline(2)
				.write("public void buildContent(java.io.Writer writer, Context context) throws com.mitchellbosecke.pebble.error.PebbleException, java.io.IOException {")
				.newline().indent();
		if (this.parentFileName != null) {
			compiler.write("context.pushInheritanceChain(this);").newline();
			compiler.write("getParent().buildContent(writer, context);").newline();
		} else {
			body.compile(compiler);
		}

		compiler.outdent().newline().write("}");
	}

	private void compileClassFooter(Compiler compiler) {
		compiler.outdent().newline(2).write("}");
	}

	private void compileBlocks(Compiler compiler) {
		compiler.newline(2).write("public void initBlocks() {").newline().indent();
		for (NodeBlock block : blocks.values()) {
			compiler.newline().subcompile(block);
		}
		compiler.outdent().newline().write("}");
	}

	private void compileMacros(Compiler compiler) {
		compiler.newline(2).write("public void initMacros() {").newline().indent();
		for (List<NodeMacro> overloadedMacros : macros.values()) {
			for (NodeMacro macro : overloadedMacros) {
				compiler.newline(2).subcompile(macro);
			}
		}
		compiler.outdent().newline().write("}");
	}

	public boolean hasParent() {
		return parentFileName != null;
	}

	public String getParentFileName() {
		return parentFileName;
	}
}