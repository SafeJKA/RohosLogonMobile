#import <AVFoundation/AVFoundation.h>
#import "BarcodeScanner.h"

@interface BarcodeScannerViewController () <AVCaptureMetadataOutputObjectsDelegate>
{
    AVCaptureSession *_session;
    AVCaptureDevice *_device;
    AVCaptureDeviceInput *_input;
    AVCaptureMetadataOutput *_output;
    AVCaptureVideoPreviewLayer *_prevLayer;
    
    IBOutlet UIView* mPreview;
}
@end

@implementation BarcodeScannerViewController

@synthesize delegate;

- (void)viewDidLoad
{
    [super viewDidLoad];

    _session = [[AVCaptureSession alloc] init];
    _device = [AVCaptureDevice defaultDeviceWithMediaType: AVMediaTypeVideo];
    NSError *error = nil;

    _input = [AVCaptureDeviceInput deviceInputWithDevice: _device error: &error];
    if (_input) {
        [_session addInput: _input];
    } else {
        NSLog(@"Error: %@", error);
    }

    _output = [[AVCaptureMetadataOutput alloc] init];
    [_output setMetadataObjectsDelegate: self queue: dispatch_get_main_queue()];
    [_session addOutput: _output];

    _output.metadataObjectTypes = [_output availableMetadataObjectTypes];

    _prevLayer = [AVCaptureVideoPreviewLayer layerWithSession: _session];
    _prevLayer.frame = mPreview.bounds;
    _prevLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    [mPreview.layer addSublayer: _prevLayer];

    [_session startRunning];

}

- (void)captureOutput:(AVCaptureOutput *)captureOutput didOutputMetadataObjects:(NSArray *)metadataObjects fromConnection:(AVCaptureConnection *)connection
{
    CGRect highlightViewRect = CGRectZero;
    AVMetadataMachineReadableCodeObject *barCodeObject;
    NSString *detectionString = nil;
    NSArray *barCodeTypes = @[AVMetadataObjectTypeUPCECode, AVMetadataObjectTypeCode39Code, AVMetadataObjectTypeCode39Mod43Code,
            AVMetadataObjectTypeEAN13Code, AVMetadataObjectTypeEAN8Code, AVMetadataObjectTypeCode93Code, AVMetadataObjectTypeCode128Code,
            AVMetadataObjectTypePDF417Code, AVMetadataObjectTypeQRCode, AVMetadataObjectTypeAztecCode];

    for (AVMetadataObject *metadata in metadataObjects) {
        for (NSString *type in barCodeTypes) {
            if ([metadata.type isEqualToString:type])
            {
                barCodeObject = (AVMetadataMachineReadableCodeObject *)[_prevLayer transformedMetadataObjectForMetadataObject:(AVMetadataMachineReadableCodeObject *)metadata];
                highlightViewRect = barCodeObject.bounds;
                detectionString = [(AVMetadataMachineReadableCodeObject *)metadata stringValue];
                break;
            }
        }

        if (detectionString != nil)
        {
            //_label.text = detectionString;
            // Found !
            if (self.delegate)
                [self.delegate onScanWithResult: detectionString error: nil];
            
            [self dismissViewControllerAnimated: YES completion:^{
                ;
            }];
            
            break;
        }

    }

}

- (IBAction)onCancel:(id)sender
{
    if (self.delegate)
        [self.delegate onScanWithResult: nil error: nil];
    [self dismissViewControllerAnimated: YES
                             completion:^{
        
    }];
}
@end
