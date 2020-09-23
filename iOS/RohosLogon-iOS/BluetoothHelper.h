#import <CoreBluetooth/CoreBluetooth.h>

// BT service GUID
#define ROHOS_DATA_SERVICE @"46D5833A-997C-4854-9139-8C3510622ACF"

// BT characteristics GUID
#define ROHOS_DATA_CHARACTERISTICS @"55DA0C06-6BA7-4672-BA7C-43BE889AC2B3"

// BluetoothHelper is little class common for both central and peripheral BT roles
@protocol BluetoothCentralDelegate <NSObject>

// Event signals about bluetooth stack start and discovering the device
- (void)onCentralManagerInit: (NSError*)error;

// Device is sent ok down to Mac / Win
- (void)onDataSent: (NSError*)error;

@end

@protocol BluetoothPeripheralDelegate <NSObject>

- (void)onDataReceived: (NSData*)data;

@end

@interface BluetoothCentral: NSObject<CBCentralManagerDelegate, CBPeripheralDelegate>

- (id)init;
- (void)dealloc;
- (void)start;
- (void)stop;
- (void)sendData: (NSData*)data;
- (BOOL)isActive;

+ (BOOL)isAuthorized;

@property (retain) id<BluetoothCentralDelegate> delegate;

@end

@interface BluetoothPeripheral : NSObject<CBPeripheralManagerDelegate>

- (id)init;
- (void)dealloc;

- (void)start;
- (void)stop;

@property (retain) id<BluetoothPeripheralDelegate> delegate;

@end
