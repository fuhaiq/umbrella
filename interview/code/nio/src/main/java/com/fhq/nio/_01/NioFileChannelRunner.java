package com.fhq.nio._01;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NioFileChannelRunner implements CommandLineRunner {

	private static Logger log = LoggerFactory.getLogger(NioFileChannelRunner.class);

	@Autowired
	private RandomAccessFile file;

	@Override
	public void run(String... args) throws Exception {
		try (var channel = file.getChannel()) {
			ByteBuffer buf = ByteBuffer.allocate(48);
			int byteRead = -1;
			while ((byteRead = channel.read(buf)) != -1) {
				log.info("读取 " + byteRead + "字节");
				buf.flip();
				while (buf.hasRemaining()) {
					System.out.print((char) buf.get());
				}
				buf.clear();
			}
		}

	}

}
