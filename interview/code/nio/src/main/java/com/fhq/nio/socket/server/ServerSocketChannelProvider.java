package com.fhq.nio.socket.server;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import com.google.inject.throwingproviders.CheckedProvider;

public interface ServerSocketChannelProvider extends CheckedProvider<ServerSocketChannel> {
   ServerSocketChannel get() throws IOException;
}
