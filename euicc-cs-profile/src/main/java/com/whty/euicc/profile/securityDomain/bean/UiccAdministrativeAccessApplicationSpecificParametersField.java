package com.whty.euicc.profile.securityDomain.bean;

import com.whty.euicc.profile.parent.JavaBean;
/**
 * SecurityDomain子元素
 * 结构类型：简单类型
 * @author Administrator
 *
 */
public class UiccAdministrativeAccessApplicationSpecificParametersField extends
		JavaBean {
	private String tag = "82";

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	@Override
	public String toString() {
		return "UiccAdministrativeAccessApplicationSpecificParametersField [length="
				+ length + ", tag=" + tag + ", value=" + value + "]";
	}

}
