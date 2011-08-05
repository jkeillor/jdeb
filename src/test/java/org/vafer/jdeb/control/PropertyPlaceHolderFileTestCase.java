package org.vafer.jdeb.control;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.StringReader;

import org.apache.tools.ant.util.ReaderInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vafer.jdeb.utils.VariableResolver;

@RunWith(MockitoJUnitRunner.class)
public class PropertyPlaceHolderFileTestCase {

    @Mock
    private VariableResolver mockVariableResolver;
    
    private PropertyPlaceHolderFile placeHolder;
    
    @Test
    public void findReplace() throws Exception {
        when(mockVariableResolver.get("artifactId")).thenReturn("jdeb");
        when(mockVariableResolver.get("myProperty1")).thenReturn("custom1");
        when(mockVariableResolver.get("myProperty2")).thenReturn("custom2");
        
        InputStream inputStream = new ReaderInputStream(new StringReader("#!/bin/sh\ncat [[artifactId]][[myProperty1]] \necho '[[myProperty2]]'\n"));

        placeHolder = new PropertyPlaceHolderFile("", inputStream, mockVariableResolver);
        
        String actual = placeHolder.toString();
        assertThat(actual, is("#!/bin/sh\ncat jdebcustom1 \necho 'custom2'\n"));
    }
    
    @Test
    public void name() throws Exception {
        InputStream inputStream = new ReaderInputStream(new StringReader(""));
        placeHolder = new PropertyPlaceHolderFile("myName", inputStream, mockVariableResolver);
        assertThat(placeHolder.getName(), is("myName"));
    }
    
}
