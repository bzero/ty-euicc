package com.whty.smpp.esme.session;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whty.framework.netty.MsgSend;
import com.whty.smpp.esme.Deliver;
import com.whty.smpp.esme.Message;
import com.whty.smpp.esme.Session;
import com.whty.smpp.esme.constants.Configuration;
import com.whty.smpp.esme.constants.SmppConstants;
import com.whty.smpp.esme.exception.UnrecoverablePduException;
import com.whty.smpp.esme.message.BaseBind;
import com.whty.smpp.esme.message.BindReceiver;
import com.whty.smpp.esme.message.BindReceiverResp;
import com.whty.smpp.esme.message.DeliverSm;
import com.whty.smpp.esme.message.DeliverSmResp;
import com.whty.smpp.esme.message.EnquireLink;
import com.whty.smpp.esme.message.EnquireLinkResp;
import com.whty.smpp.esme.message.Pdu;
import com.whty.smpp.esme.message.PduRequest;
import com.whty.smpp.esme.message.PduResponse;
import com.whty.smpp.esme.message.Unbind;
import com.whty.smpp.esme.message.UnbindResp;
/**
 * 
 * @ClassName ReceiverSession
 * @author Administrator
 * @date 2017-3-10 下午1:40:10
 * @Description TODO(这里用一句话描述这个类的作用)
 */
public class ReceiverSession implements Session {

	private static final Logger logger = LoggerFactory.getLogger(ReceiverSession.class);
	private Configuration config;
	private ReceiverConnection connection;
	private Deliver deliver = new Deliver() {
		@Override
		public void handler(DeliverSm requestPdu) {
			
		}
	};;
	private String sessionId;
	private boolean authenticated;
	private Object lock = new Object();

	public ReceiverSession() {
		this(null, false, null, null);
	}
	
	public ReceiverSession(ReceiverConnection connection, boolean authenticated, Configuration config, Deliver deliver) {
		super();
		this.connection = connection;
		this.config = config;
		this.sessionId = java.util.UUID.randomUUID().toString();
		this.authenticated = authenticated;
		if (deliver != null) {
			this.deliver = deliver;
		}
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public boolean isAuthenticated() {
		return authenticated;
	}
	
	private void setAuthenticated(boolean value) {
        this.authenticated = value;
    }

	@Override
	public void submit(String content, String spNumber, String userNumber) {
		EnquireLink submit = new EnquireLink();
        send(submit);
	}

	@Override
	public void heartbeat() {
		if(isAuthenticated()) {
            EnquireLink activeTest=new EnquireLink();
            send(activeTest);
        }
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean authenticate() {
		BaseBind bindRequest = null;
		try {
			bindRequest = createBindRequest(config);
		} catch (UnrecoverablePduException e1) {
			e1.printStackTrace();
		}
		send(bindRequest);
		synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException ex){
                setAuthenticated(false);
            }
        }
		return isAuthenticated();
	}
	
	@SuppressWarnings("rawtypes")
	protected BaseBind createBindRequest(Configuration config)
			throws UnrecoverablePduException {
		BaseBind bind = new BindReceiver();
		bind.setSystemId(config.getSystemId());
		bind.setPassword(config.getPassword());
		bind.setSystemType(config.getSystemType());
		bind.setInterfaceVersion(config.getInterfaceVersion());
		bind.setAddressRange(config.getAddressRange());
		return bind;
	}

	/**
	 * @author Administrator
	 * @date 2017-2-9
	 * @throws IOException
	 * @Description TODO(这里用一句话描述这个方法的作用)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		//如果连接上了，那么就发送unbind消息
        if(isAuthenticated() ) {
            Unbind exit = new Unbind();
            send(exit);
            synchronized (lock) {
                try {
                    lock.wait(6000);
                } catch (InterruptedException ex){
                    setAuthenticated(false);
                }
            }
        }
		connection.close();
	}

	@Override
	public void send(Message message) {
		connection.send(message);
	}
	
	@Override
	public byte[] sendMsg(MsgSend msgSend, long timeoutMillis) {
		logger.info(">>>>>>>>>>>>>  msg send to smsc  >>>>>>>>>>>>>> src:{},dest:{},msg:{} ",msgSend.getSrc(),msgSend.getDest(),msgSend.getMsg());
		return null;
	}
	
	/**
	 * @author Administrator
	 * @date 2017-2-8
	 * @param message
	 * @throws IOException
	 * @Description TODO(接受到模拟器返回的消息后，针对不同的消息分别进行不同的处理)
	 * @see com.netgao.sms.protocol.Session#process(com.netgao.sms.protocol.Message)
	 */
	@Override
	public void process(Message message) throws IOException {
		Pdu pdu = (Pdu) message;
		PduRequest requestPdu = null;
		PduResponse responsePdu = null;
		int cmd = pdu.getCommandId();
		switch (cmd) {
			// 如果esme收到smsc发送的enquireLink消息:
		case SmppConstants.CMD_ID_ENQUIRE_LINK:
			requestPdu = (PduRequest) pdu;
			logger.info("pdu receiver enquire link message, received PDU: {}", requestPdu);
			EnquireLink req = (EnquireLink) pdu;
			EnquireLinkResp rsp = new EnquireLinkResp();
			rsp.setSequenceNumber(req.getSequenceNumber());
			send(rsp);
			break;
			
		case SmppConstants.CMD_ID_UNBIND:	
			// 如果esme收到smsc发送的unbind消息：
			requestPdu = (PduRequest) pdu;
			logger.info("pdu receiver unbind message, received PDU: {}", requestPdu);
			Unbind reqUnBind = (Unbind) pdu;
			UnbindResp rspUnBind = new UnbindResp();
			rspUnBind.setSequenceNumber(reqUnBind.getSequenceNumber());
			send(rspUnBind);
			// 断开连接
			connection.close();
			break;
			
			// 如果esme收到smsc主动发起的deliver消息
		case SmppConstants.CMD_ID_DELIVER_SM:
			// 返回给 smsc 的返回消息
			requestPdu = (PduRequest) pdu;
			logger.info("pdu receiver deliver message, received PDU: {}", requestPdu);
			DeliverSm request = (DeliverSm) pdu;
			DeliverSmResp response = new DeliverSmResp();
			response.setSequenceNumber(request.getSequenceNumber());
			send(response);
			deliver.handler(request);
			break;
			
			// 如果esme收到smsc主动发起的data消息
		case SmppConstants.CMD_ID_DATA_SM:
			requestPdu = (PduRequest) pdu;
			logger.info("pdu receiver data message, received PDU: {}", requestPdu);
			break;
			
		default:
			// 收到SMSC发给esme的返回消息，消息由esme主动发起
			if (pdu instanceof PduResponse) {
				responsePdu = (PduResponse) pdu;
				logger.info("pdu receiver response message, received PDU: {}", responsePdu);
				if (responsePdu instanceof BindReceiverResp) {
					// 如果是绑定返回消息
					synchronized (lock) {
			            if(responsePdu.getCommandStatus()==0){
			                setAuthenticated(true);
			                logger.info("smpp bind success host=" + connection.getHost() + ",port=" + connection.getPort() + ",clientId=" + connection.getSystemId());
			            } else {
			                setAuthenticated(false);
			                logger.error("smpp bind failure, host=" + connection.getHost() + ",port=" + connection.getPort() + ",clientId=" + connection.getSystemId() + ",status=" + responsePdu.getCommandStatus());
			            }
			            lock.notifyAll();
			        } 
				} else {
					
					logger.info("receiver other response message >>> {}", responsePdu);
				}
			} else {
				logger.info("<<<<<<<<<<<<<<<<<<   smsc send message to esme  <<<<<<<<<<<<<<<<<<<<");
				logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				logger.info("smsc send request msg to esme,message >>> {}", pdu);
				logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			}
			return;
		}
	}

}
