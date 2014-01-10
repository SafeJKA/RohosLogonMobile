package com.rohos.logon1;

import java.nio.ByteBuffer;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Alex on 03.01.14.
  Rohos Loogn Key authentication record + encryption routine.

 * @author AlexShilon
 *
 */
class AuthRecord {
    /*
    Save/restore this fields in DB !
     */
    String qr_user;         // user name or rohos disk name
    String qr_secret_key;   // encryption key
    String qr_data;
    String qr_host_name;
    String qr_host_ip;
    int qr_host_port;
    String displayName;
    String settingsSet; // 'net.qrcode.'
    int someIntParamForFuture;
    int someIntParamForFuture2;
    String someStrParamForFuture;
    String someStrParamForFuture2;

    /*
    these fields are used in runtime
    Do not save/restore it in DB
     */
    String url; // initial URL from QR scan code...
    String plainHexAuthStr; // for test purposes

    /*
    encrypt (time + 0 + 7 + qr_data)
     */
    String getEncryptedDataString()
    {
        try {

            // for example 0x52B4284E - represent 2014 year.
            // "2014-1970 = 44years"
            // so 'int' should be OK to store at least (44 * 3) years of seconds
            int intSec = (int)(System.currentTimeMillis() / 1000);

            Random r = new Random();
            int randomInt = r.nextInt();
            //char loByteRand = (char)((randomInt << 28) >> 28);

            // 14 = int + int + char + char + datalen
            // '01' means - protocol version
            //
            byte[] byteData = ByteBuffer.allocate(14 + qr_data.length() / 2)
                    .putInt(randomInt) // Random, adding entropy to first 16 bytes of data block
                    .putInt(intSec) // OTP parameter
                    .putChar( '0')  // protocol signature '01'
                    .putChar( '1')
                    .putChar((char)(qr_data.length() / 2)) // data len
                    .put(HexEncoding.decode(qr_data)) // data itself
                    .array();

            plainHexAuthStr = HexEncoding.encode(byteData).substring(0, 30);

            // create key - 16 bytes only for AES128
            String keyStr = qr_secret_key.substring(0, 32);

            if (keyStr.length() > 32) // AES128 encryption key should be 16 bytes only.
                keyStr = qr_secret_key.substring(0, 32);

            SecretKeySpec secretKey = new SecretKeySpec(HexEncoding.decode(keyStr), "AES");

            // AES128 encryption
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedByteData = cipher.doFinal(byteData);

            return HexEncoding.encode(encryptedByteData);

        } catch (Exception e) {
            return "ERR."+e.toString();
        }

    }
}
