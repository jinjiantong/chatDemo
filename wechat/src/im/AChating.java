/**
 * wechatdonal
 */
package im;

import im.model.IMMessage;
import im.model.Notice;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONObject;

import qiniu.auth.JSONObjectRet;
import qiniu.io.IO;
import qiniu.io.PutExtra;
import qiniu.utils.Config;
import qiniu.utils.Mac;
import qiniu.utils.PutPolicy;

import com.google.gson.Gson;

import bean.JsonMessage;
import swipeback.SwipeBackActivity;
import tools.DateUtil;
import tools.Logger;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import config.AppActivity;
import config.CommonValue;
import config.MessageManager;
import config.NoticeManager;
import config.XmppConnectionManager;

/**
 * wechat
 *
 * @author donal
 *
 */
public abstract class AChating extends AppActivity{
	private Chat chat = null;
	protected List<IMMessage> message_pool = new ArrayList<IMMessage>();
	protected String to;
	private static int pageSize = 10;
	private List<Notice> noticeList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		to = getIntent().getStringExtra("to");
		message_pool = MessageManager.getInstance(context)
				.getMessageListByFrom(to, 1, pageSize);
		if (null != message_pool && message_pool.size() > 0)
			Collections.sort(message_pool);
		
		NoticeManager.getInstance(context).updateStatusByFrom(to, Notice.READ);
		if (to == null)
			return;
		chat = XmppConnectionManager.getInstance().getConnection()
				.getChatManager().createChat(to, null);
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(receiver);
		super.onPause();
	}

	@Override
	protected void onResume() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(CommonValue.NEW_MESSAGE_ACTION);
		registerReceiver(receiver, filter);
		super.onResume();

	}
     //新消息传来 接收通知	
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Notice notice = (Notice) intent.getSerializableExtra("notice");
			if (CommonValue.NEW_MESSAGE_ACTION.equals(action)) {
				IMMessage message = intent.getParcelableExtra(IMMessage.IMMESSAGE_KEY);
				String jid=message.getFromSubJid();
				if (!message.getFromSubJid().equals(to)) {
					return;
				}
				message_pool.add(message);
				receiveNewMessage(message);
				refreshMessage(message_pool);
			}
		}

	};
	
	protected abstract void receiveNotice(Notice notice);
	
	protected abstract void receiveNewMessage(IMMessage message);

	protected abstract void refreshMessage(List<IMMessage> messages);
	
	protected List<IMMessage> getMessages() {
		return message_pool;
	}
	
	protected void sendMessage(String messageContent) throws Exception {
		JsonMessage msg = new JsonMessage();
		msg.file = "";
		msg.messageType = CommonValue.kWCMessageTypePlain;
		msg.text = messageContent;
		Gson gson = new Gson();
		String json = gson.toJson(msg);
		
		String time = (System.currentTimeMillis()/1000)+"";
		Message message = new Message();
		message.setProperty(IMMessage.KEY_TIME, time);
		message.setBody(json);
		chat.sendMessage(message);

		IMMessage newMessage = new IMMessage();
		newMessage.setMsgType(1);
		newMessage.setFromSubJid(chat.getParticipant());
		newMessage.setContent(json);
		newMessage.setTime(time);
		message_pool.add(newMessage);
		MessageManager.getInstance(context).saveIMMessage(newMessage);
		refreshMessage(message_pool);
		sendNoti(to);
	}
	
	protected void sendMediaMessage(IMMessage newMessage) throws Exception {
//		JsonMessage msg = new JsonMessage();
//		msg.file = url;
//		msg.messageType = CommonValue.kWCMessageTypeImage;
//		msg.text = "[图片]";
//		Gson gson = new Gson();
//		String json = gson.toJson(msg);
//		
		String time = (System.currentTimeMillis()/1000)+"";
		Message message = new Message();
		message.setProperty(IMMessage.KEY_TIME, time);
		message.setBody(newMessage.getContent());
		chat.sendMessage(message);
//
//		IMMessage newMessage = new IMMessage();
		newMessage.setMsgType(1);
		newMessage.setFromSubJid(chat.getParticipant());
		newMessage.setTime(time);
		newMessage.setType(0); 
//		message_pool.add(newMessage);
		MessageManager.getInstance(context).saveIMMessage(newMessage);
		refreshMessage(message_pool);
		sendNoti(to);
	}
	
	protected Boolean addNewMessage() {
		List<IMMessage> newMsgList = MessageManager.getInstance(context)
				.getMessageListByFrom(to, message_pool.size(), pageSize);
		if (newMsgList != null && newMsgList.size() > 0) {
			message_pool.addAll(newMsgList);
			Collections.sort(message_pool);
			return true;
		}
		return false;
	}
	
	protected int addNewMessage(int currentPage) {
		List<IMMessage> newMsgList = MessageManager.getInstance(context)
				.getMessageListByFrom(to, currentPage, pageSize);
		if (newMsgList != null && newMsgList.size() > 0) {
			message_pool.addAll(newMsgList);
			Collections.sort(message_pool);
			return newMsgList.size();
		}
		return 0;
	}

	protected void resh() {
		refreshMessage(message_pool);
	}
	
	class MsgListener implements MessageListener {

		@Override
		public void processMessage(Chat arg0, Message message) {
			
		}
	}
	
	protected void uploadPhotoToQiniu(String filePath) {
		
		JsonMessage msg = new JsonMessage();
		msg.file = filePath;
		msg.messageType = CommonValue.kWCMessageTypeImage;
		msg.text = "[图片]";
		Gson gson = new Gson();
		String json = gson.toJson(msg);
		
		String time = (System.currentTimeMillis()/1000)+"";

		IMMessage newMessage = new IMMessage();
		newMessage.setMsgType(1);
		newMessage.setFromSubJid(chat.getParticipant());
		newMessage.setContent(json);
		newMessage.setTime(time);
		newMessage.setType(CommonValue.kWCMessageStatusWait);
		message_pool.add(newMessage);
		refreshMessage(message_pool);
	}
	
	protected void uploadVoiceToQiniu(String filePath) {
		
		JsonMessage msg = new JsonMessage();
		msg.file = filePath;
		msg.messageType = CommonValue.kWCMessageTypeVoice;
		msg.text = "[语音]";
		Gson gson = new Gson();
		String json = gson.toJson(msg);
		
		String time = (System.currentTimeMillis()/1000)+"";

		IMMessage newMessage = new IMMessage();
		newMessage.setMsgType(1);
		newMessage.setFromSubJid(chat.getParticipant());
		newMessage.setContent(json);
		newMessage.setTime(time);
		newMessage.setType(CommonValue.kWCMessageStatusWait);
		message_pool.add(newMessage);
		refreshMessage(message_pool);
	}
	
	private void sendNoti(String to) {
		Intent intent = new Intent(CommonValue.SEND_MESSAGE_ACTION);
		intent.putExtra("to", to);
		sendBroadcast(intent);
	}
}
