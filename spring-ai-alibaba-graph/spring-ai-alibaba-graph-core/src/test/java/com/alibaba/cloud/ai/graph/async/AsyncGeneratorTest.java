package com.alibaba.cloud.ai.graph.async;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsyncGeneratorTest {
    static class AsyncGeneratorWithResult implements AsyncGenerator<String> {
        final List<String> elements;
        int index = -1;

        AsyncGeneratorWithResult( List<String> elements ) {
            this.elements = elements;
        }

        @Override
        public Data<String> next() {
            ++index;
            if( index >= elements.size() ) {
                index = -1;
                return Data.done( elements.size() );
            }
            return Data.of( elements.get( index ) );
        }

    }
    @Test
    public void asyncGeneratorWithResultTest() throws Exception {
        var generator = new AsyncGeneratorWithResult(
                List.of( "e1", "e2", "e3", "n1", "n2", "n3", "n4", "n5", "e4", "e5", "e6", "e7"));

        AsyncGenerator<String> it = new AsyncGenerator.WithResult<>(generator);

        it.stream().forEach( System.out::print );
        System.out.println();

        assertTrue( AsyncGenerator.resultValue(it).isPresent() );
        assertEquals( 12, AsyncGenerator.resultValue(it).get() );

        for( var element : it ) {
            System.out.print( element );
        }

        assertTrue( AsyncGenerator.resultValue(it).isPresent() );
        assertEquals( 12, AsyncGenerator.resultValue(it).get() );
    }
}
