//
//  MainViewController.m
//
//
//  Created by Alexandr Silonosov on 02-1-2014.
//  Copyright (c) 2014 Tesline-Service SRL. All rights reserved.
//  Rohos Logon Key client for iOS
//  rohos.com
//

#import "MainViewController.h"
#import <CommonCrypto/CommonCryptor.h>

#import "MQTTClient.h"
#import "MQTTCFSocketTransport.h"

#import "AuthRecord.h"
#import "BarcodeScanner.h"

#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include <arpa/inet.h>

AuthRecord* ar = nil;

#define AUTHREC_LIST_KEY @"listOfAuthRecords"

#define USER_NAME_KEY       @"userName"
#define HOST_NAME_KEY       @"hostName"
#define SECRET_NAME_KEY     @"secret"
#define DATA_NAME_KEY       @"data"
#define PORT_NAME_KEY       @"port"
#define IP_NAME_KEY         @"ip"


@interface MainViewController ()

@end

@implementation MainViewController
{
    BluetoothCentral* mBluetooth;
    NSData* mBluetoothSendData;
    MQTTSession* mQttSession;
}

@synthesize resultsView;
@synthesize tableView;
@synthesize recordsView;
@synthesize bigLogoView;
@synthesize resultsToDisplay;
@synthesize errorLabel;


- (void)viewDidLoad
{
    [super viewDidLoad];
	
    ar = [[AuthRecord alloc] init];
    
    // Do any additional setup after loading the view, typically from a nib.
    //self.qrReader = [[NSMutableSet alloc] init];
    //QRCodeReader *qrcodeReader = [[QRCodeReader alloc] init];
    //[self.qrReader addObject: qrcodeReader];
    
    // adding Single Tap recognozer to Label and BigLogo images to open "rohos.com... "
    
    UITapGestureRecognizer *singleTap = [[UITapGestureRecognizer alloc] initWithTarget: self
                                                                                action: @selector(learnMoreSingleTapRecognized:)];
    singleTap.numberOfTapsRequired = 1;
    [resultsView addGestureRecognizer: singleTap];
    
    singleTap = [[UITapGestureRecognizer alloc] initWithTarget: self
                                                        action: @selector(learnMoreSingleTapRecognized:)];
    singleTap.numberOfTapsRequired = 1;
    [bigLogoView addGestureRecognizer: singleTap];
    
    // the mutalbe array of all debts
    NSUserDefaults * defaults = [NSUserDefaults standardUserDefaults];
    mRecords = [[defaults objectForKey: AUTHREC_LIST_KEY] mutableCopy];
    
    if (mRecords == nil)
        mRecords = [[NSMutableArray alloc] init];

    
    if ([mRecords count] == 0)
        recordsView.hidden = YES;
    else
        [self refreshAuthRecordsList];
  
    mBluetooth = [[BluetoothCentral alloc] init];
    mBluetooth.delegate = self;
}

- (void)learnMoreSingleTapRecognized:(UIGestureRecognizer *)gestureRecognizer
{
    [[UIApplication sharedApplication] openURL: [NSURL URLWithString: @"http://rohos.com/mob"] options: @{} completionHandler: nil];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (int)saveAuthRecord:( AuthRecord *) r
{
    // pointer to standart user defaults
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    
    // the mutalbe array of all debts
    // mRecords = [[defaults objectForKey:AUTHREC_LIST_KEY] mutableCopy];
    // create new record
    
    NSDictionary* newRecord = nil;
    
    if (mRecords != nil && [mRecords count] >0)
    {
        for (NSDictionary* rec in mRecords)
        {
            NSString *uName = [rec objectForKey: USER_NAME_KEY];
            NSString *hostName = [rec objectForKey: HOST_NAME_KEY];
            if ( [uName isEqualToString: r.userName ] && [hostName isEqualToString: r.hostName ] )
            {
                // the same record...
                // todo - update secrt key and data...
                return 0;
            }
        }
    }

    
    if (newRecord == nil)
    {
    // to save CGFloat you need to wrap it into NSNumber
        NSNumber * port = [NSNumber numberWithInt: r.hostPort];
    
        newRecord = @{
            USER_NAME_KEY: r.userName,
            HOST_NAME_KEY: r.hostName,
            SECRET_NAME_KEY: r.secretKey,
            DATA_NAME_KEY: r.data,
            PORT_NAME_KEY: port,
            IP_NAME_KEY: r.hostIP
        };
        
        if (mRecords == nil)
            mRecords = [[NSMutableArray alloc] init];
    }
    
    [mRecords addObject: newRecord];
    [defaults setObject: mRecords forKey: AUTHREC_LIST_KEY];
    
    // Do not forget to save changes
    [defaults synchronize];
    
    return 0;
}


- (int)refreshAuthRecordsList
{
    bigLogoView.hidden = YES;
    recordsView.hidden = NO;

    [self.tableView reloadData];

    return 0;
}


- (int)sendMQTTPacket: (AuthRecord*)r
{
    // Show loading indicator
    self.errorLabel.hidden = YES;
    self.errorLabel.text = @"";
    
    // Configure location
    MQTTCFSocketTransport *transport = [[MQTTCFSocketTransport alloc] init];
    transport.host = @"node02.myqtthub.com";
    transport.port = 1883;
    
    mQttSession = [[MQTTSession alloc] init];
    mQttSession.transport = transport;
    mQttSession.userName = @"rohos";
    mQttSession.password = @"PASSWORD SHOULD BE HERE";
    mQttSession.clientId = @"rohos.logon";
    
    [mQttSession connectWithConnectHandler:^(NSError *error) {
        if (error == nil)
        {
            // Publish string
            [self->mQttSession publishData: [r.authSignalString dataUsingEncoding: NSUTF8StringEncoding]
                                   onTopic: r.hostName
                                    retain: NO
                                       qos: MQTTQosLevelAtMostOnce
                            publishHandler: ^(NSError* error) {
                                                if (error)
                                                {
                                                    self.errorLabel.text = error.description;
                                                }
            }];
        }
        else
        {
            self.errorLabel.text = error.description;
            self.errorLabel.hidden = NO;
            
            NSLog(@"MQTT connection error: %@", error);
        }
    }];
    
    r.serverReplyStr = @"Authentication signal has been sent!";
    
    return YES;
}
 
//
// send Authentication Signal of default record
// called when app is just reopened
//
- (void)sendSignalForRecord: (int) recordIndex
{
    if (mRecords == nil || [mRecords count] < recordIndex + 1)
    {
        return;
    }
    
    NSDictionary * record = [mRecords objectAtIndex: recordIndex];
    
    AuthRecord* r = [[AuthRecord alloc] init];
    
    r.hostName = record[HOST_NAME_KEY];
    r.hostPort = [record[PORT_NAME_KEY] intValue];
    r.secretKey = record[SECRET_NAME_KEY];
    r.data = record[DATA_NAME_KEY];
    r.userName = record[USER_NAME_KEY];
    
    [self sendSignalUpdateUI: r];
}

//
// send Authentication Signal by using the record ar
//
//
- (int)sendSignalUpdateUI: (AuthRecord *) ar
{
    if ([ar isEmpty])
        return 0;
    
    NSLog(@"host: %@", ar.hostName);
    
    NSString *strEncrypted = [ar getEncryptedDataString];
    
    NSLog(@"%@", strEncrypted);
    
    // lets send MQTT over WiFi...
    
    if ([self sendMQTTPacket: ar] >0 )
    {
        [resultsView setText: ar.serverReplyStr];
    }
    else
    {
        [resultsView setText: @"Signal sent but not unlocked"];
    }
  
    if (mBluetooth.isActive)
        [mBluetooth sendData: [ar.serverReplyStr dataUsingEncoding: NSUTF8StringEncoding]];
    else
    {
        // [mBluetoothSendData release];
        mBluetoothSendData = [ar.serverReplyStr dataUsingEncoding: NSUTF8StringEncoding];
        [mBluetooth start];
        // Logic continues in onCentralManagerInit handler
    }
    return 0;
}

// delete lis button..
- (IBAction)sendSignalPressed: (id)sender
{
    [mRecords removeAllObjects];
 
    // do not forget to save changes
    NSUserDefaults * defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject: mRecords forKey: AUTHREC_LIST_KEY];
    [defaults synchronize];
    
    [self refreshAuthRecordsList];
    
    recordsView.hidden = YES;
    bigLogoView.hidden = NO;
    
    [resultsView setText: @"Install Rohos Logon Key on your desktop to enable authentication by phone. rohos.com/mob"]; //NSLocalizedString(@"Intro", @"How to start")];
}

- (IBAction)scanPressed:(id)sender
{
    /*
     // enable to allow quick QR-code recognition
    
    [self parseUriString:@"rohos1://192.168.2.106:1205/insp?USER=Jane(KEY=0be5a4cc7508b9e9edd99cedd04a7e6d15b87faf(DATA=74bf9bfe531a52da3615255185aa2e9268bca0fda5498908e49e02ccbc63aee30de1a9cbc3d0ac739b715a1a4cfb63b4db1832384718c5e3ae3abbe77b6daf18"];
     
    return;
    */
    
    BarcodeScannerViewController* scanner = [[BarcodeScannerViewController alloc] initWithNibName: @"BarcodeScanner" bundle:[NSBundle mainBundle]];
    scanner.delegate = self;
    
    [self presentViewController: scanner animated: YES completion:^{
       // Nothing here
    }];
}

/*
 parse Rohos Logon Key generated URL with authentication data
 
 URL example: rohos1://192.168.2.106:1205/insp?USER=Jane(KEY=0be5a4cc7508b9e9edd99cedd04a7e6d15b87faf(DATA=74bf9bfe531a52da3615255185aa2e9268bca0fda5498908e49e02ccbc63aee30de1a9cbc3d0ac739b715a1a4cfb63b4db1832384718c5e3ae3abbe77b6daf18
 
*/
- (void)parseUriString: (NSString*)strUri
{
    NSLog(@"URI string: %@", strUri);
    // replace all '(' with '&' - thats a trick for android.
    strUri = [strUri stringByReplacingOccurrencesOfString:@"(" withString:@"&"];
  
    // replace all ' ' with %20 - because [NSURL URLWithString] does not accept spaces
    strUri = [strUri stringByReplacingOccurrencesOfString: @" " withString: @"%20"];
    NSURL *url = [NSURL URLWithString:strUri];
    
    ar.userName = @"";
    
    NSLog(@"scheme: %@", [url scheme]);
    NSLog(@"host: %@", [url host]);
    NSLog(@"port: %@", [url port]);
    NSLog(@"path: %@", [url path]);
    NSLog(@"path components: %@", [url pathComponents]);
    NSLog(@"parameterString: %@", [url parameterString]);
    NSLog(@"query: %@", [url query]);
    NSLog(@"fragment: %@", [url fragment]);
    
    if ( ![[url scheme] isEqualToString:@"rohos1"])
    {
        return;
    }
    
    ar.hostPort = [[url port] intValue];
    ar.hostName = [NSString stringWithFormat: @"/%@", [[url path] substringFromIndex:1]];
    ar.hostIP = [NSString stringWithString: [url host]];
    
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    for (NSString *param in [[url query] componentsSeparatedByString:@"&"])
    {
        NSArray *elts = [param componentsSeparatedByString: @"="];
        if ([elts count] < 2)
          continue;
      
      NSString* value = [elts[1] stringByReplacingOccurrencesOfString: @"%20" withString: @" "];
      [params setObject: value  forKey: elts[0]];
    }
    
    ar.userName = params[@"USER"];
    ar.secretKey = params[@"KEY"];
    ar.data = params[@"DATA"];
    
    [resultsView setText: [NSString stringWithFormat:@"Auth data : %@", ar.hostName ]];
    
    [self saveAuthRecord: ar];
    [self refreshAuthRecordsList];
    [self sendSignalUpdateUI: ar];
}

// ZXing returns here
//
- (void)onScanWithResult:(NSString *)data error:(NSError *)error
{
    if (!error && data)
    {
        self.resultsToDisplay = data;
        if (self.isViewLoaded)
        {
            [resultsView setText: resultsToDisplay];
            [resultsView setNeedsDisplay];
            [self parseUriString: data];
        }
    }
    
    // Barcode scanner view controller will be dismissed after return from this method
}


/*
 TableView interface
 
 */
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [mRecords count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *simpleTableIdentifier = @"SimpleTableItem";
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier: simpleTableIdentifier];
    
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:simpleTableIdentifier];
    }
    NSDictionary * record = [mRecords objectAtIndex:indexPath.row];
    
    NSArray<NSString*>* parts = [record[HOST_NAME_KEY] componentsSeparatedByString: @"."];
    
    if (parts.count)
    {
        cell.textLabel.text = [NSString stringWithFormat: @"%@ %@", record[USER_NAME_KEY], parts[0]];
        cell.textLabel.numberOfLines = 0;
        //cell.textLabel.lineBreakMode = NSLineBreakByClipping;
    }
    else
        cell.textLabel.text = [NSString stringWithFormat: @"%@ [%@]", record[USER_NAME_KEY], record[HOST_NAME_KEY]];
    
    // Image
    cell.imageView.image = [UIImage imageNamed:@"unlockpc.png"];
    
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [self sendSignalForRecord: (int)indexPath.row];
}

- (void)onCentralManagerInit:(NSError *)error
{
  if (error)
      NSLog(@"BT init & discovery error: %@", [error localizedDescription]);
  else
  if (mBluetoothSendData)
      [mBluetooth sendData: mBluetoothSendData];
}

- (void)onDataSent:(NSError *)error
{
  // Silence all errors for now. But dump to console.
  if (error)
      NSLog(@"BT send error: %@", [error localizedDescription]);
}

@end

