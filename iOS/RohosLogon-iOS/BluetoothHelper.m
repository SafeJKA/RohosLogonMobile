#import "BluetoothHelper.h"

@implementation BluetoothCentral
{
  CBCentralManager *mCentralManager;
  CBPeripheral *mDiscoveredPeripheral;
  CBCharacteristic* mDiscoveredCharacteristics;
  CBMutableCharacteristic *mTransferCharacteristic;
  id<BluetoothCentralDelegate> mDelegate;
}

@synthesize delegate = mDelegate;


- (id)init
{
  if (![super init])
    return nil;
  
  return self;
}

- (void)dealloc
{
  [self stop];
  //[super dealloc];
}

- (void)start
{
  if (mCentralManager)
    return;
  
  mCentralManager = [[CBCentralManager alloc] initWithDelegate: self queue: nil];
  // Further processing will happen in delegate methods
}

- (void)stop
{
  [self cleanup];
  mDiscoveredCharacteristics = nil;
  mDiscoveredPeripheral = nil;
  mCentralManager = nil;
}

- (void)sendData: (NSData*)data
{
  if (!mDiscoveredCharacteristics || !mDiscoveredPeripheral)
    return;
  
  @try
  {
    NSLog(@"Sending data to BT peripheral");
    [mDiscoveredPeripheral writeValue: data
                    forCharacteristic: mDiscoveredCharacteristics
                                 type: CBCharacteristicWriteWithResponse];
  }
  @catch(NSError* e)
  {
    [mDelegate onDataSent: e];
    return;
  }
  [mDelegate onDataSent: nil];
}

- (BOOL)isActive
{
  return mCentralManager != nil;
}

- (void)centralManagerDidUpdateState:(CBCentralManager *)central
{
  // Need working bluetooth
   if (central.state != CBCentralManagerStatePoweredOn)
    return;
  
  if (central.state == CBCentralManagerStatePoweredOn) {
    // Scan
    NSLog(@"Start scanning for peripherals");
    [mCentralManager scanForPeripheralsWithServices: @[[CBUUID UUIDWithString: ROHOS_DATA_SERVICE]]
                                            options: @{ CBCentralManagerScanOptionAllowDuplicatesKey : @YES }];
  }
}


- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)RSSI
{
  NSLog(@"Discovered %@ at %@", peripheral.name, RSSI);
  
  if (mDiscoveredPeripheral != peripheral) {
    // Save a local copy of the peripheral, so CoreBluetooth doesn't get rid of it
    mDiscoveredPeripheral = peripheral;
    
    // And connect
    NSLog(@"Connecting to peripheral %@", peripheral);
    [mCentralManager connectPeripheral: mDiscoveredPeripheral options: nil];
  }
}

- (void)centralManager: (CBCentralManager*)central didFailToConnectPeripheral: (CBPeripheral*)peripheral error: (NSError*)error
{
  NSLog(@"Failed to connect to %@", peripheral.name);
  
  // Tell about discover fail device
  [mDelegate onCentralManagerInit: error];
  
  // Clear state
  [self cleanup];
}

- (void)centralManager: (CBCentralManager*)central didConnectPeripheral:(CBPeripheral*)peripheral
{
  NSLog(@"Central manager connected to %@", peripheral.name);
  
  [mCentralManager stopScan];
  NSLog(@"Central manager stopped scanning");
  
  NSLog(@"Discover services for %@", peripheral.name);
  
  peripheral.delegate = self;
  
  [peripheral discoverServices:@[[CBUUID UUIDWithString: ROHOS_DATA_SERVICE]]];
}

- (void)peripheral: (CBPeripheral*)peripheral didDiscoverServices: (NSError*)error
{
  if (error)
  {
    NSLog(@"Central manager failed to discover services at peripheral %@", peripheral.name);
    [self cleanup];
    return;
  }
  
  NSLog(@"Discover characteristics in services");
  for (CBService *service in peripheral.services)
  {
    [peripheral discoverCharacteristics: @[[CBUUID UUIDWithString: ROHOS_DATA_CHARACTERISTICS]] forService: service];
  }
}

- (void)peripheral: (CBPeripheral*)peripheral didDiscoverCharacteristicsForService: (CBService*)service error: (NSError*)error
{
  if (error)
  {
    [mDelegate onCentralManagerInit: error];
    [self cleanup];
    return;
  }
  
  BOOL foundTarget = NO;
  
  NSLog(@"Discovered characteristics for service %@", [service.UUID UUIDString]);
  
  for (CBCharacteristic *characteristic in service.characteristics)
  {
    if ([characteristic.UUID isEqual: [CBUUID UUIDWithString: ROHOS_DATA_CHARACTERISTICS]])
    {
      mDiscoveredCharacteristics = characteristic;
      
      NSLog(@"Found target data characteristics %@", [characteristic.UUID UUIDString]);
      
      // Wait for data from device
      //[peripheral setNotifyValue: YES forCharacteristic: characteristic];
      
      foundTarget = YES;
      
      // Send notification "device found and connected"
      [mDelegate onCentralManagerInit: nil];
    }
  }
  
  if (!foundTarget)
  {
    [mDelegate onCentralManagerInit: [NSError errorWithDomain: @"Bluetooth" code: -1 userInfo: nil]];
  }
}

// Got data from remote device
- (void)peripheral: (CBPeripheral*)peripheral didUpdateValueForCharacteristic: (CBCharacteristic*)characteristic error: (NSError*)error
{
  if (error)
  {
    NSLog(@"Error");
    return;
  }
  
  NSString *stringFromData = [[NSString alloc] initWithData: characteristic.value encoding: NSUTF8StringEncoding];
  
  // Have we got everything we need?
  if ([stringFromData isEqualToString: @"EOM"])
  {
    [peripheral setNotifyValue: NO forCharacteristic: characteristic];
    
    [mCentralManager cancelPeripheralConnection: peripheral];
  }
  
  //[stringFromData release];
}

- (void)peripheral:(CBPeripheral *)peripheral didUpdateNotificationStateForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error
{
  if (![characteristic.UUID isEqual: [CBUUID UUIDWithString: ROHOS_DATA_CHARACTERISTICS]])
  {
    return;
  }
  
  if (characteristic.isNotifying) {
    NSLog(@"Notification began on %@", characteristic);
  } else {
    // Notification has stopped
    [mCentralManager cancelPeripheralConnection:peripheral];
  }
}

- (void)centralManager: (CBCentralManager*)central didDisconnectPeripheral: (CBPeripheral*)peripheral error: (NSError*)error
{
  mDiscoveredPeripheral = nil;
  mDiscoveredCharacteristics = nil;
  [self cleanup];
  return;
  
  // Restart scanning
  //[mCentralManager scanForPeripheralsWithServices: @[[CBUUID UUIDWithString: ROHOS_DATA_SERVICE]] options: @{ CBCentralManagerScanOptionAllowDuplicatesKey : @YES }];
}



- (void)cleanup
{
  // See if we are subscribed to a characteristic on the peripheral
  if (mDiscoveredPeripheral.services != nil)
  {
    for (CBService *service in mDiscoveredPeripheral.services)
    {
      if (service.characteristics != nil)
      {
        for (CBCharacteristic *characteristic in service.characteristics)
        {
          if ([characteristic.UUID isEqual:[CBUUID UUIDWithString: ROHOS_DATA_CHARACTERISTICS]])
          {
            if (characteristic.isNotifying)
            {
              [mDiscoveredPeripheral setNotifyValue: NO forCharacteristic: characteristic];
              return;
            }
          }
        }
      }
    }
  }
  
  if (mDiscoveredPeripheral)
    [mCentralManager cancelPeripheralConnection: mDiscoveredPeripheral];
}

@end

@implementation BluetoothPeripheral
{
  CBPeripheralManager *mPeripheralManager;
  CBMutableCharacteristic *mTransferCharacteristic;
  id<BluetoothPeripheralDelegate> mDelegate;
}

@synthesize delegate = mDelegate;

- (id)init
{
  if (![super init])
    return nil;
  
  return self;
}

- (void)dealloc
{
  [self stop];
  //[super dealloc];
}

- (void)start
{
  if (mPeripheralManager)
    return;
  
  mPeripheralManager = [[CBPeripheralManager alloc] initWithDelegate: self queue: nil];
  [mPeripheralManager startAdvertising: @{ CBAdvertisementDataServiceUUIDsKey : @[[CBUUID UUIDWithString: ROHOS_DATA_SERVICE]] }];
}

- (void)stop
{
  mPeripheralManager = nil;
  mTransferCharacteristic = nil;
}

- (void)peripheralManagerDidUpdateState:(CBPeripheralManager *)peripheral
{
  NSLog(@"CBPeripheralManagerDidUpdateState to %d", (int)peripheral.state);
  
  if (peripheral.state != CBPeripheralManagerStatePoweredOn)
    return;
  
  if (peripheral.state == CBPeripheralManagerStatePoweredOn)
  {
    mTransferCharacteristic = [[CBMutableCharacteristic alloc] initWithType: [CBUUID UUIDWithString: ROHOS_DATA_CHARACTERISTICS]
                                                                 properties: CBCharacteristicPropertyWriteWithoutResponse | CBCharacteristicPropertyWrite
                                                                      value: nil
                                                                permissions: CBAttributePermissionsWriteEncryptionRequired | CBAttributePermissionsWriteable];
    
    CBMutableService *transferService = [[CBMutableService alloc] initWithType: [CBUUID UUIDWithString: ROHOS_DATA_SERVICE] primary: YES];
    
    transferService.characteristics = @[mTransferCharacteristic];
    
    [mPeripheralManager addService: transferService];
  }
}

- (void)peripheralManager: (CBPeripheralManager*)peripheral central: (CBCentral*)central didSubscribeToCharacteristic: (CBCharacteristic*)characteristic
{

}

- (void)peripheralManager:(CBPeripheralManager *)peripheral didReceiveWriteRequests:(NSArray<CBATTRequest *> *)requests
{
  for (CBATTRequest* request in requests)
  {
    // As we provide only single characteristics - there is no checks for valid one
    NSLog(@"Request with data %@", [NSString stringWithUTF8String: [request.value bytes]]);
    
    // Send Success answer
    [mPeripheralManager respondToRequest: request withResult: CBATTErrorSuccess];
  
    // Tell about received data
    [mDelegate onDataReceived: request.value];
  }
}

@end
