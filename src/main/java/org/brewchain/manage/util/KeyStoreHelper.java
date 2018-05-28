package org.brewchain.manage.util;

import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.manage.util.KeyStoreFile.CipherParams;
import org.brewchain.manage.util.KeyStoreFile.KeyStoreParams;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

@NActorProvider
@Instantiate(name = "KeyStore_Helper")
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Data
public class KeyStoreHelper implements ActorService {
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	public byte[] getPriv(String keyStoreText, String pwd) {
		// verify the pwd
		KeyStoreFile oKeyStoreFile = parse(keyStoreText);
		if (!oKeyStoreFile.getPwd().equals(encApi.hexEnc(encApi.sha3Encode(pwd.getBytes())))) {
			log.error("pwd is wrong");
		}
		// get cryptoKey
		byte[] cryptoKey = getCryptoKey(oKeyStoreFile, pwd);
		try {
			return decrypt(encApi.hexDec(oKeyStoreFile.getCipherText()), cryptoKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String generate(KeyPairs oKeyPairs,String pwd) {
		KeyStoreFile oKeyStoreFile = new KeyStoreFile();
		oKeyStoreFile.setKsType("");
		KeyStoreParams oKeyStoreParams = oKeyStoreFile.new KeyStoreParams();
		oKeyStoreParams.setSalt(IDGenerator.getInstance().generate());
		oKeyStoreParams.setC(16);
		oKeyStoreFile.setParams(oKeyStoreParams);
		oKeyStoreFile.setPwd(encApi.hexEnc(encApi.sha3Encode(pwd.getBytes())));
		byte[] cryptoKey = getCryptoKey(oKeyStoreFile, pwd);
		oKeyStoreFile.setCipher("aes-128-ctr");
		String iv = IDGenerator.getInstance().generate();
		CipherParams oCipherParams = oKeyStoreFile.new CipherParams();
		oCipherParams.setIv(iv);
		oKeyStoreFile.setCipherParams(oCipherParams);
		oKeyStoreFile.setCipherText(encApi.hexEnc(encrypt(oKeyPairs.getAddress(), cryptoKey, encApi.hexDec(iv))));

		return parseToJsonStr(oKeyStoreFile);
	}

	private byte[] encrypt(String srcData, byte[] seed, byte[] iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES");

			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(seed);
			kgen.init(128, secureRandom);
			SecretKey secretKey = kgen.generateKey();
			SecretKeySpec oSecretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

			cipher.init(Cipher.ENCRYPT_MODE, oSecretKeySpec);
			return cipher.doFinal(encApi.hexDec(srcData));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static byte[] decrypt(byte[] encData, byte[] seed) throws Exception {
		try {
			Cipher cipher = Cipher.getInstance("AES");// 创建密码器

			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(seed);
			kgen.init(128, secureRandom);
			SecretKey secretKey = kgen.generateKey();
			SecretKeySpec oSecretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

			cipher.init(Cipher.DECRYPT_MODE, oSecretKeySpec);// 初始化
			return cipher.doFinal(encData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private byte[] getCryptoKey(KeyStoreFile ksFile, String pwd) {
		if (ksFile.getKsType().equals("pbkdf2")) {
			SecretKeyFactory f;
			try {
				f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
				KeySpec ks = new PBEKeySpec(pwd.toCharArray(), ksFile.getParams().getSalt().getBytes(),
						ksFile.getParams().getC());
				SecretKey s = f.generateSecret(ks);
				Key k = new SecretKeySpec(s.getEncoded(), "AES");
				return k.getEncoded();
			} catch (NoSuchAlgorithmException e) {
				log.error(e.getMessage());
			} catch (InvalidKeySpecException e) {
				log.error(e.getMessage());
			}
		}
		log.error("the keystore type is wrong::" + ksFile.getKsType());
		return null;
	}

	public KeyStoreFile parse(String jsonText) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(jsonText, KeyStoreFile.class);
		} catch (JsonParseException e) {
			log.error("keystore json parse error::" + e.getMessage());
		} catch (JsonMappingException e) {
			log.error("keystore json can not mapping to keystore object::" + e.getMessage());
		} catch (IOException e) {
			log.error("can not read json str::" + e.getMessage());
		}
		return null;
	}

	public String parseToJsonStr(KeyStoreFile oKeyStoreFile) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(oKeyStoreFile);
		} catch (JsonProcessingException e) {
			log.error("generate keystore text error::" + e.getMessage());
		}
		return null;
	}
}
