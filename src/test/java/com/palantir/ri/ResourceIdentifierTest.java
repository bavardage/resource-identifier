// Copyright 2015 Palantir Technologies
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.palantir.ri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ResourceIdentifierTest {
    private static List<String> goodIds;
    private static List<String> badIds;

    @BeforeClass
    public static void setUp() {
        goodIds = new ArrayList<>();
        goodIds.add("ri.service.instance.folder.testContext.foo");
        goodIds.add("ri.service-123.north-east.folder.context.foo.bar");
        goodIds.add("ri.a1p2p3.south-west.data-set.my-context.hello_WORLD-123");
        goodIds.add("ri.my-service.instance1.graph-node._._");
        goodIds.add("ri.service.1instance.type.emptyContext.name");
        goodIds.add("ri.my-service..graph-node.context.noInstance");
        goodIds.add("ri.my-service.instance.graph-node..noContext");
        goodIds.add("ri.my-service..graph-node.noInstance.multiple.extra.dots");

        badIds = new ArrayList<>();
        badIds.add("");
        badIds.add("badString");
        badIds.add("ri.123.instance.type.context.name");
        badIds.add("ri.service.CAPLOCK.type.context.name");
        badIds.add("ri.service.instance.-123.context.name");
        badIds.add("ri..instance.type.context.noService");
        badIds.add("ri.service.instance.type..");
        badIds.add("id.bad.id.class.b.name");
        badIds.add("ri:service::instance:type:context:name");
        badIds.add("ri.service.instance.type.noLocator.");
        badIds.add("ri.service.instance.type.context.name!@#");
        badIds.add("ri.service.instance.type.context!@#.name");
        badIds.add("ri.service(name)..folder.master.foo");
    }

    @Test
    public void testIsValidGood() {
        for (String rid : goodIds) {
            assertTrue(ResourceIdentifier.isValid(rid));
        }
    }

    @Test
    public void testIsValidBad() {
        for (String rid : badIds) {
            assertFalse(ResourceIdentifier.isValid(rid));
        }
        assertFalse(ResourceIdentifier.isValid(null));
    }

    @Test
    public void testConstructionErrorMessage() {
        try {
            ResourceIdentifier.of("ri.bad.....dots");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal resource identifier format: ri.bad.....dots", e.getMessage());
        }
        try {
            ResourceIdentifier.of("123Service", "", "type", "context", "name");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal service format: 123Service", e.getMessage());
        }
        try {
            ResourceIdentifier.of("service", "Instance", "type", "context", "name");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal instance format: Instance", e.getMessage());
        }
        try {
            ResourceIdentifier.of("service", "i", "type-name", "!@#$", "name");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal context format: !@#$", e.getMessage());
        }
        try {
            ResourceIdentifier.of("service", "i", "type-name", "context", "!@#$");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal locator format: !@#$", e.getMessage());
        }
        try {
            ResourceIdentifier.of(null, null, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal service format: null", e.getMessage());
        }
        try {
            ResourceIdentifier.of("service", null, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal instance format: null", e.getMessage());
        }
        try {
            ResourceIdentifier.of("service", "", null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Illegal type format: null", e.getMessage());
        }
    }

    @Test
    public void testReconstruction() {
        for (String rid : goodIds) {
            ResourceIdentifier resourceId = ResourceIdentifier.of(rid);
            String service = resourceId.getService();
            String instance = resourceId.getInstance();
            String type = resourceId.getType();
            String context = resourceId.getContext();
            String oid = resourceId.getLocator();
            assertEquals(resourceId, ResourceIdentifier.of(service, instance, type, context, oid));
        }
    }

    @Test
    public void testSerialization() throws IOException {
        ObjectMapper om = new ObjectMapper();
        ResourceIdentifier rid = ResourceIdentifier.of("ri.service.instance.type.context.name");
        ResourceIdentifier rid1 = ResourceIdentifier.of("ri.service..type-123.context-_9.aBC-name_123");
        ResourceIdentifier rid2 = ResourceIdentifier.of("myservice", "instance-1", "folder", "my-contexT", "foo.bar");
        ResourceIdentifier rid3 = ResourceIdentifier.of("myservice", "", "data", "_myContext_", "MyDATA");
        String serializedString = om.writeValueAsString(rid);
        String serializedString1 = om.writeValueAsString(rid1);
        String serializedString2 = om.writeValueAsString(rid2);
        String serializedString3 = om.writeValueAsString(rid3);
        ResourceIdentifier value = om.readValue(serializedString, ResourceIdentifier.class);
        ResourceIdentifier value1 = om.readValue(serializedString1, ResourceIdentifier.class);
        ResourceIdentifier value2 = om.readValue(serializedString2, ResourceIdentifier.class);
        ResourceIdentifier value3 = om.readValue(serializedString3, ResourceIdentifier.class);
        assertEquals(rid, value);
        assertEquals(rid1, value1);
        assertEquals(rid2, value2);
        assertEquals(rid3, value3);
    }

    @Test
    public void testStringConstruction() {
        assertEquals("ri.service..type.context.name", ResourceIdentifier.of("service", "", "type", "context", "name").toString());
        assertEquals("ri.service.instance.type.ctx.name", ResourceIdentifier.of("service", "instance", "type", "ctx", "name").toString());
    }

    @Test
    public void testEqualsHashCode() {
        ResourceIdentifier prevRid = null;
        for (int i = 0; i < goodIds.size(); ++i) {
            ResourceIdentifier curRid = ResourceIdentifier.of(goodIds.get(i));
            ResourceIdentifier curRid2 = ResourceIdentifier.of(goodIds.get(i));
            assertEquals(curRid, curRid);
            assertEquals(curRid, curRid2);
            assertEquals(curRid.toString(), curRid2.toString());
            assertEquals(curRid.hashCode(), curRid2.hashCode());
            assertNotEquals(curRid, NotEqualsObj.INSTANCE);
            if (prevRid != null) {
                assertNotEquals(prevRid, curRid);
                assertNotEquals(prevRid.hashCode(), curRid.hashCode());
            }
            prevRid = curRid;
        }
    }

    private enum NotEqualsObj {
        INSTANCE
    }
}
