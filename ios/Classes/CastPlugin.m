#import "CastPlugin.h"
#import <cast/cast-Swift.h>

@implementation CastPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftCastPlugin registerWithRegistrar:registrar];
}
@end
