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

import java.lang.reflect.InvocationTargetException;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.AdaptedScriptLanguage;
import org.scijava.script.ScriptLanguage;
import org.scijava.util.ClassUtils;

/**
 * An adapter of the JavaScript interpreter to the SciJava scripting interface.
 * 
 * @author Curtis Rueden
 * @see ScriptEngine
 */
@Plugin(type = ScriptLanguage.class, name = "JavaScript")
public class JavaScriptScriptLanguage extends AdaptedScriptLanguage {

	@Parameter
	private LogService log;

	public JavaScriptScriptLanguage() {
		super("javascript");
	}

	@Override
	public ScriptEngine getScriptEngine() {
		final ScriptEngine engine = super.getScriptEngine();
		try {
			engine.eval("function load(path) {\n"
					+ "  importClass(Packages.sun.org.mozilla.javascript.internal.Context);\n"
					+ "  importClass(Packages.java.io.FileReader);\n"
					+ "  var cx = Context.getCurrentContext();\n"
					+ "  cx.evaluateReader(this, new FileReader(path), path, 1, null);\n"
					+ "}");
		}
		catch (ScriptException e) {
			e.printStackTrace();
		}
		return engine;
	}

	@Override
	public Object decode(final Object object) {
		// NB: JavaScript objects come out of the engine wrapped as
		// JavaScript-specific objects (e.g., NativeJavaObject), which must be
		// unwrapped. Unfortunately, we don't necessarily have direct compile-time
		// access to the JavaScript Wrapper interface implemented by the
		// NativeJavaObject wrapper. But we can access it via reflection. It is
		// typically org.mozilla.javascript.Wrapper, except for Oracle's shaded
		// implementation, which is sun.org.mozilla.javascript.internal.Wrapper.
		// Either way, the package will match that of the wrapped object itself.
		if (object == null) return null;
		final Class<?> objectClass = object.getClass();
		final String packageName = objectClass.getPackage().getName();
		final Class<?> wrapperClass =
			ClassUtils.loadClass(packageName + ".Wrapper");
		if (wrapperClass == null || !wrapperClass.isAssignableFrom(objectClass)) {
			return object;
		}
		try {
			return wrapperClass.getMethod("unwrap").invoke(object);
		}
		catch (final IllegalArgumentException exc) {
			log.warn(exc);
		}
		catch (final IllegalAccessException exc) {
			log.warn(exc);
		}
		catch (final InvocationTargetException exc) {
			log.warn(exc);
		}
		catch (final NoSuchMethodException exc) {
			log.warn(exc);
		}
		return null;
	}

}
