package com.whty.efs.packets.message.request;

import com.whty.efs.packets.message.MsgBody;
import com.whty.efs.packets.message.MsgType;



@MsgType("receiveNotificationBySms")
public class ReceiveNotificationBySmsReqBody extends EuiccReqBody implements MsgBody{
	private String tpud; 

	public String getTpud() {
		return tpud;
	}

	public void setTpud(String tpud) {
		this.tpud = tpud;
	}
	
//	private String isdPAid;
//	private int sequenceNum;
//	private String notificationType;
	
//	public String getIsdPAid() {
//		return isdPAid;
//	}
//
//	public void setIsdPAid(String isdPAid) {
//		this.isdPAid = isdPAid;
//	}
//	
//	public int getSequenceNum() {
//		return sequenceNum;
//	}
//
//	public void setSequenceNum(int sequenceNum) {
//		this.sequenceNum = sequenceNum;
//	}
//	
//	public String getNotificationType() {
//		return notificationType;
//	}
//
//	public void setNotificationType(String notificationType) {
//		this.notificationType = notificationType;
//	}
}
