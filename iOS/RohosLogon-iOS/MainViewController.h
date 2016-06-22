//
//  MainViewController.h
//
//
//  Created by Alexandr Silonosov on 13-3-13.
//  Copyright (c) 2014 Tesline-Service SRL. All rights reserved.
//  Rohos Logon Key client for iOS
//  rohos.com
//

#import <UIKit/UIKit.h>
#import <ZXingWidgetController.h>
#import "BluetoothHelper.h"

@interface MainViewController : UIViewController
                                <ZXingDelegate, UITableViewDelegate, UITableViewDataSource, BluetoothCentralDelegate>
{
    UITextView *resultsView;
    UIView *recordsView;
    NSString *resultsToDisplay;
    UITableView *tableView;
    UIImageView *bigLogoView;
    
    NSMutableSet *qrReader;
    
    // array of dictionaries username=XX hostname=YY
    NSMutableArray * mRecords;
    
}

@property (retain, nonatomic) IBOutlet UITextView *resultsView;
@property (retain, nonatomic) IBOutlet UIView *recordsView;
@property (retain, nonatomic) IBOutlet UITableView *tableView;
@property (retain, nonatomic) IBOutlet UIImageView *bigLogoView;

@property (nonatomic, copy) NSString *resultsToDisplay;

@property(retain, nonatomic) NSMutableSet *qrReader;

- (IBAction)scanPressed:(id)sender;
- (IBAction)sendSignalPressed:(id)sender;
- (void)sendSignalForRecord: (int) recordIndex;
@end
