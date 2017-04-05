package myGameEngine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.*;

public class JSEngine {

	private ScriptEngine engine;
	private static JSEngine inst;

	private JSEngine() {
		ScriptEngineManager factory = new ScriptEngineManager();

		engine = factory.getEngineByName("js");
	}

	public static JSEngine getInst() {
		if(inst == null)
			inst = new JSEngine();
		return inst;
	}
	public ScriptEngine getEngine() { return engine; }
	public void execute(String script) {
		try {
			FileReader fr = new FileReader(script);
			engine.eval(fr);
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
