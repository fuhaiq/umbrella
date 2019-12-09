package com.fhq.nio.pipe.select;

import java.nio.channels.Pipe.SinkChannel;

public interface RunnableFactory {
   Runnable create(SinkChannel sink, int number);
}
