#ifndef __AUTH_RECORD_H
#define __AUTH_RECORD_H

/*
 Authentication Record structure with encryption routine
 */
@interface AuthRecord: NSObject

@property (retain) NSString* userName;
@property (retain) NSString* hostName;
@property (retain) NSString* secretKey;
@property (retain) NSString* data;
@property (retain) NSString* hostIP;
@property int hostPort;
@property (retain) NSString* authSignalString;
@property (retain) NSString* serverReplyStr;

- (id) init;
- (BOOL) isEmpty;
- (NSData*) AES128Encrypt: (NSData*) data;
- (NSData*) dataByIntepretingHexString: (NSString*) hexString;
- (NSString*) hexadecimalString: (NSData*) data;
- (NSString*) getEncryptedDataString;






@end

#endif

