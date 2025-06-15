/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import org.apache.commons.jexl3.parser.JexlNode;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;


/**
 * Script debug base implementation.
 *
 */
public abstract class JexlDebugger implements JexlProbe {

    /** execution threads known to debugger. */
    protected Map<Thread, ThreadInfo> threads;
    /** script sources known to debugger. */
    protected Map<JexlInfo, String> sources;
    /** breakpoints. */
    protected Map<Long, JexlInfo> breakpoints;
    /** stañkframe id counter. */
    protected LongAdder frameId;
    /** breakpoint id counter. */
    protected LongAdder breakpointId;
    /** if debugger is enabled to recieve notifications, off by default. */
    protected volatile boolean enabled;
    /** if debugger should break on new thread execution. */
    protected volatile boolean breakOnNewThread;

    public JexlDebugger() {
        sources = new ConcurrentHashMap<> ();
        threads = new ConcurrentHashMap<> ();
        breakpoints = new ConcurrentHashMap<> ();
        frameId = new LongAdder();
    }

    /**
     * The various thread states of debugged thread.
     */
    public enum ThreadCommand {
        /** The thread should run as normal. */
        CONTINUE,
        /** The thread should pause its execution. */
        PAUSE,
        /** The thread should cancel its execution. */
        ABORT,
        /** The thread should step in to call or next statement. */
        STEP_IN,
        /** The thread should step over to next statement. */
        STEP_OVER,
        /** The thread should step out to end of call or next statement. */
        STEP_OUT;
    }

    protected class ThreadInfo {

        protected final Thread thread;
        /** execution thread stack frames. */
        protected final Map<Long, StackFrameInfo> frames;
        /** execution thread command. */
        protected volatile ThreadCommand command;
        /** execution thread is paused. */
        protected volatile boolean paused;

        protected ThreadInfo(final Thread t) {
            thread = t;
            frames = Collections.synchronizedMap(new LinkedHashMap<> ());
            command = breakOnNewThread ? ThreadCommand.PAUSE : null;
        }

        public Thread getThread() {
            return thread;
        }

        public boolean isPaused() {
            return paused;
        }

        public void setPaused(boolean value) {
            paused = value;
        }

        public synchronized ThreadCommand getCommand() {
            return command;
        }

        public synchronized void setCommand(ThreadCommand value) {
            command = value;
        }

        protected synchronized ThreadCommand waitCommand() {
            ThreadCommand result = command;
            command = null;
            return result;
        }

        protected void addFrame(long id) {
            frames.put(id, new StackFrameInfo(this, id));
        }

        protected void removeFrame(long id) {
            frames.remove(id);
        }

        public Set<Long> getStackFrames() {
            return Collections.unmodifiableSet(frames.keySet());
        }

        public StackFrameInfo getStackFrameInfo(long id) {
            return frames.get(id);
        }

        public boolean isEmpty() {
            return frames.isEmpty();
        }
    }

    public class StackFrameInfo {

        /** tread info. */
        protected final ThreadInfo threadInfo;
        /** stañkframe id. */
        protected final long frameId;
        /** interpreter state. */
        protected final Map<JexlNode,StatementInfo> state;

        protected StackFrameInfo(final ThreadInfo ti, final long frameId) {
            this.threadInfo = ti;
            this.frameId = frameId;
            this.state = new LinkedHashMap();
        }

        public ThreadInfo getThreadInfo() {
            return threadInfo;
        }

        protected void startStatement(final JexlNode node, final StatementInfo stmt) {
            state.put(node, stmt);

            do {
                ThreadCommand cmd = threadInfo.waitCommand();

                if (cmd != null) {
                    synchronized (threadInfo) {
                        switch (cmd) {
                            case PAUSE : 
                               if (!threadInfo.isPaused()) {
                                   threadInfo.setPaused(true);
                                   executionStopped(this, "pause");
                               }
                               break;
                            case CONTINUE : 
                               if (threadInfo.isPaused()) {
                                   threadInfo.setPaused(false);
                                   executionContinued(this);
                               }
                               break;
                            case ABORT : 
                               threadInfo.setPaused(false);
                               throw new JexlException.Cancel(node.jexlInfo());
                            case STEP_IN : 
                               if (threadInfo.isPaused()) {
                                   threadInfo.setPaused(false);
                                   executionContinued(this);
                               }
                               executionContinued(this);
                               break;
                            case STEP_OVER : 
                               if (threadInfo.isPaused()) {
                                   threadInfo.setPaused(false);
                                   executionContinued(this);
                               }
                               break;
                            case STEP_OUT : 
                               if (threadInfo.isPaused()) {
                                   threadInfo.setPaused(false);
                                   executionContinued(this);
                               }
                               break;
                        }
                    }
                }
            } while (threadInfo.isPaused());
        }

        protected void endStatement(final JexlNode node) {
            state.remove(node);
        }

        public StatementInfo getStatement(final JexlNode node) {
            return state.get(node);
        }

    }

    protected static class StatementInfo {

        protected final JexlInfo stmt;
        protected final JexlNode node;

        protected Object result;
        protected Throwable exception;

        protected Map<String, VarInfo> locals = new LinkedHashMap<> ();
        protected Map<String, VarInfo> parameters = new LinkedHashMap<> ();
        protected Map<String, VarInfo> capturedVariables = new LinkedHashMap<> ();

        protected StatementInfo(final JexlInfo stmt, final JexlNode node) {
            this.stmt = stmt;
            this.node = node;
        }

        protected Map<String, VarInfo> getLocals() {
            return locals;
        }

        protected VarInfo getLocal(final String name) {
            return locals.get(name);
        }

        protected Map<String, VarInfo> getParameters() {
            return parameters;
        }

        protected VarInfo getParameter(final String name) {
            return parameters.get(name);
        }

        protected Map<String, VarInfo> getCapturedVariables() {
            return capturedVariables;
        }

        protected VarInfo getCapturedVar(final String name) {
            return capturedVariables.get(name);
        }

        protected JexlInfo getStatement() {
            return stmt;
        }

        protected JexlNode getNode() {
            return node;
        }

        protected Object getResult() {
            return result;
        }

        protected void captureFrame(JexlProbe.Frame frame) {
            captureVariables(frame.getLocals(), locals);
            captureVariables(frame.getParameters(), parameters);
            captureVariables(frame.getCapturedVariables(), capturedVariables);
        }

        protected void captureVariables(JexlProbe.Scope l, Map<String, VarInfo> vars) {
            String[] names = l.getNames();
            if (names != null) {
                for (String name : names) {
                    JexlProbe.Variable i = l.getVariableInfo(name);
                    if (i != null) {
                       vars.put(name, new VarInfo(name, i.getType(), i.isRequired(), i.isFinal(), l.getVariable(name)));
                    } else {
                       vars.put(name, null);
                    }
                }
            }
        }

        protected void setResult(Object value) {
            result = value;
        }

        protected Throwable getException() {
            return exception;
        }

        protected void setException(Throwable value) {
            exception = value;
        }

    }

    protected static class VarInfo {

        protected final String name;
        protected final Class type;
        protected final boolean isFinal;
        protected final boolean isRequired;

        protected final Object value;

        protected VarInfo(final String name, final Class type, final boolean isFinal, final boolean isRequired, final Object value) {
            this.name = name;
            this.type = type;
            this.isFinal = isFinal;
            this.isRequired = isRequired;
            this.value = value;
        }

        protected String getName() {
            return name;
        }

        protected Class getType() {
            return type;
        }

        protected Object getValue() {
            return value;
        }

        protected boolean isFinal() {
            return isFinal;
        }

        protected boolean isRequired() {
            return isRequired;
        }

    }

    public Set<JexlInfo> getSources() {
        return Collections.unmodifiableSet(sources.keySet());
    }

    public String getSource(JexlInfo script) {
        return sources.get(script);
    }

    public Set<Thread> getThreads() {
        return Collections.unmodifiableSet(threads.keySet());
    }

    public boolean isBreakOnNewThread() {
        return breakOnNewThread;
    }

    public void setBreakOnNewThread(boolean value) {
        breakOnNewThread = value;
    }

    public long addBreakpoint(JexlInfo source) {

        for (Map.Entry<Long, JexlInfo> entry : breakpoints.entrySet()) {
            if (entry.getValue().equals(source)) {
                return entry.getKey();
            }
        }

        breakpointId.increment();
        long id = breakpointId.longValue();

        breakpoints.put(id, source);

        return id;
    }

    public boolean removeBreakpoint(long id) {
        return breakpoints.remove(id) != null;
    }

    public boolean removeBreakpoint(JexlInfo source) {

        long id = -1;
        for (Map.Entry<Long, JexlInfo> entry : breakpoints.entrySet()) {
            if (entry.getValue().equals(source)) {
                id = entry.getKey();
                break;
            }
        }

        return id != -1 ? removeBreakpoint(id) : false;
    }

    // JexlDebugger event interface

    public void executionStopped(StackFrameInfo si, String reason) {

    }

    public void executionContinued(StackFrameInfo si) {

    }

    public void threadStarted(ThreadInfo thread) {

    }

    public void threadExited(ThreadInfo thread) {

    }

    // JexlProbe interface

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public long loadSource(JexlInfo script, String source) {
        sources.put(script, source);
        return sources.size();
    }

    @Override
    public synchronized long startScript(JexlInfo script) {
        frameId.increment();
        long id = frameId.longValue();

        ThreadInfo ti = threads.computeIfAbsent(Thread.currentThread(), x -> {
           ThreadInfo result = new ThreadInfo(x);
           threadStarted(result);
           return result;
        });

        ti.addFrame(id);

        return id;
    }

    @Override
    public synchronized boolean endScript(Frame frame, Object result, Throwable any) {
        long id = frame.getFrameId();

        ThreadInfo ti = threads.get(Thread.currentThread());

        ti.removeFrame(id);

        if (ti.isEmpty()) {
            Thread t = Thread.currentThread();
            threadExited(ti);
            threads.remove(t);
        }

        return true;
    }

    @Override
    public boolean startStatement(JexlInfo source, JexlNode node, Frame frame) {

        long id = frame.getFrameId();
        ThreadInfo ti = threads.get(Thread.currentThread());

        StackFrameInfo sfi = ti.getStackFrameInfo(id);

        StatementInfo stmt = new StatementInfo(source, node);
        stmt.captureFrame(frame);

        sfi.startStatement(node, stmt);

        return true;
    }

    @Override
    public boolean endStatement(JexlInfo source, JexlNode node, Frame frame, Object result, Throwable any) {

        long id = frame.getFrameId();
        ThreadInfo ti = threads.get(Thread.currentThread());

        StackFrameInfo sfi = ti.getStackFrameInfo(id);
        StatementInfo stmt = sfi.getStatement(node);

        stmt.setResult(result);
        stmt.setException(any);
        stmt.captureFrame(frame);

        sfi.endStatement(node);

        return true;
    }

}
