package com.project.oauth.service.social;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
public class KakaoOauth implements SocialOauth {
	
	private final String KAKAO_SNS_BASE_URL = "https://kauth.kakao.com/oauth/authorize";
	private final String KAKAO_SNS_CLIENT_ID = "997ef5222d48c1992e7b7420272c77e2";
	private final String KAKAO_SNS_CALLBACK_URL = "http://localhost:8084/auth/kakao/callback";
	private final String KAKAO_SNS_TOKEN_BASE_URL = "https://kauth.kakao.com/oauth/token";
	private final String KAKAO_SNS_PROFILE_URL = "https://kapi.kakao.com/v2/user/me";
	
    @Override
    public String getOauthRedirectURL() {
    	Map<String, Object> params = new HashMap<>();
		params.put("response_type", "code");
		params.put("client_id", KAKAO_SNS_CLIENT_ID);
		params.put("redirect_uri", KAKAO_SNS_CALLBACK_URL);

		String parameterString = params.entrySet().stream().map(x -> x.getKey() + "=" + x.getValue())
				.collect(Collectors.joining("&"));

		return KAKAO_SNS_BASE_URL + "?" + parameterString;
    }

	@Override
	public String requestAccessToken(String code) {
		String access_Token = "";
		try {
			URL url = new URL(KAKAO_SNS_TOKEN_BASE_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setDoOutput(true);
		
			Map<String, Object> params = new HashMap<>();
			params.put("code", code);
			params.put("client_id", KAKAO_SNS_CLIENT_ID);
			params.put("redirect_uri", KAKAO_SNS_CALLBACK_URL);
			params.put("grant_type", "authorization_code");

			String parameterString = params.entrySet().stream().map(x -> x.getKey() + "=" + x.getValue())
					.collect(Collectors.joining("&"));

			BufferedOutputStream bous = new BufferedOutputStream(conn.getOutputStream());
			bous.write(parameterString.getBytes());
			bous.flush();
			bous.close();

			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			StringBuilder sb = new StringBuilder();
			String line;
			String result="";
			while ((line = br.readLine()) != null) {
				sb.append(line);
				result += line;
			}
			JsonParser parser = new JsonParser();
			JsonElement element = parser.parse(result);
			
			System.out.println("result : "+result);
			
			access_Token = element.getAsJsonObject().get("access_token").getAsString();
			if (conn.getResponseCode() == 200) {
				return access_Token;
			}
			return "????????? ????????? ?????? ?????? ??????";
		} catch (IOException e) {
			throw new IllegalArgumentException("??? ??? ?????? ????????? Access Token ?????? URL ????????? :: " + KAKAO_SNS_TOKEN_BASE_URL);
		}
			
	}

	@Override
	public String getUserInfo(String access_Token) {

		HashMap<String, Object> kakaoUserInfo = new HashMap<>();
		try {
			URL url = new URL(KAKAO_SNS_PROFILE_URL+"?access_token=");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization","Bearer " + access_Token);

			int responseCode = conn.getResponseCode();
			System.out.println("responseCode : "+responseCode);
			
			if(responseCode==200) {
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String line = "";
				String result = "";
				
				while ((line = br.readLine()) != null) {
					result += line;
				} 
				
				System.out.println("result : "+result);

				JsonParser parser = new JsonParser();
				JsonElement element = parser.parse(result);
				System.out.println("element : "+element);
				JsonObject account = element.getAsJsonObject().get("kakao_account").getAsJsonObject();
				System.out.println("account : "+account);
				String email = account.getAsJsonObject().get("email").getAsString();
				
				JsonObject properties = element.getAsJsonObject().get("properties").getAsJsonObject();
				String name = properties.getAsJsonObject().get("nickname").getAsString();
				String profileImg = properties.getAsJsonObject().get("thumbnail_image").getAsString();
				
				kakaoUserInfo.put("name", name);
				kakaoUserInfo.put("email", email);
				kakaoUserInfo.put("profileImg",profileImg);
				System.out.println("login Controller : " + kakaoUserInfo);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return kakaoUserInfo.toString();
	}
		
}