package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.state.OverAllStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OverAllStateSerializerTest {

    @Mock
    private AgentStateFactory<OverAllState> stateFactory;

    @Mock
    private ObjectOutput objectOutput;

    @Mock
    private ObjectInput objectInput;

    @InjectMocks
    private OverAllStateSerializer serializer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = IllegalArgumentException.class)
    public void write_ObjectIsNull_ShouldThrowIllegalArgumentException() throws IOException {
        serializer.write(null, objectOutput);
    }

    @Test(expected = IllegalArgumentException.class)
    public void write_OutputIsNull_ShouldThrowIllegalArgumentException() throws IOException {
        OverAllState state = mock(OverAllState.class);
        serializer.write(state, null);
    }

    @Test
    public void write_ValidInputs_ShouldWriteObject() throws IOException {
        OverAllState state = mock(OverAllState.class);
        serializer.write(state, objectOutput);
        verify(objectOutput).writeObject(state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void read_InputIsNull_ShouldThrowIllegalArgumentException() throws IOException, ClassNotFoundException {
        serializer.read(null);
    }

    @Test(expected = IllegalStateException.class)
    public void read_DeserializedObjectIsNull_ShouldThrowIllegalStateException() throws IOException, ClassNotFoundException {
        when(objectInput.readObject()).thenReturn(null);
        serializer.read(objectInput);
    }

    @Test
    public void read_ValidInputs_ShouldReturnOverAllState() throws IOException, ClassNotFoundException {
        OverAllState state = mock(OverAllState.class);
        when(objectInput.readObject()).thenReturn(state);
        OverAllState result = serializer.read(objectInput);
        assertEquals(state, result);
    }

    @Test(expected = IOException.class)
    public void read_ClassNotFoundException_ShouldThrowIOException() throws IOException, ClassNotFoundException {
        when(objectInput.readObject()).thenThrow(new ClassNotFoundException());
        OverAllState read = serializer.read(objectInput);
        System.out.println("read = " + read);
    }

    @Test(expected = IOException.class)
    public void read_IOException_ShouldThrowIOException() throws IOException, ClassNotFoundException {
        when(objectInput.readObject()).thenThrow(new IOException());
        serializer.read(objectInput);
    }
}
