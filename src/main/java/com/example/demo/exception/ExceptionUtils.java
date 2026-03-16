package com.example.demo.exception;

public class ExceptionUtils {

    private ExceptionUtils() {}

    /**
     * Returns a string like:
     * "UserServiceImpl.createUser(UserServiceImpl.java:52)"
     * pointing to the exact line where the exception originated.
     */
    public static String origin(Throwable ex) {
        if (ex == null) return "unknown";
        StackTraceElement[] stack = ex.getStackTrace();
        if (stack == null || stack.length == 0) return "unknown";

        // skip JDK / Spring internal frames, find first app frame
        for (StackTraceElement frame : stack) {
            if (frame.getClassName().startsWith("com.example.demo")) {
                return frame.getClassName()
                        .substring(frame.getClassName().lastIndexOf('.') + 1)
                        + "." + frame.getMethodName()
                        + "(" + frame.getFileName() + ":" + frame.getLineNumber() + ")";
            }
        }
        // fallback to raw top frame
        StackTraceElement top = stack[0];
        return top.getClassName()
                .substring(top.getClassName().lastIndexOf('.') + 1)
                + "." + top.getMethodName()
                + "(" + top.getFileName() + ":" + top.getLineNumber() + ")";
    }
}
