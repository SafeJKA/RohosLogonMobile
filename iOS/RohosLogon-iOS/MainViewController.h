//
//  MainViewController.h
//  CodeScanTest
//
//  Created by Steven on 13-3-13.
//  Copyright (c) 2013年 Steven. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <ZXingWidgetController.h>

@interface MainViewController : UIViewController
                                <ZXingDelegate, UITableViewDelegate, UITableViewDataSource>
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

@end
