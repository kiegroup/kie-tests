package org.kie.tests.workbench.tests.authoring.common;

import java.io.PrintStream;

public class Greeter {

    public String createGreeting(String earthling) {
        return "Hello, " + earthling + "!";
    }

    void greet(PrintStream out, String earthling) {
        out.println(createGreeting(earthling));
    }
}
