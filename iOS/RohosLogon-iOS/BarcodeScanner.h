#import <UIKit/UIKit.h>

@protocol BarcodeScannerDelegate <NSObject>

- (void)onScanWithResult:(NSString*)data error:(NSError*)error;

@end

@interface BarcodeScannerViewController : UIViewController

@property (weak) id<BarcodeScannerDelegate> delegate;

@end
