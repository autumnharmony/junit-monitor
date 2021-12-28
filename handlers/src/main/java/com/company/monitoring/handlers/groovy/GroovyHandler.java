package com.company.monitoring.handlers.groovy;

import com.company.monitoring.api.Handler;
import com.company.monitoring.api.File;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class GroovyHandler implements Handler<File> {

    private final String groovy;

    public GroovyHandler(String groovy) {
        this.groovy = groovy;
    }

    @Override
    public void handle(File data) {
        GroovyShell shell = new GroovyShell();
        Bindings bindings = new SimpleBindings();
        bindings.put("name", data.getName());
        bindings.put("content", data.getContent());
        ScriptEngine scriptEngine = new GroovyScriptEngineFactory().getScriptEngine();
        try {
            scriptEngine.eval(groovy, bindings);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getType() {
        return "xml";
    }

}
