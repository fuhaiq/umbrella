package com.fhq.nio.pipe.select;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.List;
import com.google.inject.throwingproviders.CheckedProvider;

public interface PipesProvider extends CheckedProvider<List<Pipe>>{

   List<Pipe> get() throws IOException;

}
