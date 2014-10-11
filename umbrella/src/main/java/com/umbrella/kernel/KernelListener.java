package com.umbrella.kernel;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.PacketArrivedEvent;
import com.wolfram.jlink.PacketListener;

public class KernelListener implements PacketListener{
	
	@Override
	public boolean packetArrived(PacketArrivedEvent evt) throws MathLinkException {
		KernelLink ml = (KernelLink) evt.getSource();
		JSONObject result = new JSONObject();
		switch (evt.getPktType()) {
		case MathLink.RETURNTEXTPKT:
			String _return = ml.getString();
			if(Kernel.ABORT.equals(_return)){
				result.put("type", "abort");
			} else {
				result.put("type", "return");
				result.put("data", _return);
			}
			ml.result().add(result);
			break;
		case MathLink.MESSAGEPKT:
			result.put("type", "error");
			ml.result().add(result);
			ml.getString();
			break;
		case MathLink.TEXTPKT: {
			String txt = ml.getString();
			JSONArray results = ml.result();
			if(results.size() > 0) {
				JSONObject last = results.getJSONObject(results.size() - 1);
				if(last.getString("type").equals("error")  && last.get("data") == null) {
					last.put("data", txt);
					break;
				}
			}
			result.put("type", "text");
			result.put("data", txt);
			ml.result().add(result);
			break;
		}
		case MathLink.DISPLAYPKT: {
			byte[] imageData = ml.getByteString((byte) 0);
			result.put("type", "image");
			result.put("data", imageData);
			ml.result().add(result);
			break;
		}
		default:
			// e.g. MessagePacket
			break;
		}
		return true;
	}
}
