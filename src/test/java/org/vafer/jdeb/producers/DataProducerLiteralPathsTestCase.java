package org.vafer.jdeb.producers;

import static org.mockito.Mockito.verify;

import org.apache.tools.tar.TarEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

@RunWith(MockitoJUnitRunner.class)
public class DataProducerLiteralPathsTestCase {

    private static final String[] INCLUDES = {};
    private static final String[] EXCLUDES = {};
    
    @Mock
    private DataConsumer mockDataConsumer;
    
    private Mapper[] mappers = new Mapper[0];
    private DataProducer dataProducer;
    
    @Test
    public void basic() throws Exception {
        String[] paths = {"/var/log", "/var/lib"};
        dataProducer = new DataProducerLiteralPaths(paths, INCLUDES, EXCLUDES, mappers);
        dataProducer.produce(mockDataConsumer);
        verify(mockDataConsumer).onEachDir("/var/log", "", "root", 0, "root", 0, TarEntry.DEFAULT_DIR_MODE, 0);
        verify(mockDataConsumer).onEachDir("/var/lib", "", "root", 0, "root", 0, TarEntry.DEFAULT_DIR_MODE, 0);
    }
    
}
