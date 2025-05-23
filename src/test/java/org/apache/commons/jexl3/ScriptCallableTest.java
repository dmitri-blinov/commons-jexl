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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.jexl3.internal.Script;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests around asynchronous script execution and interrupts.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ScriptCallableTest extends JexlTestCase {
    //private Log logger = LogFactory.getLog(JexlEngine.class);
    public ScriptCallableTest() {
        super("ScriptCallableTest");
    }

    @Test
    public void testFuture() throws Exception {
        final JexlScript e = JEXL.createScript("while(true);");
        final FutureTask<Object> future = new FutureTask<>(e.callable(null));

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(future);
        Object t = 42;
        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            executor.shutdown();
        }

        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
    }

    @Test
    public void testCallableCancel() throws Exception {
        final Semaphore latch = new Semaphore(0);
        final JexlContext ctxt = new MapContext();
        ctxt.set("latch", latch);

        final JexlScript e = JEXL.createScript("latch.release(); while(true);");
        final Script.CallableScript c = (Script.CallableScript) e.callable(ctxt);
        Object t = 42;
        final Callable<Object> kc = () -> {
            latch.acquire();
            return c.cancel();
        };
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Future<?> future = executor.submit(c);
        final Future<?> kfc = executor.submit(kc);
        List<Runnable> lr;
        try {
            Assert.assertTrue((Boolean) kfc.get());
            t = future.get();
            Assert.fail("should have been cancelled");
        } catch (final ExecutionException xexec) {
            // ok, ignore
            Assert.assertTrue(xexec.getCause() instanceof JexlException.Cancel);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(c.isCancelled());
        Assert.assertTrue(lr == null || lr.isEmpty());
    }

    public static class CancellationContext extends MapContext implements JexlContext.CancellationHandle {
        private final AtomicBoolean cancellation;

        CancellationContext(final AtomicBoolean c) {
            cancellation = c;
        }
        @Override
        public AtomicBoolean getCancellation() {
            return cancellation;
        }
    }

    // JEXL-317
    @Test
    public void testCallableCancellation() throws Exception {
        final Semaphore latch = new Semaphore(0);
        final AtomicBoolean cancel = new AtomicBoolean(false);
        final JexlContext ctxt = new CancellationContext(cancel);
        ctxt.set("latch", latch);

        final JexlScript e = JEXL.createScript("latch.release(); while(true);");
        final Script.CallableScript c = (Script.CallableScript) e.callable(ctxt);
        Object t = 42;
        final Callable<Object> kc = () -> {
            latch.acquire();
            return cancel.compareAndSet(false, true);
        };
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Future<?> future = executor.submit(c);
        final Future<?> kfc = executor.submit(kc);
        List<Runnable> lr;
        try {
            Assert.assertTrue((Boolean) kfc.get());
            t = future.get();
            Assert.fail("should have been cancelled");
        } catch (final ExecutionException xexec) {
            // ok, ignore
            Assert.assertTrue(xexec.getCause() instanceof JexlException.Cancel);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(c.isCancelled());
        Assert.assertTrue(lr == null || lr.isEmpty());
    }

    @Test
    public void testCallableTimeout() throws Exception {
        List<Runnable> lr = null;
        final Semaphore latch = new Semaphore(0);
        final JexlContext ctxt = new MapContext();
        ctxt.set("latch", latch);

        final JexlScript e = JEXL.createScript("latch.release(); while(true);");
        final Callable<Object> c = e.callable(ctxt);
        Object t = 42;

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        try {
            latch.acquire();
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCallableClosure() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("function(t) {while(t);}");
        final Callable<Object> c = e.callable(null, Boolean.TRUE);
        Object t = 42;

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    public static class TestContext extends MapContext implements JexlContext.NamespaceResolver {
        @Override
        public Object resolveNamespace(final String name) {
            return name == null ? this : null;
        }

        public int wait(final int s) throws InterruptedException {
            Thread.sleep(1000 * s);
            return s;
        }

        public int waitInterrupt(final int s) {
            try {
                Thread.sleep(1000 * s);
                return s;
            } catch (final InterruptedException xint) {
                Thread.currentThread().interrupt();
            }
            return -1;
        }

        public int runForever() {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
            return 1;
        }

        public int interrupt() throws InterruptedException {
            Thread.currentThread().interrupt();
            return 42;
        }

        public void sleep(final long millis) throws InterruptedException {
            Thread.sleep(millis);
        }

        public int hangs(final Object t) {
            return 1;
        }
    }

    @Test
    public void testNoWait() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("wait(0)");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            final Future<?> future = executor.submit(c);
            final Object t = future.get(2, TimeUnit.SECONDS);
            Assert.assertTrue(future.isDone());
            Assert.assertEquals(0, t);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testWait() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("wait(1)");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            final Future<?> future = executor.submit(c);
            final Object t = future.get(2, TimeUnit.SECONDS);
            Assert.assertEquals(1, t);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelWait() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("wait(10)");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            final Future<?> future = executor.submit(c);
            Object t = 42;
            try {
                t = future.get(100, TimeUnit.MILLISECONDS);
                Assert.fail("should have timed out");
            } catch (final TimeoutException xtimeout) {
                // ok, ignore
                future.cancel(true);
            }
            Assert.assertTrue(future.isCancelled());
            Assert.assertEquals(42, t);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelWaitInterrupt() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("waitInterrupt(42)");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelForever() throws Exception {
        List<Runnable> lr = null;
        final Semaphore latch = new Semaphore(0);
        final JexlContext ctxt = new TestContext();
        ctxt.set("latch", latch);

        final JexlScript e = JEXL.createScript("latch.release(); runForever()");
        final Callable<Object> c = e.callable(ctxt);

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            latch.acquire();
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelLoopWait() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("while (true) { wait(10) }");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testInterruptVerboseStrict() throws Exception {
        runInterrupt(new JexlBuilder().silent(false).strict(true).create());
    }

    @Test
    public void testInterruptVerboseLenient() throws Exception {
        runInterrupt(new JexlBuilder().silent(false).strict(false).create());
    }

    @Test
    public void testInterruptSilentStrict() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(true).create());
    }

    @Test
    public void testInterruptSilentLenient() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(false).create());
    }

    @Test
    public void testInterruptCancellable() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(true).cancellable(true).create());
    }

    /**
     * Redundant test with previous ones but impervious to JEXL engine configuration.
     * @throws Exception if there is a regression
     */
    private void runInterrupt(final JexlEngine jexl) throws Exception {
        List<Runnable> lr = null;
        final ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            final JexlContext ctxt = new TestContext();

            // run an interrupt
            final JexlScript sint = jexl.createScript("interrupt(); return 42");
            Object t = null;
            Script.CallableScript c = (Script.CallableScript) sint.callable(ctxt);
            try {
                t = c.call();
                if (c.isCancellable()) {
                    Assert.fail("should have thrown a Cancel");
                }
            } catch (final JexlException.Cancel xjexl) {
                if (!c.isCancellable()) {
                    Assert.fail("should not have thrown " + xjexl);
                }
            }
            Assert.assertTrue(c.isCancelled());
            Assert.assertNotEquals(42, t);

            // self interrupt
            Future<Object> f = null;
            c = (Script.CallableScript) sint.callable(ctxt);
            try {
                f = exec.submit(c);
                t = f.get();
                if (c.isCancellable()) {
                    Assert.fail("should have thrown a Cancel");
                }
            } catch (final ExecutionException xexec) {
                if (!c.isCancellable()) {
                    Assert.fail("should not have thrown " + xexec);
                }
            }
            Assert.assertTrue(c.isCancelled());
            Assert.assertNotEquals(42, t);

            // timeout a sleep
            final JexlScript ssleep = jexl.createScript("sleep(30000); return 42");
            try {
                f = exec.submit(ssleep.callable(ctxt));
                t = f.get(100L, TimeUnit.MILLISECONDS);
                Assert.fail("should timeout");
            } catch (final TimeoutException xtimeout) {
                if (f != null) {
                    f.cancel(true);
                }
            }
            Assert.assertNotEquals(42, t);

            // cancel a sleep
            try {
                final Future<Object> fc = exec.submit(ssleep.callable(ctxt));
                final Runnable cancels = () -> {
                    try {
                        Thread.sleep(200L);
                    } catch (final Exception xignore) {

                    }
                    fc.cancel(true);
                };
                exec.submit(cancels);
                t = f.get(100L, TimeUnit.MILLISECONDS);
                Assert.fail("should be cancelled");
            } catch (final CancellationException xexec) {
                // this is the expected result
            }

            // timeout a while(true)
            final JexlScript swhile = jexl.createScript("while(true); return 42");
            try {
                f = exec.submit(swhile.callable(ctxt));
                t = f.get(100L, TimeUnit.MILLISECONDS);
                Assert.fail("should timeout");
            } catch (final TimeoutException xtimeout) {
                if (f != null) {
                    f.cancel(true);
                }
            }
            Assert.assertNotEquals(42, t);

            // cancel a while(true)
            try {
                final Future<Object> fc = exec.submit(swhile.callable(ctxt));
                final Runnable cancels = () -> {
                    try {
                        Thread.sleep(200L);
                    } catch (final Exception xignore) {

                    }
                    fc.cancel(true);
                };
                exec.submit(cancels);
                t = fc.get();
                Assert.fail("should be cancelled");
            } catch (final CancellationException xexec) {
                // this is the expected result
            }
            Assert.assertNotEquals(42, t);
        } finally {
            lr = exec.shutdownNow();
        }
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testHangs() throws Exception {
        final JexlScript e = JEXL.createScript("hangs()");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            final Future<?> future = executor.submit(c);
            final Object t = future.get(1, TimeUnit.SECONDS);
            Assert.fail("hangs should not be solved");
        } catch(final ExecutionException xexec) {
            Assert.assertTrue(xexec.getCause() instanceof JexlException.Method);
        } finally {
            executor.shutdown();
        }
    }

    public static class AnnotationContext extends MapContext implements JexlContext.AnnotationProcessor {
        @Override
        public Object processAnnotation(final String name, final Object[] args, final Callable<Object> statement) throws Exception {
            if ("timeout".equals(name) && args != null && args.length > 0) {
                final long ms = args[0] instanceof Number
                          ? ((Number) args[0]).longValue()
                          : Long.parseLong(args[0].toString());
                final Object def = args.length > 1? args[1] : null;
                if (ms > 0) {
                    final ExecutorService executor = Executors.newFixedThreadPool(1);
                    Future<?> future = null;
                    try {
                        future = executor.submit(statement);
                        return future.get(ms, TimeUnit.MILLISECONDS);
                    } catch (final TimeoutException xtimeout) {
                        if (future != null) {
                            future.cancel(true);
                        }
                    } finally {
                        executor.shutdown();
                    }

                }
                return def;
            }
            return statement.call();
        }

        public void sleep(final long ms) throws InterruptedException {
           Thread.sleep(ms);
        }

    }

    @Test
    public void testTimeout() throws Exception {
        JexlScript script = JEXL.createScript("(flag)->{ @timeout(100) { while(flag); return 42 }; 'cancelled' }");
        final JexlContext ctxt = new AnnotationContext();
        Object result = null;
        try {
            result = script.execute(ctxt, true);
        } catch (final Exception xany) {
            if (xany.getCause() != null) {
                Assert.fail(xany.getCause().toString());
            } else {
                Assert.fail(xany.toString());
            }
        }
        Assert.assertEquals("cancelled", result);

        result = script.execute(ctxt, false);
        Assert.assertEquals(42, result);
        script = JEXL.createScript("(flag)->{ @timeout(100, 'cancelled') { while(flag); 42; } }");
        try {
            result = script.execute(ctxt, true);
        } catch (final Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals("cancelled", result);

        result = script.execute(ctxt, false);
        Assert.assertEquals(42, result);
        script = JEXL.createScript("@timeout(100) {sleep(1000); 42; } -42;");
        try {
            result = script.execute(ctxt);
        } catch (final Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals(-42, result);

        script = JEXL.createScript("@timeout(100) {sleep(1000); return 42; } return -42;");
        try {
            result = script.execute(ctxt);
        } catch (final Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals(-42, result);
        script = JEXL.createScript("@timeout(1000) {sleep(100); return 42; } return -42;");
        try {
            result = script.execute(ctxt);
        } catch (final Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals(42, result);
    }
}
