#import "AuthRecord.h"
#import <Foundation/Foundation.h>
#import <CommonCrypto/CommonCryptor.h>

@implementation AuthRecord

@synthesize userName;
@synthesize hostName;
@synthesize secretKey;
@synthesize hostIP;
@synthesize hostPort;
@synthesize authSignalString;
@synthesize serverReplyStr;

- (id) init
{
    self = [super init];
    if (!self)
        return nil;
    
    self.userName = @"";
    
    return self;
}
    
- (BOOL) isEmpty
{
    if (self.userName == nil || [self.userName length] == 0)
        return YES;
    return NO;
}
    
- (NSData*) AES128Encrypt: (NSData*) data
{
    // 'key' should be 32 bytes for AES256, will be null-padded otherwise
    char keyPtr[kCCKeySizeAES256+4]; // room for terminator (unused)
    bzero(keyPtr, sizeof(keyPtr)); // fill with zeroes (for padding)
    if ([secretKey length] > 64)
        return nil;
        
    NSData *key = [self dataByIntepretingHexString: secretKey];
    memcpy(keyPtr, [key bytes], [key length]);
    // fetch key data
    //[key getCString:keyPtr maxLength:sizeof(keyPtr) encoding:NSUTF8StringEncoding];
    
    NSUInteger dataLength = [data length]+2;
   
    //See the doc: For block ciphers, the output size will always be less than or
    //equal to the input size plus the size of one block.
    //That's why we need to add the size of one block here
    size_t bufferSize = dataLength + kCCBlockSizeAES128;
    void *buffer = malloc(bufferSize);
    
    size_t numBytesEncrypted = 0;
    CCCryptorStatus cryptStatus = CCCrypt(kCCEncrypt, kCCAlgorithmAES, /*kCCOptionPKCS7Padding |*/ kCCOptionECBMode,
                                          keyPtr, kCCKeySizeAES128,
                                          NULL /* initialization vector (optional) */,
                                          [data bytes], dataLength, /* input */
                                          buffer, bufferSize, /* output */
                                          &numBytesEncrypted);
    if (cryptStatus == kCCSuccess) {
        //the returned NSData takes ownership of the buffer and will free it on deallocation
        return [NSData dataWithBytesNoCopy: buffer length: numBytesEncrypted];
    }
    
    free(buffer); //free the buffer;
    return nil;
}
    

// HexToData
- (NSData*) dataByIntepretingHexString: (NSString*) hexString
{
    char const *chars = hexString.UTF8String;
    NSUInteger charCount = strlen(chars);
    if (charCount % 2 != 0) {
        return nil;
    }
    NSUInteger byteCount = charCount / 2;
    uint8_t *bytes = (uint8_t*)malloc(byteCount);
    for (int i = 0; i < byteCount; ++i) {
        unsigned int value;
        sscanf(chars + i * 2, "%2x", &value);
        bytes[i] = value;
    }
    return [NSData dataWithBytesNoCopy:bytes length:byteCount freeWhenDone:YES];
}
    
// DataToHex
- (NSString*) hexadecimalString: (NSData*) data
{
    /* Returns hexadecimal string of NSData. Empty string if data is empty.   */
    const unsigned char *dataBuffer = (const unsigned char *)[data bytes];
    
    if (!dataBuffer)
        return [NSString string];
    
    NSUInteger          dataLength  = [data length];
    NSMutableString     *hexString  = [NSMutableString stringWithCapacity:(dataLength * 2)];
    
    for (int i = 0; i < dataLength; ++i)
        [hexString appendString: [NSString stringWithFormat: @"%02lx", (unsigned long)dataBuffer[i]]];
    
    return [NSString stringWithString: hexString];
}

// Ccreates unique Authentication Signal based on TimeStamp, Random and authentication data
- (NSString*) getEncryptedDataString
{
    //// XXXXXXXX TTTTTTTT '0001' '0002'
    ////
    uint32_t intSeconds = (uint32_t)([[NSDate date] timeIntervalSince1970]);
    uint32_t intRand = arc4random();
    uint8_t shortP = 0;
    uint8_t shortP1 = '0';
    uint8_t shortP2 = '1';
    
    // lets convert for Android compatibility
    uint32_t secondsAsABigEndianNumber = CFSwapInt32HostToBig(intSeconds);
   
    NSData* dataBytes = [self dataByIntepretingHexString: self.data];
    uint8_t dataLen = [dataBytes length];
    
    NSMutableData *payload = [[NSMutableData alloc] init];
    [payload appendBytes: &intRand length: sizeof(uint32_t)];
    [payload appendBytes: &secondsAsABigEndianNumber length: sizeof(uint32_t)];
    [payload appendBytes: &shortP length: sizeof(uint8_t)]; // put zero
    [payload appendBytes: &shortP1 length: sizeof(uint8_t)]; // put value . 0030
    [payload appendBytes: &shortP length: sizeof(uint8_t)];
    [payload appendBytes: &shortP2 length: sizeof(uint8_t)];
    [payload appendBytes: &shortP length: sizeof(uint8_t)];
    [payload appendBytes: &dataLen length: sizeof(uint8_t)];
    [payload appendBytes: [dataBytes bytes] length: dataLen];
    
    Byte buff1[200];
    memcpy(buff1, [payload bytes], [payload length]);
    
    
    //NSData *rawData = [NSData dataWithBytes: [payload bytes] length: [payload length] ];
    NSData *cipher = [self AES128Encrypt: payload];
    if (cipher!=nil)
    {
        // Formatting Authentication signal : HOSTNAME.USERNAME.HEXDATA
        NSString* enc_str = [self hexadecimalString: cipher];
        authSignalString = [NSString stringWithFormat: @"%@.%@.%@", userName, hostName, enc_str];
        return authSignalString;
    }
    
    return @""; //oops!
}
@end

