/*
 * #%L
 * JSR-223-compliant JavaScript scripting language plugin.
 * %%
 * Copyright (C) 2008 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.plugins.scripting.javascript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutionException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * JavaScript unit tests.
 * 
 * @author Johannes Schindelin
 * @author Curtis Rueden
 */
public class JavaScriptTest {

	@Test
	public void testBasic() throws InterruptedException, ExecutionException,
		IOException, ScriptException
	{
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);
		final String script = "$x = 1 + 2;";
		assertResult(3.0, scriptService.run("add.js", script, true).get());
	}

	@Test
	public void testLocals() throws ScriptException {
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);

		final ScriptLanguage language = scriptService.getLanguageByExtension("js");
		final ScriptEngine engine = language.getScriptEngine();
		assertTrue(engine.getClass().getName().endsWith(".RhinoScriptEngine"));
		engine.put("$hello", 17);
		assertEquals("17", engine.eval("$hello").toString());
		assertEquals("17", engine.get("$hello").toString());

		final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.clear();
		assertNull(engine.get("$hello"));
	}

	@Test
	public void testParameters() throws InterruptedException, ExecutionException,
		IOException, ScriptException
	{
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);

		final String script = "" + //
			"// @ScriptService ss\n" + //
			"// @OUTPUT String language\n" + //
			"language = ss.getLanguageByName('JavaScript').getLanguageName()\n";
		final ScriptModule m = scriptService.run("hello.js", script, true).get();

		final Object actual = m.getOutput("language");
		final String expected =
			scriptService.getLanguageByName("JavaScript").getLanguageName();
		assertEquals(expected, actual);

		final Object result = m.getReturnValue();
		assertEquals(expected, result);
	}

	@Test
	public void testLoad() throws IOException, InterruptedException, ExecutionException, ScriptException {
		final File tmp = File.createTempFile("js-lib-", ".js");
		final Writer writer = new FileWriter(tmp);
		writer.write("function three() { return 4; }");
		writer.close();

		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);
		final String script = "load('" + tmp.getPath() + "'); three();";
		assertResult(4.0, scriptService.run("three.js", script, false).get());
		assertTrue(tmp.delete());
	}

	// -- Helper methods --

	private void assertResult(final double expected, final ScriptModule m) {
		// NB: Some JVMs return Integer, others Double. Let's be careful here.
		final Number result = (Number) m.getReturnValue();
		assertEquals(expected, result.doubleValue(), 0.0);
	}

}
