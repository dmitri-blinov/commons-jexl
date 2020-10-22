/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal.introspection;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Checks the Introspector implementation
 */
public class IntrospectorTest {

    protected static Class[] classes = {
      AbstractCollection.class,  	
      AbstractList.class,  	
      AbstractMap.class, 	
      AbstractMap.SimpleEntry.class, 	
      AbstractMap.SimpleImmutableEntry.class, 	
      AbstractQueue.class, 	
      AbstractSequentialList.class, 	
      AbstractSet.class, 	
      ArrayDeque.class, 	
      ArrayList.class, 	
      Arrays.class, 	
      BitSet.class, 	
      Calendar.class, 	
      Collections.class, 	
      Currency.class, 	
      Date.class, 	
      Dictionary.class, 	
      EnumMap.class, 	
      EnumSet.class, 	
      EventListenerProxy.class,
      EventObject.class, 	
      FormattableFlags.class, 	
      Formatter.class, 	
      GregorianCalendar.class, 	
      HashMap.class, 	
      HashSet.class, 	
      Hashtable.class, 	
      IdentityHashMap.class, 	
      LinkedHashMap.class, 	
      LinkedHashSet.class, 	
      LinkedList.class, 	
      ListResourceBundle.class, 	
      Locale.class, 	
      Locale.Builder.class, 	
      Objects.class, 	
      Observable.class, 	
      PriorityQueue.class, 	
      Properties.class, 	
      PropertyPermission.class, 	
      PropertyResourceBundle.class, 	
      Random.class, 	
      ResourceBundle.class, 	
      ResourceBundle.Control.class, 	
      Scanner.class, 	
      ServiceLoader.class, 	
      SimpleTimeZone.class, 	
      Stack.class, 	
      StringTokenizer.class, 	
      Timer.class, 	
      TimerTask.class, 	
      TimeZone.class, 	
      TreeMap.class, 	
      TreeSet.class, 	
      UUID.class, 	
      Vector.class, 	
      WeakHashMap.class,
    };

    protected Introspector is = new Introspector(new org.apache.commons.logging.impl.SimpleLog("ROOT"), IntrospectorTest.class.getClassLoader());

    protected static Object[] EMPTY_ARGS = new Object[] {};
    protected static Object[] OBJECT_ARGS = new Object[] {Object.class};
    protected static AtomicLong totalTime = new AtomicLong();

    protected void resolveMethods() {
       for (Class c : classes) {
          is.getMethod(c, "toString", EMPTY_ARGS);
          is.getMethod(c, "hashCode", EMPTY_ARGS);
          is.getMethod(c, "equals", OBJECT_ARGS);
       }
    }

    protected long resolveTask(int count) {
       long timer = System.currentTimeMillis();
       for (int i = 0; i < count; i++)
          resolveMethods();
       timer = System.currentTimeMillis() - timer;   
       return timer;
    }

    @Test
    public void checkSingleThreadPerformanceTest() {
        long timer = resolveTask(1000);
        System.out.println("Introspector single test: " + timer);
    }

    public class TestTask implements Runnable {
      @Override
      public void run() {
        totalTime.addAndGet(resolveTask(1000));
      }
    }

    @Test
    public void checkMultipleThreadPerformanceTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 64; i++) executor.execute(new TestTask());
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("Introspector multiple test: " + totalTime.longValue());
    }

}
