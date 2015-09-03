package io.socket.emitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;

import com.alibaba.fastjson.JSONObject;

import io.socket.emitter.protocol.Packet;
import io.socket.emitter.protocol.PacketBinary;
import io.socket.emitter.protocol.PacketJson;
import io.socket.emitter.protocol.PacketText;
import redis.clients.jedis.Jedis;

public class Emitter {

	private static enum Flag {
		JSON, VOLATILE, BROADCAST;

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private static int EVENT = 2;
	
	private static int BINARY_EVENT = 5;

	private final String key = "socket.io#emitter";
	
	private ArrayList<String> rooms = new ArrayList<String>();
	
	private HashMap<String, Object> flags = new HashMap<String, Object>();
	
	private final Jedis jedis;

	public Emitter(Jedis jedis) {
		this.jedis = jedis;
	}

	public Emitter json() {
		return get(Flag.JSON);
	}

	public Emitter _volatile() {
		return get(Flag.VOLATILE);
	}

	public Emitter broadcast() {
		return get(Flag.BROADCAST);
	}

	private Emitter get(Flag flag) {
		this.flags.put(flag.toString(), true);
		return this;
	}

	public Emitter to(String room) {
		if (!rooms.contains(room)) {
			this.rooms.add(room);
		}
		return this;
	}
	
	public Emitter in(String room) {
		return this.to(room);
	}

	public Emitter of(String nsp) {
		this.flags.put("nsp", nsp);
		return this;
	}

	public Emitter emit(String event, String... data) throws IOException {
		PacketText packet = new PacketText();
		packet.setType(EVENT);

		packet.getData().add(event);
		for (int i = 0; i < data.length; i++) {
			packet.getData().add(data[i]);
		}

		return this.emit(packet);
	}

	public Emitter emit(String event, JSONObject data) throws IOException {
		PacketJson packet = new PacketJson();
		packet.setType(EVENT);

		packet.setData(data);
		packet.setEvent(event);

		return this.emit(packet);
	}

	public Emitter emit(byte[] b) throws IOException {
		PacketBinary packet = new PacketBinary();
		packet.setType(BINARY_EVENT);
		packet.setData(b);

		return this.emit(packet);
	}

	private Emitter emit(Packet packet) throws IOException {

		if (this.flags.containsKey("nsp")) {
			packet.setNsp((String) this.flags.get("nsp"));
			this.flags.remove("nsp");
		} else {
			packet.setNsp("/");
		}

		packet.setRooms(this.rooms);
		packet.setFlags(this.flags);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		MessagePack msgpack = new MessagePack();

		Packer packer = msgpack.createPacker(out);
		packer.write(packet);

		byte[] msg = out.toByteArray();
		
		jedis.publish(this.key.getBytes(Charset.forName("UTF-8")), msg);
		// reset state
		this.rooms = new ArrayList<String>();
		this.flags = new HashMap<String, Object>();

		return this;
	}
}