package com.fhq.nio.pipe;

import java.io.IOException;
import java.nio.channels.Pipe;
import com.google.inject.throwingproviders.CheckedProvider;

public interface PipeProvider extends CheckedProvider<Pipe> {
   Pipe get() throws IOException;
}
