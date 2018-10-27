package com.springboot.ionix.rutrest.controller;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.ionix.rutrest.model.PlainRutRequestModel;

@RestController
public class RutController {
	@Autowired
	RestTemplate restTemplate;
	
	//Receive post request (Json, read readme file from resources folder for use postman or similar tools)
	@PostMapping(path = "/postrut", consumes = "application/json", produces = "application/json")
	public String postPlainRut(@RequestBody PlainRutRequestModel rut) throws JsonParseException, IOException, JSONException {
		//starTime stores initial request time
		Long startTime = Instant.now().toEpochMilli();
		//retrieves plain rut from request (post)
		String plainRut = rut.getRut();
		String encryptedRut = null;
		//Logic for DES encryption for "rut" (test/search service uses "1-9" as input, encryption result should be "FyaSTkGi8So=")
		try {
			DESKeySpec keySpec = new DESKeySpec("ionix123456".getBytes("UTF8"));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key=keyFactory.generateSecret(keySpec);
			byte[] cleartext = plainRut.getBytes("UTF8");
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			encryptedRut = Base64.encodeBase64String(cipher.doFinal(cleartext));
			//System.out.println(encryptedRut);
		} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeySpecException
				| NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//hardcoded local URL emulating http://192.168.1.53:8089/test/search?rut= Ionix private test service JSON response
		String url = ("http://localhost:8080/test/search/"+encryptedRut);
		String jsonString = restTemplate.getForObject(url, String.class);
		//Mock service response to console
		System.out.println(jsonString);
		ObjectMapper mapper = new ObjectMapper();
	    JsonNode actualObj = mapper.readTree(jsonString);
	    
	    //System.out.println("responseCode: " + actualObj.get("responseCode"));
	    Long endTime = Instant.now().toEpochMilli();
        Long elapsedTime = endTime - startTime;
        //System.out.println("Elapsed time in milli seconds: "+elapsedTime);
        
        JSONObject obj = new org.json.JSONObject();
        JSONObject result = new org.json.JSONObject();
        if (actualObj.get("responseCode").toString().equals("0")) {
			result.put("registerCount", actualObj.size());
			obj.put("responseCode", "0");
			obj.put("description", "OK");
			obj.put("elapsedTime", elapsedTime);
			obj.put("result", result);
		} else {
			obj.put("error: ", "bad request");
			obj.put("message: ", "Please refer to readme file");
		}
		return obj.toString();
		
	}
	
	//Mock Ionix response
	@GetMapping(path = "/test/search/{rut}", produces = "application/json")
	public String getRutData (@PathVariable String rut){
		//Bad request
		String response = "{\"responseCode\" : \"500\", \"description\": \"Please refer to readme file\"}";
		//Success
		if (rut.equals("FyaSTkGi8So=")) {
			response = "{ \"responseCode\": 0, \"description\": \"OK\", \"result\": { \"items\":[ {\"name\":\"John\", \"detail\": {\"email\":\"jdoe@gmail.com\", \"phone_number\":\"+130256897875\"}}, {\"name\":\"Anna\", \"detail\": {\"email\":\"asmith@gmail.com\", \"phone_number\":\"+5689874521\"}}, {\"name\":\"Peter\",\"detail\": {\"email\":\"pjones@gmail.com\", \"phone_number\":\"+668978542365\"}} ] } }\r\n";
		}
		
		return response;
		
	}
	

	
	@Bean
	public RestTemplate rest() {
	return new RestTemplate();
	}
}
