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
package org.apache.commons.jexl3;

import java.io.File;
import java.io.FileReader;
import java.util.Collections;

import com.google.gson.Gson;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for pragmas
 */
public class ComposePermissionsTest extends JexlTestCase {
  static final String SAMPLE_JSON =  "src/test/scripts/sample.json";
  /**
   * Create a new test case.
   */
  public ComposePermissionsTest() {
    super("PermissionsTest");
  }

  @Test
  public void testComposePermissions() throws Exception {
    runComposePermissions(JexlPermissions.UNRESTRICTED);
  }

  @Test
  public void testComposePermissions1() throws Exception {
    runComposePermissions(new JexlPermissions.Delegate(JexlPermissions.UNRESTRICTED) {
      @Override public String toString() {
        return "delegate:" + base.toString();
      }
    });
  }

  @Test
  public void testComposePermissions2() throws Exception {
    runComposePermissions(new JexlPermissions.ClassPermissions(JexlPermissions.UNRESTRICTED, Collections.emptySet()));
  }

  void runComposePermissions(JexlPermissions p) throws Exception {
    final String check = "http://example.com/content.jpg";
    final File jsonFile = new File(SAMPLE_JSON);
    Gson gson = new Gson();
    Object json = gson.fromJson(new FileReader(jsonFile), Object.class);
    Assert.assertNotNull(json);

    // will succeed because java.util.Map is allowed and gson LinkedTreeMap is one
    JexlEngine j0 = createEngine(false, p);
    JexlScript s0 = j0.createScript("json.pageInfo.pagePic", "json");
    Object r0 = s0.execute(null, json);
    Assert.assertEquals(check, r0);

    // will fail if gson package is denied
    JexlEngine j1 = createEngine(false, p.compose("com.google.gson.internal {}"));
    JexlScript s1 = j1.createScript("json.pageInfo.pagePic", "json");
    try {
      Object r1 = s1.execute(null, json);
      Assert.fail("gson restricted");
    } catch (JexlException.Property xproperty) {
      Assert.assertEquals("pageInfo", xproperty.getProperty());
    }

    // will fail since gson package is denied
     j1 = createEngine(false, p.compose("com.google.gson.internal { LinkedTreeMap {} }"));
     s1 = j1.createScript("json.pageInfo.pagePic", "json");
    try {
      Object r1 = s1.execute(null, json);
      Assert.fail("gson LinkTreeMap restricted");
    } catch (JexlException.Property xproperty) {
      Assert.assertEquals("pageInfo", xproperty.getProperty());
    }

    // will not fail since gson objects
    j1 = createEngine(false, JexlPermissions.RESTRICTED);
    s1 = j1.createScript("json.pageInfo.pagePic", "json");
    Object r1 = s1.execute(null, json);
    Assert.assertEquals(check, r0);
  }
}
