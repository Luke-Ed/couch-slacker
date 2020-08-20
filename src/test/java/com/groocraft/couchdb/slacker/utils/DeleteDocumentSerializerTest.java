package com.groocraft.couchdb.slacker.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.groocraft.couchdb.slacker.TestDocument;
import com.groocraft.couchdb.slacker.structure.BulkRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeleteDocumentSerializerTest {

    @Test
    public void test() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(new DeleteDocumentSerializer<>(TestDocument.class));
        mapper.registerModule(module);
        BulkRequest<TestDocument> request = new BulkRequest<>(List.of(new TestDocument("id", "rev", "value", "value2")));
        assertEquals("{\"docs\":[{\"_id\":\"id\",\"_rev\":\"rev\",\"value\":\"value\",\"value2\":\"value2\",\"_deleted\":true}]}",
                mapper.writeValueAsString(request), "Object should be serialized as is, only _deleted should be added");
    }

}