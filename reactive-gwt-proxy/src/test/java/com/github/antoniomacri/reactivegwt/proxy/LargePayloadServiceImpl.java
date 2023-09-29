package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class LargePayloadServiceImpl extends RemoteServiceServlet implements
		LargePayloadService {
	public LargePayloadServiceImpl() {
	}

	@Override
	public int[] testLargeResponseArray() {
		int[] result = new int[ARRAY_SIZE];
		for (int i = 0; i < result.length; i++) {
			result[i] = i;
		}
		return result;
	}

	@Override
	public List<UserInfo> testLargeResponsePayload() {
		List<UserInfo> result = new ArrayList<UserInfo>();
		for (int i = 0; i < PAYLOAD_SIZE; i++) {
			UserInfo userInfo = new UserInfo();
			userInfo.setId("user_id_" + i);
			userInfo.setEmail("user" + i + "@example.com");
			result.add(userInfo);
		}
		return result;
	}
}
