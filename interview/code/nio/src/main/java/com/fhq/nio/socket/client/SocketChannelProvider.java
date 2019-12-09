package com.fhq.nio.socket.client;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import com.google.inject.throwingproviders.CheckedProvider;

public interface SocketChannelProvider extends CheckedProvider<SocketChannel>{
   SocketChannel get() throws IOException;
}
