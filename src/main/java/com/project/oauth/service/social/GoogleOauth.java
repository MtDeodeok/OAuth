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
import com.google.gson.JsonParser;

@Component
public class GoogleOauth implements SocialOauth {
	/* 
	 * @Value("${sns.google.url}") private String GOOGLE_SNS_BASE_URL; 
	 * @Value("${sns.google.client.id}") private String GOOGLE_SNS_CLIENT_ID;
	 * @Value("${sns.google.callback.url}") private String GOOGLE_SNS_CALLBACK_URL;
	 * @Value("${sns.google.client.secret}") private String GOOGLE_SNS_CLIENT_SECRET;
	 * 
	 * 위의 Value를 사용하기 위해선 application.properties 설정
	 * sns.google.url=https://accounts.google.com/o/oauth2/v2/auth
	 * sns.google.client.id=432841929887-eu7njbndsviqfu6o5466o2ju7mu1br7e.apps.googleusercontent.com
	 * sns.google.client.secret=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
	 * sns.google.callback.url=http://localhost:8084/auth/google/callback
	 */

	private final String GOOGLE_SNS_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
	private final String GOOGLE_SNS_CLIENT_ID = "432841929887-eu7njbndsviqfu6o5466o2ju7mu1br7e.apps.googleusercontent.com";
	private final String GOOGLE_SNS_CALLBACK_URL = "http://localhost:8084/auth/google/callback";
	private final String GOOGLE_SNS_CLIENT_SECRET = "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";
	private final String GOOGLE_SNS_TOKEN_BASE_URL = "https://oauth2.googleapis.com/token";
	private final String GOOGLE_SNS_PROFILE_URL = "https://www.googleapis.com/userinfo/v2/me";
	
	@Override
	public String getOauthRedirectURL() {
		Map<String, Object> params = new HashMap<>();
		params.put("scope", "profile email");
		params.put("response_type", "code");
		params.put("client_id", GOOGLE_SNS_CLIENT_ID);
		params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL);

		String parameterString = params.entrySet().stream().map(x -> x.getKey() + "=" + x.getValue())
				.collect(Collectors.joining("&"));

		return GOOGLE_SNS_BASE_URL + "?" + parameterString;
	}

	// Spring Boot RestTemplate 활용한 방식
	/*
	 * @Override public String requestAccessToken(String code) { RestTemplate
	 * restTemplate = new RestTemplate();
	 * 
	 * Map<String, Object> params = new HashMap<>(); params.put("code", code);
	 * params.put("client_id", GOOGLE_SNS_CLIENT_ID); params.put("client_secret",
	 * GOOGLE_SNS_CLIENT_SECRET); params.put("redirect_uri",
	 * GOOGLE_SNS_CALLBACK_URL); params.put("grant_type", "authorization_code");
	 * 
	 * ResponseEntity<String> responseEntity =
	 * restTemplate.postForEntity(GOOGLE_SNS_TOKEN_BASE_URL, params, String.class);
	 * 
	 * if (responseEntity.getStatusCode() == HttpStatus.OK) { return
	 * responseEntity.getBody(); } return "구글 로그인 요청 처리 실패"; }
	 */
	
	//java 표준 URL 통신 방식(Nomal HTTP Request)
	@Override
	public String requestAccessToken(String code) {
		String access_Token = "";
		try {
			URL url = new URL(GOOGLE_SNS_TOKEN_BASE_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setDoOutput(true);

			Map<String, Object> params = new HashMap<>();
			params.put("code", code);
			params.put("client_id", GOOGLE_SNS_CLIENT_ID);
			params.put("client_secret", GOOGLE_SNS_CLIENT_SECRET);
			params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL);
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
			return "구글 로그인 요청 처리 실패";
		} catch (IOException e) {
			throw new IllegalArgumentException("알 수 없는 로그인 Access Token 요청 URL 입니다 :: " + GOOGLE_SNS_TOKEN_BASE_URL);
		}
	}

	@Override
	public String getUserInfo(String access_Token) {

		//요청하는 클라이언트마다 가진 정보가 다를 수 있기에 HashMap타입으로 선언
		HashMap<String, Object> googleUserInfo = new HashMap<>();
		try {
			URL url = new URL(GOOGLE_SNS_PROFILE_URL+"?access_token=");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			// 요청에 필요한 Header에 포함될 내용
			conn.setRequestProperty("Authorization","Bearer" + access_Token);

			int responseCode = conn.getResponseCode();
			System.out.println("responseCode : "+responseCode);
			
			if(responseCode==200) {
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String line = "";
				String result = "";
				
				while ((line = br.readLine()) != null) {
					result += line;
				} 
				JsonParser parser = new JsonParser();
				
				System.out.println("result : "+result);
				
				JsonElement element = parser.parse(result);
				String name = element.getAsJsonObject().get("name").getAsString();
				String email = element.getAsJsonObject().get("email").getAsString();
				String profileImg = element.getAsJsonObject().get("picture").getAsString();
				
				googleUserInfo.put("name", name);
				googleUserInfo.put("email", email);
				googleUserInfo.put("profileImg",profileImg);
				System.out.println("login Controller : " + googleUserInfo);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return googleUserInfo.toString();
	}
}