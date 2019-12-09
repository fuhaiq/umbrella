package com.fhq.nio.socket.server;

import java.io.IOException;
import java.nio.channels.Selector;
import com.google.inject.throwingproviders.CheckedProvider;

public interface SelectorProvider extends CheckedProvider<Selector> {
   Selector get() throws IOException;
}